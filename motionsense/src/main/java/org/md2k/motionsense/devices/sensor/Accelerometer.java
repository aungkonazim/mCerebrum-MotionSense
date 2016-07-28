package org.md2k.motionsense.devices.sensor;

import android.content.Context;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by monowar on 4/21/16.
 */
public class Accelerometer extends Sensor {
    public Accelerometer(Context context) {
        super(context, DataSourceType.ACCELEROMETER);
    }

    @Override
    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=super.createDataSourceBuilder(platform);
        dataSourceBuilder=dataSourceBuilder.setMetadata(METADATA.NAME, "Accelerometer")
                .setDataDescriptors(createDataDescriptors())
                .setMetadata(METADATA.MIN_VALUE, "-100")
                .setMetadata(METADATA.MAX_VALUE, "100")
                .setMetadata(METADATA.DATA_TYPE, DataTypeIntArray.class.getSimpleName())
                .setMetadata(METADATA.DESCRIPTION, "Accelerometer Measurement");
        return dataSourceBuilder;
    }
    ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        dataDescriptors.add(createDataDescriptor("Accelerometer X",-100, 100, "meter/sec^2"));
        dataDescriptors.add(createDataDescriptor("Accelerometer Y",-100, 100, "meter/sec^2"));
        dataDescriptors.add(createDataDescriptor("Accelerometer Z",-100, 100, "meter/sec^2"));
        return dataDescriptors;
    }
    public void insert(DataTypeIntArray dataTypeIntArray){
        try {
            DataKitAPI.getInstance(context).insert(dataSourceClient, dataTypeIntArray);
        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }
}
