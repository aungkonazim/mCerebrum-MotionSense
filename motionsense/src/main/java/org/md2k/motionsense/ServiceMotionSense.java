package org.md2k.motionsense;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.messagehandler.ResultCallback;
import org.md2k.motionsense.bluetooth.MyBlueTooth;
import org.md2k.motionsense.bluetooth.OnConnectionListener;
import org.md2k.motionsense.bluetooth.OnReceiveListener;
import org.md2k.motionsense.devices.Device;
import org.md2k.motionsense.devices.Devices;
import org.md2k.motionsense.devices.sensor.Accelerometer;
import org.md2k.motionsense.devices.sensor.Battery;
import org.md2k.motionsense.devices.sensor.Gyroscope;
import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeInt;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.LogStorage;
import org.md2k.utilities.UI.AlertDialogs;
import org.md2k.utilities.permission.PermissionInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * - Nazir Saleheen <nazir.saleheen@gmail.com>
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

public class ServiceMotionSense extends Service {
    private static final String TAG = ServiceMotionSense.class.getSimpleName();
    public static final String INTENT_RESTART = "intent_restart";
    public static final String INTENT_STOP = "stop";
    public static final int BUFFER_SIZE = 10;

    private MyBlueTooth myBlueTooth = null;
    private Devices devices;
    private Map<String, BluetoothDevice> bluetoothDevices = new HashMap<String, BluetoothDevice>();
    private DataKitAPI dataKitAPI = null;
    private HashMap<String, Integer> hm = new HashMap<>();
    private long starttimestamp = 0;
    private Map<String, List<Data>> dataQueue = new HashMap<String, List<Data>>();
    private Map<String, Long> lastSampleTimestamps = new HashMap<>();
    private Map<String, Long> lastSampleSeqNumbers = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.getPermissions(this, new ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                if (!result) {
                    Toast.makeText(getApplicationContext(), "!PERMISSION DENIED !!! Could not continue...", Toast.LENGTH_SHORT).show();
                    stopSelf();
                } else {
                    load();
                }
            }
        });
    }
    void load(){

        LogStorage.startLogFileStorageProcess(getApplicationContext().getPackageName());

        Log.d(TAG, "onCreate()...");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiverStop,
                new IntentFilter(INTENT_STOP));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverRestart, new IntentFilter(INTENT_RESTART));

        if (readSettings())
            connectDataKit();
        else {
            showAlertDialogConfiguration(this);
            Log.d(TAG, "setSettingsDataKit()...");
            stopSelf();
        }
    }

    private boolean readSettings() {
        devices = new Devices(getApplicationContext());
        for (int i = 0; i < devices.size(); i++) {
            dataQueue.put(devices.get(i).getDeviceId(), new ArrayList<Data>());
            lastSampleTimestamps.put(devices.get(i).getDeviceId(), 0L);
            lastSampleSeqNumbers.put(devices.get(i).getDeviceId(), 0L);
        }
        return devices.size() != 0;
    }

    private void initializeBluetoothConnection() {
        myBlueTooth = new MyBlueTooth(ServiceMotionSense.this, onConnectionListener, onReceiveListener);
    }

    OnConnectionListener onConnectionListener = new OnConnectionListener() {
        @Override
        public void onConnected() {
            myBlueTooth.scanOn(new UUID[]{Constants.DEVICE_SERVICE_UUID});
        }

        @Override
        public void onDisconnected() {
            stopSelf();

        }
    };

    private int byteArrayToIntLE(byte[] bytes) {
        return java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
    }

    private int byteArrayToIntBE(byte[] bytes) {
        return java.nio.ByteBuffer.wrap(bytes).getShort();
    }

    OnReceiveListener onReceiveListener = new OnReceiveListener() {
        @Override
        public synchronized void onReceived(Message msg) {
            switch (msg.what) {
                case MyBlueTooth.MSG_ADV_CATCH_DEV:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                    Log.d(TAG,"monowar...catch_dev..dev="+bluetoothDevice.getAddress()+" devicesize="+devices.size()+"...");
                    if(bluetoothDevices.containsKey(bluetoothDevice.getAddress())) return;
                    for (int i = 0; i < devices.size(); i++)
                        if (bluetoothDevice.getAddress().equals(devices.get(i).getDeviceId())) {
                            myBlueTooth.connect((BluetoothDevice) msg.obj);
                            bluetoothDevices.put(devices.get(i).getDeviceId(), bluetoothDevice);
                            break;
                        }
                    Log.d(TAG,"monowar...catch_dev..dev="+bluetoothDevice.getAddress()+" devicesize="+devices.size()+"...done");
                    break;

                case MyBlueTooth.MSG_CONNECTED:

                    break;
                case MyBlueTooth.MSG_DATA_RECV:
                    DataTypeInt dataTypeInt;
                    DataTypeDoubleArray dataTypeDoubleArray;
                    BlData blData = (BlData) msg.obj;
                    Device device = devices.get(blData.getDeviceId());
                    String deviceId = device.getDeviceId();

                    if (blData.getType() == BlData.DATATYPE_BATTERY) {
                        int sample = blData.getData()[0];
                        dataTypeInt = new DataTypeInt(DateTime.getDateTime(), sample);
                        ((Battery) device.getSensor(DataSourceType.BATTERY)).insert(dataTypeInt);
                        updateView(DataSourceType.BATTERY, dataTypeInt, blData.getDeviceId(), device.getPlatformId());
                    } else if (blData.getType() == BlData.DATATYPE_ACLGYR) {
                        List<Data> buffer = dataQueue.get(deviceId);
                        int sequenceNumber = byteArrayToIntBE(new byte[]{blData.getData()[18], blData.getData()[19]});
                        Log.d(TAG,"[MOTION_SENSE_SEQ] seqnum="+sequenceNumber + "; deviceId="+deviceId);
                        insertToQueue(buffer, new Data(blData, sequenceNumber), deviceId);
                    }
                    break;
            }
        }
    };

    private void insertToQueue(List<Data> buffer, Data data, String deviceId) {
        long lastSampleTimestamp = lastSampleTimestamps.get(deviceId);
        long lastSampleSeqNum = lastSampleSeqNumbers.get(deviceId);
        long gyroOffset = -1;

        if (lastSampleTimestamp>0 && data.timestamp - lastSampleTimestamp> 500) {
            gyroOffset = correctTimestamp(buffer, lastSampleTimestamp, lastSampleSeqNum)/2;
        } else if (buffer.size() == BUFFER_SIZE) {
            gyroOffset = correctTimestamp(buffer, lastSampleTimestamp, lastSampleSeqNum)/2;
        }
        if (gyroOffset!=-1) {
            for (int i = 0; i < buffer.size(); i++)
                insertData(buffer.get(i).timestamp, gyroOffset, buffer.get(i).blData);
            Log.d(TAG,"[MOTION_SENSE] Insert data, size="+(buffer.size()-1));
            lastSampleTimestamps.put(deviceId, buffer.get(buffer.size()-1).timestamp);
            lastSampleSeqNumbers.put(deviceId, (long)buffer.get(buffer.size()-1).sequenceNumber);
            buffer.clear();
        }
        buffer.add(data);
    }

    /*
    *
    *
    * */
    private long correctTimestamp(List<Data> buffer, long lastSampleTimestamp, long lastSampleSeqNum) {

        long startTS = lastSampleTimestamp;
        long endTS = buffer.get(buffer.size()-1).timestamp;

        long startSeqNum = lastSampleSeqNum;
        long endSeqNum = buffer.get(buffer.size() - 1).sequenceNumber;

        if (lastSampleTimestamp ==0 ) {
            startTS = buffer.get(0).timestamp;
            startSeqNum = buffer.get(0).sequenceNumber;
        }
        long offset = (endTS-startTS)/(buffer.size());

        if (lastSampleTimestamp>0 && endSeqNum > startSeqNum) {
            offset = (endTS - startTS) / (endSeqNum - startSeqNum);
            for (int i = 0; i <buffer.size() ; i++) {
                buffer.get(i).timestamp = endTS - (endSeqNum - buffer.get(i).sequenceNumber) * offset;
            }
        } else {
            for (int i = 0; i <buffer.size() ; i++) {
                buffer.get(i).timestamp = startTS + (i) * offset;
            }
        }
        Log.d(TAG,"[MOTION_SENSE_SEQ] seq=("+startSeqNum+", "+endSeqNum+"), diff="+(endSeqNum-startSeqNum)+ "; buffSize="+(buffer.size())+"; offset="+offset);

        return offset;
    }

    void insertData(long timestamp, long gyroOffset, BlData blData) {
        DataTypeDoubleArray dataTypeDoubleArray;
        Device device = devices.get(blData.getDeviceId());

        double[] sample = new double[3];
        sample[0] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[0], blData.getData()[1]}));
        sample[1] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[2], blData.getData()[3]}));
        sample[2] = convertAccelADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[4], blData.getData()[5]}));
        dataTypeDoubleArray = new DataTypeDoubleArray(timestamp, sample);
        ((Accelerometer) device.getSensor(DataSourceType.ACCELEROMETER)).insert(dataTypeDoubleArray);
        device.dataQuality.add(sample[0]);
        updateView(DataSourceType.ACCELEROMETER, dataTypeDoubleArray, blData.getDeviceId(), device.getPlatformId());
        sample = new double[3];
        sample[0] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[6], blData.getData()[7]}));
        sample[1] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[8], blData.getData()[9]}));
        sample[2] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[10], blData.getData()[11]}));
        dataTypeDoubleArray = new DataTypeDoubleArray(timestamp - gyroOffset, sample);
        ((Gyroscope) device.getSensor(DataSourceType.GYROSCOPE)).insert(dataTypeDoubleArray);
        updateView(DataSourceType.GYROSCOPE, dataTypeDoubleArray, blData.getDeviceId(), device.getPlatformId());
        sample = new double[3];
        sample[0] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[12], blData.getData()[13]}));
        sample[1] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[14], blData.getData()[15]}));
        sample[2] = convertGyroADCtoSI(byteArrayToIntBE(new byte[]{blData.getData()[16], blData.getData()[17]}));
        dataTypeDoubleArray = new DataTypeDoubleArray(timestamp, sample);
        ((Gyroscope) device.getSensor(DataSourceType.GYROSCOPE)).insert(dataTypeDoubleArray);
        updateView(DataSourceType.GYROSCOPE, dataTypeDoubleArray, blData.getDeviceId(), device.getPlatformId());

        int sequenceNumber = byteArrayToIntBE(new byte[]{blData.getData()[18], blData.getData()[19]});

    }

    private double convertGyroADCtoSI(double x) {
        return 250.0 * x / 32768;
    }

    private double convertAccelADCtoSI(double x) {
        return 1.0 * x / 16384;
    }

    private void updateView(String dataSourceType, DataType data, String deviceId, String platformId) {
        if (starttimestamp == 0) starttimestamp = DateTime.getDateTime();
        Intent intent = new Intent(ActivityMain.INTENT_NAME);
        intent.putExtra("operation", "data");
        String dataSourceUniqueId = dataSourceType + '_' + platformId;
        if (!hm.containsKey(dataSourceUniqueId)) {
            hm.put(dataSourceUniqueId, 0);
        }
        hm.put(dataSourceUniqueId, hm.get(dataSourceUniqueId) + 1);
        intent.putExtra("count", hm.get(dataSourceUniqueId));
        intent.putExtra("timestamp", DateTime.getDateTime());
        intent.putExtra("starttimestamp", starttimestamp);
        intent.putExtra("data", data);
        intent.putExtra("datasourcetype", dataSourceType);
        intent.putExtra("deviceid", deviceId);
        intent.putExtra("platformid", platformId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private void connectDataKit() {
        dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
        Log.d(TAG, "datakitapi connected=" + dataKitAPI.isConnected());
        try {
            dataKitAPI.connect(new org.md2k.datakitapi.messagehandler.OnConnectionListener() {
                @Override
                public void onConnected() {
                    try {
                        devices.register();
                        initializeBluetoothConnection();
                    } catch (DataKitException e) {
//                        clearDataKitSettingsBluetooth();
                        stopSelf();
                        e.printStackTrace();
                    }
                }
            });
        } catch (DataKitException e) {
            Log.d(TAG, "onException...");
            stopSelf();
        }
    }

    private void disconnectDataKit() {
        Log.d(TAG, "disconnectDataKit()...");
        if (devices != null)
            try {
                devices.unregister();
            } catch (DataKitException e) {
                e.printStackTrace();
            }
        if (dataKitAPI != null) {
            dataKitAPI.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()...");
        clearDataKitSettingsBluetooth();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void clearDataKitSettingsBluetooth() {
        Log.d(TAG, "clearDataKitSettingsBluetooth...");
        clearSettingsBluetooth();
        disconnectDataKit();
    }

    private void clearSettingsBluetooth() {
        clearBlueTooth();
    }

    private void clearBlueTooth() {
        Log.d(TAG, "clearBlueTooth()...");
        myBlueTooth.scanOff();
        for (int i = 0; i < devices.size(); i++)
            myBlueTooth.disconnect(devices.get(i).getDeviceId());
//        myBlueTooth.disconnect();
        myBlueTooth.close();
    }

    void showAlertDialogConfiguration(final Context context) {
        AlertDialogs.AlertDialog(this, "Error: MotionSense Settings", "Please configure MotionSense", R.drawable.ic_error_red_50dp, "Settings", "Cancel", null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    Intent intent = new Intent(context, ActivitySettings.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });
    }

    private BroadcastReceiver mMessageReceiverRestart = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            AutoSensePlatform autoSensePlatform = (AutoSensePlatform) intent.getSerializableExtra(AutoSensePlatform.class.getSimpleName());
            String deviceId = intent.getStringExtra("device_id");
            myBlueTooth.disconnect(deviceId);
            if (bluetoothDevices.containsKey(deviceId)) {
                myBlueTooth.connect(bluetoothDevices.get(deviceId));
            }
        }
    };

    private BroadcastReceiver mMessageReceiverStop = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            org.md2k.utilities.Report.Log.d(TAG, "Stop");
            org.md2k.utilities.Report.Log.w(TAG, "time=" + DateTime.convertTimeStampToDateTime(DateTime.getDateTime()) + ",timestamp=" + DateTime.getDateTime() + ",broadcast_receiver_stop_service");
            stopSelf();
        }
    };


}

class Data {
    long timestamp;
    BlData blData;
    int sequenceNumber;

    public Data(long timestamp, BlData blData) {
        this.timestamp = timestamp;
        this.blData = blData;
    }

    public Data(BlData blData) {
        this.timestamp = DateTime.getDateTime();
        this.blData = blData;
    }

    public Data(BlData blData, int sequenceNumber) {
        this.timestamp = DateTime.getDateTime();
        this.blData = blData;
        this.sequenceNumber = sequenceNumber;
    }
}
