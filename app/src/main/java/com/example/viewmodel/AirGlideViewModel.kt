package com.example.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class AirGlideUiState(
    val currentScreen: AppScreen = AppScreen.TRACKPAD,
    val activeTab: com.example.model.AppNavTab = com.example.model.AppNavTab.TRACKPAD,
    val selectedTheme: com.example.model.MousePadTheme = com.example.model.MousePadTheme.STEALTH,
    val customImagePath: String? = null,
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
    // Ecosystem, Deck & Utilities State
    val autoScrollSpeed: Float = 0f,
    val isAutoScrolling: Boolean = false,
    val lastCopiedText: String = "",
    val fileTransferStatus: String? = null,
    val isFileTransferring: Boolean = false,
    val walkAwayLockEnabled: Boolean = false,
    val connectedDisplaysCount: Int = 1,
    val activeDisplayIndex: Int = 1,
    // Laptop Receiver Installer & Drop State
    val isWifiDropServerRunning: Boolean = false,
    val wifiDropServerUrl: String = "",
    val wifiDropDownloadCount: Int = 0,
    val wifiDropStatus: String? = null,
    // Device Connectivity & History
    val isWifiEnabled: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val isLocalNetworkActive: Boolean = true,
    val bluetoothDevices: List<com.example.model.BluetoothDeviceInfo> = emptyList(),
    val selectedBluetoothDevice: com.example.model.BluetoothDeviceInfo? = null,
    val bluetoothStatus: String = "Disconnected",
    val discoveredHosts: List<DiscoveredHost> = emptyList(),
    val deviceHistory: List<DeviceHistoryItem> = emptyList(),
    // Host Mode State
    val hostDevices: List<ConnectedDevice> = emptyList(),
    val hostActiveDeviceId: String? = null,
    val hostCursorPosition: Offset = Offset(400f, 400f),
    val hostClickFlash: ClickButton? = null,
    val hostScrollFlash: Float = 0f,
    val hostPacketLogs: List<String> = emptyList(),
    val isHostRunning: Boolean = false,
    val customQuickKeys: List<String> = com.example.model.KeyboardCatalog.DEFAULT_KEY_CODES
)

/** The laptop counts as "live" when we heard from it within this window. */
private const val LIVE_WINDOW_MS = 6000L

class AirGlideViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("airglide_prefs", Context.MODE_PRIVATE)
    private val haptics = HapticFeedbackHelper(application)
    val airMotionSensor = AirMotionSensorManager(application)
    private val discoveryManager = NetworkDiscoveryManager(application, viewModelScope)

    private val deviceId: String
    private val deviceName: String

    private val udpClient: UdpClient
    private val udpHostServer: UdpHostServer
    val bluetoothClient: com.example.network.BluetoothClient
    val wifiDropServer: com.example.installer.WifiDropServer

    private var scrollAccumulator: Float = 0f
    private var manualTarget: Boolean = false
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
        val savedScrollSensitivity = sharedPrefs.getFloat("scroll_sensitivity", 1.0f)
        manualTarget = sharedPrefs.getBoolean("manual_target", false)
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
            scrollSensitivity = savedScrollSensitivity,
            invertScroll = savedInvertScroll,
            hapticFeedback = savedHaptics
        )

        udpClient = UdpClient(deviceId, deviceName, viewModelScope)
        udpHostServer = UdpHostServer(viewModelScope)
        bluetoothClient = com.example.network.BluetoothClient(deviceId, deviceName, viewModelScope)
        wifiDropServer = com.example.installer.WifiDropServer(viewModelScope)

        val history = loadDeviceHistory(defaultTargetIp, savedPort)
        val wifiOn = NetworkUtils.isWifiHardwareEnabled(application)
        val btOn = NetworkUtils.isBluetoothHardwareEnabled(application)
        val netConnected = NetworkUtils.isWifiOrHotspotConnected(application)

        val savedKeysStr = sharedPrefs.getString("custom_quick_keys", null)
        val initialKeys = if (!savedKeysStr.isNullOrBlank()) {
            savedKeysStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(8)
        } else {
            com.example.model.KeyboardCatalog.DEFAULT_KEY_CODES
        }

        val savedThemeId = sharedPrefs.getString("trackpad_theme", com.example.model.MousePadTheme.STEALTH.id)
        val initialTheme = com.example.model.MousePadTheme.values().firstOrNull { it.id == savedThemeId }
            ?: com.example.model.MousePadTheme.STEALTH
        val savedImgPath = sharedPrefs.getString("custom_trackpad_image_path", null)

        val savedModeStr = sharedPrefs.getString("connection_mode", ConnectionMode.WIFI_UDP.name)
        val initialMode = try {
            ConnectionMode.valueOf(savedModeStr ?: ConnectionMode.WIFI_UDP.name)
        } catch (_: Exception) {
            ConnectionMode.WIFI_UDP
        }
        val lastBtAddr = sharedPrefs.getString("last_bt_address", null)
        val lastBtName = sharedPrefs.getString("last_bt_name", null)

        _uiState.update {
            it.copy(
                settings = initialSettings,
                layout = initialLayout,
                selectedTheme = initialTheme,
                customImagePath = savedImgPath,
                localIp = localIp,
                deviceHistory = history,
                isWifiEnabled = wifiOn,
                isBluetoothEnabled = btOn,
                isLocalNetworkActive = netConnected,
                customQuickKeys = initialKeys,
                connectionMode = initialMode,
                activeHostName = if (initialMode == ConnectionMode.BLUETOOTH) (lastBtName ?: "Bluetooth Host") else (if (defaultTargetIp.isNotBlank()) defaultTargetIp else "PC Receiver")
            )
        }

        if (initialMode == ConnectionMode.BLUETOOTH) {
            scanBluetoothDevices()
            if (!lastBtAddr.isNullOrBlank() && !lastBtName.isNullOrBlank()) {
                bluetoothClient.connect(lastBtAddr, lastBtName)
            }
        }

        udpClient.configureTarget(defaultTargetIp, savedPort)

        // Start Auto-Discovery listener
        discoveryManager.start()

        // Observe discovered laptops
        viewModelScope.launch {
            discoveryManager.discoveredHosts.collect { hosts ->
                // Auto-connect ONLY if the user has not typed / picked an address themselves.
                if (!manualTarget && hosts.isNotEmpty()) {
                    val currentTarget = _uiState.value.settings.targetHostIp
                    val targetSeen = hosts.any { it.ipAddress == currentTarget }
                    if (!targetSeen) {
                        val first = hosts.first()
                        applyTarget(first.ipAddress, first.port, first.hostName, manual = false)
                    }
                }
                _uiState.update { it.copy(discoveredHosts = hosts) }
                refreshLiveStatus()
            }
        }

        // Re-evaluate "live" every second, so it also turns off when the laptop goes silent.
        viewModelScope.launch {
            while (true) {
                refreshLiveStatus()
                delay(1000)
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
        viewModelScope.launch {
            bluetoothClient.isConnected.collect { connected ->
                if (_uiState.value.connectionMode == ConnectionMode.BLUETOOTH) {
                    _uiState.update { it.copy(isLiveConnected = connected) }
                }
            }
        }
        viewModelScope.launch {
            bluetoothClient.connectionStatus.collect { status ->
                _uiState.update { it.copy(bluetoothStatus = status) }
            }
        }
        viewModelScope.launch {
            bluetoothClient.connectedDeviceName.collect { name ->
                if (_uiState.value.connectionMode == ConnectionMode.BLUETOOTH && name != null) {
                    _uiState.update { it.copy(activeHostName = name) }
                }
            }
        }
        viewModelScope.launch {
            bluetoothClient.packetsSent.collect { count ->
                if (_uiState.value.connectionMode == ConnectionMode.BLUETOOTH) {
                    _uiState.update { it.copy(packetsSent = count) }
                }
            }
        }
        viewModelScope.launch {
            wifiDropServer.isRunning.collect { running ->
                _uiState.update { it.copy(isWifiDropServerRunning = running) }
            }
        }
        viewModelScope.launch {
            wifiDropServer.serverUrl.collect { url ->
                _uiState.update { it.copy(wifiDropServerUrl = url) }
            }
        }
        viewModelScope.launch {
            wifiDropServer.downloadCount.collect { count ->
                _uiState.update { it.copy(wifiDropDownloadCount = count) }
            }
        }
        viewModelScope.launch {
            wifiDropServer.lastDownloadStatus.collect { status ->
                _uiState.update { it.copy(wifiDropStatus = status) }
            }
        }

        airMotionSensor.onMotionDelta = { dx, dy ->
            if (_uiState.value.isAirMotionActive) {
                onPointerMove(dx, dy)
            }
        }
    }

    // Navigation & Tab Selection
    fun selectTab(tab: com.example.model.AppNavTab) {
        _uiState.update { it.copy(activeTab = tab) }
        refreshHardwareStatus()
        discoveryManager.triggerScan()
    }

    fun selectTheme(theme: com.example.model.MousePadTheme) {
        _uiState.update { it.copy(selectedTheme = theme) }
        sharedPrefs.edit().putString("trackpad_theme", theme.id).apply()
    }

    fun setCustomTrackpadImage(path: String?) {
        val theme = if (path != null) com.example.model.MousePadTheme.CUSTOM_PHOTO else com.example.model.MousePadTheme.STEALTH
        _uiState.update { it.copy(customImagePath = path, selectedTheme = theme) }
        sharedPrefs.edit()
            .putString("custom_trackpad_image_path", path)
            .putString("trackpad_theme", theme.id)
            .apply()
    }

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

    private fun dispatchCommand(command: RemoteCommand) {
        val wire = command.toWireString(deviceId)
        if (_uiState.value.connectionMode == ConnectionMode.BLUETOOTH || bluetoothClient.isConnected.value) {
            bluetoothClient.send(wire)
        }
        if (_uiState.value.connectionMode == ConnectionMode.WIFI_UDP) {
            udpClient.sendCommand(command)
        }
    }

    fun scanBluetoothDevices() {
        val devices = bluetoothClient.getPairedDevices()
        _uiState.update { it.copy(bluetoothDevices = devices) }
    }

    fun connectBluetooth(device: com.example.model.BluetoothDeviceInfo) {
        sharedPrefs.edit()
            .putString("connection_mode", ConnectionMode.BLUETOOTH.name)
            .putString("last_bt_address", device.address)
            .putString("last_bt_name", device.name)
            .apply()

        _uiState.update {
            it.copy(
                selectedBluetoothDevice = device,
                connectionMode = ConnectionMode.BLUETOOTH,
                activeHostName = device.name
            )
        }
        bluetoothClient.connect(device.address, device.name)
    }

    fun disconnectBluetooth() {
        bluetoothClient.disconnect()
        _uiState.update { it.copy(selectedBluetoothDevice = null, isLiveConnected = false) }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        sharedPrefs.edit()
            .putString("connection_mode", mode.name)
            .apply()

        _uiState.update { it.copy(connectionMode = mode) }
        if (mode == ConnectionMode.BLUETOOTH) {
            scanBluetoothDevices()
            val lastBtAddr = sharedPrefs.getString("last_bt_address", null)
            val lastBtName = sharedPrefs.getString("last_bt_name", null)
            if (!bluetoothClient.isConnected.value && !lastBtAddr.isNullOrBlank() && !lastBtName.isNullOrBlank()) {
                bluetoothClient.connect(lastBtAddr, lastBtName)
            }
            _uiState.update {
                it.copy(
                    isLiveConnected = bluetoothClient.isConnected.value,
                    activeHostName = bluetoothClient.connectedDeviceName.value ?: (lastBtName ?: "Bluetooth Host")
                )
            }
        } else {
            val live = (System.currentTimeMillis() - udpClient.lastAckMs) <= LIVE_WINDOW_MS
            _uiState.update {
                it.copy(
                    isLiveConnected = live,
                    activeHostName = sharedPrefs.getString("target_ip", "Laptop") ?: "Laptop"
                )
            }
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
            dispatchCommand(RemoteCommand.Move(intDx.toFloat(), intDy.toFloat()))
        }
    }

    fun onScroll(rawDy: Float) {
        val settings = _uiState.value.settings
        val inverted = if (settings.invertScroll) -1f else 1f
        // Accumulate fractional drag distance to prevent slow scrolls from being truncated to 0
        scrollAccumulator += rawDy * settings.scrollSensitivity * inverted * 0.05f

        val steps = scrollAccumulator.toInt()
        if (steps != 0) {
            scrollAccumulator -= steps
            if (settings.hapticFeedback) {
                haptics.scrollTick()
            }
            dispatchCommand(RemoteCommand.Scroll(steps.toFloat()))
        }
    }

    fun onLeftClick() {
        if (_uiState.value.dragLockActive) {
            // Releasing held drag lock cleanly
            _uiState.update { it.copy(dragLockActive = false) }
            if (_uiState.value.settings.hapticFeedback) {
                haptics.heavyClick()
            }
            dispatchCommand(RemoteCommand.MouseUp(ClickButton.LEFT))
        } else {
            if (_uiState.value.settings.hapticFeedback) {
                haptics.click()
            }
            dispatchCommand(RemoteCommand.Click(ClickButton.LEFT))
        }
    }

    fun onLeftDown() {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
        dispatchCommand(RemoteCommand.MouseDown(ClickButton.LEFT))
    }

    fun onLeftUp() {
        dispatchCommand(RemoteCommand.MouseUp(ClickButton.LEFT))
    }

    fun onRightClick() {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
        dispatchCommand(RemoteCommand.Click(ClickButton.RIGHT))
    }

    fun onMiddleClick() {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        dispatchCommand(RemoteCommand.Click(ClickButton.MIDDLE))
    }

    fun toggleDragLock() {
        val newLock = !_uiState.value.dragLockActive
        _uiState.update { it.copy(dragLockActive = newLock) }
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
        if (newLock) {
            dispatchCommand(RemoteCommand.MouseDown(ClickButton.LEFT))
        } else {
            dispatchCommand(RemoteCommand.MouseUp(ClickButton.LEFT))
        }
    }

    fun onKeyPress(key: String) {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        dispatchCommand(RemoteCommand.Key(key))
    }

    fun onTextEntered(text: String) {
        if (text.isNotEmpty()) {
            dispatchCommand(RemoteCommand.Text(text))
            _uiState.update { it.copy(keyboardInputText = "") }
        }
    }

    fun setKeyboardText(text: String) {
        _uiState.update { it.copy(keyboardInputText = text) }
    }

    fun updateQuickKeys(keys: List<String>) {
        val trimmed = keys.take(8)
        _uiState.update { it.copy(customQuickKeys = trimmed) }
        sharedPrefs.edit().putString("custom_quick_keys", trimmed.joinToString(",")).apply()
    }

    fun resetQuickKeys() {
        val defaultKeys = com.example.model.KeyboardCatalog.DEFAULT_KEY_CODES
        _uiState.update { it.copy(customQuickKeys = defaultKeys) }
        sharedPrefs.edit().putString("custom_quick_keys", defaultKeys.joinToString(",")).apply()
    }

    fun toggleKeyboard() {
        _uiState.update { it.copy(isKeyboardOpen = !it.isKeyboardOpen) }
    }

    fun triggerAction(actionId: String) {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        dispatchCommand(RemoteCommand.Action(actionId))
    }

    fun jumpDisplay() {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
        dispatchCommand(RemoteCommand.DisplayJump("next"))
    }

    fun switchTab(direction: Int) {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.scrollTick()
        }
        dispatchCommand(RemoteCommand.TabSwitch(direction))
    }

    fun syncClipboardToPc(text: String) {
        if (text.isNotBlank()) {
            val base64 = android.util.Base64.encodeToString(text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            dispatchCommand(RemoteCommand.ClipboardSync(base64))
            _uiState.update { it.copy(lastCopiedText = text) }
            if (_uiState.value.settings.hapticFeedback) {
                haptics.click()
            }
        }
    }

    private var autoScrollJob: Job? = null

    fun setAutoScrollSpeed(speed: Float) {
        _uiState.update { it.copy(autoScrollSpeed = speed, isAutoScrolling = speed != 0f) }
        autoScrollJob?.cancel()
        if (speed != 0f) {
            autoScrollJob = viewModelScope.launch(Dispatchers.IO) {
                while (isActive) {
                    dispatchCommand(RemoteCommand.Scroll(speed))
                    delay(55L)
                }
            }
        }
    }

    fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        _uiState.update { it.copy(autoScrollSpeed = 0f, isAutoScrolling = false) }
    }

    fun toggleWalkAwayLock() {
        val next = !_uiState.value.walkAwayLockEnabled
        _uiState.update { it.copy(walkAwayLockEnabled = next) }
        sharedPrefs.edit().putBoolean("walk_away_lock", next).apply()
    }

    fun uploadFileToPc(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isFileTransferring = true, fileTransferStatus = "Preparing file...") }
            try {
                var fileName = "dropped_file.bin"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }

                val targetIp = _uiState.value.settings.targetHostIp
                if (targetIp.isBlank()) {
                    _uiState.update { it.copy(isFileTransferring = false, fileTransferStatus = "Cannot send: PC not connected via Wi-Fi") }
                    return@launch
                }

                _uiState.update { it.copy(fileTransferStatus = "Sending $fileName to PC...") }
                val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8")
                val url = java.net.URL("http://$targetIp:5056/upload?filename=$encodedName")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 40000
                conn.setRequestProperty("Content-Type", "application/octet-stream")

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    conn.outputStream.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    _uiState.update { it.copy(isFileTransferring = false, fileTransferStatus = "✓ Received: $fileName saved to PC Downloads!") }
                    if (_uiState.value.settings.hapticFeedback) {
                        haptics.heavyClick()
                    }
                } else {
                    _uiState.update { it.copy(isFileTransferring = false, fileTransferStatus = "Transfer failed (HTTP $code)") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFileTransferring = false, fileTransferStatus = "Transfer failed: ${e.message}") }
            }
        }
    }

    // =========================================================================
    // Laptop Receiver Direct Drop & Installation Methods
    // =========================================================================

    fun startWifiDropServer(context: android.content.Context) {
        wifiDropServer.start(context)
    }

    fun stopWifiDropServer() {
        wifiDropServer.stop()
    }

    fun beamReceiverViaBluetooth(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(wifiDropStatus = "Extracting receiver for Bluetooth transfer...") }
            val ok = com.example.installer.BluetoothDropManager.beamReceiverViaBluetooth(context)
            _uiState.update {
                it.copy(
                    wifiDropStatus = if (ok) "✓ Bluetooth beam started! Tap 'Accept' on your Windows laptop."
                    else "Could not start Bluetooth beam. Please ensure phone & laptop are paired."
                )
            }
        }
    }

    fun saveReceiverToPhoneDownloads(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(wifiDropStatus = "Saving receiver to phone storage...") }
            val result = com.example.installer.AssetInstallerManager.saveToPhoneDownloads(context)
            _uiState.update { it.copy(wifiDropStatus = result) }
        }
    }

    fun shareReceiverGeneric(context: android.content.Context) {
        viewModelScope.launch {
            com.example.installer.BluetoothDropManager.shareReceiverGeneric(context)
        }
    }

    private fun applyTarget(ip: String, port: Int, name: String?, manual: Boolean) {
        val cleanIp = ip.trim()
        val hosts = _uiState.value.discoveredHosts
        val matchedName = name ?: hosts.find { it.ipAddress == cleanIp }?.hostName ?: "Laptop"

        manualTarget = manual
        _uiState.update {
            it.copy(
                settings = it.settings.copy(targetHostIp = cleanIp, targetPort = port),
                activeHostName = matchedName,
                isLiveConnected = false
            )
        }
        sharedPrefs.edit()
            .putString("target_ip", cleanIp)
            .putInt("target_port", port)
            .putBoolean("manual_target", manual)
            .apply()
        udpClient.configureTarget(cleanIp, port)
        saveToDeviceHistory(cleanIp, port, matchedName)
    }

    fun setTargetHostIp(ip: String, optionalName: String? = null, manual: Boolean = true) {
        applyTarget(ip, _uiState.value.settings.targetPort, optionalName, manual)
    }

    fun setTargetPort(port: Int) {
        val state = _uiState.value
        applyTarget(state.settings.targetHostIp, port, state.activeHostName, manualTarget)
    }

    private fun refreshLiveStatus() {
        val now = System.currentTimeMillis()
        val state = _uiState.value
        val target = state.settings.targetHostIp
        val seenHost = state.discoveredHosts.firstOrNull { it.ipAddress == target }
        val beaconLive = seenHost != null && (now - seenHost.lastSeenMs) < LIVE_WINDOW_MS
        val lastAck = udpClient.lastAckMs
        val ackLive = lastAck != 0L && (now - lastAck) < LIVE_WINDOW_MS
        val live = beaconLive || ackLive
        val name = seenHost?.hostName ?: state.activeHostName
        if (state.isLiveConnected != live || state.activeHostName != name) {
            _uiState.update { it.copy(isLiveConnected = live, activeHostName = name) }
        }
    }

    fun connectToDiscoveredHost(host: DiscoveredHost) {
        applyTarget(host.ipAddress, host.port, host.hostName, manual = false)
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
    }

    fun disconnectFromDevice() {
        if (_uiState.value.settings.hapticFeedback) {
            haptics.heavyClick()
        }
        dispatchCommand(RemoteCommand.Disconnect)
        if (_uiState.value.connectionMode == ConnectionMode.BLUETOOTH) {
            bluetoothClient.disconnect()
        }
        manualTarget = true
        sharedPrefs.edit()
            .putString("target_ip", "")
            .putBoolean("manual_target", true)
            .apply()
        _uiState.update {
            it.copy(
                isLiveConnected = false,
                activeHostName = "Disconnected",
                selectedBluetoothDevice = null,
                settings = it.settings.copy(targetHostIp = "")
            )
        }
        udpClient.clearTarget()
        navigateTo(AppScreen.HUB_SETTINGS)
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
        dispatchCommand(RemoteCommand.Key("PAGE_UP"))
        return true
    }

    fun onVolumeDown(): Boolean {
        if (!_uiState.value.useVolumeButtonsForNavigation) return false
        if (_uiState.value.settings.hapticFeedback) {
            haptics.click()
        }
        dispatchCommand(RemoteCommand.Key("PAGE_DOWN"))
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
