package org.md2k.motionsense.device;

import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.motionsense.Constants;
import org.md2k.motionsense.configuration.Configuration;
import org.md2k.motionsense.device.Device;
import org.md2k.motionsense.device.sensor.Sensor;

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
class Devices {
    private ArrayList<Device> devices;

    Devices(String directory, String fileName) {
        devices=new ArrayList<>();
        ArrayList<DataSource> dataSources = Configuration.read(directory, fileName);
        if(dataSources==null) return;
        for(int i=0;i<dataSources.size();i++){
            add(dataSources.get(i));
        }
    }
    void writeConfiguration(String directory, String fileName) throws IOException {
        ArrayList<DataSource> dataSources=new ArrayList<>();
        for(int i = 0; i< devices.size(); i++){
            dataSources.addAll(devices.get(i).getDataSources());
        }
        Configuration.write(directory, fileName, dataSources);
    }
    void add(DataSource dataSource){

        Device device = find(dataSource.getPlatform());
        if(device==null) {
            device = new Device(dataSource.getPlatform());
            devices.add(device);
        }
        device.add(dataSource);
    }

    private Device find(Platform platform){
        for(int i = 0; i< devices.size(); i++){
            if(devices.get(i).equals(platform)) return devices.get(i);
        }
        return null;
    }
    Device find(String deviceId){
        for(int i = 0; i< devices.size(); i++){
            if(devices.get(i).getDeviceId().equals(deviceId)) return devices.get(i);
        }
        return null;
    }

    Device get(int i) {
        return devices.get(i);
    }

    void start() throws DataKitException {
        for (int i = 0; i < devices.size(); i++) {
            devices.get(i).start();
        }
    }
    void stop() throws DataKitException {
        for (int i = 0; i < devices.size(); i++) {
            devices.get(i).stop();
        }
    }
    void delete(String deviceId){
        for(int i = 0; i< devices.size(); i++)
            if(devices.get(i).getDeviceId().equals(deviceId)) {
                devices.remove(i);
                return;
            }
    }

    int size() {
        return devices.size();
    }

    Device findId(String id) {
        for(int i = 0; i< devices.size(); i++){
            if(devices.get(i).getId().equals(id)) return devices.get(i);
        }
        return null;
    }
    Device find(String type, String id){
        for(int i = 0; i< devices.size(); i++){
            if(type!=null && devices.get(i).getType()!=null && !devices.get(i).getType().equals(type)) continue;
            if(id!=null && devices.get(i).getId()!=null && !devices.get(i).getId().equals(id)) continue;
            return devices.get(i);
        }
        return null;
    }

    ArrayList<DataSource> getDataSources(String type, String id) {
        ArrayList<DataSource> dataSources=new ArrayList<>();
        Device device=find(type,id);
        if(device!=null) {
            for(Sensor sensor: device.getSensors().values())
                dataSources.add(sensor.getDataSource());
/*
            for (int i = 0; i < device.getSensors().size(); i++) {
                dataSources.add(device.getSensors().get(i).getDataSource());
            }
*/
        }
        return dataSources;
    }
}
