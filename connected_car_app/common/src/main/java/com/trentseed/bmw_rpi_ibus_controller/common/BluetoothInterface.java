package com.trentseed.bmw_rpi_ibus_controller.common;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Set;
import java.util.UUID;

public class BluetoothInterface {

    private static BluetoothInterface instance;
    private BluetoothConnectionManager mBluetoothConnectionManager;
    private Context mContext;
    private BluetoothConnectionManager.ConnectionListener mListener;
    public interface IBUSPacketListener {
        void onIBUSPacketReceived(String data);
    }
    public static IBUSPacketListener mIBUSPacketListener;
    private static final UUID SERVICE_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private static final String RASPBERRY_PI_DEVICE_NAME = "e39rpi0";

    private BluetoothInterface(Context context, BluetoothConnectionManager.ConnectionListener listener) {
        mContext = context;
        mListener = listener;
    }

    public static synchronized BluetoothInterface getInstance(Context context, BluetoothConnectionManager.ConnectionListener listener) {
        if (instance == null) {
            instance = new BluetoothInterface(context, listener);
        }
        return instance;
    }

    public static synchronized BluetoothInterface getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BluetoothInterface must be initialized with a context and listener first.");
        }
        return instance;
    }

    public void connectToRaspberryPi() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e("BMW", "Bluetooth not supported on this device.");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.e("BMW", "Bluetooth is not enabled.");
            return;
        }

        // Check for permissions before accessing bonded devices
        if (!hasBluetoothPermissions()) {
            Log.e("BMW", "Bluetooth permissions not granted.");
            mListener.onPermissionsDenied();
            return;
        }

        if (mBluetoothConnectionManager != null && (mBluetoothConnectionManager.isConnected() || mBluetoothConnectionManager.isConnecting())) {
            Log.d("BMW", "Already connected, skipping connection attempt.");
            return;
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            BluetoothDevice raspberryPiDevice = null;
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(RASPBERRY_PI_DEVICE_NAME)) {
                    raspberryPiDevice = device;
                    break;
                }
            }
            if (raspberryPiDevice == null) {
                Log.e("BMW", "Raspberry Pi device not found in paired devices.");
                return;
            }
            if (mBluetoothConnectionManager == null) {
                mBluetoothConnectionManager = new BluetoothConnectionManager(mContext, raspberryPiDevice, SERVICE_UUID, new BluetoothConnectionManager.ConnectionListener() {
                    @Override
                    public void onConnected() {
                        mListener.onConnected();
                    }

                    @Override
                    public void onDisconnected() {
                        mListener.onDisconnected();
                    }

                    @Override
                    public void onDataReceived(String data) {
                        BluetoothDataHolder.INSTANCE.updateData(data);
                        if (mIBUSPacketListener != null) {
                            mIBUSPacketListener.onIBUSPacketReceived(data);
                        }
                    }

                    @Override
                    public void onConnecting() {
                        mListener.onConnecting();
                    }

                    @Override
                    public void onPermissionsDenied() {
                        mListener.onPermissionsDenied();
                    }
                });
            }
            mBluetoothConnectionManager.connect();
        } catch (SecurityException e) {
            Log.e("BMW", "SecurityException while getting bonded devices: " + e.getMessage());
            mListener.onPermissionsDenied();
        }
    }

    public void sendData(String data) {
        if (mBluetoothConnectionManager != null) {
            mBluetoothConnectionManager.sendData(data);
        }
    }

    public boolean isConnected() {
        return mBluetoothConnectionManager != null && mBluetoothConnectionManager.isConnected();
    }

    public void disconnect() {
        if (mBluetoothConnectionManager != null) {
            mBluetoothConnectionManager.disconnect();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (mBluetoothConnectionManager != null) {
            mBluetoothConnectionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasBluetoothPermissions() {
        BluetoothHelper bluetoothHelper = new BluetoothHelper(mContext);
        return bluetoothHelper.checkBluetoothPermissions();
    }
}