package com.trentseed.bmw_rpi_ibus_controller.common

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BluetoothConnectionManager(
    private val context: Context,
    private val device: BluetoothDevice,
    private val serviceUUID: UUID,
    private val listener: ConnectionListener
) {

    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onDataReceived(data: String)
        fun onConnecting()
        fun onPermissionsDenied()
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readThread: Job? = null
    private var heartbeatJob: Job? = null
    private val connectionScope = CoroutineScope(Dispatchers.IO)
    var isConnected = false
    val isConnecting = AtomicBoolean(false)
    private val HEARTBEAT_TIMEOUT = 10000L // 10 seconds
    private val RECONNECT_DELAY_BASE = 5000L // 5 seconds
    private val MAX_RECONNECT_ATTEMPTS = 5
    private var reconnectAttempts = 0
    private var lastHeartbeatReceived = System.currentTimeMillis()
    private val socketLock = ReentrantLock()

    private val BLUETOOTH_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    fun connect() {
        if (!checkPermissions()) {
            listener.onPermissionsDenied()
            return
        }
        if (isConnecting.get() || isConnected) return
        isConnecting.set(true)
        listener.onConnecting()
        connectionScope.launch {
            connectInternal()
        }
    }

    private suspend fun connectInternal() {
        try {
            socketLock.withLock {
                bluetoothSocket = if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    device.createInsecureRfcommSocketToServiceRecord(serviceUUID)
                } else {
                    null
                }
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
                isConnected = true
                reconnectAttempts = 0
            }
            withContext(Dispatchers.Main) {
                listener.onConnected()
                showToast("Connected to ${device.name}")
            }
            startReading()
            startHeartbeatMonitoring()
        } catch (e: IOException) {
            socketLock.withLock {
                isConnecting.set(false)
                if (!isConnected) {
                    bluetoothSocket?.close()
                    bluetoothSocket = null
                }
            }
            withContext(Dispatchers.Main) {
                showToast("Connection failed: ${e.message}")
            }
            Log.e("BluetoothConnectionManager", "Connection failed", e)
            disconnect()
            reconnect()
        } finally {
            socketLock.withLock {
                if (!isConnected) {
                    bluetoothSocket?.close()
                    bluetoothSocket = null
                }
            }
        }
    }

    private fun startReading() {
        readThread = connectionScope.launch {
            val buffer = ByteArray(2048)
            while (isActive && isConnected) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val receivedData = String(buffer, 0, bytes)
                        if (isHeartbeat(receivedData)) {
                            lastHeartbeatReceived = System.currentTimeMillis()
                            sendHeartbeatAck()
                        } else {
                            withContext(Dispatchers.Main) {
                                listener.onDataReceived(receivedData)
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e("BluetoothConnectionManager", "Error reading data", e)
                    disconnect()
                    reconnect()
                }
            }
        }
    }

    private fun startHeartbeatMonitoring() {
        heartbeatJob = connectionScope.launch {
            while (isActive && isConnected) {
                delay(HEARTBEAT_TIMEOUT)
                if (System.currentTimeMillis() - lastHeartbeatReceived > HEARTBEAT_TIMEOUT) {
                    Log.e("BluetoothConnectionManager", "Heartbeat timeout")
                    disconnect()
                    reconnect()
                }
            }
        }
    }

    private fun isHeartbeat(data: String): Boolean {
        return try {
            val json = JSONObject(data)
            json.getString("type") == "heartbeat"
        } catch (e: Exception) {
            false
        }
    }

    private fun sendHeartbeatAck() {
        try {
            val heartbeatAck = JSONObject().apply {
                put("type", "heartbeat_ack")
            }.toString()
            outputStream?.write(heartbeatAck.toByteArray())
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("BluetoothConnectionManager", "Error sending heartbeat ack", e)
        }
    }

    private fun reconnect() {
        Log.i("BluetoothConnectionManager", "reconnect: " + isConnecting.get() + " " + isConnected)
        if (isConnecting.get() || isConnected) {
            Log.e("BluetoothConnectionManager", "reconnect: isConnecting or isConnected")
            return
        }
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            val delayMillis = RECONNECT_DELAY_BASE * reconnectAttempts
            connectionScope.launch {
                delay(delayMillis)
                withContext(Dispatchers.Main) {
                    showToast("Attempting to reconnect... (Attempt $reconnectAttempts)")
                }
                connectInternal()
            }
        } else {
            connectionScope.launch {
                withContext(Dispatchers.Main) {
                    showToast("Max reconnect attempts reached. Please check the connection.")
                }
            }
        }
    }

    fun sendData(data: String) {
        connectionScope.launch {
            try {
                outputStream?.write(data.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                Log.e("BluetoothConnectionManager", "Error sending data", e)
                disconnect()
                reconnect()
            }
        }
    }

    fun disconnect() {
        connectionScope.launch {
            disconnectInternal()
        }
    }

    private suspend fun disconnectInternal() {
        var disconnected = false
        try {
            socketLock.withLock {
                Log.i("BluetoothConnectionManager", "disconnectInternal: " + isConnecting.get())
                if (!isConnected && !isConnecting.get()) return
                isConnected = false
                isConnecting.set(false)
                readThread?.cancel()
                heartbeatJob?.cancel()
                bluetoothSocket?.close()
                disconnected = true
            }
        } catch (e: IOException) {
            Log.e("BluetoothConnectionManager", "Error closing socket", e)
        } finally {
            socketLock.withLock {
                bluetoothSocket = null
                inputStream = null
                outputStream = null
            }
            if (disconnected) {
                withContext(Dispatchers.Main) {
                    listener.onDisconnected()
                    showToast("Disconnected")
                }
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        Log.e("BluetoothConnectionManager", "TOAST: $message")
    }

    private fun checkPermissions(): Boolean {
        val permissionsToRequest = BLUETOOTH_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                context as androidx.activity.ComponentActivity,
                permissionsToRequest.toTypedArray(),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
            return false
        }
        return true
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                connect()
            } else {
                listener.onPermissionsDenied() // Call this if permissions are denied
            }
        }
    }
}