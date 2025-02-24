package com.trentseed.bmw_rpi_ibus_controller.common;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread that handles Bluetooth RFCOMM channel reading from Raspberry Pi
 */
class ConnectedThread extends Thread {

    public class Config {
        public static final int BLUETOOTH_BUFFER_SIZE = 1024; // Example default
    }

    public class InvalidJSONException extends Exception {
        public InvalidJSONException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public void run() {
        while (true) {
            try {
                byte[] buffer = new byte[Config.BLUETOOTH_BUFFER_SIZE];
                int bytes = BluetoothInterface.mBluetoothInputStream.read(buffer);

                if (bytes == -1) {
                    Log.w("BMW", "Bluetooth stream closed.");
                    BluetoothInterface.checkConnection();
                    break;
                }

                if (bytes == 0) {
                    Log.d("BMW", "No data received from Bluetooth.");
                    continue;
                }

                String strBuffer = new String(buffer, 0, bytes).trim();
                validateJSON(strBuffer);

                Log.d("BMW", "Data In (length " + bytes + ") = " + strBuffer.substring(0, Math.min(strBuffer.length(), 100)) + "...");

                ControllerMessage msg = new Gson().fromJson(strBuffer, ControllerMessage.class);
                List<IBUSPacket> newPackets = new ArrayList<>();
                for (final IBUSPacket ibPacket : msg.getIBUSPackets()) {
                    newPackets.add(ibPacket);
                }
                // Trigger the callback
                if (BluetoothInterface.mIBUSPacketListener != null) {
                    BluetoothInterface.mIBUSPacketListener.onIBUSPacketsReceived(newPackets);
                }

            } catch (InvalidJSONException e) {
                Log.e("BMW", "Invalid JSON received: " + e.getMessage(), e);
                // Consider if you want to continue or break here
            } catch (IOException e) {
                Log.e("BMW", "Error reading from Bluetooth: " + e.getMessage(), e);
                BluetoothInterface.checkConnection();
                // Consider retrying a few times before giving up
                // Or notify the user about the error
                break; // Or return, depending on your error handling strategy
            }
        }
    }

    /**
     * Validates if the provided string input is JSON parsable.
     *
     * @param json The string to validate.
     * @throws InvalidJSONException If the JSON is invalid or empty.
     */
    private void validateJSON(String json) throws InvalidJSONException {
        if (json == null || json.trim().isEmpty()) {
            throw new InvalidJSONException("JSON string is empty or null", null);
        }
        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException jse) {
            throw new InvalidJSONException("Invalid JSON syntax", jse);
        }
    }
}