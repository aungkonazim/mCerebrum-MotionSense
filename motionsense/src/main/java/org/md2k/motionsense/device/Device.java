package org.md2k.motionsense.device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.motionsense.ActivityMain;
import org.md2k.motionsense.Constants;
import org.md2k.motionsense.MyApplication;
import org.md2k.motionsense.device.sensor.DataQualityAccelerometer;
import org.md2k.motionsense.device.sensor.DataQualityLed;
import org.md2k.motionsense.device.sensor.Sensor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

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
public abstract class Device extends AbstractTranslate {
    private Platform platform;
    HashMap<String, Sensor> sensors;
    private Subscription subscriptionDevice;
    private Subscription subscriptionDataQuality;
    private Subscription subscriptionDeviceContinuous;
    private HashMap<String, Integer> hm = new HashMap<>();
    private long startTimestamp = 0;
    private long lastReceived=0;
    private static final long TIMEOUT_VALUE = 30000;
    private static final int DELAY = 3000;

    Device(Platform platform) {
        super();
        this.platform = platform;
        sensors = new HashMap<>();
    }

    public void add(DataSource dataSource) {
        if (sensors.get(Sensor.getKey(dataSource)) != null) return;
        Sensor sensor = Sensor.create(dataSource);
        if (sensor != null)
            sensors.put(Sensor.getKey(dataSource), sensor);
    }

    boolean equals(Platform platform) {

        if (getId() == null && platform.getId() != null) return false;
        if (getId() != null && platform.getId() == null) return false;
        if (getId() != null && platform.getId() != null && !getId().equals(platform.getId()))
            return false;

        if (getType() == null && platform.getType() != null) return false;
        if (getType() != null && platform.getType() == null) return false;
        if (getType() != null && platform.getType() != null && !getType().equals(platform.getType()))
            return false;

        String curDeviceId=getDeviceId();
        String pDeviceId=null;
        if(platform.getMetadata()!=null && platform.getMetadata().get(METADATA.DEVICE_ID)!=null)
            pDeviceId=platform.getMetadata().get(METADATA.DEVICE_ID);

        if(curDeviceId==null && pDeviceId==null) return true;
        if(curDeviceId!=null && pDeviceId==null) return false;
        if(curDeviceId==null && pDeviceId!=null) return false;
        if(curDeviceId.equals(pDeviceId)) return true;
        return false;
    }
    private void calculateDataQualityAccelerometer(){
        DataQualityAccelerometer sensor = (DataQualityAccelerometer) sensors.get(Sensor.KEY_DATA_QUALITY_ACCELEROMETER);
        if (sensor != null) {
            DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sensor.getStatus());
            sensor.insert(dataTypeInt);
            updateView(Sensor.KEY_DATA_QUALITY_ACCELEROMETER, dataTypeInt);
        }
    }

    private void calculateDataQualityLed(){
        DataQualityLed sensor = (DataQualityLed) sensors.get(Sensor.KEY_DATA_QUALITY_LED);
        if (sensor != null) {
            DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sensor.getStatus());
            Log.d("data_quality_led","final result="+dataTypeInt.getSample());
            sensor.insert(dataTypeInt);
            updateView(Sensor.KEY_DATA_QUALITY_LED, dataTypeInt);
        }
    }

    void start() throws DataKitException {
        for (Sensor sensor : sensors.values())
            sensor.register();

        subscriptionDataQuality = Observable.interval(DELAY, DELAY, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        calculateDataQualityAccelerometer();
                        calculateDataQualityLed();
                    }
                });
        subscriptionDeviceContinuous=Observable.interval(0, TIMEOUT_VALUE, TimeUnit.MILLISECONDS).map(new Func1<Long, Boolean>() {
            @Override
            public Boolean call(Long aLong) {
                if(DateTime.getDateTime()-lastReceived>=TIMEOUT_VALUE){
                    Log.d("abc","disconnect time="+DateTime.getDateTime());
                    unsubscribeDevice();
                    return false;
                }else return true;
            }
        }).filter(new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean aBoolean) {
                return !aBoolean;
            }
        }).flatMap(new Func1<Boolean, Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call(Boolean aBoolean) {
                return Observable.timer(5, TimeUnit.SECONDS).map(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        Log.d("abc","connect time="+DateTime.getDateTime());
                        connectDevice();
                        return true;
                    }
                });
            }
        }).subscribe(new Observer<Boolean>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Boolean aBoolean) {

            }
        });

    }
    private void connectDevice(){
        RxBleDevice device = MyApplication.getRxBleClient().getBleDevice(getDeviceId());
        subscriptionDevice = device.establishConnection(false)
                .flatMap(rxBleConnection -> Observable.merge(rxBleConnection.setupNotification(Constants.IMU_SERV_CHAR_UUID), rxBleConnection.setupNotification(Constants.BATTERY_SERV_CHAR_UUID)))
                .flatMap(notificationObservable -> notificationObservable).onBackpressureBuffer(512)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<byte[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        unsubscribeDevice();
//                        Log.d("abc","error="+e.getMessage());
                    }

                    @Override
                    public void onNext(byte[] bytes) {
                        lastReceived=DateTime.getDateTime();
                        if (bytes.length == 1)
                            insertBattery(bytes[0]);
                        else {
                            insertToQueue(new Data(getType(), bytes, DateTime.getDateTime()));
                        }
                    }
                });

    }

    private void insertBattery(double value) {
        DataTypeDoubleArray battery = new DataTypeDoubleArray(DateTime.getDateTime(), new double[]{value});
        if (sensors.get(Sensor.KEY_BATTERY) != null) {
            sensors.get(Sensor.KEY_BATTERY).insert(battery);
            updateView(Sensor.KEY_BATTERY, battery);
        }
    }

    abstract void insertData(long timestamp, long gyroOffset, Data blData);


    void stop() {
        try {
            if (subscriptionDeviceContinuous != null && !subscriptionDeviceContinuous.isUnsubscribed())
                subscriptionDeviceContinuous.unsubscribe();
        }catch (Exception ignored){}
        try {
            if (subscriptionDataQuality != null && !subscriptionDataQuality.isUnsubscribed())
                subscriptionDataQuality.unsubscribe();
        }catch (Exception ignored){}

        unsubscribeDevice();
        for (Sensor sensor : sensors.values())
            try {
                sensor.unregister();
            } catch (DataKitException e) {
                e.printStackTrace();
            }
    }

    public String getId() {
        return platform.getId();
    }

    public String getType() {
        return platform.getType();
    }

    public String getDeviceId() {
        if (platform.getMetadata() == null) return null;
        return platform.getMetadata().get(METADATA.DEVICE_ID);
    }

    public String getName() {
        return platform.getMetadata().get(METADATA.NAME);
    }

    public HashMap<String, Sensor> getSensors() {
        return sensors;
    }

    ArrayList<DataSource> getDataSources() {
        ArrayList<DataSource> dataSources = new ArrayList<>();
        for (Sensor sensor : sensors.values())
            dataSources.add(sensor.getDataSource());
        return dataSources;
    }

    void updateView(String key, DataType data) {
        String deviceId = getDeviceId(), platformId = getId();
        if (startTimestamp == 0) startTimestamp = DateTime.getDateTime();
        Intent intent = new Intent(ActivityMain.INTENT_NAME);
        intent.putExtra("operation", "data");
        String dataSourceUniqueId = key + '_' + platformId;
        if (!hm.containsKey(dataSourceUniqueId)) {
            hm.put(dataSourceUniqueId, 0);
        }
        hm.put(dataSourceUniqueId, hm.get(dataSourceUniqueId) + 1);
        intent.putExtra("count", hm.get(dataSourceUniqueId));
        intent.putExtra("timestamp", DateTime.getDateTime());
        intent.putExtra("starttimestamp", startTimestamp);
        intent.putExtra("data", data);
        intent.putExtra("key", key);
        intent.putExtra("deviceid", deviceId);
        intent.putExtra("platformid", platformId);
        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(intent);
    }
    private void unsubscribeDevice(){
        try {
            if (subscriptionDevice != null && !subscriptionDevice.isUnsubscribed())
                subscriptionDevice.unsubscribe();
        }catch (Exception ignored){}

    }
}
