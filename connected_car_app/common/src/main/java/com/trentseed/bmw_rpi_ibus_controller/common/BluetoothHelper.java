package com.trentseed.bmw_rpi_ibus_controller.common;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BluetoothHelper {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private final Context mContext;

    public BluetoothHelper(Context context) {
        mContext = context;
    }

    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = mContext.getSystemService(BluetoothManager.class).getAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean checkBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestBluetoothPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, REQUEST_BLUETOOTH_PERMISSIONS);
    }
}