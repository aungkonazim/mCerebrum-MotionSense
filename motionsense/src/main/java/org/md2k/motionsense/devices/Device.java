package org.md2k.motionsense.devices;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakitapi.Constants;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.motionsense.ServiceMotionSense;
import org.md2k.motionsense.devices.sensor.Accelerometer;
import org.md2k.motionsense.devices.sensor.Battery;
import org.md2k.motionsense.devices.sensor.DataQuality;
import org.md2k.motionsense.devices.sensor.Gyroscope;
import org.md2k.motionsense.devices.sensor.LED;
import org.md2k.motionsense.devices.sensor.Sensor;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.motionsense.devices.sensor.SequenceNumber;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.data_format.DATA_QUALITY;

import java.util.ArrayList;

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
public class Device {
    public static final int DELAY = 3000;
    public static final int RESTART_NO_DATA = 9000;

    private static final String TAG = Device.class.getSimpleName();
    protected String platformType;
    public SequenceNumber sequenceNumber;
    protected String platformId;
    protected String deviceId;
    protected Context context;
    protected String name;
    protected ArrayList<Sensor> sensors;

    public DataQuality dataQuality;
    int noData = 0;
    Handler handler;

    public Device(Context context, String platformType, String platformId, String deviceId, String name) {
        this.context = context;
        this.platformType = platformType;
        this.platformId=platformId;
        this.deviceId = deviceId;
        this.name=name;
        sensors =new ArrayList<>();
        sensors.add(new Accelerometer(context));
        sensors.add(new Gyroscope(context));
        sensors.add(new Battery(context));
        if(platformType.equals(PlatformType.MOTION_SENSE_HRV))
            sensors.add(new LED(context));

        dataQuality = new DataQuality(context);
        sequenceNumber = new SequenceNumber(context);
        handler = new Handler();
        Log.d(TAG, "dataQualities=" + this + " platformId=" + platformId + " platformType=" + platformType + " deviceId=" + deviceId);
    }


    private Runnable runnableDataQuality = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"runnableDataQuality...deviceId="+deviceId);
            int status=-1;
            try {
                status  = dataQuality.getStatus();
                Log.d(TAG,"runnableDataQuality...abc status="+status);

                dataQuality.insertToDataKit(status);
                Log.d(TAG,"runnableDataQuality...status="+status);
            } catch (DataKitException e) {
                Log.d(TAG,"runnableDataQuality...ERROR=");

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));
                return;
            }
            if (status == DATA_QUALITY.BAND_OFF)
                noData += DELAY;
            else noData = 0;
            if (noData >= RESTART_NO_DATA) {
                noData = 0;
                Intent intent = new Intent(ServiceMotionSense.INTENT_RESTART);
                intent.putExtra("device_id",deviceId);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            handler.postDelayed(this, DELAY);
        }
    };

    public void register() throws DataKitException {
        Platform platform = createPlatform();
        for (int i = 0; i < sensors.size(); i++) {
            sensors.get(i).register(platform);
        }
        dataQuality.register(platform);
        sequenceNumber.register(platform);

        handler.removeCallbacks(runnableDataQuality);
        handler.post(runnableDataQuality);
    }

    public void unregister() throws DataKitException {
        for (int i = 0; i < sensors.size(); i++) {
            sensors.get(i).unregister();
        }
        handler.removeCallbacks(runnableDataQuality);
        dataQuality.unregister();
        sequenceNumber.unregister();
    }

    public Platform createPlatform(){
        return new PlatformBuilder().setType(platformType)
                .setId(platformId)
                .setMetadata(METADATA.DEVICE_ID, deviceId).setMetadata(METADATA.NAME, name).build();
    }

    public String getPlatformType() {
        return platformType;
    }

    public String getPlatformId() {
        return platformId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Sensor> getSensors() {
        return sensors;
    }

    public Sensor getSensor(String type) {
        for(int i=0;i<sensors.size();i++)
            if(sensors.get(i).getDataSourceType().equals(type))
                return sensors.get(i);
        return null;
    }
}
