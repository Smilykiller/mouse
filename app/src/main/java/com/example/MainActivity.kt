package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppNavTab
import com.example.model.AppRole
import com.example.model.AppScreen
import com.example.ui.dialogs.DesktopScriptDialog
import com.example.ui.dialogs.PermissionsAndSecurityDialog
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.DeviceConnectivityScreen
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.HostReceiverScreen
import com.example.ui.screens.HubSettingsScreen
import com.example.ui.screens.ThemesScreen
import com.example.ui.theme.AirGlideTheme
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
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

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            viewModel.scanBluetoothDevices()
        }
    }

    // Always ensure the mobile app defaults to Controller mode and Trackpad screen
    LaunchedEffect(Unit) {
        viewModel.setRole(AppRole.CONTROLLER)
        viewModel.navigateTo(AppScreen.TRACKPAD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            btPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
    }

    val cyanAccent = Color(0xFF06B6D4)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TrueBlack,
        bottomBar = {
            // Modern bottom tab navigation (auto-hides in Mousepad mode to prevent palm mis-touches, or when keyboard is active)
            if (!uiState.isKeyboardOpen && 
                uiState.activeTab != AppNavTab.TRACKPAD && 
                uiState.currentScreen != AppScreen.DEVICE_CONNECTION) {
                NavigationBar(
                    containerColor = Color(0xFF10131B),
                    tonalElevation = 8.dp
                ) {
                    // 1. Mousepad
                    NavigationBarItem(
                        selected = uiState.activeTab == AppNavTab.TRACKPAD,
                        onClick = { viewModel.selectTab(AppNavTab.TRACKPAD) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Mouse,
                                contentDescription = "Mousepad",
                                tint = if (uiState.activeTab == AppNavTab.TRACKPAD) cyanAccent else GrayMuted
                            )
                        },
                        label = {
                            Text(
                                text = "Mousepad",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.activeTab == AppNavTab.TRACKPAD) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == AppNavTab.TRACKPAD) cyanAccent else GraySecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF1E293B)
                        )
                    )

                    // 2. Themes
                    NavigationBarItem(
                        selected = uiState.activeTab == AppNavTab.THEMES,
                        onClick = { viewModel.selectTab(AppNavTab.THEMES) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes",
                                tint = if (uiState.activeTab == AppNavTab.THEMES) cyanAccent else GrayMuted
                            )
                        },
                        label = {
                            Text(
                                text = "Themes",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.activeTab == AppNavTab.THEMES) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == AppNavTab.THEMES) cyanAccent else GraySecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF1E293B)
                        )
                    )

                    // 3. Help
                    NavigationBarItem(
                        selected = uiState.activeTab == AppNavTab.HELP,
                        onClick = { viewModel.selectTab(AppNavTab.HELP) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help",
                                tint = if (uiState.activeTab == AppNavTab.HELP) cyanAccent else GrayMuted
                            )
                        },
                        label = {
                            Text(
                                text = "Help",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.activeTab == AppNavTab.HELP) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == AppNavTab.HELP) cyanAccent else GraySecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF1E293B)
                        )
                    )

                    // 4. Settings
                    NavigationBarItem(
                        selected = uiState.activeTab == AppNavTab.SETTINGS,
                        onClick = { viewModel.selectTab(AppNavTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (uiState.activeTab == AppNavTab.SETTINGS) cyanAccent else GrayMuted
                            )
                        },
                        label = {
                            Text(
                                text = "Settings",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.activeTab == AppNavTab.SETTINGS) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == AppNavTab.SETTINGS) cyanAccent else GraySecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TrueBlack)
        ) {
            if (uiState.currentScreen == AppScreen.DEVICE_CONNECTION) {
                DeviceConnectivityScreen(
                    state = uiState,
                    onBack = { viewModel.navigateTo(AppScreen.TRACKPAD) },
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
            } else {
                // Categorized Tab Pages
                when (uiState.activeTab) {
                    AppNavTab.TRACKPAD -> {
                        if (uiState.role == AppRole.CONTROLLER) {
                            ControllerScreen(
                                state = uiState,
                                onBackToHub = { viewModel.selectTab(AppNavTab.SETTINGS) },
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
                                onDisconnect = { viewModel.disconnectFromDevice() },
                                onLeftDown = { viewModel.onLeftDown() },
                                onLeftUp = { viewModel.onLeftUp() },
                                onUpdateQuickKeys = { keys -> viewModel.updateQuickKeys(keys) },
                                onResetQuickKeys = { viewModel.resetQuickKeys() },
                                onOpenFullHelp = { viewModel.selectTab(AppNavTab.HELP) }
                            )
                        } else {
                            HostReceiverScreen(
                                state = uiState,
                                onSelectActiveDevice = { devId -> viewModel.selectHostActiveDevice(devId) }
                            )
                        }
                    }

                    AppNavTab.THEMES -> {
                        ThemesScreen(
                            currentTheme = uiState.selectedTheme,
                            customImagePath = uiState.customImagePath,
                            currentLayout = uiState.layout,
                            onSelectTheme = { theme -> viewModel.selectTheme(theme) },
                            onSetCustomImage = { path -> viewModel.setCustomTrackpadImage(path) },
                            onSelectLayout = { layout -> viewModel.setLayout(layout) }
                        )
                    }

                    AppNavTab.HELP -> {
                        HelpScreen()
                    }

                    AppNavTab.SETTINGS -> {
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
                            onRoleChange = { r -> viewModel.setRole(r) },
                            onSelectConnectionMode = { mode -> viewModel.setConnectionMode(mode) },
                            onScanBluetooth = { viewModel.scanBluetoothDevices() },
                            onConnectBluetooth = { device -> viewModel.connectBluetooth(device) },
                            onDisconnectBluetooth = { viewModel.disconnectBluetooth() },
                            onOpenMousepad = { viewModel.selectTab(AppNavTab.TRACKPAD) }
                        )
                    }
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
