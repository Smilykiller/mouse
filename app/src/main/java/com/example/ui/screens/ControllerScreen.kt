package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.model.TouchpadLayout
import com.example.ui.components.RemoteKeyboardBar
import com.example.ui.components.ScrollStrip
import com.example.ui.components.TrackpadSurface
import com.example.ui.components.TwinClickButtons
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DotAmberMild
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.DotRedMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState

@Composable
fun ControllerScreen(
    state: AirGlideUiState,
    onBackToHub: () -> Unit,
    onPointerMove: (Float, Float) -> Unit,
    onScroll: (Float) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onMiddleClick: () -> Unit,
    onToggleDragLock: () -> Unit,
    onKeyPress: (String) -> Unit,
    onSendText: (String) -> Unit,
    onKeyboardTextChange: (String) -> Unit,
    onToggleKeyboard: () -> Unit,
    onDisconnect: () -> Unit,
    onLeftDown: () -> Unit = {},
    onLeftUp: () -> Unit = {},
    onZoom: (Boolean) -> Unit = {},
    onWinTab: () -> Unit = {},
    onWinD: () -> Unit = {},
    onAltTab: () -> Unit = {},
    onWinA: () -> Unit = {},
    onOpenPresenter: () -> Unit = {},
    onOpenGamepad: () -> Unit = {},
    onOpenMediaRemote: () -> Unit = {},
    onToggleAirMotion: () -> Unit = {},
    onJumpDisplay: () -> Unit = {},
    onSwitchTab: (Int) -> Unit = {},
    onSetAutoScrollSpeed: (Float) -> Unit = {},
    onStopAutoScroll: () -> Unit = {},
    onUpdateQuickKeys: (List<String>) -> Unit = {},
    onResetQuickKeys: () -> Unit = {},
    onOpenFullHelp: () -> Unit = {},
    onTriggerAction: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToHub() }

    val focusRequester = remember { FocusRequester() }
    val isKbOpen = state.isKeyboardOpen
    var showHelpDialog by remember { mutableStateOf(false) }
    var showToolsDropdown by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        QuickTouchpadHelpDialog(
            layout = state.layout,
            onDismiss = { showHelpDialog = false },
            onOpenFullHelp = onOpenFullHelp
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .imePadding() // Automatically shrinks above Android software keyboard
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(250)),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ==============================================================
            // 1. PRIMARY TOP NAVIGATION BAR (Fixed, Never Collides or Overlaps)
            // ==============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Settings Hub & Connection Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Back to Settings Hub
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBackToHub() }
                            .testTag("controller_back_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Settings Hub",
                                tint = WhitePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hub",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                        }
                    }

                    // Connection Badge (Wi-Fi / BT)
                    val isBt = state.connectionMode == ConnectionMode.BLUETOOTH
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (state.isLiveConnected) Color(0xFF0F291E) else CardDark,
                        border = BorderStroke(1.dp, if (state.isLiveConnected) DotGreenMild else BorderDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBackToHub() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isLiveConnected) DotGreenMild else DotAmberMild)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBt) "BT" else "Wi-Fi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isLiveConnected) DotGreenMild else GraySecondary
                            )
                        }
                    }

                    // Laptop Battery & CPU Telemetry Badge (if received from laptop receiver)
                    state.laptopTelemetry?.let { telem ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CardDark,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = "🔋${telem.batteryPct}%${if (telem.isCharging) "⚡" else ""} • ${telem.cpuPct}%",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telem.batteryPct < 20 && !telem.isCharging) DotAmberMild else Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Right side: Quick Keyboard Toggle + All Options Dropdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Keyboard 1-Tap Quick Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isKbOpen) ElevatedSurface else CardDark,
                        border = BorderStroke(1.dp, if (isKbOpen) Color(0xFF38BDF8) else BorderDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleKeyboard() }
                            .testTag("controller_keyboard_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Keyboard",
                                tint = if (isKbOpen) Color(0xFF38BDF8) else WhitePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Keys",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isKbOpen) Color(0xFF38BDF8) else WhitePrimary
                            )
                        }
                    }

                    // Options & Tools Dropdown Menu
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (showToolsDropdown) Color(0xFF1E293B) else CardDark,
                            border = BorderStroke(1.dp, if (showToolsDropdown) Color(0xFF38BDF8) else BorderDark),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showToolsDropdown = true }
                                .testTag("pad_tools_dropdown_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Tools ▾",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showToolsDropdown) Color(0xFF38BDF8) else WhitePrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showToolsDropdown,
                            onDismissRequest = { showToolsDropdown = false },
                            modifier = Modifier
                                .background(Color(0xFF161A24))
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Text("📽️", fontSize = 15.sp) },
                                text = { Text("Presenter Remote", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    onOpenPresenter()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Text("🎮", fontSize = 15.sp) },
                                text = { Text("Virtual Gamepad", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    onOpenGamepad()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Text("🎵", fontSize = 15.sp) },
                                text = { Text("Media Player Remote", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    onOpenMediaRemote()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Text("🪄", fontSize = 15.sp) },
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Air Mouse (Gyro)", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (state.isAirMotionActive) Text("ON", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    showToolsDropdown = false
                                    onToggleAirMotion()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp)) },
                                text = { Text("Jump Display Screen", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    onJumpDisplay()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Text("📜", fontSize = 15.sp) },
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Auto-Scroll", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (state.isAutoScrolling) Text("ON", color = DotGreenMild, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    showToolsDropdown = false
                                    if (state.isAutoScrolling) onStopAutoScroll() else onSetAutoScrollSpeed(2.0f)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Text("🔍", fontSize = 15.sp) },
                                text = { Text("Screen Zoom In (+)", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    onTriggerAction("MAGNIFIER_IN")
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Text("🔎", fontSize = 15.sp) },
                                text = { Text("Screen Zoom Out (-)", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    onTriggerAction("MAGNIFIER_OUT")
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Text("✕", fontSize = 15.sp) },
                                text = { Text("Reset / Close Magnifier", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    onTriggerAction("MAGNIFIER_CLOSE")
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp)) },
                                text = { Text("Touchpad Gestures Help", color = WhitePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                                onClick = {
                                    showToolsDropdown = false
                                    showHelpDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // ==============================================================
            // 2. SMOOTHLY SCROLLABLE HORIZONTAL QUICK CHIPS (No Overlapping)
            // ==============================================================
            val headerScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(headerScrollState)
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Air Mouse Quick Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.isAirMotionActive) Color(0xFF0284C7) else CardDark,
                    border = BorderStroke(1.dp, if (state.isAirMotionActive) Color(0xFF38BDF8) else BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleAirMotion() }
                ) {
                    Text(
                        text = if (state.isAirMotionActive) "🪄 Air Mouse ON" else "🪄 Air Mouse",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isAirMotionActive) Color.White else GraySecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Jump Display Screen Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onJumpDisplay() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesktopWindows,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Jump Screen",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GraySecondary
                        )
                    }
                }

                // Auto-Scroll Quick Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.isAutoScrolling) DotGreenMild.copy(alpha = 0.2f) else CardDark,
                    border = BorderStroke(1.dp, if (state.isAutoScrolling) DotGreenMild else BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (state.isAutoScrolling) onStopAutoScroll()
                            else onSetAutoScrollSpeed(2.0f)
                        }
                ) {
                    Text(
                        text = if (state.isAutoScrolling) "📜 Auto-Scroll ON" else "📜 Auto-Scroll",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isAutoScrolling) DotGreenMild else GraySecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Presenter Quick Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenPresenter() }
                ) {
                    Text(
                        text = "📽️ Presenter",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GraySecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Gamepad Quick Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenGamepad() }
                ) {
                    Text(
                        text = "🎮 Gamepad",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GraySecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Media Quick Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenMediaRemote() }
                ) {
                    Text(
                        text = "🎵 Media",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GraySecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Quick Zoom Out Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTriggerAction("MAGNIFIER_OUT") }
                ) {
                    Text(
                        text = "🔍 Zoom -",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Quick Zoom In Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTriggerAction("MAGNIFIER_IN") }
                ) {
                    Text(
                        text = "🔍 Zoom +",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Gestures Help Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showHelpDialog = true }
                        .testTag("controller_help_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Help",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Help",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // Mousepad Area: Expands to 100% of remaining screen with ZERO bottom keys to eliminate mis-touches!
            val mousePadWeight = if (isKbOpen) 0.55f else 1.0f

            when (state.layout) {
                TouchpadLayout.SPLIT -> {
                    // Split Layout: Trackpad + Dedicated Scroll Strip (No bottom buttons!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mousePadWeight),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrackpadSurface(
                            onPointerMove = onPointerMove,
                            onTap = onLeftClick,
                            onDoubleTap = onLeftClick,
                            onLongPress = onRightClick,
                            onScroll = onScroll,
                            onLeftDown = onLeftDown,
                            onLeftUp = onLeftUp,
                            onZoom = onZoom,
                            onWinTab = onWinTab,
                            onWinD = onWinD,
                            onAltTab = onAltTab,
                            onWinA = onWinA,
                            theme = state.selectedTheme,
                            customImagePath = state.customImagePath,
                            dragLockActive = state.dragLockActive,
                            onToggleDragLock = onToggleDragLock,
                            label = if (isKbOpen) "TOUCHPAD" else "TOUCHPAD • DOUBLE-TAP TO DRAG",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )

                        ScrollStrip(
                            onScroll = onScroll,
                            label = "SCROLL",
                            modifier = Modifier.testTag("split_scroll_strip")
                        )
                    }
                }

                TouchpadLayout.CLASSIC -> {
                    // Classic Laptop Full-Area Trackpad (No bottom buttons to prevent palm mis-touches)
                    TrackpadSurface(
                        onPointerMove = onPointerMove,
                        onTap = onLeftClick,
                        onDoubleTap = onLeftClick,
                        onLongPress = onRightClick,
                        onScroll = onScroll,
                        onLeftDown = onLeftDown,
                        onLeftUp = onLeftUp,
                        onZoom = onZoom,
                        onWinTab = onWinTab,
                        onWinD = onWinD,
                        onAltTab = onAltTab,
                        onWinA = onWinA,
                        theme = state.selectedTheme,
                        customImagePath = state.customImagePath,
                        dragLockActive = state.dragLockActive,
                        onToggleDragLock = onToggleDragLock,
                        label = if (isKbOpen) "TRACKPAD" else "EXPANSIVE LAPTOP TRACKPAD • DOUBLE-TAP TO DRAG",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mousePadWeight)
                    )
                }

                TouchpadLayout.GESTURE -> {
                    // 100% Pure Gesture Surface (Zero bottom keys, effortless gestures)
                    TrackpadSurface(
                        onPointerMove = onPointerMove,
                        onTap = onLeftClick,
                        onDoubleTap = onLeftClick,
                        onLongPress = onRightClick,
                        onScroll = onScroll,
                        onLeftDown = onLeftDown,
                        onLeftUp = onLeftUp,
                        onZoom = onZoom,
                        onWinTab = onWinTab,
                        onWinD = onWinD,
                        onAltTab = onAltTab,
                        onWinA = onWinA,
                        theme = state.selectedTheme,
                        customImagePath = state.customImagePath,
                        dragLockActive = state.dragLockActive,
                        onToggleDragLock = onToggleDragLock,
                        label = if (isKbOpen) "GESTURE PAD" else "FULL GESTURE SURFACE (DOUBLE-TAP TO DRAG)",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mousePadWeight)
                    )
                }
            }

            // Remote Keyboard Bar (Only visible when user toggles keyboard)
            RemoteKeyboardBar(
                isOpen = isKbOpen,
                customKeyCodes = state.customQuickKeys,
                focusRequester = focusRequester,
                onSendText = onSendText,
                onKeyPress = onKeyPress,
                onUpdateQuickKeys = onUpdateQuickKeys,
                onResetQuickKeys = onResetQuickKeys,
                onClose = onToggleKeyboard
            )
        }
    }
}

@Composable
private fun QuickTouchpadHelpDialog(
    layout: TouchpadLayout,
    onDismiss: () -> Unit,
    onOpenFullHelp: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF161A24),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${layout.label} Guide",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            Text(
                                text = "Gestures & Actions",
                                fontSize = 12.sp,
                                color = GraySecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GrayMuted
                        )
                    }
                }

                // Layout Specific Instructions
                Surface(
                    color = Color(0xFF0F1219),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (layout) {
                            TouchpadLayout.GESTURE -> {
                                GestureHelpRow("👆", "Move Pointer", "Slide 1 finger across the full screen")
                                GestureHelpRow("👆", "Left Click", "Single tap anywhere on the screen")
                                GestureHelpRow("✌️", "Right Click", "Tap with 2 fingers or long-press")
                                GestureHelpRow("↕️", "Two-Finger Scroll", "Slide 2 fingers up and down")
                                GestureHelpRow("🔒", "Double-Tap Drag", "Double-tap and slide without lifting to drag & select! Lift to drop.")
                                GestureHelpRow("⏱️", "Hold & Drag", "Press and hold in place for 0.4s until haptic pulse, then slide to drag.")
                            }
                            TouchpadLayout.SPLIT -> {
                                GestureHelpRow("👆", "Move Pointer", "Slide 1 finger across the trackpad")
                                GestureHelpRow("📜", "Vertical Scroll", "Slide along the dedicated right strip")
                                GestureHelpRow("👆", "Left Click", "Tap once anywhere on the surface")
                                GestureHelpRow("✌️", "Right Click", "2-finger tap or long press")
                                GestureHelpRow("🔒", "Click & Drag", "Double-tap & slide, or hold in place for 0.4s to drag!")
                            }
                            TouchpadLayout.CLASSIC -> {
                                GestureHelpRow("👆", "Move Pointer", "Slide 1 finger across the expansive pad")
                                GestureHelpRow("👆", "Left Click", "Single tap anywhere on the surface")
                                GestureHelpRow("✌️", "Right Click", "2-finger tap or long press")
                                GestureHelpRow("↕️", "Two-Finger Scroll", "Slide 2 fingers up and down")
                                GestureHelpRow("🔒", "Click & Drag", "Double-tap & slide, or hold in place for 0.4s to drag!")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📖 Full Help Page",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onDismiss()
                                onOpenFullHelp()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Got it", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureHelpRow(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 16.sp, modifier = Modifier.padding(top = 1.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
            Text(text = desc, fontSize = 11.sp, color = GraySecondary, lineHeight = 15.sp)
        }
    }
}
