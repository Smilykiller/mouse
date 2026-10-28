package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRole
import com.example.model.AppScreen
import com.example.model.TouchpadLayout
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.DotRedMild
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary

@Composable
fun AirGlideTopBar(
    currentScreen: AppScreen,
    role: AppRole,
    layout: TouchpadLayout,
    targetIp: String,
    targetPort: Int,
    localIp: String,
    packetsSent: Long,
    isKeyboardOpen: Boolean,
    isNetworkConnected: Boolean,
    onNavigateBackToHub: () -> Unit,
    onOpenConnectivity: () -> Unit,
    onRoleChange: (AppRole) -> Unit,
    onLayoutChange: (TouchpadLayout) -> Unit,
    onToggleKeyboard: () -> Unit,
    onOpenScriptModal: () -> Unit,
    onOpenSecurityModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var layoutMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button (if inside Trackpad/Host) or Logo Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentScreen != AppScreen.HUB_SETTINGS) {
                        IconButton(
                            onClick = onNavigateBackToHub,
                            modifier = Modifier.size(36.dp).testTag("topbar_back_to_hub")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Settings Hub",
                                tint = WhitePrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isNetworkConnected) DotGreenMild else DotRedMild)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = "AirGlide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WhitePrimary
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Role badge with switcher
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onRoleChange(if (role == AppRole.CONTROLLER) AppRole.HOST else AppRole.CONTROLLER)
                            }
                            .testTag("role_switch_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (role == AppRole.CONTROLLER) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                                contentDescription = "Role",
                                tint = WhitePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (role == AppRole.CONTROLLER) "Controller" else "Host",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WhitePrimary
                            )
                        }
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentScreen == AppScreen.TRACKPAD && role == AppRole.CONTROLLER) {
                        // Layout Switcher Dropdown
                        Box {
                            IconButton(
                                onClick = { layoutMenuExpanded = true },
                                modifier = Modifier.testTag("layout_selector_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = "Switch Layout",
                                    tint = WhitePrimary
                                )
                            }
                            DropdownMenu(
                                expanded = layoutMenuExpanded,
                                onDismissRequest = { layoutMenuExpanded = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                TouchpadLayout.values().forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = item.label,
                                                    fontWeight = if (item == layout) FontWeight.Bold else FontWeight.Normal,
                                                    color = WhitePrimary
                                                )
                                                Text(
                                                    text = item.description,
                                                    fontSize = 11.sp,
                                                    color = GraySecondary
                                                )
                                            }
                                        },
                                        onClick = {
                                            onLayoutChange(item)
                                            layoutMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Remote Keyboard Toggle
                        IconButton(
                            onClick = onToggleKeyboard,
                            modifier = Modifier.testTag("keyboard_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Toggle Keyboard",
                                tint = if (isKeyboardOpen) WhitePrimary else GraySecondary
                            )
                        }
                    }

                    // Connectivity Shortcut Button
                    IconButton(
                        onClick = onOpenConnectivity,
                        modifier = Modifier.testTag("topbar_connectivity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Device Connectivity",
                            tint = if (isNetworkConnected) WhitePrimary else GrayMuted
                        )
                    }

                    // Desktop Server Script Modal Button
                    IconButton(
                        onClick = onOpenScriptModal,
                        modifier = Modifier.testTag("desktop_script_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Laptop Server Script",
                            tint = GraySecondary
                        )
                    }

                    // Security Info Modal Button
                    IconButton(
                        onClick = onOpenSecurityModal,
                        modifier = Modifier.testTag("security_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions and Security",
                            tint = GrayMuted
                        )
                    }
                }
            }

            // Subtitle status row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (role == AppRole.CONTROLLER) {
                    Text(
                        text = if (isNetworkConnected) "Target: $targetIp:$targetPort (Local: $localIp)" else "Local Wi-Fi / Hotspot needed",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GraySecondary
                    )
                    Text(
                        text = "Packets: $packetsSent",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = WhitePrimary
                    )
                } else {
                    Text(
                        text = "Host listening on port $targetPort (IP: $localIp)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = WhitePrimary
                    )
                }
            }
        }
    }
}
