package org.md2k.motionsense.device;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.polidea.rxandroidble.RxBleDevice;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.mcerebrum.core.data_format.DATA_QUALITY;
import org.md2k.motionsense.ActivityMain;
import org.md2k.motionsense.Constants;
import org.md2k.motionsense.MyApplication;
import org.md2k.motionsense.device.sensor.DataQualityAccelerometer;
import org.md2k.motionsense.device.sensor.Sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.MissingBackpressureException;

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
    private Subscription subscription;
    private Subscription subscriptionDataQuality;
    private HashMap<String, Integer> hm = new HashMap<>();
    private long startTimestamp = 0;
    private static final long TIMEOUT_VALUE = 10000L;
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

    void start() throws DataKitException {
        for (Sensor sensor : sensors.values())
            sensor.register();
        RxBleDevice device = MyApplication.getRxBleClient().getBleDevice(getDeviceId());
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
                        DataQualityAccelerometer sensor = (DataQualityAccelerometer) sensors.get(Sensor.KEY_DATA_QUALITY_ACCELEROMETER);
                        if (sensor != null) {
                            DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sensor.getStatus());
                            sensor.insert(dataTypeInt);
                            updateView(Sensor.KEY_DATA_QUALITY_ACCELEROMETER, dataTypeInt);
                        }
                    }
                });
        subscription = device.establishConnection(true)
                .flatMap(rxBleConnection -> Observable.merge(rxBleConnection.setupNotification(Constants.IMU_SERV_CHAR_UUID), rxBleConnection.setupNotification(Constants.BATTERY_SERV_CHAR_UUID)))
                .flatMap(notificationObservable -> notificationObservable).onBackpressureBuffer(1024)
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_VALUE, TimeUnit.MILLISECONDS)
                .retryWhen(observable -> observable.flatMap(error -> Observable.just(new Object()))).subscribe(new Observer<byte[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(byte[] bytes) {
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

    /*
        void insertData(long timestamp, long gyroOffset, Data blData) {
            DataTypeDoubleArray acl, gyr1, gyr2, led, raw, seq;
            double[] aclSample=blData.getAccelerometer();
            acl=new DataTypeDoubleArray(timestamp, aclSample);
            if(sensors.get(Sensor.KEY_DATA_QUALITY_ACCELEROMETER)!=null)
                ((DataQualityAccelerometer)sensors.get(Sensor.KEY_DATA_QUALITY_ACCELEROMETER)).add(aclSample[0]);
            if(sensors.get(Sensor.KEY_ACCELEROMETER)!=null) {
                sensors.get(Sensor.KEY_ACCELEROMETER).insert(acl);
                updateView(Sensor.KEY_ACCELEROMETER, acl);
            }

            if(getType().equals(PlatformType.MOTION_SENSE) && sensors.get(Sensor.KEY_GYROSCOPE)!=null){
                gyr1=new DataTypeDoubleArray(timestamp-gyroOffset, blData.getGyroscope());
                gyr2=new DataTypeDoubleArray(timestamp, blData.getGyroscope2());
                sensors.get(Sensor.KEY_GYROSCOPE).insert(gyr1);
                sensors.get(Sensor.KEY_GYROSCOPE).insert(gyr2);
                updateView(Sensor.KEY_GYROSCOPE, gyr1);
                updateView(Sensor.KEY_GYROSCOPE, gyr2);
            }
            if(getType().equals(PlatformType.MOTION_SENSE_HRV) && sensors.get(Sensor.KEY_LED)!=null){
                led=new DataTypeDoubleArray(timestamp, blData.getLED());
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
    */
    void stop() throws DataKitException {
        if (subscription != null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
        if (subscriptionDataQuality != null && !subscriptionDataQuality.isUnsubscribed())
            subscriptionDataQuality.unsubscribe();
        for (Sensor sensor : sensors.values())
            sensor.unregister();
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
}
