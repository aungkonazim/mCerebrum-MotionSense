package org.md2k.motionsense;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.md2k.datakitapi.DataKitAPI;
import org.md2k.datakitapi.exception.DataKitException;
import org.md2k.mcerebrum.commons.permission.Permission;
import org.md2k.motionsense.device.DeviceManager;

import java.util.HashMap;
import java.util.Map;

import br.com.goncalves.pugnotification.notification.PugNotification;
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
    public static final String INTENT_STOP = "stop";
    public static final String ACTION_LOCATION_CHANGED = "android.location.PROVIDERS_CHANGED";

    private DeviceManager deviceManager;
    private DataKitAPI dataKitAPI = null;
    private Map<String, Long> lastSampleTimestamps = new HashMap<>();
    private Map<String, Long> lastSampleSeqNumbers = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Permission.hasPermission(this)) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(!mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.enable();
            LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                removeNotification();
                load();
            }
            else {
                Toasty.error(this, "Please turn on GPS", Toast.LENGTH_SHORT).show();
                showNotification("Turn on GPS", "Wrist data can't be recorded. (Please click to turn on GPS)");
                stopSelf();
            }
        } else {
            Toasty.error(getApplicationContext(), "!PERMISSION is not GRANTED !!! Could not continue...", Toast.LENGTH_SHORT).show();
            showNotification("Permission required", "MotionSense app can't continue. (Please click to grant permission)");
            stopSelf();
        }
    }
    private void showNotification(String title, String message) {
        Bundle bundle = new Bundle();
        bundle.putInt(ActivityMain.OPERATION, ActivityMain.OPERATION_START_BACKGROUND);
        PugNotification.with(this).load().identifier(21).title(title).smallIcon(R.mipmap.ic_launcher)
                .message(message).autoCancel(true).click(ActivityMain.class, bundle).simple().build();
    }
    private void removeNotification() {
        PugNotification.with(this).cancel(21);
    }

    void load() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_LOCATION_CHANGED);
        registerReceiver(mReceiver, filter);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiverStop,
                new IntentFilter(INTENT_STOP));
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
        try{
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiverStop);
        }catch (Exception e){

        }
        try{
        unregisterReceiver(mReceiver);
        }catch (Exception e){

        }
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

    private BroadcastReceiver mMessageReceiverStop = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Toasty.error(ServiceMotionSense.this, "Bluetooth is off. Please turn on bluetooth", Toast.LENGTH_SHORT).show();
                        showNotification("Turn on Bluetooth", "Wrist data con't be recorded. Please click to turn on bluetooth");
                        stopSelf();
                }
            } else if (action.equals(ACTION_LOCATION_CHANGED)) {
                LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                } else {
                    Toasty.error(context, "Please turn on GPS", Toast.LENGTH_SHORT).show();
                    showNotification("Turn on GPS", "Wrist data can't be recorded. (Please click to turn on GPS)");
                    stopSelf();
                }
            }
        }
    };
}

