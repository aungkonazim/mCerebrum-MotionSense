package org.md2k.motionsense.bluetooth;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import org.md2k.motionsense.BlData;
import org.md2k.motionsense.Constants;
import org.md2k.motionsense.IBleListener;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class BleService extends Service {
    private final static String TAG = "BleService";
    private final static String LOG_TAG = "BLE_LOG";


    private BluetoothAdapter mBluetoothAdapter;
    private Map<String, BluetoothGatt> bluetoothGatts = new HashMap<String, BluetoothGatt>();
    IBleListener mAppListener = null;

    //Create Binder
    private final IBinder mBinder = new MyServiceLocalBinder();

    private boolean mIsDiscoveredService = false;
    private boolean mIsACLConnected = false;
    private boolean mIsConnected = false;
    private boolean mIsBonded = false;

    private static final int MSG_CONNECT = 1;
    private static final int MSG_DISCONNECT = 2;
    private static final int MSG_DISCONNECT_DEVICE = 21;
    private static final int MSG_REQ_TIMEOUT = 3;
    private static final int MSG_SCAN_START = 4;
    private static final int MSG_SCAN_STOP = 5;
    private static final int MSG_NOTIFY_ACL_CONNECTED = 10;
    private static final int MSG_NOTIFY_ACL_DISCONNECTED = 11;
    private static final int MSG_NOTIFY_BOND_NONE = 12;
    private static final int MSG_NOTIFY_BOND_BONDED = 13;
    private static final int MSG_DATA = 100;
    private static final int CONNECTION_WAIT_TIME = 50;
    Handler handlerServiceStart;

    public void BleDisconnect(String deviceId) {
        Log.d(TAG, "[IN]BleDisconnect");
            Message msg = new Message();
            msg.obj = deviceId;
            msg.what = MSG_DISCONNECT_DEVICE;
            mHandler.sendMessage(msg);
    }

    class BleRequest {
        public static final int TYPE_NONE = 0;
        public static final int TYPE_DESC_WRITE = 1;
        public static final int TYPE_DESC_READ = 2;
        public static final int TYPE_CHAR_WRITE = 3;
        public static final int TYPE_CHAR_READ = 4;
        public static final int TYPE_MAX = 5;
        public int type;
        public Object o;
        public Date date;
    }

    private static final int BLE_REQ_RETRY_MAX = 20;
    private static final int BLE_REQ_TIMEOUT_MS = 1500;

    private Queue<BleRequest> mBleReqQueue = new LinkedList<BleRequest>();
    private Queue<BleRequest> mBleReqQueueAfterAuth = new LinkedList<BleRequest>();
    private int mBleReqRetryCount = 0;
    private Timer mBleReqTimer = null;
    private boolean mBleReqExecuting = false;
    int index = -1;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "[IN]onCreate");
        handlerServiceStart = new Handler();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "[IN]onDestroy");
        super.onDestroy();
        mBluetoothAdapter = null;
        bleReq_QueueClear();
        mBleReqQueueAfterAuth.clear();
        if (mBleReqTimer != null) {
            mBleReqTimer.cancel();
        }
        mBleReqTimer = null;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "[IN]onBind");

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "[IN]onUnbind");
        //Nothing to do
        //When onUnbind is overridden with return true, onRebind will be called at next bind
        unregisterReceiver(mReceiver);
        return false;
    }

    public void setCurrentContext(Context c, IBleListener listener) {
        Log.d(TAG, "[IN]setCurrentContext");
        mAppListener = listener;
    }

    public void BleScan(UUID[] uuids) {
        Log.d(TAG, "[IN]BleScan");
        Message msg = new Message();
        msg.what = MSG_SCAN_START;
        msg.obj = uuids;

        mHandler.sendMessage(msg);
    }

    public void BleScanOff() {
        Log.d(TAG, "[IN]BleScanOff");
        Message msg = new Message();
        msg.what = MSG_SCAN_STOP;
        mHandler.sendMessage(msg);
    }

    public boolean BleIsConnected() {
        return mIsACLConnected && mIsBonded;
    }

    public void BleConnectDev(Object object) {
        Log.d(TAG, "[IN]BleConnectDev");
        Message msg = new Message();
        msg.what = MSG_CONNECT;
        msg.obj = object;
        mHandler.sendMessage(msg);
    }

    public void BleDisconnect() {
        Log.d(TAG, "[IN]BleDisconnect");
        Message msg = new Message();
        msg.what = MSG_DISCONNECT;
        mHandler.sendMessage(msg);
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    try {
                        Log.d(TAG, "[IN]onLeScan");
                        Log.d(TAG, "[ADDR]" + device.getAddress());
                        Log.d(TAG, "[DEV NAME]" + device.getName());

                        mAppListener.BleAdvCatchDevice(device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            BluetoothDevice device;

            switch (msg.what) {
                case MSG_NOTIFY_ACL_DISCONNECTED:
                    device = (BluetoothDevice) msg.obj;
                    if (device  != null && bluetoothGatts.containsKey(device.getAddress())) {
                        Log.i(TAG, "[LOG]ACL_DISCONNECTED");
                        mIsACLConnected = false;
                        releaseConnection();
                    }
                    break;

                case MSG_NOTIFY_ACL_CONNECTED:
                    device = (BluetoothDevice) msg.obj;
                    if (device  != null && bluetoothGatts.containsKey(device.getAddress())) {
                        mIsACLConnected = true;
                        Log.i(TAG, "[LOG]ACL_CONNECTED");
                        Log.d(TAG, "[LOG]Bond state = " + String.format("%d", device.getBondState()));
                    }
                    break;

                case MSG_NOTIFY_BOND_NONE:
                    device = (BluetoothDevice) msg.obj;
                    if (device  != null && bluetoothGatts.containsKey(device.getAddress())) {
                        Log.i(TAG, "[LOG]Bond state = NONE");
                        mIsBonded = false;
                    }
                    break;

                case MSG_NOTIFY_BOND_BONDED:
                    device = (BluetoothDevice) msg.obj;
                    if (device  != null && bluetoothGatts.containsKey(device.getAddress())) {
                        Log.i(TAG, "[LOG]Bond state = BONDED");
                        mIsBonded = true;

                        // Notify connection state to Activity
                        if (mIsACLConnected) {
                            Log.i(LOG_TAG, "[LOG_OUT]CONNECT");
                            try {
                                mAppListener.BleConnected();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        // Add to Request Queue
                        while (true) {
                            BleRequest req = mBleReqQueueAfterAuth.poll();
                            if (req == null) {
                                break;
                            }
                            bleRequest(req.type, req.o);
                        }
                    }
                    break;

                case MSG_CONNECT:

                    BluetoothDevice mBluetoothDevice = (BluetoothDevice) msg.obj;
                    String deviceAddress=mBluetoothDevice.getAddress();
                    if (bluetoothGatts.containsKey(mBluetoothDevice.getAddress())){
                        BluetoothGatt mBluetoothGatt = bluetoothGatts.get(deviceAddress);
                        if (mBluetoothGatt!= null) {
                            mBluetoothGatt.disconnect();
                            mBluetoothGatt.close();
                        }
                        bluetoothGatts.remove(deviceAddress);
                    }
                    BluetoothGatt gatt = mBluetoothDevice.connectGatt(BleService.this, false, mGattCallback);
                    bluetoothGatts.put(deviceAddress, gatt);

                    Log.i(TAG, "[LOG-CON]connectGatt: size="+bluetoothGatts.size()+", "+mBluetoothDevice.getAddress()+"..."+gatt.getDevice().getAddress());
                    break;

                case MSG_DISCONNECT:
                    Log.d(TAG, "mBluetoothGatt.disconnect()");
                    Log.i(TAG, "[LOG-CON]disconnectGatt all: size="+bluetoothGatts.size());

                    for (String deviceAdd: bluetoothGatts.keySet()) {
                        BluetoothGatt mBluetoothGatt = bluetoothGatts.get(deviceAdd);
                        if (mBluetoothGatt!= null) {
                            mBluetoothGatt.disconnect();
                            mBluetoothGatt.close();
                        }
                    }
                    bluetoothGatts.clear();

                    if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                        Log.i(TAG, "Bluetooth is disable now.");
                        releaseConnection();
                    }
                    break;

                case MSG_DISCONNECT_DEVICE:
                    Log.d(TAG, "mBluetoothGattDevice.disconnect()");

                    deviceAddress = (String) msg.obj;
                    Log.i(TAG, "[LOG-CON]disconnectGattDevice: size="+bluetoothGatts.size()+", "+deviceAddress);

                    BluetoothGatt mBluetoothGatt = bluetoothGatts.get(deviceAddress);
                        if (mBluetoothGatt!= null) {
                            mBluetoothGatt.disconnect();
                            mBluetoothGatt.close();
                    }
                    bluetoothGatts.remove(deviceAddress);
                    if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                        Log.i(TAG, "Bluetooth is disable now.");
                        releaseConnection();
                    }
                    break;

                case MSG_SCAN_START:
                    UUID[] uuids = (UUID[]) msg.obj;
                    bleReq_QueueClear();
                    mBleReqQueueAfterAuth.clear();
                    if (mBleReqTimer != null) {
                        mBleReqTimer.cancel();
                    }
                    mBleReqTimer = null;

                    if (mBluetoothAdapter != null) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);

                        Log.d(TAG, "[IN]mBluetoothAdapter:" + mBluetoothAdapter);
                        Log.d(TAG, "[CALL]startLeScan(mLeScanCallback)");
                        if (uuids == null) {
                            mBluetoothAdapter.startLeScan(mLeScanCallback);
                        } else {
                            mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);
                        }
                    } else {
                        Log.d(TAG, "[LOG]mBluetoothAdapter = null");
                    }
                    break;

                case MSG_SCAN_STOP:
                    if (mBluetoothAdapter != null)
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    break;

                case MSG_REQ_TIMEOUT:
                    mBleReqTimer.cancel();
                    mBleReqTimer = null;
                    if (mBleReqRetryCount < BLE_REQ_RETRY_MAX) {
                        Log.d(TAG, "bleReq retry.");
                        BleRequest req = mBleReqQueue.peek();
                        bleReq_QueueExec(req);
                        mBleReqRetryCount++;
                    } else {
                        Log.d(TAG, "bleReq retry ... NG.");
                        bleReq_QueueDelRequest();

                        // execute next one
                        bleReq_QueueExec();
                    }
                    break;
                case MSG_DATA:
                    try {
                        BlData blData= (BlData) msg.obj;
                        mAppListener.BleDataRecv(blData);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    //  https://github.com/devunwired/accessory-samples/blob/master/BluetoothGatt/src/com/example/bluetoothgatt/MainActivity.java
    /*
     * In this callback, we've created a bit of a state machine to enforce that only
     * one characteristic be read or written at a time until all of our sensors
     * are enabled and we are registered to get notifications.
     */
    private BluetoothGattCallback
            mGattCallback = new BluetoothGattCallback() {

        /* State Machine Tracking */
        private int mState = 0;

        private void reset() { mState = 0; }

        private void advance() { mState++; }




        /*
         * Enable notification of changes on the data characteristic for each sensor
         * by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
         * configuration descriptor.
         */
        private void setNotifyNextSensor(BluetoothGatt gatt) {
            Log.d(TAG,"setNotifyNextSensor()...");
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Set notify pressure cal");
                    characteristic = gatt.getService(Constants.IMU_SERVICE_UUID)
                            .getCharacteristic(Constants.IMU_SERV_CHAR_UUID);
                    break;
                case 1:
                    Log.d(TAG, "Set notify pressure");
                    characteristic = gatt.getService(Constants.BATTERY_SERVICE_UUID)
                            .getCharacteristic(Constants.BATTERY_SERV_CHAR_UUID);
                    break;
                default:
                    Log.i(TAG, "All Sensors Enabled");
                    return;
            }

            //Enable local notifications
            gatt.setCharacteristicNotification(characteristic, true);
            //Enabled remote notifications
            BluetoothGattDescriptor desc = characteristic.getDescriptor(Constants.CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: "+status+" -> "+connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 * Once successfully connected, we must next discover all the services on the
                 * device before we can read and write their characteristics.
                 */
                gatt.discoverServices();
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered: "+status);
//            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
            /*
             * With services discovered, we are going to reset our state machine and start
             * working through the sensors we need to enable
             */
            Log.d(TAG,"reset()..");
            reset();
            if (gatt != null)
                setNotifyNextSensor(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //For each read, pass the data up to the UI thread to update the display
            Log.d(TAG,"onCharacteristicRead..."+characteristic.getUuid());
            if (Constants.IMU_SERV_CHAR_UUID.equals(characteristic.getUuid())) {
                Log.d(TAG,"ACL read...");
            }
            if (Constants.BATTERY_SERV_CHAR_UUID.equals(characteristic.getUuid())) {
                Log.d(TAG,"Battery read...");
            }

            //After reading the initial value, next we enable notifications
            setNotifyNextSensor(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
             * After notifications are enabled, all updates from the device on characteristic
             * value changes will be posted here.  Similar to read, we hand these up to the
             * UI thread to update the display.
             */
            if (Constants.IMU_SERV_CHAR_UUID.equals(characteristic.getUuid())) {
                BlData blData=new BlData(gatt.getDevice().getAddress(), BlData.DATATYPE_ACLGYR, characteristic.getValue());
                mHandler.sendMessage(Message.obtain(null, MSG_DATA, blData));
            }
            if (Constants.BATTERY_SERV_CHAR_UUID.equals(characteristic.getUuid())) {
                BlData blData=new BlData(gatt.getDevice().getAddress(), BlData.DATATYPE_BATTERY, characteristic.getValue());
                mHandler.sendMessage(Message.obtain(null, MSG_DATA, blData));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Once notifications are enabled, we move to the next sensor and start over with enable
            Log.d(TAG,"onDescriptorWrite()...");
            advance();
            setNotifyNextSensor(gatt);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: "+rssi);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };

    public class MyServiceLocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }

    private boolean sendMessage(int what, Object obj) {
        Message message = new Message();
        message.what = what;
        message.obj = obj;
        boolean ret = mHandler.sendMessage(message);
        if (!ret) {
            Log.e(TAG, "[LOG]Handler.sendMessage() error (" + String.format("%d", message.what) + ")");
        }
        return ret;
    }

    private void releaseConnection() {
        Log.i(LOG_TAG, "[LOG_OUT]DISCONNECT");
        mIsACLConnected = false;
        mIsConnected = false;
        mIsBonded = false;
        mIsDiscoveredService = false;

        try {
            Log.d(TAG, "[LOG]mAppListener.BleDisConnected()");
            mAppListener.BleDisConnected();

            bleReq_QueueClear();
            mBleReqQueueAfterAuth.clear();
            if (mBleReqTimer != null) {
                mBleReqTimer.cancel();
            }
            mBleReqTimer = null;

            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                Log.d(TAG, "[LOG]mBluetoothAdapter = null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "[IN]BondReceiver onReceive: " + intent.getAction());

            if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int prev_bond_state = intent.getExtras().getInt(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE);
                int bond_state = intent.getExtras().getInt(BluetoothDevice.EXTRA_BOND_STATE);
                Log.i(TAG, "[LOG]ACTION_BOND_STATE_CHANGED: " + String.format("bond_state prev=%d, now=%d", prev_bond_state, bond_state));
                if ((prev_bond_state == BluetoothDevice.BOND_BONDING) && (bond_state == BluetoothDevice.BOND_BONDED)) {
                    Log.d(TAG, "[LOG](prev_bond_state==BluetoothDevice.BOND_BONDING)&&(bond_state==BluetoothDevice.BOND_BONDED)");
                    Log.d(TAG, "[LOG]not Pairing Device!!!");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    sendMessage(MSG_NOTIFY_BOND_BONDED, device);
                }

            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Log.d(TAG, "[LOG]ACTION_ACL_DISCONNECTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                sendMessage(MSG_NOTIFY_ACL_DISCONNECTED, device);

            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                Log.d(TAG, "[LOG]ACTION_ACL_CONNECTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                sendMessage(MSG_NOTIFY_ACL_CONNECTED, device);
                sendMessage(MSG_NOTIFY_BOND_BONDED, device);

            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Log.d(TAG, "[LOG]ACTION_STATE_CHANGED");
                int state = intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE);
                if ((state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF)
                        && (mIsConnected || mIsACLConnected)) {
                    sendMessage(MSG_NOTIFY_ACL_DISCONNECTED, null);
                }
            }
        }
    };

    private boolean bleRequest(int reqType, Object o) {
        Log.d(TAG, "[IN]bleRequest");

        BleRequest req = new BleRequest();
        req.type = reqType;
        req.o = o;
        req.date = new Date();
        bleReq_QueueAdd(req);

        bleReq_QueueExec(); // If any request isn't queued, immediately this request is executed.
        return true;
    }

    private boolean bleRequestAfterAuth(int reqType, Object o) {
        Log.d(TAG, "[IN]bleRequestAfterAuth");

        BleRequest req = new BleRequest();
        req.type = reqType;
        req.o = o;
        req.date = new Date();
        mBleReqQueueAfterAuth.offer(req);
        return true;
    }

    private boolean bleReq_QueueAdd(BleRequest req) {
        mBleReqQueue.offer(req); // add
        Log.d(TAG, "[LOG]add queue - type:" + req.type + " num:" + mBleReqQueue.size());
        return true;
    }

    private boolean bleReq_QueueDelRequest() {
        BleRequest req = mBleReqQueue.remove(); // del
        mBleReqExecuting = false;
        mBleReqRetryCount = 0;
        Log.d(TAG, "[LOG]del queue - type:" + req.type + " num:" + mBleReqQueue.size());
        return true;
    }

    private boolean bleReq_QueueExec() {
        Log.d(TAG, "[IN]bleReq_QueueExec");

        if (mBleReqQueue.isEmpty()) {
            Log.d(TAG, "[LOG]bleReq_Queue is empty.");
            return false;
        }

        if (mBleReqExecuting) {
            Log.d(TAG, "[LOG]Other request is executed.");
            return false;
        }

        if (bluetoothGatts.size() == 0) {
            Log.d(TAG, "[LOG]bluetoothGatts.size == 0.");
            return false;
        }

        BleRequest req = mBleReqQueue.peek(); // get
        if (req.type <= BleRequest.TYPE_NONE
                || req.type >= BleRequest.TYPE_MAX) {
            Log.d(TAG, "[LOG]Unknown reqType.");
            return false;
        }

        bleReq_QueueExec(req);
        mBleReqRetryCount = 0;
        return true;
    }

    private boolean bleReq_QueueExec(BleRequest req) {
        Log.d(TAG, "[IN]bleReq_QueueExec(BleRequest)");

        mBleReqExecuting = true;

        // request timeout timer
        mBleReqTimer = new Timer();
        mBleReqTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "[IN]Timer.run");
                if (mBleReqExecuting) {
                    Message message = new Message();
                    message.what = MSG_REQ_TIMEOUT;
                    mHandler.sendMessage(message);
                    if (mBleReqTimer != null) {
                        mBleReqTimer.cancel();
                    }
                }
            }
        }, BLE_REQ_TIMEOUT_MS); // oneshot

        return true;
    }

    private boolean bleReq_QueueConfirmRsp(int rspType, Object rsp) {
        Log.d(TAG, "[IN]bleReq_QueueConfirmRsp");

        boolean ret = false;

        if (mBleReqQueue.isEmpty()) {
            Log.d(TAG, "[LOG]bleReq_Queue is empty.");
            return false;
        }

        if (!mBleReqExecuting) {
            Log.d(TAG, "[LOG]not request.");
            return false;
        }

        BleRequest req = mBleReqQueue.peek(); // get
        if (req.type != rspType) {
            Log.d(TAG, "[LOG]reqType don't match.");
            return false;
        }

        switch (req.type) {
            case BleRequest.TYPE_DESC_WRITE:
            case BleRequest.TYPE_DESC_READ:
                Log.d(TAG, "[LOG]confirm rsp: DESC_READ/WRITE");
                BluetoothGattDescriptor d_req = (BluetoothGattDescriptor) req.o;
                BluetoothGattDescriptor d_rsp = (BluetoothGattDescriptor) rsp;
                if (d_req.getUuid().equals(d_rsp.getUuid()) &&
                        d_req.getCharacteristic().getUuid().equals(d_rsp.getCharacteristic().getUuid())) {
                    ret = true;
                }
                break;
            case BleRequest.TYPE_CHAR_WRITE:
            case BleRequest.TYPE_CHAR_READ:
                Log.d(TAG, "[LOG]confirm rsp: CHAR_READ/WRITE");
                BluetoothGattCharacteristic c_req = (BluetoothGattCharacteristic) req.o;
                BluetoothGattCharacteristic c_rsp = (BluetoothGattCharacteristic) rsp;
                if (c_req.getUuid().equals(c_rsp.getUuid())) {
                    ret = true;
                }
                break;
            case BleRequest.TYPE_NONE:
            default:
                break;
        }

        if (ret) {
            if (mBleReqTimer != null) {
                mBleReqTimer.cancel();
            }
            mBleReqTimer = null;
            bleReq_QueueDelRequest();
        }

        return ret;
    }

    private void bleReq_QueueClear() {
        Log.d(TAG, "[IN]bleReq_QueueClear");
        mBleReqQueue.clear();
        mBleReqExecuting = false;
        mBleReqRetryCount = 0;
    }
}
