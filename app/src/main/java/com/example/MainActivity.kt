package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppRole
import com.example.model.AppScreen
import com.example.ui.components.AirGlideTopBar
import com.example.ui.dialogs.DesktopScriptDialog
import com.example.ui.dialogs.PermissionsAndSecurityDialog
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.DeviceConnectivityScreen
import com.example.ui.screens.HostReceiverScreen
import com.example.ui.screens.HubSettingsScreen
import com.example.ui.theme.AirGlideTheme
import com.example.ui.theme.TrueBlack
import com.example.viewmodel.AirGlideViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AirGlideViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen awake while mouse controller is in use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AirGlideTheme(darkTheme = true) {
                AirGlideApp(viewModel = viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (event?.repeatCount == 0 && viewModel.onVolumeUp()) {
                return true
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event?.repeatCount == 0 && viewModel.onVolumeDown()) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (viewModel.uiState.value.useVolumeButtonsForNavigation) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHardwareStatus()
        viewModel.triggerDiscoveryScan()
    }
}

@Composable
fun AirGlideApp(viewModel: AirGlideViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showScriptDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    // Always ensure the mobile app defaults to Controller mode
    LaunchedEffect(Unit) {
        viewModel.setRole(AppRole.CONTROLLER)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TrueBlack,
        topBar = {
            if (uiState.currentScreen == AppScreen.HUB_SETTINGS) {
                AirGlideTopBar(
                    currentScreen = uiState.currentScreen,
                    role = uiState.role,
                    layout = uiState.layout,
                    targetIp = uiState.settings.targetHostIp,
                    targetPort = uiState.settings.targetPort,
                    localIp = uiState.localIp,
                    packetsSent = uiState.packetsSent,
                    isKeyboardOpen = uiState.isKeyboardOpen,
                    isNetworkConnected = uiState.isLocalNetworkActive,
                    onNavigateBackToHub = { viewModel.navigateBack() },
                    onOpenConnectivity = { viewModel.navigateTo(AppScreen.DEVICE_CONNECTION) },
                    onRoleChange = { newRole -> viewModel.setRole(newRole) },
                    onLayoutChange = { newLayout -> viewModel.setLayout(newLayout) },
                    onToggleKeyboard = { viewModel.toggleKeyboard() },
                    onOpenScriptModal = { showScriptDialog = true },
                    onOpenSecurityModal = { showSecurityDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TrueBlack)
        ) {
            when (uiState.currentScreen) {
                AppScreen.HUB_SETTINGS -> {
                    HubSettingsScreen(
                        state = uiState,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onUpdateSensitivity = { s -> viewModel.updateSensitivity(s) },
                        onUpdateScrollSensitivity = { s -> viewModel.updateScrollSensitivity(s) },
                        onToggleInvertScroll = { viewModel.toggleInvertScroll() },
                        onToggleHaptics = { viewModel.toggleHaptics() },
                        onToggleAirMotion = { viewModel.toggleAirMotion() },
                        onCalibrateAirMotion = { viewModel.calibrateAirMotionCenter() },
                        onUpdateAirMotionSensitivity = { s -> viewModel.updateAirMotionSensitivity(s) },
                        onToggleVolumeButtons = { viewModel.toggleVolumeButtons() },
                        onConnectToDiscoveredHost = { host -> viewModel.connectToDiscoveredHost(host) },
                        onDisconnect = { viewModel.disconnectFromDevice() },
                        onLayoutChange = { l -> viewModel.setLayout(l) },
                        onRoleChange = { r -> viewModel.setRole(r) }
                    )
                }

                AppScreen.TRACKPAD -> {
                    if (uiState.role == AppRole.CONTROLLER) {
                        ControllerScreen(
                            state = uiState,
                            onBackToHub = { viewModel.navigateBack() },
                            onPointerMove = { dx, dy -> viewModel.onPointerMove(dx, dy) },
                            onScroll = { delta -> viewModel.onScroll(delta) },
                            onLeftClick = { viewModel.onLeftClick() },
                            onRightClick = { viewModel.onRightClick() },
                            onMiddleClick = { viewModel.onMiddleClick() },
                            onToggleDragLock = { viewModel.toggleDragLock() },
                            onKeyPress = { key -> viewModel.onKeyPress(key) },
                            onSendText = { text -> viewModel.onTextEntered(text) },
                            onKeyboardTextChange = { text -> viewModel.setKeyboardText(text) },
                            onToggleKeyboard = { viewModel.toggleKeyboard() },
                            onDisconnect = { viewModel.disconnectFromDevice() }
                        )
                    } else {
                        HostReceiverScreen(
                            state = uiState,
                            onSelectActiveDevice = { devId -> viewModel.selectHostActiveDevice(devId) }
                        )
                    }
                }

                AppScreen.DEVICE_CONNECTION -> {
                    DeviceConnectivityScreen(
                        state = uiState,
                        onBack = { viewModel.navigateBack() },
                        onConnectToIp = { ip, port ->
                            viewModel.setTargetHostIp(ip)
                            viewModel.setTargetPort(port)
                        },
                        onSelectDiscoveredHost = { host ->
                            viewModel.connectToDiscoveredHost(host)
                        },
                        onDisconnect = { viewModel.disconnectFromDevice() },
                        onSelectHistoryDevice = { historyItem ->
                            viewModel.setTargetHostIp(historyItem.ipAddress, historyItem.name)
                            viewModel.setTargetPort(historyItem.port)
                        },
                        onDeleteHistoryItem = { item -> viewModel.removeHistoryItem(item) },
                        onSelectHostActiveDevice = { devId -> viewModel.selectHostActiveDevice(devId) },
                        onOpenCompanionScript = { showScriptDialog = true },
                        onRefreshHardware = {
                            viewModel.refreshHardwareStatus()
                            viewModel.triggerDiscoveryScan()
                        }
                    )
                }
            }
        }
    }

    if (showScriptDialog) {
        DesktopScriptDialog(
            onDismiss = { showScriptDialog = false }
        )
    }

    if (showSecurityDialog) {
        PermissionsAndSecurityDialog(
            onDismiss = { showSecurityDialog = false }
        )
    }
}
