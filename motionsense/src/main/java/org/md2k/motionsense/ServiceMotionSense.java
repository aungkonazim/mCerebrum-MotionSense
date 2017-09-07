package org.md2k.motionsense;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.mcerebrum.commons.permission.Permission;
import org.md2k.motionsense.device.DeviceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.dmoral.toasty.Toasty;

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
    public static final String INTENT_RESTART = "intent_restart";
    public static final String INTENT_STOP = "stop";

    private DeviceManager deviceManager;
    private DataKitAPI dataKitAPI = null;
    private Map<String, List<Data>> dataQueue = new HashMap<String, List<Data>>();
    private Map<String, Long> lastSampleTimestamps = new HashMap<>();
    private Map<String, Long> lastSampleSeqNumbers = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Permission.hasPermission(this)) {
            load();
        } else {
            Toasty.error(getApplicationContext(), "!PERMISSION DENIED !!! Could not continue...", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    void load() {
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiverStop,
                new IntentFilter(INTENT_STOP));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiverRestart, new IntentFilter(INTENT_RESTART));

        if (readSettings())
            connectDataKit();
        else {
            showAlertDialogConfiguration(this);
            stopSelf();
        }
    }

    private boolean readSettings() {
        deviceManager = new DeviceManager();
        for (int i = 0; i < deviceManager.size(); i++) {
            dataQueue.put(deviceManager.get(i).getDeviceId(), new ArrayList<Data>());
            lastSampleTimestamps.put(deviceManager.get(i).getDeviceId(), 0L);
            lastSampleSeqNumbers.put(deviceManager.get(i).getDeviceId(), 0L);
        }
        return deviceManager.size() != 0;
    }


    private void connectDataKit() {
        dataKitAPI = DataKitAPI.getInstance(getApplicationContext());
        try {
            dataKitAPI.connect(new org.md2k.datakitapi.messagehandler.OnConnectionListener() {
                @Override
                public void onConnected() {
                    try {
                        deviceManager.start();
                    } catch (DataKitException e) {
//                        clearDataKitSettingsBluetooth();
                        stopSelf();
                        e.printStackTrace();
                    }
                }
            });
        } catch (DataKitException e) {
            stopSelf();
        }
    }


    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverRestart);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverStop);
        try {
            deviceManager.stop();
        } catch (DataKitException ignored) {

        }
        if (dataKitAPI != null) {
            dataKitAPI.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    void showAlertDialogConfiguration(final Context context) {
/*
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
*/
    }

    private BroadcastReceiver mMessageReceiverRestart = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            AutoSensePlatform autoSensePlatform = (AutoSensePlatform) intent.getSerializableExtra(AutoSensePlatform.class.getSimpleName());
/*
            String deviceId = intent.getStringExtra("device_id");
            if (myBlueTooth != null && deviceId != null) {
                myBlueTooth.disconnect(deviceId);
                if (bluetoothDevices.containsKey(deviceId)) {
                    myBlueTooth.connect(bluetoothDevices.get(deviceId));
                }
            }
*/
        }
    };

    private BroadcastReceiver mMessageReceiverStop = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
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

    Data(BlData blData, int sequenceNumber) {
        this.timestamp = DateTime.getDateTime();
        this.blData = blData;
        this.sequenceNumber = sequenceNumber;
    }
}
