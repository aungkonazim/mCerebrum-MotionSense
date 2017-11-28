package org.md2k.motionsense.device;

import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.motionsense.device.sensor.DataQualityAccelerometer;
import org.md2k.motionsense.device.sensor.DataQualityLed;
import org.md2k.motionsense.device.sensor.Sensor;

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
class DeviceMotionSenseHRV extends Device{

    DeviceMotionSenseHRV(Platform platform) {
        super(platform);
    }
    void insertData(long timestamp, long gyroOffset, Data blData) {
        DataTypeDoubleArray acl, gyr, led, raw, seq;
        double[] aclSample=blData.getAccelerometer();
        acl=new DataTypeDoubleArray(timestamp, aclSample);
        if(sensors.get(Sensor.KEY_DATA_QUALITY_ACCELEROMETER)!=null)
            ((DataQualityAccelerometer)sensors.get(Sensor.KEY_DATA_QUALITY_ACCELEROMETER)).add(aclSample[0]);

        if(sensors.get(Sensor.KEY_ACCELEROMETER)!=null) {
            sensors.get(Sensor.KEY_ACCELEROMETER).insert(acl);
            updateView(Sensor.KEY_ACCELEROMETER, acl);
        }

        if(sensors.get(Sensor.KEY_GYROSCOPE)!=null){
            gyr=new DataTypeDoubleArray(timestamp, blData.getGyroscope());
            sensors.get(Sensor.KEY_GYROSCOPE).insert(gyr);
            updateView(Sensor.KEY_GYROSCOPE, gyr);
        }
        double[] ledSample=blData.getLED();
        if(sensors.get(Sensor.KEY_DATA_QUALITY_LED)!=null)
            ((DataQualityLed)sensors.get(Sensor.KEY_DATA_QUALITY_LED)).add(ledSample);
        if(sensors.get(Sensor.KEY_LED)!=null){
            led=new DataTypeDoubleArray(timestamp, ledSample);
            sensors.get(Sensor.KEY_LED).insert(led);
            updateView(Sensor.KEY_LED, led);
        }
        if(sensors.get(Sensor.KEY_RAW)!=null){
            raw=new DataTypeDoubleArray(timestamp, blData.getRawData());
            sensors.get(Sensor.KEY_RAW).insert(raw);
            updateView(Sensor.KEY_RAW, raw);
        }
        if(sensors.get(Sensor.KEY_SEQUENCE_NUMBER)!=null){
            seq=new DataTypeDoubleArray(timestamp, blData.getSequenceNumber());
            sensors.get(Sensor.KEY_SEQUENCE_NUMBER).insert(seq);
            updateView(Sensor.KEY_SEQUENCE_NUMBER, seq);
        }
   }
}
