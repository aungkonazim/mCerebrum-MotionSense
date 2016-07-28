// BleListener.aidl.aidl
package org.md2k.motionsense;

// Declare any non-default types here with import statements

import android.bluetooth.BluetoothDevice;
import org.md2k.motionsense.BlData;

interface IBleListener {
	void BleAdvCatch();
	void BleConnected();
	void BleDisConnected();
	void BleDataRecv(in BlData blData);
	void BleCtsDataRecv(in byte[] data);
	void BleBpfDataRecv(in byte[] data);
	void BleAdvCatchDevice(in BluetoothDevice dev);
	void BleWsfDataRecv(in byte[] data);
}
