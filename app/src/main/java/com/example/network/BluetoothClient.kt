package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

/**
 * Handles Bluetooth RFCOMM connection to the Windows desktop receiver.
 * Employs a multi-tier fallback architecture:
 * 1. Dedicated AirGlide custom UUID (insecure & secure)
 * 2. Standard Serial Port Profile (SPP) UUID (insecure & secure)
 * 3. Direct RFCOMM channel reflection scanning (channels 1 through 5)
 * Guarantees reliable zero-Wi-Fi low latency control of mouse and keyboard.
 */
class BluetoothClient(
    private val deviceId: String,
    private val deviceName: String,
    private val scope: CoroutineScope
) {
    private val tag = "AirGlideBluetooth"

    // Dedicated AirGlide RFCOMM service UUID (unblocked by Windows kernel COM drivers)
    private val airGlideUuid: UUID = UUID.fromString("4a9b0001-1234-4abc-8def-0123456789ab")

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

    private val _telemetry = MutableStateFlow<com.example.model.LaptopTelemetry?>(null)
    val telemetry: StateFlow<com.example.model.LaptopTelemetry?> = _telemetry.asStateFlow()

    private val packetChannel = Channel<String>(capacity = 1024)

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
     * Connect to a target Bluetooth device by its MAC address using multi-tier connection strategies.
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
                // Cancel ongoing discovery before connecting to prevent RFCOMM bandwidth throttling
                try { adapter.cancelDiscovery() } catch (_: Exception) {}

                val device = adapter.getRemoteDevice(deviceAddress)
                val connectedSocket = establishSocket(device)

                socket = connectedSocket
                outputStream = connectedSocket.outputStream
                _isConnected.value = true
                _connectedDeviceName.value = targetDeviceName
                _connectionStatus.value = "Connected to $targetDeviceName"
                _lastError.value = null

                // Send immediate presence handshake
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

    @SuppressLint("MissingPermission")
    private fun establishSocket(device: BluetoothDevice): BluetoothSocket {
        val errors = mutableListOf<String>()

        // Strategy 1: Dedicated AirGlide Insecure RFCOMM (Matches Windows StreamSocket null-auth)
        try {
            val s = device.createInsecureRfcommSocketToServiceRecord(airGlideUuid)
            s.connect()
            Log.i(tag, "Connected via AirGlide Insecure RFCOMM")
            return s
        } catch (e: Exception) {
            errors.add("AirGlide Insecure: ${e.message}")
        }

        // Strategy 2: Dedicated AirGlide Secure RFCOMM
        try {
            val s = device.createRfcommSocketToServiceRecord(airGlideUuid)
            s.connect()
            Log.i(tag, "Connected via AirGlide Secure RFCOMM")
            return s
        } catch (e: Exception) {
            errors.add("AirGlide Secure: ${e.message}")
        }

        // Strategy 3: Standard SPP Insecure RFCOMM
        try {
            val s = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
            s.connect()
            Log.i(tag, "Connected via Standard SPP Insecure RFCOMM")
            return s
        } catch (e: Exception) {
            errors.add("SPP Insecure: ${e.message}")
        }

        // Strategy 4: Standard SPP Secure RFCOMM
        try {
            val s = device.createRfcommSocketToServiceRecord(sppUuid)
            s.connect()
            Log.i(tag, "Connected via Standard SPP Secure RFCOMM")
            return s
        } catch (e: Exception) {
            errors.add("SPP Secure: ${e.message}")
        }

        // Strategy 5: Insecure Reflection Channel scan (ports 1 to 5)
        for (port in 1..5) {
            try {
                val m = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                val s = m.invoke(device, port) as BluetoothSocket
                s.connect()
                Log.i(tag, "Connected via Insecure Reflection Channel $port")
                return s
            } catch (_: Exception) {}
        }

        // Strategy 6: Secure Reflection Channel scan (ports 1 to 5)
        for (port in 1..5) {
            try {
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                val s = m.invoke(device, port) as BluetoothSocket
                s.connect()
                Log.i(tag, "Connected via Secure Reflection Channel $port")
                return s
            } catch (_: Exception) {}
        }

        throw IOException("Could not connect to ${device.name ?: device.address}. Please ensure the laptop has AirGlide receiver open.\n" + errors.joinToString("\n"))
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
                        val parts = line.split(":")
                        if (parts.size >= 7) {
                            val bat = parts[3].toIntOrNull() ?: 100
                            val isChg = parts[4] == "1"
                            val cpu = parts[5].toIntOrNull() ?: 0
                            val ram = parts[6].toIntOrNull() ?: 0
                            _telemetry.value = com.example.model.LaptopTelemetry(
                                batteryPct = bat,
                                isCharging = isChg,
                                cpuPct = cpu,
                                ramPct = ram,
                                lastUpdatedMs = System.currentTimeMillis()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Bluetooth reader ended: ${e.message}")
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
