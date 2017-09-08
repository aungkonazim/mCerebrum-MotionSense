package org.md2k.motionsense.device;

import android.content.Context;
import android.widget.Toast;

import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.motionsense.Constants;
import org.md2k.motionsense.configuration.Configuration;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
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
public class DeviceManager {
    private Devices devicesConfigured;
    private Devices devicesDefault;
    private MetaData metaData;

    public DeviceManager() {
        devicesConfigured=new Devices(Configuration.CONFIG_DIRECTORY, Configuration.CONFIG_FILENAME);
        devicesDefault=new Devices(Configuration.CONFIG_DIRECTORY, Configuration.DEFAULT_CONFIG_FILENAME);
        metaData=new MetaData();
    }
    public void add(String type, String id, String deviceId){
        ArrayList<DataSource> dataSources;
        if(hasDefault() && devicesDefault.getDataSources(type, id).size()!=0){
            dataSources=devicesDefault.getDataSources(type,id);
        }else{
            dataSources=metaData.getDataSources(type);
        }
        for(int i=0;i<dataSources.size();i++) {
            DataSource temp=metaData.getDataSource(dataSources.get(i).getType(), dataSources.get(i).getId(), dataSources.get(i).getPlatform().getType());
            Platform platform=new PlatformBuilder(temp.getPlatform()).setType(type).setId(id).setMetadata(METADATA.DEVICE_ID, deviceId).build();
            DataSource dataSource=new DataSourceBuilder(temp).setPlatform(platform).build();
            devicesConfigured.add(dataSource);
        }
    }
    public boolean isValid(){
        if(devicesConfigured.size()!=devicesDefault.size()) return false;
        for(int i=0;i<devicesConfigured.size();i++){
            boolean flag=false;
            for(int j=0;j<devicesDefault.size();j++){
                if(devicesConfigured.find(devicesDefault.get(j).getType(), devicesDefault.get(j).getId())==null)
                    continue;
                flag=true;
            }
            if(!flag) return false;
        }
        return true;
    }
    public boolean hasDefault(){
        return devicesDefault.size() != 0;
    }
    public int size(){
        return devicesConfigured.size();
    }
    public void writeConfiguration(Context context){
        try {
            devicesConfigured.writeConfiguration(Configuration.CONFIG_DIRECTORY, Configuration.CONFIG_FILENAME);
        } catch (IOException e) {
            Toast.makeText(context, "Error: Could not Save..."+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public Device find(String deviceId){
        return devicesConfigured.find(deviceId);
    }
    public Device get(int i) {
        return devicesConfigured.get(i);
    }

    public void start() throws DataKitException {
        devicesConfigured.start();
    }
    public void stop() throws DataKitException {
        devicesConfigured.stop();
    }
    public void delete(String deviceId){
        devicesConfigured.delete(deviceId);
    }
    public boolean isMotionSense(String name, String serviceID) {
        return !(name == null || serviceID == null) && name.equals(Constants.MOTION_SENSE) && serviceID.equals(Constants.DEVICE_UUID.toString());
    }
    public boolean isMotionSenseHRV(String name, String serviceID) {
        return !(name == null || serviceID == null) && name.equals(Constants.MOTION_SENSE_HRV) && serviceID.equals(Constants.DEVICE_UUID.toString());
    }

    public boolean isConfigured(String deviceId) {
        return devicesConfigured.find(deviceId) != null;
    }
    public boolean isConfigured(String id, String deviceId) {
        return isConfigured(deviceId) || devicesConfigured.findId(id) != null;
    }

    public Devices get() {
        return devicesConfigured;
    }
}
