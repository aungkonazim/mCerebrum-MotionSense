package org.md2k.motionsense.devices.sensor;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.motionsense.ServiceMotionSense;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by monowar on 4/21/16.
 */
public class Battery extends Sensor {
    public Battery(Context context) {
        super(context, DataSourceType.BATTERY);
    }

    @Override
    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=super.createDataSourceBuilder(platform);
        dataSourceBuilder=dataSourceBuilder.setMetadata(METADATA.NAME, "Battery")
                .setDataDescriptors(createDataDescriptors())
                .setMetadata(METADATA.MIN_VALUE, "0")
                .setMetadata(METADATA.MAX_VALUE, "100")
                .setMetadata(METADATA.DATA_TYPE, DataTypeInt.class.getSimpleName())
                .setMetadata(METADATA.DESCRIPTION, "Current battery level as a percentage from 0%% to 100%%");
        return dataSourceBuilder;
    }
    ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        HashMap<String, String> dataDescriptor = new HashMap<>();
        dataDescriptor.put(METADATA.NAME, "Batter Level");
        dataDescriptor.put(METADATA.MIN_VALUE, "0");
        dataDescriptor.put(METADATA.MAX_VALUE, "100");
        dataDescriptor.put(METADATA.DESCRIPTION, "Current battery level as a percentage from 0%% to 100%%");
        dataDescriptor.put(METADATA.DATA_TYPE, int.class.getName());
        dataDescriptors.add(dataDescriptor);
        return dataDescriptors;
    }
    public void insert(DataTypeInt dataTypeInt){
        try {
            DataKitAPI.getInstance(context).insert(dataSourceClient, dataTypeInt);
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));
        }
    }
}
