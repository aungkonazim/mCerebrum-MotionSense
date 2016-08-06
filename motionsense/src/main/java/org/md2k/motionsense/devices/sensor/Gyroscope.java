package org.md2k.motionsense.devices.sensor;

import android.content.Context;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
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
public class Gyroscope extends Sensor {
    public Gyroscope(Context context) {
        super(context, DataSourceType.GYROSCOPE);
    }

    @Override
    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=super.createDataSourceBuilder(platform);
        dataSourceBuilder=dataSourceBuilder.setMetadata(METADATA.NAME, "Gyroscope")
                .setDataDescriptors(createDataDescriptors())
                .setMetadata(METADATA.MIN_VALUE, "-250")
                .setMetadata(METADATA.MAX_VALUE, "250")
                .setMetadata(METADATA.DATA_TYPE, DataTypeIntArray.class.getSimpleName())
                .setMetadata(METADATA.DESCRIPTION, "Gyroscope Measurement");
        return dataSourceBuilder;
    }
    ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        dataDescriptors.add(createDataDescriptor("Gyroscope X",-250, 250, "degree/second"));
        dataDescriptors.add(createDataDescriptor("Gyroscope Y",-250, 250, "degree/second"));
        dataDescriptors.add(createDataDescriptor("Gyroscope Z",-250, 250, "degree/second"));
        return dataDescriptors;
    }

    public void insert(DataTypeDoubleArray dataTypeDoubleArray){
        try {
            DataKitAPI.getInstance(context).insertHighFrequency(dataSourceClient, dataTypeDoubleArray);
        } catch (DataKitException e) {
            e.printStackTrace();
        }
    }

}
