package com.example.network

import android.util.Log
import com.example.model.ClickButton
import com.example.model.ConnectedDevice
import com.example.model.RemoteCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class UdpHostServer(
    private val scope: CoroutineScope
) {
    private val tag = "AirGlideHostServer"
    private var socket: DatagramSocket? = null
    private var listenJob: Job? = null

    val isRunning = MutableStateFlow(false)
    val listeningPort = MutableStateFlow(5055)

    private val _devices = MutableStateFlow<Map<String, ConnectedDevice>>(emptyMap())
    val devices = _devices.asStateFlow()

    private val _activeDeviceId = MutableStateFlow<String?>(null)
    val activeDeviceId = _activeDeviceId.asStateFlow()

    // Host simulated cursor for live testing & tablet host mode
    val cursorX = MutableStateFlow(500f)
    val cursorY = MutableStateFlow(500f)
    val lastClickEvent = MutableStateFlow<ClickButton?>(null)
    val lastScrollDelta = MutableStateFlow(0f)
    val lastReceivedKey = MutableStateFlow<String?>(null)

    private val _packetLog = MutableStateFlow<List<String>>(emptyList())
    val packetLog = _packetLog.asStateFlow()

    fun start(port: Int = 5055) {
        if (isRunning.value) return
        listenJob = scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                listeningPort.value = port
                isRunning.value = true
                Log.i(tag, "Host server listening on UDP port $port")

                val buffer = ByteArray(1024)
                while (isActive && socket?.isClosed == false) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val message = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    val senderIp = packet.address.hostAddress ?: "Unknown"

                    processPacket(message, senderIp)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(tag, "Host server listening error", e)
                }
            } finally {
                isRunning.value = false
            }
        }
    }

    private fun processPacket(message: String, senderIp: String) {
        // Wire format:
        // P:<deviceId>:<deviceName>
        // M:<dx>,<dy>:<deviceId>
        // C:<btn>:<deviceId>
        // D:<btn>:<deviceId>
        // U:<btn>:<deviceId>
        // S:<dy>:<deviceId>
        // K:<key>:<deviceId>
        // T:<text>:<deviceId>

        val parts = message.split(":")
        if (parts.size < 2) return

        val cmdType = parts[0]

        when (cmdType) {
            "P" -> {
                if (parts.size >= 3) {
                    val devId = parts[1]
                    val devName = parts[2]
                    registerOrUpdateDevice(devId, devName, senderIp)
                }
            }
            "M" -> {
                val devId = if (parts.size >= 3) parts[2] else "unknown"
                registerOrUpdateDevice(devId, "Controller $senderIp", senderIp)
                if (isDeviceAllowed(devId)) {
                    val coords = parts[1].split(",")
                    if (coords.size == 2) {
                        val dx = coords[0].toFloatOrNull() ?: 0f
                        val dy = coords[1].toFloatOrNull() ?: 0f
                        cursorX.value = (cursorX.value + dx).coerceIn(0f, 1920f)
                        cursorY.value = (cursorY.value + dy).coerceIn(0f, 1080f)
                        logPacket("[$devId] MOVE dx=$dx, dy=$dy")
                    }
                } else {
                    logPacket("[$devId] BLOCKED (Not active device)")
                }
            }
            "C" -> {
                val devId = if (parts.size >= 3) parts[2] else "unknown"
                if (isDeviceAllowed(devId)) {
                    val btn = when (parts[1]) {
                        "L" -> ClickButton.LEFT
                        "R" -> ClickButton.RIGHT
                        "M" -> ClickButton.MIDDLE
                        else -> ClickButton.LEFT
                    }
                    lastClickEvent.value = btn
                    logPacket("[$devId] CLICK ${btn.label}")
                }
            }
            "S" -> {
                val devId = if (parts.size >= 3) parts[2] else "unknown"
                if (isDeviceAllowed(devId)) {
                    val dy = parts[1].toFloatOrNull() ?: 0f
                    lastScrollDelta.value = dy
                    logPacket("[$devId] SCROLL dy=$dy")
                }
            }
            "K" -> {
                val devId = if (parts.size >= 3) parts[2] else "unknown"
                if (isDeviceAllowed(devId)) {
                    val key = parts[1]
                    lastReceivedKey.value = key
                    logPacket("[$devId] KEY $key")
                }
            }
            "T" -> {
                val devId = if (parts.size >= 3) parts[2] else "unknown"
                if (isDeviceAllowed(devId)) {
                    val text = parts[1]
                    lastReceivedKey.value = text
                    logPacket("[$devId] TEXT \"$text\"")
                }
            }
        }
    }

    private fun registerOrUpdateDevice(devId: String, devName: String, senderIp: String) {
        val current = _devices.value.toMutableMap()
        val existing = current[devId]
        val activeId = _activeDeviceId.value

        val updated = if (existing != null) {
            existing.copy(
                deviceName = devName.ifBlank { existing.deviceName },
                ipAddress = senderIp,
                lastSeenMs = System.currentTimeMillis(),
                packetsReceived = existing.packetsReceived + 1,
                isActiveSelected = (devId == activeId)
            )
        } else {
            // If first device discovered and no active device is set yet, auto-select it!
            if (activeId == null) {
                _activeDeviceId.value = devId
            }
            ConnectedDevice(
                deviceId = devId,
                deviceName = devName,
                ipAddress = senderIp,
                lastSeenMs = System.currentTimeMillis(),
                packetsReceived = 1,
                isActiveSelected = (devId == _activeDeviceId.value)
            )
        }
        current[devId] = updated
        _devices.value = current
    }

    fun setActiveDevice(deviceId: String) {
        _activeDeviceId.value = deviceId
        val updated = _devices.value.mapValues { (id, dev) ->
            dev.copy(isActiveSelected = (id == deviceId))
        }
        _devices.value = updated
        logPacket("ACTIVE DEVICE LOCKED: $deviceId")
    }

    private fun isDeviceAllowed(devId: String): Boolean {
        val active = _activeDeviceId.value ?: return true // If none specifically locked, allow first
        return devId == active
    }

    private fun logPacket(entry: String) {
        val current = _packetLog.value.toMutableList()
        current.add(0, entry)
        if (current.size > 25) {
            current.removeAt(current.size - 1)
        }
        _packetLog.value = current
    }

    fun stop() {
        listenJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        isRunning.value = false
    }
}
