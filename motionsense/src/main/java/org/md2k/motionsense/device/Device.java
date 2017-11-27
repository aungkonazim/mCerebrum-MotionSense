package org.md2k.motionsense.device;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.METADATA;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.motionsense.ActivityMain;
import org.md2k.motionsense.Constants;
import org.md2k.motionsense.MyApplication;
import org.md2k.motionsense.ServiceMotionSense;
import org.md2k.motionsense.device.sensor.DataQualityAccelerometer;
import org.md2k.motionsense.device.sensor.DataQualityLed;
import org.md2k.motionsense.device.sensor.Sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
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
    private long lastReceived = 0;
    private static final long TIMEOUT_VALUE = 30000;
    private static final int DELAY = 3000;
    long t;
    private Subscription subscriptionStop;

    Device(Platform platform) {
        super();
        this.platform = platform;
        sensors = new HashMap<>();
    }

    public void add(DataSource dataSource) {
        if (sensors.get(Sensor.getKey(dataSource)) != null) return;
        MetaData metaData = new MetaData();
        DataSource dd;
        ArrayList<DataSource> dataSources = metaData.getDataSources(dataSource.getPlatform().getType());
        for (int i = 0; i < dataSources.size(); i++) {
            if (dataSource.getPlatform().getMetadata() == null) {
                Platform platform = new PlatformBuilder(dataSource.getPlatform()).setType(dataSource.getPlatform().getType()).setId(dataSource.getPlatform().getId()).build();
                dd = new DataSourceBuilder(dataSources.get(i)).setPlatform(platform).build();

            } else {
                String deviceId = dataSource.getPlatform().getMetadata().get(METADATA.DEVICE_ID);
                Platform platform = new PlatformBuilder(dataSource.getPlatform()).setType(dataSource.getPlatform().getType()).setId(dataSource.getPlatform().getId()).setMetadata(METADATA.DEVICE_ID, deviceId).build();
                dd = new DataSourceBuilder(dataSources.get(i)).setPlatform(platform).build();
            }
            Sensor s = Sensor.create(dd);
            if (s != null)
                sensors.put(Sensor.getKey(dd), s);
        }

/*
        Sensor sensor = Sensor.create(dataSource);
        if (sensor != null)
            sensors.put(Sensor.getKey(dataSource), sensor);
        if (dataSource.getPlatform().getType().equalsIgnoreCase(PlatformType.MOTION_SENSE_HRV)) {
            DataSource d = metaData.getDataSource(DataSourceType.DATA_QUALITY, DataSourceType.LED, PlatformType.MOTION_SENSE_HRV);
            String deviceId = dataSource.getPlatform().getMetadata().get(METADATA.DEVICE_ID);
            Platform platform = new PlatformBuilder(dataSource.getPlatform()).setType(dataSource.getPlatform().getType()).setId(dataSource.getPlatform().getId()).setMetadata(METADATA.DEVICE_ID, deviceId).build();
            DataSource dd = new DataSourceBuilder(d).setPlatform(platform).build();
            Sensor s = Sensor.create(dataSource);
            if (s != null)
                sensors.put(Sensor.getKey(dataSource), s);
        }
*/
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

        String curDeviceId = getDeviceId();
        String pDeviceId = null;
        if (platform.getMetadata() != null && platform.getMetadata().get(METADATA.DEVICE_ID) != null)
            pDeviceId = platform.getMetadata().get(METADATA.DEVICE_ID);

        if (curDeviceId == null && pDeviceId == null) return true;
        if (curDeviceId != null && pDeviceId == null) return false;
        if (curDeviceId == null && pDeviceId != null) return false;
        if (curDeviceId.equals(pDeviceId)) return true;
        return false;
    }

    private void calculateDataQualityAccelerometer() {
        DataQualityAccelerometer sensor = (DataQualityAccelerometer) sensors.get(Sensor.KEY_DATA_QUALITY_ACCELEROMETER);
        if (sensor != null) {
            DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sensor.getStatus());
            sensor.insert(dataTypeInt);
            updateView(Sensor.KEY_DATA_QUALITY_ACCELEROMETER, dataTypeInt);
        }
    }

    private void calculateDataQualityLed() {
        DataQualityLed sensor = (DataQualityLed) sensors.get(Sensor.KEY_DATA_QUALITY_LED);
        if (sensor != null) {
            DataTypeInt dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sensor.getStatus());
            Log.d("data_quality_led", "final result=" + dataTypeInt.getSample());
            sensor.insert(dataTypeInt);
            updateView(Sensor.KEY_DATA_QUALITY_LED, dataTypeInt);
        }
    }

    void start() {
        for (Sensor sensor : sensors.values())
            sensor.register();

        subscriptionDataQuality = Observable.interval(DELAY, DELAY, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));

                    }

                    @Override
                    public void onNext(Long aLong) {
                        calculateDataQualityAccelerometer();
                        calculateDataQualityLed();
                    }
                });
//        unsubscribeDevice();
//        subscribeDevice();
        t = DateTime.getDateTime();
        subscriptionDeviceContinuous = Observable.interval(0, TIMEOUT_VALUE, TimeUnit.MILLISECONDS).map(new Func1<Long, Boolean>() {
            @Override
            public Boolean call(Long aLong) {
                RxBleDevice device = MyApplication.getRxBleClient().getBleDevice(getDeviceId());
                if (DateTime.getDateTime() - lastReceived >= TIMEOUT_VALUE || device.getConnectionState()== RxBleConnection.RxBleConnectionState.DISCONNECTED) {
                    Log.d("abc", "unsubscribe calls due to timeout...disconnect time=" + DateTime.getDateTime()+" device = "+getDeviceId());
                    unsubscribeDevice(true);
                    Log.d("abc", "after unsubscribe..device="+getDeviceId());
                    return false;
                }
                else return true;
            }
        }).subscribe(new Observer<Boolean>() {
            @Override
            public void onCompleted() {
                Log.d("abc","subscriptionDeviceContinuous=completed");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("abc","subscriptionDeviceContinuous=error="+e.getMessage());
                LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(new Intent(ServiceMotionSense.INTENT_STOP));
            }

            @Override
            public void onNext(Boolean aBoolean) {
                Log.d("abc","subscriptionDeviceContinuous=onNext");
            }
        });

    }

    private void subscribeDevice() {
        RxBleDevice device = MyApplication.getRxBleClient().getBleDevice(getDeviceId());
        Log.d("abc","subscribe...device="+getDeviceId()+" " + device.getConnectionState().toString());
        if(subscriptionStop!=null && !subscriptionStop.isUnsubscribed())
            subscriptionStop.unsubscribe();
        subscriptionDevice = Observable.just(true).flatMap(new Func1<Boolean, Observable<? extends RxBleConnection>>() {
            @Override
            public Observable<? extends RxBleConnection> call(Boolean aBoolean) {
                return device.establishConnection(false);
            }
        }).flatMap(new Func1<RxBleConnection, Observable<? extends Observable<byte[]>>>() {
            @Override
            public Observable<? extends Observable<byte[]>> call(RxBleConnection rxBleConnection) {
                Log.d("abc", "after connected...device="+getDeviceId()+" " + device.getConnectionState().toString()+" timediff=" + (DateTime.getDateTime() - t));
                return Observable.merge(rxBleConnection.setupNotification(Constants.IMU_SERV_CHAR_UUID), rxBleConnection.setupNotification(Constants.BATTERY_SERV_CHAR_UUID));
            }
        }).flatMap(notificationObservable -> notificationObservable).onBackpressureBuffer(64)
                .retryWhen(new Func1<Observable<? extends Throwable>, Observable<?>>() {
                    @Override
                    public Observable<?> call(Observable<? extends Throwable> observable) {
                        return observable.delay(5, TimeUnit.SECONDS).filter(throwable -> {
                            Log.e("abc", "error inside backpressure="+throwable.toString());
                            return throwable instanceof BleDisconnectedException;
                        });
                    }
                })
                .subscribe(new Observer<byte[]>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("abc", "after unsubscribe ... error=" + e.getMessage());
                        unsubscribeDevice(true);
                    }

                    @Override
                    public void onNext(byte[] bytes) {
//                        Log.d("abc","received len="+bytes.length);
                        lastReceived = DateTime.getDateTime();
                        if (bytes.length == 1)
                            insertBattery(bytes[0]);
                        else {
                            if (platform.getType().equals(PlatformType.MOTION_SENSE_HRV))
                                insertData(lastReceived, 0, new Data(getType(), bytes, DateTime.getDateTime()));
                            else
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
        } catch (Exception ignored) {
        }
        try {
            if (subscriptionDataQuality != null && !subscriptionDataQuality.isUnsubscribed())
                subscriptionDataQuality.unsubscribe();
        } catch (Exception ignored) {
        }
        Log.d("abc", "unsubscribe is called by stop() device=" + getDeviceId());
        unsubscribeDevice(false);

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

    private void unsubscribeDevice(boolean restart) {
        RxBleDevice device = MyApplication.getRxBleClient().getBleDevice(getDeviceId());
        Log.d("abc", "unsubscribe starts...device="+getDeviceId()+" restart="+restart+" device status="+device.getConnectionState());
        try {
            if (subscriptionStop != null && !subscriptionStop.isUnsubscribed())
                subscriptionStop.unsubscribe();
        } catch (Exception ignored) {
            Log.e("abc", "unsubscribe: exception = " + ignored.getMessage()+" device status="+device.getConnectionState());
        }

        try {
            if (subscriptionDevice != null && !subscriptionDevice.isUnsubscribed())
                subscriptionDevice.unsubscribe();
        } catch (Exception ignored) {
            Log.e("abc", "unsubscribe: exception = " + ignored.getMessage()+" device status="+device.getConnectionState());
        }
        if (!restart) {
            Log.d("abc", "quit from application device="+getDeviceId());
            return;
        } else {
            Log.d("abc", "trying to disconnect.. device="+getDeviceId()+" status="+device.getConnectionState());
            subscriptionStop = Observable.interval(0, 500, TimeUnit.MILLISECONDS).map(new Func1<Long, Boolean>() {
                @Override
                public Boolean call(Long aLong) {
                    RxBleDevice device = MyApplication.getRxBleClient().getBleDevice(getDeviceId());
                    Log.d("abc", "unsubscribe continues... device="+getDeviceId()+" " + device.getConnectionState().toString());
                    if (device.getConnectionState() == RxBleConnection.RxBleConnectionState.DISCONNECTED) {
                        subscribeDevice();
                        return false;
                    } else return true;
                }
            }).takeWhile(new Func1<Boolean, Boolean>() {
                @Override
                public Boolean call(Boolean aBoolean) {
                    Log.d("abc", "unsubscribe continues... device="+getDeviceId()+" " + device.getConnectionState().toString()+" takeWhile="+aBoolean);
                    return aBoolean;
                }
            }).subscribe();
        }
        Log.d("abc", ".............unsubscribe device="+getDeviceId());

    }

}
