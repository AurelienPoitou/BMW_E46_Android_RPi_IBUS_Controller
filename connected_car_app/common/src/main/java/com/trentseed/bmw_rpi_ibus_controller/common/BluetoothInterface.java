package com.trentseed.bmw_rpi_ibus_controller.common;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles Bluetooth communication with Raspberry Pi
 */
public class BluetoothInterface {

    // Bluetooth objects
    public static Activity mActivity;
    public static List<IBUSPacket> mArrayListIBUSActivity = new ArrayList<>();
    public static BluetoothHelper mBluetoothHelper;
    public static BluetoothDevice mBluetoothDevice;
    public static BluetoothSocket mBluetoothSocket;
    public static InputStream mBluetoothInputStream;
    public static OutputStream mBluetoothOutputStream;
    public static UUID serviceUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    public static String remoteBluetoothAddress = "DC:A6:32:78:36:FF";
    public static ConnectedThread listenThread;
    public static Boolean isConnecting = false;

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final Logger logger = LogConfig.getLogger();

    /**
     * Connects to Raspberry Pi via Bluetooth.
     * Note: Python services must be running on remote device.
     */
    public static void connectToRaspberryPi() {
        logger.info("attempting to connect to controller...");
        try {
            // Request Bluetooth permissions
            checkAndRequestPermissions();

            // Ensure permissions are granted before proceeding
            for (String permission : BLUETOOTH_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                    showToast("Bluetooth permissions are required");
                    return;
                }
            }

            // Connect to device and get input stream
            BluetoothInterface.isConnecting = true;
            mArrayListIBUSActivity = new ArrayList<>();
            mBluetoothDevice = mBluetoothHelper.getAdapter().getRemoteDevice(remoteBluetoothAddress);
            logger.info("Attempting to create socket to service UUID");
            mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(serviceUUID);
            logger.info("Socket created, attempting to connect");
            mBluetoothSocket.connect();
            logger.info("Socket connected");
            mBluetoothInputStream = mBluetoothSocket.getInputStream();
            mBluetoothOutputStream = mBluetoothSocket.getOutputStream();
            BluetoothInterface.isConnecting = false;

            // Start listening for data on new thread
            logger.info("starting connected thread...");
            listenThread = new ConnectedThread();
            listenThread.start();
        } catch (Exception e) {
            BluetoothInterface.isConnecting = true;
            logger.info("exception connecting to controller: " + e.getMessage());
            showToast("Unable To Connect via Bluetooth");
        }
    }

    /**
     * Determines if Bluetooth connection has been established with Raspberry Pi
     * @return boolean
     */
    public static boolean isConnected() {
        return mBluetoothHelper.isBluetoothSupported() && mBluetoothDevice != null && mBluetoothSocket.isConnected();
    }

    /**
     * Determines if Bluetooth is in progress with establishing connection
     * @return boolean
     */
    public static boolean isConnecting() {
        return isConnecting;
    }

    /**
     * Checks Bluetooth connection, and connects if necessary.
     */
    public static void checkConnection() {
        if (!BluetoothInterface.isConnected()) {
            if (!mBluetoothHelper.isBluetoothEnabled()) {
                showToast("Bluetooth is not enabled or not available");
                return;
            }
            BluetoothInterface.connectToRaspberryPi();
        }
    }

    /**
     * Disconnect Bluetooth RFCOMM connection
     */
    public static void disconnect() {
        try {
            mBluetoothSocket.close();
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {
            mBluetoothDevice = null;
            mBluetoothSocket = null;
        }
    }

    /**
     * Checks and requests Bluetooth permissions
     */
    private static void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : BLUETOOTH_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(mActivity,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    /**
     * Show toast message on main thread
     */
    private static void showToast(String message) {
        mActivity.runOnUiThread(() -> {
            Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
        });
    }
}
