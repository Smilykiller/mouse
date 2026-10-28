package com.example.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    private var multicastLock: WifiManager.MulticastLock? = null

    fun acquireMulticastLock(context: Context) {
        try {
            if (multicastLock == null) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifiManager?.createMulticastLock("AirGlideMulticastLock")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (_: Exception) {
        }
    }

    fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {
        }
        multicastLock = null
    }

    private class LocalAddress(
        val interfaceName: String,
        val address: Inet4Address,
        val broadcast: InetAddress?
    )

    private fun isCellularInterface(name: String): Boolean {
        val n = name.lowercase()
        return n.startsWith("rmnet") || n.startsWith("ccmni") || n.startsWith("pdp") ||
            n.startsWith("v4-rmnet") || n.startsWith("tun") || n.startsWith("clat")
    }

    private fun isLanInterface(name: String): Boolean {
        val n = name.lowercase()
        return n.startsWith("wlan") || n.startsWith("swlan") || n.startsWith("ap") ||
            n.startsWith("eth") || n.startsWith("p2p")
    }

    /** All usable IPv4 addresses of this phone (Wi-Fi, hotspot, ethernet, ...). */
    private fun localIpv4Addresses(): List<LocalAddress> {
        val result = mutableListOf<LocalAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
            for (intf in Collections.list(interfaces)) {
                if (intf.isLoopback || !intf.isUp) continue
                val name = intf.name ?: ""
                for (ia in intf.interfaceAddresses) {
                    val addr = ia.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        result.add(LocalAddress(name, addr, ia.broadcast))
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    /**
     * The phone's address on the local network. Prefers Wi-Fi / hotspot / ethernet
     * and skips mobile-data interfaces (their address is useless for a LAN).
     */
    fun getLocalIpAddress(): String {
        val all = localIpv4Addresses()
        val best = all.firstOrNull { isLanInterface(it.interfaceName) && it.address.isSiteLocalAddress }
            ?: all.firstOrNull { it.address.isSiteLocalAddress && !isCellularInterface(it.interfaceName) }
            ?: all.firstOrNull { !isCellularInterface(it.interfaceName) }
        return best?.address?.hostAddress ?: "127.0.0.1"
    }

    /** True when the phone has a private LAN address (joined to Wi-Fi OR hosting a hotspot). */
    fun hasLocalNetwork(): Boolean {
        return localIpv4Addresses().any {
            it.address.isSiteLocalAddress && !isCellularInterface(it.interfaceName)
        }
    }

    /**
     * Every broadcast address we should ping: the directed broadcast of each LAN
     * interface plus the limited broadcast 255.255.255.255.
     */
    fun getBroadcastAddresses(): List<InetAddress> {
        val result = LinkedHashSet<InetAddress>()
        for (local in localIpv4Addresses()) {
            if (isCellularInterface(local.interfaceName)) continue
            val b = local.broadcast
            if (b != null) result.add(b)
        }
        try {
            result.add(InetAddress.getByName("255.255.255.255"))
        } catch (_: Exception) {
        }
        return result.toList()
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isWifiHardwareEnabled(context: Context): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.isWifiEnabled == true
        } catch (_: Exception) {
            true
        }
    }

    fun isBluetoothHardwareEnabled(context: Context): Boolean {
        return try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
            adapter?.isEnabled == true
        } catch (_: Exception) {
            false
        }
    }

    /** Wi-Fi connected, OR this phone is hosting a hotspot / has a LAN address. */
    fun isWifiOrHotspotConnected(context: Context): Boolean {
        return isWifiConnected(context) || hasLocalNetwork()
    }

    fun suggestSubnetHostIp(localIp: String): String {
        val parts = localIp.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.${parts[2]}.1"
        } else {
            "192.168.1.1"
        }
    }
}
