package com.trentseed.bmw_rpi_ibus_controller.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

public class BluetoothHelper {
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothHelper(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    public boolean isBluetoothEnabled() {
        return isBluetoothSupported() && bluetoothAdapter.isEnabled();
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }
}

