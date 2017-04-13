package org.md2k.motionsense.devices.sensor;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeIntArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.motionsense.ServiceMotionSense;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Accelerometer extends Sensor {
    public Accelerometer(Context context, String frequency) {
        super(context, DataSourceType.ACCELEROMETER, frequency);
    }

    @Override
    public DataSourceBuilder createDataSourceBuilder(Platform platform){
        DataSourceBuilder dataSourceBuilder=super.createDataSourceBuilder(platform);
        dataSourceBuilder=dataSourceBuilder.setMetadata(METADATA.NAME, "Accelerometer")
                .setDataDescriptors(createDataDescriptors())
                .setMetadata(METADATA.MIN_VALUE, "-2")
                .setMetadata(METADATA.MAX_VALUE, "2")
                .setMetadata(METADATA.DATA_TYPE, DataTypeIntArray.class.getSimpleName())
                .setMetadata(METADATA.FREQUENCY,frequency)
                .setMetadata(METADATA.DESCRIPTION, "Accelerometer Measurement");
        return dataSourceBuilder;
    }
    private ArrayList<HashMap<String, String>> createDataDescriptors() {
        ArrayList<HashMap<String, String>> dataDescriptors = new ArrayList<>();
        dataDescriptors.add(createDataDescriptor("Accelerometer X",-2, 2, "g"));
        dataDescriptors.add(createDataDescriptor("Accelerometer Y",-2, 2, "g"));
        dataDescriptors.add(createDataDescriptor("Accelerometer Z",-2, 2, "g"));
        return dataDescriptors;
    }

    public void insert(DataTypeDoubleArray dataTypeDoubleArray){
        try {
            DataKitAPI.getInstance(context).insertHighFrequency(dataSourceClient, dataTypeDoubleArray);
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));
        }
    }
}
