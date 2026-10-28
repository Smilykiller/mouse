package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppRole
import com.example.model.AppScreen
import com.example.model.AppSettings
import com.example.model.ClickButton
import com.example.model.ConnectedDevice
import com.example.model.ConnectionMode
import com.example.model.DeviceHistoryItem
import com.example.model.DiscoveredHost
import com.example.model.RemoteCommand
import com.example.model.TouchpadLayout
import com.example.network.NetworkDiscoveryManager
import com.example.network.NetworkUtils
import com.example.network.UdpClient
import com.example.network.UdpHostServer
import com.example.sensor.AirMotionSensorManager
import com.example.util.HapticFeedbackHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AirGlideUiState(
    val currentScreen: AppScreen = AppScreen.TRACKPAD,
    val role: AppRole = AppRole.CONTROLLER,
    val layout: TouchpadLayout = TouchpadLayout.SPLIT,
    val connectionMode: ConnectionMode = ConnectionMode.WIFI_UDP,
    val settings: AppSettings = AppSettings(),
    val localIp: String = "127.0.0.1",
    val packetsSent: Long = 0L,
    val statusMessage: String = "Ready to stream",
    val isKeyboardOpen: Boolean = false,
    val keyboardInputText: String = "",
    val dragLockActive: Boolean = false,
    val isConnected: Boolean = true,
    val isLiveConnected: Boolean = false,
    val activeHostName: String = "Laptop",
    val isAirMotionActive: Boolean = false,
    val useVolumeButtonsForNavigation: Boolean = true,
    val airMotionSensitivity: Float = 2.8f,
    // Device Connectivity & History
    val isWifiEnabled: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val isLocalNetworkActive: Boolean = true,
    val discoveredHosts: List<DiscoveredHost> = emptyList(),
    val deviceHistory: List<DeviceHistoryItem> = emptyList(),
    // Host Mode State
    val hostDevices: List<ConnectedDevice> = emptyList(),
    val hostActiveDeviceId: String? = null,
    val hostCursorPosition: Offset = Offset(400f, 400f),
    val hostClickFlash: ClickButton? = null,
    val hostScrollFlash: Float = 0f,
    val hostPacketLogs: List<String> = emptyList(),
    val isHostRunning: Boolean = false
)

class AirGlideViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("airglide_prefs", Context.MODE_PRIVATE)
    private val haptics = HapticFeedbackHelper(application)
    val airMotionSensor = AirMotionSensorManager(application)
    private val discoveryManager = NetworkDiscoveryManager(application, viewModelScope)

    private val deviceId: String
    private val deviceName: String

    private val udpClient: UdpClient
    private val udpHostServer: UdpHostServer

    private var scrollAccumulator: Float = 0f
    private var moveAccumulatorX: Float = 0f
    private var moveAccumulatorY: Float = 0f

    private val _uiState = MutableStateFlow(AirGlideUiState())
    val uiState: StateFlow<AirGlideUiState> = _uiState.asStateFlow()

    init {
        var savedId = sharedPrefs.getString("device_id", null)
        if (savedId == null) {
            savedId = UUID.randomUUID().toString().substring(0, 8)
            sharedPrefs.edit().putString("device_id", savedId).apply()
        }
        deviceId = savedId
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        val savedIp = sharedPrefs.getString("target_ip", "") ?: ""
        val savedPort = sharedPrefs.getInt("target_port", 5055)
        val savedSensitivity = sharedPrefs.getFloat("sensitivity", 1.3f)
        val savedInvertScroll = sharedPrefs.getBoolean("invert_scroll", false)
        val savedHaptics = sharedPrefs.getBoolean("haptics", true)
        val savedLayoutName = sharedPrefs.getString("layout", TouchpadLayout.SPLIT.name)
        val initialLayout = try {
            TouchpadLayout.valueOf(savedLayoutName ?: TouchpadLayout.SPLIT.name)
        } catch (_: Exception) {
            TouchpadLayout.SPLIT
        }

        val localIp = NetworkUtils.getLocalIpAddress()
        val defaultTargetIp = if (savedIp.isNotBlank()) savedIp else NetworkUtils.suggestSubnetHostIp(localIp)

        val initialSettings = AppSettings(
            targetHostIp = defaultTargetIp,
            targetPort = savedPort,
            sensitivity = savedSensitivity,
            invertScroll = savedInvertScroll,
            hapticFeedback = savedHaptics
        )

        udpClient = UdpClient(deviceId, deviceName, viewModelScope)
        udpHostServer = UdpHostServer(viewModelScope)

        val history = loadDeviceHistory(defaultTargetIp, savedPort)
        val wifiOn = NetworkUtils.isWifiHardwareEnabled(application)
        val btOn = NetworkUtils.isBluetoothHardwareEnabled(application)
        val netConnected = NetworkUtils.isWifiOrHotspotConnected(application)

        _uiState.update {
            it.copy(
                settings = initialSettings,
                layout = initialLayout,
                localIp = localIp,
                deviceHistory = history,
                isWifiEnabled = wifiOn,
                isBluetoothEnabled = btOn,
                isLocalNetworkActive = netConnected
            )
        }

        udpClient.configureTarget(defaultTargetIp, savedPort)

        // Start Auto-Discovery listener
        discoveryManager.start()

        // Observe discovered laptop beacons
        viewModelScope.launch {
            discoveryManager.discoveredHosts.collect { hosts ->
                val currentTarget = _uiState.value.settings.targetHostIp
                val isLive = hosts.any { it.ipAddress == currentTarget && (System.currentTimeMillis() - it.lastSeenMs) < 6000 }
                val activeName = hosts.find { it.ipAddress == currentTarget }?.hostName ?: "Laptop"

                // Auto-connect to detected laptop if current target is not active
                if (hosts.isNotEmpty()) {
                    val matchingHost = hosts.find { it.ipAddress == currentTarget }
                    if (matchingHost == null || !isLive) {
                        val firstHost = hosts.first()
                        setTargetHostIp(firstHost.ipAddress, firstHost.hostName)
                        setTargetPort(firstHost.port)
                    }
                }

                _uiState.update {
                    it.copy(
                        discoveredHosts = hosts,
                        isLiveConnected = isLive,
                        activeHostName = activeName
                    )
                }
            }
        }

        // Observe client packet counts
        viewModelScope.launch {
            udpClient.packetsSent.collect { count ->
                _uiState.update { it.copy(packetsSent = count) }
            }
        }

        // Observe host server state (if in host receiver role)
        viewModelScope.launch {
            udpHostServer.devices.collect { devMap ->
                _uiState.update { it.copy(hostDevices = devMap.values.toList()) }
            }
        }
        viewModelScope.launch {
            udpHostServer.activeDeviceId.collect { activeId ->
                _uiState.update { it.copy(hostActiveDeviceId = activeId) }
            }
        }
        viewModelScope.launch {
            udpHostServer.packetLog.collect { logs ->
                _uiState.update { it.copy(hostPacketLogs = logs) }
            }
        }
        viewModelScope.launch {
            udpHostServer.isRunning.collect { running ->
                _uiState.update { it.copy(isHostRunning = running) }
            }
        }
        viewModelScope.launch {
            udpHostServer.cursorX.collect { x ->
                val currentY = udpHostServer.cursorY.value
                _uiState.update { it.copy(hostCursorPosition = Offset(x, currentY)) }
            }
        }
        viewModelScope.launch {
            udpHostServer.lastClickEvent.collect { btn ->
                _uiState.update { it.copy(hostClickFlash = btn) }
            }
        }

        airMotionSensor.onMotionDelta = { dx, dy ->
            if (_uiState.value.isAirMotionActive) {
                onPointerMove(dx, dy)
            }
        }
    }

    // Navigation
    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
        refreshHardwareStatus()
        discoveryManager.triggerScan()
    }

    fun navigateBack() {
        val cur = _uiState.value.currentScreen
        if (cur != AppScreen.HUB_SETTINGS) {
            _uiState.update { it.copy(currentScreen = AppScreen.HUB_SETTINGS) }
        }
    }

    fun onPointerMove(rawDx: Float, rawDy: Float) {
        val sensitivity = _uiState.value.settings.sensitivity
        val speedSq = rawDx * rawDx + rawDy * rawDy
        val factor = if (speedSq > 150f) sensitivity * 1.25f else sensitivity

        moveAccumulatorX += rawDx * factor
        moveAccumulatorY += rawDy * factor

        val intDx = moveAccumulatorX.toInt()
        val intDy = moveAccumulatorY.toInt()

        if (intDx != 0 || intDy != 0) {
            moveAccumulatorX -= intDx
            moveAccumulatorY -= intDy
            udpClient.sendCommand(RemoteCommand.Move(intDx.toFloat(), intDy.toFloat()))
        }
    }

    fun onScroll(rawDy: Float) {
        val settings = _uiState.value.settings
        val inverted = if (settings.invertScroll) -1f else 1f
        // Accumulate fractional drag distance to prevent slow scrolls from being truncated to 0
        scrollAccumulator += rawDy * settings.scrollSensitivity * inverted * 0.25f

        val steps = scrollAccumulator.toInt()
        if (steps != 0) {
            scrollAccumulator -= steps
            if (settings.hapticFeedback) {
                haptics.scrollTick()
            }
            udpClient.sendCommand(RemoteCommand.Scroll(steps.toFloat()))
        }
    }

    fun onLeftClick() {
        if (_uiState.value.dragLockActive) {
            // Releasing held drag lock cleanly
            _uiState.update { it.copy(dragLockActive = false) }
            if (_uiState.value.settings.hapticFeedback) {
                haptics.heavyClick()
            }
            udpClient.sendCommand(RemoteCommand.MouseUp(ClickButton.LEFT))
        } else {
            if (_uiState.value.settings.hapticFeedback) {
                haptics.click()
            }
            udpClient.sendCommand(RemoteCommand.Click(ClickButton.LEFT))
        }
    }

    fun onRightClick() {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
        udpClient.sendCommand(RemoteCommand.Click(ClickButton.RIGHT))
    }

    fun onMiddleClick() {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        udpClient.sendCommand(RemoteCommand.Click(ClickButton.MIDDLE))
    }

    fun toggleDragLock() {
        val newLock = !_uiState.value.dragLockActive
        _uiState.update { it.copy(dragLockActive = newLock) }
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
        if (newLock) {
            udpClient.sendCommand(RemoteCommand.MouseDown(ClickButton.LEFT))
        } else {
            udpClient.sendCommand(RemoteCommand.MouseUp(ClickButton.LEFT))
        }
    }

    fun onKeyPress(key: String) {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        udpClient.sendCommand(RemoteCommand.Key(key))
    }

    fun onTextEntered(text: String) {
        if (text.isNotEmpty()) {
            udpClient.sendCommand(RemoteCommand.Text(text))
            _uiState.update { it.copy(keyboardInputText = "") }
        }
    }

    fun setKeyboardText(text: String) {
        _uiState.update { it.copy(keyboardInputText = text) }
    }

    fun toggleKeyboard() {
        _uiState.update { it.copy(isKeyboardOpen = !it.isKeyboardOpen) }
    }

    fun setTargetHostIp(ip: String, optionalName: String? = null) {
        val cleanIp = ip.trim()
        val hosts = _uiState.value.discoveredHosts
        val matchedName = optionalName ?: hosts.find { it.ipAddress == cleanIp }?.hostName ?: "Laptop"

        _uiState.update {
            it.copy(
                settings = it.settings.copy(targetHostIp = cleanIp),
                activeHostName = matchedName
            )
        }
        sharedPrefs.edit().putString("target_ip", cleanIp).apply()
        udpClient.configureTarget(cleanIp, _uiState.value.settings.targetPort)

        saveToDeviceHistory(cleanIp, _uiState.value.settings.targetPort, matchedName)
    }

    fun setTargetPort(port: Int) {
        _uiState.update {
            it.copy(settings = it.settings.copy(targetPort = port))
        }
        sharedPrefs.edit().putInt("target_port", port).apply()
        udpClient.configureTarget(_uiState.value.settings.targetHostIp, port)
    }

    fun connectToDiscoveredHost(host: DiscoveredHost) {
        setTargetHostIp(host.ipAddress, host.hostName)
        setTargetPort(host.port)
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
    }

    fun triggerDiscoveryScan() {
        discoveryManager.triggerScan()
    }

    fun setLayout(layout: TouchpadLayout) {
        _uiState.update { it.copy(layout = layout) }
        sharedPrefs.edit().putString("layout", layout.name).apply()
    }

    fun setRole(role: AppRole) {
        _uiState.update { it.copy(role = role) }
        if (role == AppRole.HOST) {
            udpHostServer.start(_uiState.value.settings.targetPort)
        } else {
            udpHostServer.stop()
        }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        _uiState.update { it.copy(connectionMode = mode) }
    }

    fun selectHostActiveDevice(deviceId: String) {
        udpHostServer.setActiveDevice(deviceId)
    }

    fun updateSensitivity(value: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(sensitivity = value)) }
        sharedPrefs.edit().putFloat("sensitivity", value).apply()
    }

    fun updateScrollSensitivity(value: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(scrollSensitivity = value)) }
        sharedPrefs.edit().putFloat("scroll_sensitivity", value).apply()
    }

    fun toggleInvertScroll() {
        val newInvert = !_uiState.value.settings.invertScroll
        _uiState.update { it.copy(settings = it.settings.copy(invertScroll = newInvert)) }
        sharedPrefs.edit().putBoolean("invert_scroll", newInvert).apply()
    }

    fun toggleHaptics() {
        val newHaptics = !_uiState.value.settings.hapticFeedback
        _uiState.update { it.copy(settings = it.settings.copy(hapticFeedback = newHaptics)) }
        sharedPrefs.edit().putBoolean("haptics", newHaptics).apply()
    }

    fun toggleAirMotion() {
        val active = airMotionSensor.toggle()
        _uiState.update { it.copy(isAirMotionActive = active) }
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
    }

    fun calibrateAirMotionCenter() {
        airMotionSensor.calibrateCenter()
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
    }

    fun updateAirMotionSensitivity(value: Float) {
        airMotionSensor.motionSensitivity = value
        _uiState.update { it.copy(airMotionSensitivity = value) }
    }

    fun toggleVolumeButtons() {
        _uiState.update { it.copy(useVolumeButtonsForNavigation = !it.useVolumeButtonsForNavigation) }
    }

    fun onVolumeUp(): Boolean {
        if (!_uiState.value.useVolumeButtonsForNavigation) return false
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        udpClient.sendCommand(RemoteCommand.Key("PAGE_UP"))
        return true
    }

    fun onVolumeDown(): Boolean {
        if (!_uiState.value.useVolumeButtonsForNavigation) return false
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        udpClient.sendCommand(RemoteCommand.Key("PAGE_DOWN"))
        return true
    }

    fun refreshHardwareStatus() {
        val app = getApplication<Application>()
        val wifiOn = NetworkUtils.isWifiHardwareEnabled(app)
        val btOn = NetworkUtils.isBluetoothHardwareEnabled(app)
        val netConnected = NetworkUtils.isWifiOrHotspotConnected(app)
        val localIp = NetworkUtils.getLocalIpAddress()

        _uiState.update {
            it.copy(
                isWifiEnabled = wifiOn,
                isBluetoothEnabled = btOn,
                isLocalNetworkActive = netConnected,
                localIp = localIp
            )
        }
    }

    private fun loadDeviceHistory(defaultIp: String, defaultPort: Int): List<DeviceHistoryItem> {
        val set = sharedPrefs.getStringSet("device_history", null)
        val items = mutableListOf<DeviceHistoryItem>()

        if (!set.isNullOrEmpty()) {
            for (raw in set) {
                val parts = raw.split("|")
                if (parts.size >= 3) {
                    val name = parts[0]
                    val ip = parts[1]
                    val port = parts[2].toIntOrNull() ?: 5055
                    val time = if (parts.size >= 4) parts[3].toLongOrNull() ?: 0L else 0L
                    items.add(DeviceHistoryItem(name, ip, port, time))
                }
            }
            items.sortByDescending { it.lastUsedMs }
        }

        if (items.isEmpty() && defaultIp.isNotBlank()) {
            val defaultItem = DeviceHistoryItem("Main Workstation", defaultIp, defaultPort, System.currentTimeMillis())
            items.add(defaultItem)
            saveHistorySet(items)
        }
        return items
    }

    fun saveToDeviceHistory(ip: String, port: Int, customName: String? = null) {
        val current = _uiState.value.deviceHistory.toMutableList()
        val existingIndex = current.indexOfFirst { it.ipAddress == ip && it.port == port }
        val name = customName ?: if (existingIndex >= 0) current[existingIndex].name else "Laptop (${ip.takeLast(6)})"

        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        }
        current.add(0, DeviceHistoryItem(name, ip, port, System.currentTimeMillis()))
        if (current.size > 8) {
            current.removeAt(current.size - 1)
        }
        _uiState.update { it.copy(deviceHistory = current) }
        saveHistorySet(current)
    }

    fun removeHistoryItem(item: DeviceHistoryItem) {
        val current = _uiState.value.deviceHistory.toMutableList()
        current.removeAll { it.ipAddress == item.ipAddress && it.port == item.port }
        _uiState.update { it.copy(deviceHistory = current) }
        saveHistorySet(current)
    }

    private fun saveHistorySet(items: List<DeviceHistoryItem>) {
        val set = items.map { "${it.name}|${it.ipAddress}|${it.port}|${it.lastUsedMs}" }.toSet()
        sharedPrefs.edit().putStringSet("device_history", set).apply()
    }

    override fun onCleared() {
        super.onCleared()
        airMotionSensor.stop()
        discoveryManager.stop()
        udpClient.close()
        udpHostServer.stop()
    }
}
