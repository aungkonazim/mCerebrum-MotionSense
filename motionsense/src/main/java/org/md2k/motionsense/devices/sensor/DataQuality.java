package org.md2k.motionsense.devices.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.data_format.DATA_QUALITY;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nsleheen on 8/29/2016.
 */
public class DataQuality {
    public final static double MINIMUM_EXPECTED_SAMPLES = 3 * (0.33) * 10.33;  //33% of a 3 second window with 10.33 sampling frequency
    public final static float MAGNITUDE_VARIANCE_THRESHOLD = (float) 0.0025;   //this threshold comes from the data we collect by placing the wrist sensor on table. It compares with the wrist accelerometer on-body from participant #11 (smoking pilot study)

    private static final String TAG = DataQuality.class.getSimpleName();
    ACLQualityCalculation aclQualityCalculation = new ACLQualityCalculation();;

    Context context;
    ArrayList<Double> samples;
    DataSourceClient dataSourceClient;
    DataSourceBuilder dataSourceBuilder;

    public DataQuality(Context context) {
        this.context = context;
        samples = new ArrayList<>();
        aclQualityCalculation = new ACLQualityCalculation();
    }

    public DataSourceBuilder createDatSourceBuilder(Platform platform) {
        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        dataSourceBuilder = dataSourceBuilder.setId(DataSourceType.ACCELEROMETER).setType(DataSourceType.DATA_QUALITY).setPlatform(platform);
        dataSourceBuilder = dataSourceBuilder.setDataDescriptors(createDataDescriptors());
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.FREQUENCY, String.valueOf(String.valueOf(16.0)) + " Hz");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.NAME, "DataQuality-ACL");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.UNIT, "");
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DESCRIPTION, "measures the Data Quality of MotionSense. Values= "+ DATA_QUALITY.METADATA_STR);
        dataSourceBuilder = dataSourceBuilder.setMetadata(METADATA.DATA_TYPE, DataTypeInt.class.getName());
        return dataSourceBuilder;
    }


    public ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, "DataQuality");
        dataDescriptor.put(METADATA.MIN_VALUE, String.valueOf(0));
        dataDescriptor.put(METADATA.MAX_VALUE, String.valueOf(4));
        dataDescriptor.put(METADATA.FREQUENCY, String.valueOf(String.valueOf(16.0)) + " Hz");
        dataDescriptor.put(METADATA.DESCRIPTION, "measures the Data Quality of Accelerometer. Values= GOOD(0), BAND_OFF(1), NOT_WORN(2), BAND_LOOSE(3), NOISE(4)");
        dataDescriptor.put(METADATA.DATA_TYPE, int.class.getName());
        dataDescriptors.add(dataDescriptor);
        return dataDescriptors;
    }

    public synchronized int getStatus() {
        try {
            int status;
            int size = samples.size();
            double samps[] = new double[size];
            for (int i = 0; i < size; i++)
                samps[i] = samples.get(i);
            samples.clear();
            status = currentQuality(samps);
            Log.d(TAG, "MOTION_SENSE_ACL: " + status);
            return status;
        }catch (Exception e){
            Log.d(TAG, "MOTION_SENSE_ACL: exception");
            return DATA_QUALITY.GOOD;
        }
    }
    public synchronized void add(double sample) {

        samples.add(sample);
    }

    public void insertToDataKit(int sample) throws DataKitException {
        DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sample);
        DataKitAPI.getInstance(context).insert(dataSourceClient, dataTypeInt);

    }

    public boolean register(Platform platform) throws DataKitException {
        dataSourceBuilder = createDatSourceBuilder(platform);
        dataSourceClient = DataKitAPI.getInstance(context).register(dataSourceBuilder);
        aclQualityCalculation = new ACLQualityCalculation();
        return dataSourceClient != null;
    }


    public void unregister() throws DataKitException {
        if (dataSourceClient != null)
            DataKitAPI.getInstance(context).unregister(dataSourceClient);
        dataSourceClient = null;

    }

    public double getMean(double[] data) {
        double sum = 0.0;
        for (double a : data)
            sum += a;
        return sum / data.length;
    }

    public double getVariance(double[] data) {
        double mean = getMean(data);
        double temp = 0;
        for (double a : data)
            temp += (mean - a) * (mean - a);
        return temp / data.length;
    }

    public double getStdDev(double[] data) {
        return Math.sqrt(getVariance(data));
    }

    public int currentQuality(double[] x) {       //just receive x axis, in fact it should work with any single axis.
        int len_x = x.length;
        if (len_x == 0) return DATA_QUALITY.BAND_OFF;

//		if(len_x<MINIMUM_EXPECTED_SAMPLES)
//			return DATA_QUALITY.BAND_OFF;

        double sd =getStdDev(x);
        Log.d(TAG, "MOTION_SENSE stdDev" + sd);

        if (sd < MAGNITUDE_VARIANCE_THRESHOLD)
            return DATA_QUALITY.NOT_WORN;

        return DATA_QUALITY.GOOD;
    }
}