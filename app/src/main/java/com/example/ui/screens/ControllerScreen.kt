package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToHub() }

    val focusRequester = remember { FocusRequester() }
    val isKbOpen = state.isKeyboardOpen

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
            // Minimal Header: Back Button, Connection Status Pill, Actions (Keyboard & Disconnect)
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Back Button
                    IconButton(
                        onClick = onBackToHub,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("controller_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WhitePrimary
                        )
                    }

                    // 2. True Connection Status Pill
                    val (dotColor, statusTitle, statusSub) = when {
                        state.isLiveConnected -> Triple(DotGreenMild, "Live", state.activeHostName)
                        state.isLocalNetworkActive -> Triple(DotAmberMild, "No reply", state.settings.targetHostIp)
                        else -> Triple(DotRedMild, "No Wi-Fi", "")
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = statusTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhitePrimary
                            )
                            if (statusSub.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.packetsSent > 0) "$statusSub • ${state.packetsSent}" else statusSub,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GraySecondary
                                )
                            }
                        }
                    }

                    // 3. Right Action Buttons: Keyboard + Disconnect
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleKeyboard,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isKbOpen) ElevatedSurface else CardDark)
                                .testTag("controller_keyboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Toggle Keyboard",
                                tint = WhitePrimary
                            )
                        }

                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2D1414))
                                .testTag("controller_disconnect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect Device",
                                tint = Color(0xFFF87171)
                            )
                        }
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
                            label = if (isKbOpen) "GESTURE PAD" else "FULL GESTURE SURFACE (TAP = CLICK)",
                            modifier = Modifier.fillMaxSize()
                        )

                        if (!isKbOpen) {
                            Surface(
                                color = DarkSurface.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
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
