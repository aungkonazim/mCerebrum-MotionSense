package org.md2k.motionsense.device.sensor;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.motionsense.MyApplication;
import org.md2k.motionsense.ServiceMotionSense;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
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
abstract public class Sensor {
    private DataSource dataSource;
    DataSourceClient dataSourceClient;
    public static final String KEY_ACCELEROMETER=DataSourceType.ACCELEROMETER;
    public static final String KEY_GYROSCOPE=DataSourceType.GYROSCOPE;
    public static final String KEY_BATTERY=DataSourceType.BATTERY;
    public static final String KEY_LED=DataSourceType.LED;
    public static final String KEY_RAW=DataSourceType.RAW;
    public static final String KEY_SEQUENCE_NUMBER=DataSourceType.SEQUENCE_NUMBER;
    public static final String KEY_DATA_QUALITY_ACCELEROMETER=DataSourceType.DATA_QUALITY+DataSourceType.ACCELEROMETER;




    public Sensor(DataSource dataSource) {
        this.dataSource=dataSource;
    }
    public boolean equals(DataSource dataSource){
        if(getId()==null && dataSource.getId()!=null) return false;
        if(getId()!=null && dataSource.getId()==null) return false;
        if(getId()!=null && dataSource.getId()!=null && !getId().equals(dataSource.getId())) return false;

        if(getType()==null && dataSource.getType()!=null) return false;
        if(getType()!=null && dataSource.getType()==null) return false;
        if(getType()!=null && dataSource.getType()!=null && !getType().equals(dataSource.getType())) return false;
        return true;
    }
    public String getId(){
        return dataSource.getId();
    }
    public String getType(){
        return dataSource.getType();
    }

    public boolean register() throws DataKitException {
        dataSourceClient = DataKitAPI.getInstance(MyApplication.getContext()).register(new DataSourceBuilder(dataSource));
        return dataSourceClient != null;
    }

    public void unregister() throws DataKitException {
        if (dataSourceClient != null)
            DataKitAPI.getInstance(MyApplication.getContext()).unregister(dataSourceClient);
    }
    public void insert(DataTypeDoubleArray dataTypeDoubleArray){
        try {
            DataKitAPI.getInstance(MyApplication.getContext()).insertHighFrequency(dataSourceClient, dataTypeDoubleArray);
        } catch (DataKitException e) {
            LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));
        }
    }
    public static Sensor create(DataSource dataSource){
        switch(getKey(dataSource)){
            case KEY_ACCELEROMETER: return new Accelerometer(dataSource);
            case KEY_GYROSCOPE: return new Gyroscope(dataSource);
            case KEY_BATTERY: return new Battery(dataSource);
            case KEY_LED: return new LED(dataSource);
            case KEY_RAW: return new Raw(dataSource);
            case KEY_SEQUENCE_NUMBER: return new SequenceNumber(dataSource);
            case KEY_DATA_QUALITY_ACCELEROMETER: return new DataQualityAccelerometer(dataSource);
            default:
                return null;
        }
    }

    public static String getKey(DataSource dataSource) {
        switch (dataSource.getType()) {
            case DataSourceType.ACCELEROMETER:
                return KEY_ACCELEROMETER;
            case DataSourceType.GYROSCOPE:
                return KEY_GYROSCOPE;
            case DataSourceType.BATTERY:
                return KEY_BATTERY;
            case DataSourceType.LED:
                return KEY_LED;
            case DataSourceType.RAW:
                return KEY_RAW;
            case DataSourceType.SEQUENCE_NUMBER:
                return KEY_SEQUENCE_NUMBER;
            case DataSourceType.DATA_QUALITY:
                if (dataSource.getId() != null && dataSource.getId().equals(DataSourceType.ACCELEROMETER))
                    return KEY_DATA_QUALITY_ACCELEROMETER;
                else if(dataSource.getId()==null) return KEY_DATA_QUALITY_ACCELEROMETER;

            default:
                return null;
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
