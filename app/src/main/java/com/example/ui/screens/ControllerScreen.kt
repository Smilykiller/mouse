package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    onUpdateQuickKeys: (List<String>) -> Unit = {},
    onResetQuickKeys: () -> Unit = {},
    onOpenFullHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToHub() }

    val focusRequester = remember { FocusRequester() }
    val isKbOpen = state.isKeyboardOpen
    var showHelpDialog by remember { mutableStateOf(false) }

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
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize(animationSpec = tween(250)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Minimal Header: Back Button, Connection Status Pill, Actions (Keyboard, Disconnect, Help)
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Back to Settings Button (Clear & unmissable)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardDark)
                            .clickable { onBackToHub() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("controller_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Settings",
                            tint = WhitePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePrimary
                        )
                    }

                    // 2. True Connection Status Pill (Wi-Fi or Bluetooth)
                    val (dotColor, statusTitle, statusSub) = when {
                        state.connectionMode == com.example.model.ConnectionMode.BLUETOOTH -> {
                            if (state.isLiveConnected) Triple(Color(0xFF38BDF8), "Bluetooth", state.activeHostName)
                            else Triple(DotAmberMild, "BT Ready", "No device")
                        }
                        state.isLiveConnected -> Triple(DotGreenMild, "Live", state.activeHostName)
                        state.isLocalNetworkActive -> Triple(DotAmberMild, "Ready", state.settings.targetHostIp)
                        else -> Triple(DotRedMild, "No Wi-Fi", "")
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            if (statusSub.isNotBlank()) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (state.packetsSent > 0) "$statusSub • ${state.packetsSent}" else statusSub,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GraySecondary
                                )
                            }
                        }
                    }

                    // 3. Right Action Buttons: Keyboard + Disconnect + Small Help Button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleKeyboard,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isKbOpen) ElevatedSurface else CardDark)
                                .testTag("controller_keyboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Toggle Keyboard",
                                tint = WhitePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2D1414))
                                .testTag("controller_disconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect Device",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Small Help Button at Right End
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .testTag("controller_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Mousepad Help",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // Click & Drag Quick Action Bar (Available across all layouts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag & Drop Mode Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (state.dragLockActive) Color(0xFFB45309) else Color(0xFF1E222D),
                    border = BorderStroke(
                        1.dp,
                        if (state.dragLockActive) Color(0xFFF59E0B) else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleDragLock() }
                        .testTag("action_drag_lock")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (state.dragLockActive) Icons.Default.Lock else Icons.Default.TouchApp,
                            contentDescription = "Click and Drag",
                            tint = if (state.dragLockActive) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.dragLockActive) "DRAG HELD (TAP TO DROP)" else "CLICK & DRAG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.dragLockActive) Color.White else Color(0xFFCBD5E1)
                        )
                    }
                }

                // Middle Click Quick Action
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E222D),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onMiddleClick() }
                        .testTag("action_middle_click")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mouse,
                            contentDescription = "Middle Click",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MIDDLE CLICK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Mousepad Area: Size dynamically reduces when keyboard is opened
            val mousePadWeight = if (isKbOpen) 0.55f else 1.0f

            when (state.layout) {
                TouchpadLayout.SPLIT -> {
                    // Split Layout: Trackpad + Dedicated Scroll Strip
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
                            theme = state.selectedTheme,
                            customImagePath = state.customImagePath,
                            dragLockActive = state.dragLockActive,
                            onToggleDragLock = onToggleDragLock,
                            label = if (isKbOpen) "TOUCHPAD" else "TOUCHPAD SURFACE (SWIPE TO MOVE)",
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

                    // Twin Click Boxes at bottom (Compact height when typing)
                    val clickHeight = if (isKbOpen) 80 else 115
                    TwinClickButtons(
                        onLeftClick = onLeftClick,
                        onRightClick = onRightClick,
                        onMiddleClick = onMiddleClick,
                        dragLockActive = state.dragLockActive,
                        onToggleDragLock = onToggleDragLock,
                        height = clickHeight
                    )
                }

                TouchpadLayout.CLASSIC -> {
                    // Classic Laptop
                    TrackpadSurface(
                        onPointerMove = onPointerMove,
                        onTap = onLeftClick,
                        onDoubleTap = onLeftClick,
                        onLongPress = onRightClick,
                        onScroll = onScroll,
                        theme = state.selectedTheme,
                        customImagePath = state.customImagePath,
                        dragLockActive = state.dragLockActive,
                        onToggleDragLock = onToggleDragLock,
                        label = if (isKbOpen) "TRACKPAD" else "EXPANSIVE LAPTOP TRACKPAD",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mousePadWeight)
                    )

                    val clickHeight = if (isKbOpen) 85 else 125
                    TwinClickButtons(
                        onLeftClick = onLeftClick,
                        onRightClick = onRightClick,
                        onMiddleClick = onMiddleClick,
                        dragLockActive = state.dragLockActive,
                        onToggleDragLock = onToggleDragLock,
                        height = clickHeight
                    )
                }

                TouchpadLayout.GESTURE -> {
                    // Gesture Surface
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(mousePadWeight)
                    ) {
                        TrackpadSurface(
                            onPointerMove = onPointerMove,
                            onTap = onLeftClick,
                            onDoubleTap = onLeftClick,
                            onLongPress = onRightClick,
                            onScroll = onScroll,
                            theme = state.selectedTheme,
                            customImagePath = state.customImagePath,
                            dragLockActive = state.dragLockActive,
                            onToggleDragLock = onToggleDragLock,
                            label = if (isKbOpen) "GESTURE PAD" else "FULL GESTURE SURFACE (TAP = CLICK)",
                            modifier = Modifier.fillMaxSize()
                        )

                        if (!isKbOpen) {
                            Surface(
                                color = DarkSurface.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(14.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = onLeftClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WhitePrimary,
                                            contentColor = TrueBlack
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("LEFT CLICK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrueBlack)
                                    }

                                    Button(
                                        onClick = onRightClick,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElevatedSurface,
                                            contentColor = WhitePrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("RIGHT CLICK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Remote Keyboard Bar
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
                                GestureHelpRow("👆", "Cursor Move", "Glide 1 finger across the full trackpad")
                                GestureHelpRow("👆", "Left Click", "Single tap anywhere on the pad")
                                GestureHelpRow("✌️", "Right Click", "Tap with 2 fingers simultaneously")
                                GestureHelpRow("↕️", "Two-Finger Scroll", "Slide 2 fingers vertically")
                                GestureHelpRow("🔒", "Click & Drag", "Tap 'HOLD DRAG' button or Double-tap & slide. Tap to drop!")
                            }
                            TouchpadLayout.SPLIT -> {
                                GestureHelpRow("👆", "Cursor Move", "Glide 1 finger on the upper trackpad")
                                GestureHelpRow("📜", "Easy Scroll", "Slide 1 finger along the dedicated right strip")
                                GestureHelpRow("🖱️", "Left & Right", "Tap the massive twin buttons at the bottom")
                                GestureHelpRow("🔒", "Click & Drag", "Hold Left button or tap [CLICK & DRAG]")
                            }
                            TouchpadLayout.CLASSIC -> {
                                GestureHelpRow("👆", "Cursor Move", "Glide 1 finger on the main upper trackpad")
                                GestureHelpRow("🖱️", "Left & Right", "Tap the bottom laptop click buttons")
                                GestureHelpRow("🔒", "Click & Drag", "Tap [CLICK & DRAG] at top or bottom-right")
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
