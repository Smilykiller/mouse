package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.model.BluetoothDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

/**
 * Handles Bluetooth RFCOMM connection to the Windows desktop receiver or other hosts.
 * Enables zero-Wi-Fi control of mouse and keyboard when mobile data/Wi-Fi is absent.
 */
class BluetoothClient(
    private val deviceId: String,
    private val deviceName: String,
    private val scope: CoroutineScope
) {
    private val tag = "AirGlideBluetooth"

    // Standard Bluetooth Serial Port Profile (SPP) UUID
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var outputStream: OutputStream? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _packetsSent = MutableStateFlow(0L)
    val packetsSent: StateFlow<Long> = _packetsSent.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val packetChannel = Channel<String>(capacity = 256)

    private var connectJob: Job? = null
    private var sendJob: Job? = null
    private var receiveJob: Job? = null

    init {
        startSendLoop()
    }

    /**
     * Retrieves all bonded (paired) Bluetooth devices on this Android phone.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        val adapter = try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    isPaired = true
                )
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to query bonded devices: ${e.message}")
            emptyList()
        }
    }

    /**
     * Connect to a target Bluetooth device by its MAC address.
     */
    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, targetDeviceName: String) {
        disconnect()

        connectJob = scope.launch(Dispatchers.IO) {
            _connectionStatus.value = "Connecting to $targetDeviceName..."
            _lastError.value = null

            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                _connectionStatus.value = "Bluetooth is disabled"
                _lastError.value = "Please turn on Bluetooth in device settings."
                return@launch
            }

            try {
                // Cancel discovery before connecting as it slows down connection
                try { adapter.cancelDiscovery() } catch (_: Exception) {}

                val device = adapter.getRemoteDevice(deviceAddress)
                var connectedSocket: BluetoothSocket? = null

                // Attempt 1: Standard RFCOMM SPP socket
                try {
                    val s = device.createRfcommSocketToServiceRecord(sppUuid)
                    s.connect()
                    connectedSocket = s
                } catch (e1: Exception) {
                    Log.w(tag, "Standard SPP connection failed, trying fallback: ${e1.message}")
                    // Attempt 2: Reflection fallback for port 1 (broadest Windows compatibility)
                    try {
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        val s = m.invoke(device, 1) as BluetoothSocket
                        s.connect()
                        connectedSocket = s
                    } catch (e2: Exception) {
                        Log.e(tag, "Fallback connection failed: ${e2.message}")
                        throw e1
                    }
                }

                socket = connectedSocket
                outputStream = connectedSocket.outputStream
                _isConnected.value = true
                _connectedDeviceName.value = targetDeviceName
                _connectionStatus.value = "Connected to $targetDeviceName"

                // Send initial presence handshake
                send("P:$deviceId:${deviceName.replace(":", "-")}")

                startReceiveLoop(connectedSocket)
            } catch (e: Exception) {
                Log.e(tag, "Bluetooth connect failed: ${e.message}")
                _isConnected.value = false
                _connectionStatus.value = "Connection failed"
                _lastError.value = e.localizedMessage ?: "Failed to connect to $targetDeviceName"
                disconnect()
            }
        }
    }

    /**
     * Send command payload to Bluetooth host.
     */
    fun send(payload: String) {
        if (!_isConnected.value) return
        packetChannel.trySend(payload)
    }

    private fun startSendLoop() {
        sendJob?.cancel()
        sendJob = scope.launch(Dispatchers.IO) {
            for (payload in packetChannel) {
                val out = outputStream ?: continue
                try {
                    val data = (payload + "\n").toByteArray(Charsets.UTF_8)
                    out.write(data)
                    out.flush()
                    _packetsSent.value += 1
                } catch (e: Exception) {
                    Log.w(tag, "Bluetooth send error: ${e.message}")
                    _lastError.value = "Send error: ${e.localizedMessage}"
                    disconnect()
                    break
                }
            }
        }
    }

    private fun startReceiveLoop(btSocket: BluetoothSocket) {
        receiveJob?.cancel()
        receiveJob = scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(btSocket.inputStream, Charsets.UTF_8))
                while (isActive && btSocket.isConnected) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("AIRGLIDE_ACK")) {
                        // Confirmed handshake or reply
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Bluetooth reader terminated: ${e.message}")
            } finally {
                if (_isConnected.value) {
                    disconnect()
                }
            }
        }
    }

    /**
     * Close the connection and reset status.
     */
    fun disconnect() {
        try {
            send("X:bye:$deviceId")
        } catch (_: Exception) {}

        try { socket?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}

        socket = null
        outputStream = null
        _isConnected.value = false
        _connectedDeviceName.value = null
        if (_connectionStatus.value.startsWith("Connected")) {
            _connectionStatus.value = "Disconnected"
        }
        connectJob?.cancel()
        receiveJob?.cancel()
    }
}
