package com.example.network

import android.content.Context
import android.util.Log
import com.example.model.DiscoveredHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class NetworkDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val tag = "AirGlideDiscovery"
    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts = _discoveredHosts.asStateFlow()

    private var listenJob: Job? = null
    private var pingJob: Job? = null
    private var socket: DatagramSocket? = null

    fun start() {
        if (listenJob?.isActive == true) return
        NetworkUtils.acquireMulticastLock(context)

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                // Try binding to discovery port 5056 or ephemeral
                socket = try {
                    DatagramSocket(null).apply {
                        reuseAddress = true
                        broadcast = true
                        bind(InetSocketAddress(5056))
                    }
                } catch (_: Exception) {
                    DatagramSocket().apply { broadcast = true }
                }

                val buffer = ByteArray(1024)
                while (isActive && socket?.isClosed == false) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    val senderIp = packet.address.hostAddress ?: ""

                    processDiscoveryMessage(msg, senderIp)
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(tag, "Discovery listener closed: ${e.message}")
                }
            }
        }

        // Periodic ping + cleanup of stale hosts
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                sendDiscoveryPing()
                cleanStaleHosts()
                delay(3000)
            }
        }
    }

    fun triggerScan() {
        scope.launch(Dispatchers.IO) {
            sendDiscoveryPing()
        }
    }

    private fun sendDiscoveryPing() {
        try {
            val broadcastAddr = NetworkUtils.getBroadcastAddress()
            val pingMsg = "AIRGLIDE_DISCOVERY_PING".toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(pingMsg, pingMsg.size, broadcastAddr, 5055)

            // Send via existing or temporary socket
            val s = socket ?: DatagramSocket().also { it.broadcast = true }
            s.send(packet)
        } catch (e: Exception) {
            Log.d(tag, "Ping error: ${e.message}")
        }
    }

    private fun processDiscoveryMessage(msg: String, senderIp: String) {
        // Expected formats:
        // AIRGLIDE_BEACON:<hostname>:<ip>:<port>
        // AIRGLIDE_SERVER_OFFER:<hostname>:<ip>:<port>
        if (msg.startsWith("AIRGLIDE_BEACON:") || msg.startsWith("AIRGLIDE_SERVER_OFFER:")) {
            val parts = msg.split(":")
            if (parts.size >= 4) {
                val hostName = parts[1]
                val ip = if (parts[2].isNotBlank() && parts[2] != "127.0.0.1") parts[2] else senderIp
                val port = parts[3].toIntOrNull() ?: 5055

                val host = DiscoveredHost(
                    hostName = hostName,
                    ipAddress = ip,
                    port = port,
                    lastSeenMs = System.currentTimeMillis()
                )

                val current = _discoveredHosts.value.toMutableList()
                val existingIndex = current.indexOfFirst { it.ipAddress == ip && it.port == port }
                if (existingIndex >= 0) {
                    current[existingIndex] = host
                } else {
                    current.add(0, host)
                }
                _discoveredHosts.value = current
                Log.i(tag, "Discovered AirGlide Host: $hostName ($ip:$port)")
            }
        }
    }

    private fun cleanStaleHosts() {
        val now = System.currentTimeMillis()
        val current = _discoveredHosts.value
        val fresh = current.filter { (now - it.lastSeenMs) < 12000 }
        if (fresh.size != current.size) {
            _discoveredHosts.value = fresh
        }
    }

    fun stop() {
        pingJob?.cancel()
        listenJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        NetworkUtils.releaseMulticastLock()
    }
}
