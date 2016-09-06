package org.md2k.motionsense.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import org.md2k.motionsense.BlData;
import org.md2k.motionsense.IBleListener;

import java.util.UUID;


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

public class MyBlueTooth {
    private static final String TAG = MyBlueTooth.class.getSimpleName();
    Context context;
    protected BleService mBleService;
    public static final int MSG_CONNECTING = 0;
    public static final int MSG_CONNECTED = 1;
    public static final int MSG_DISCONNECTED = 2;
    public static final int MSG_DATA_RECV = 3;
    public static final int MSG_ADV_CATCH_DEV = 4;
    public static final int MSG_SCAN_CANCEL = 5;
    public static final int MSG_CTS_DATA_RECV = 7;
    public static final int MSG_BPF_DATA_RECV = 9;
    public static final int MSG_WSF_DATA_RECV = 11;

    public static final int BLE_STATE_IDLE = 0;
    public static final int BLE_STATE_SCANNING = 1;
    public static final int BLE_STATE_CONNECTING = 2;
    public static final int BLE_STATE_CONNECT = 3;
    public static final int BLE_STATE_DATA_RECV = 4;
    public int mBleState = BLE_STATE_IDLE;
    OnReceiveListener onReceiveListener;
    OnConnectionListener onConnectionListener;
    boolean isConnected;


    public MyBlueTooth(Context context, OnConnectionListener onConnectionListener, OnReceiveListener onReceiveListener) {
        this.context=context;
        this.onReceiveListener=onReceiveListener;
        this.onConnectionListener=onConnectionListener;
        isConnected=false;
        if (mBleService != null){
            mBleService.setCurrentContext(context.getApplicationContext(), (IBleListener) mBinder);
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mReceiver, filter);
        context.bindService(new Intent(context, BleService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    public void scanOn(UUID[] uuids) {
        mBleService.BleScan(uuids);
    }
    public void scanOff(){
        mBleService.BleScanOff();
    }
    public void connect(BluetoothDevice bluetoothDevice){
        mBleService.BleConnectDev(bluetoothDevice);
    }

    public void close() {
        if(mReceiver != null)
            context.unregisterReceiver(mReceiver);
        if(mConnection!= null)
            context.unbindService(mConnection);
    }

    public void disconnect() {
            mBleService.BleDisconnect();
    }

    public void enable() {
        BluetoothAdapter mBluetoothAdapter=((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(btIntent);
        }
    }

    public void disable() {
        BluetoothAdapter mBluetoothAdapter=((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (mBluetoothAdapter == null) return;
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    public boolean isEnabled() {
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }
    public boolean hasSupport() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            return false;
        BluetoothAdapter mBluetoothAdapter=((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return mBluetoothAdapter != null;
    };


    private final IBleListener.Stub mBinder = new IBleListener.Stub() {
        public void BleAdvCatch() throws RemoteException {
            Log.d(TAG, "[IN]BleAdvCatch");
            mBleState = BLE_STATE_CONNECTING;
            Message msg = new Message();
            msg.what=MSG_CONNECTING;
            mHandler.sendMessage(msg);
        }

        public void BleAdvCatchDevice(BluetoothDevice dev) throws RemoteException {
            Log.d(TAG, "[IN]BleAdvCatchDevice");
            Message msg = new Message();
            msg.what=MSG_ADV_CATCH_DEV;
            msg.obj = dev;
            mHandler.sendMessage(msg);
        }

        public void BleConnected() throws RemoteException {
            Log.d(TAG, "[IN]BleConnected");
            mBleState = BLE_STATE_CONNECT;
            Message msg = new Message();
            msg.what=MSG_CONNECTED;
            mHandler.sendMessage(msg);
        }

        public void BleDisConnected() throws RemoteException {
            Log.d(TAG, "[IN]BleDisConnected");
            mBleState = BLE_STATE_IDLE;
            Message msg = new Message();
            msg.what=MSG_DISCONNECTED;
            mHandler.sendMessage(msg);
        }

        public void BleDataRecv(BlData blData) throws RemoteException {
            Message msg = new Message();
            msg.what=MSG_DATA_RECV;
            msg.obj=blData;
            mHandler.sendMessage(msg);
        }

        public void BleCtsDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleCtsDataRecv");
            Message msg = new Message();
            msg.what=MSG_CTS_DATA_RECV;
            msg.obj=data;
            mHandler.sendMessage(msg);
        }

        public void BleBpfDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleBpfDataRecv");
            Message msg = new Message();
            msg.what=MSG_BPF_DATA_RECV;
            msg.obj=data;
            mHandler.sendMessage(msg);
        }

        public void BleWsfDataRecv(byte[] data) throws RemoteException {
            Log.d(TAG, "[IN]BleWsfDataRecv");
            Message msg = new Message();
            msg.what=MSG_WSF_DATA_RECV;
            msg.obj=data;
            mHandler.sendMessage(msg);
        }
    };
    // Event handler
    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg){
            onReceiveMessage(msg);
        }
    };
    protected void onReceiveMessage(Message msg) {
        switch(msg.what){
            case MSG_CONNECTING:
                break;

            case MSG_CONNECTED:
                Log.d(TAG, "[LOG]MSG_CONNECTED");
                isConnected=true;
                onReceiveListener.onReceived(msg);
                break;

            case MSG_DISCONNECTED:
                Log.d(TAG, "[LOG]MSG_DISCONNECTED");
                isConnected=false;
                onReceiveListener.onReceived(msg);
                break;

            case MSG_ADV_CATCH_DEV:
                Log.d(TAG, "[LOG]MSG_ADV_CATCH_DEV");
                onReceiveListener.onReceived(msg);
                break;
            case MSG_DATA_RECV:
                onReceiveListener.onReceived(msg);
                break;
            default:
                break;
        }
    }
    protected ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "[IN]onServiceConnected");
            onBleServiceConnected(service);
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "[IN]onServiceDisconnected");
            mBleService = null;
            context.unbindService(mConnection);
            onConnectionListener.onDisconnected();
        }
    };
    protected void onBleServiceConnected(IBinder service) {
        Log.d(TAG, "[IN]onBleReceiveMessage");
        mBleService = ((BleService.MyServiceLocalBinder)service).getService();
        mBleService.setCurrentContext(context.getApplicationContext(), (IBleListener) mBinder);
        if(!isEnabled()) enable();
        else
            onConnectionListener.onConnected();
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        disconnect();
                        close();
                        onConnectionListener.onDisconnected();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        onConnectionListener.onConnected();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    public void disconnect(String deviceId) {
        mBleService.BleDisconnect(deviceId);
    }
    public void connect(String deviceId) {
        mBleService.BleDisconnect(deviceId);
    }
}
