package org.md2k.motionsense.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import org.md2k.motionsense.Constants;

import java.util.List;
import java.util.UUID;

/**
 * Created by monowar on 7/10/16.
 */
public class  RunnableServiceStart implements Runnable {
    private static final String TAG = RunnableServiceStart.class.getSimpleName();
    BluetoothGattService bluetoothGattService;
    BluetoothGatt gatt;
    boolean ret;

    RunnableServiceStart(BluetoothGatt gatt, BluetoothGattService bluetoothGattService) {
        Log.d(TAG,"RunnableServiceStart()..."+bluetoothGattService.getUuid());
        this.bluetoothGattService = bluetoothGattService;
        this.gatt = gatt;
    }

    @Override
    public void run() {
        if (bluetoothGattService.getUuid().equals(Constants.IMU_SERVICE_UUID)) {
            List<BluetoothGattCharacteristic> chars = bluetoothGattService.getCharacteristics();
            for (int j = 0; j < chars.size(); j++) {
                BluetoothGattCharacteristic characteristic = chars.get(j);
                if (characteristic == null) {
                    continue;
                }
                if (characteristic.getDescriptors().size() == 0) continue;
                Log.d(TAG, "characteristic UUID=" + characteristic.getUuid());

                if (characteristic.getUuid().equals(Constants.IMU_SERV_CHAR_UUID)) {
                    ret = gatt.setCharacteristicNotification(characteristic, true);
                    if (!ret) {
                        continue;
                    }
                    Log.i(TAG, "[LOG]Blood_Pressure_Measurement:characteristic.getDescriptor");
                    UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor); //descriptor write operation successfully started?

                }
            }
        }
        // When Battery Service is discovered

        if (bluetoothGattService.getUuid().equals(Constants.BATTERY_SERVICE_UUID)) {
            Log.i(TAG, "[LOG]Battery Service is discovered");

            BluetoothGattCharacteristic characteristic = bluetoothGattService.getCharacteristic(Constants.BATTERY_SERV_CHAR_UUID);
            if (characteristic == null) {
                return;
            }
            if (characteristic.getDescriptors().size() == 0) return;

            ret = gatt.setCharacteristicNotification(characteristic, true);
            if (!ret) {
                return;
            }
            UUID CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
            Log.i(TAG, "[LOG]Battery Service:characteristic.getDescriptor");
        }

    }
}
