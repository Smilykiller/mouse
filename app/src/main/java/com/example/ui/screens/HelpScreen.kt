package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Help & Gesture Guide",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePrimary
                )
                Text(
                    text = "How to glide, click, drag, scroll, and type on your laptop",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
            }
        }

        // Gesture Cards List
        HelpCard(
            icon = Icons.Default.TouchApp,
            iconTint = Color(0xFF38BDF8),
            title = "1. Glide Mouse Pointer",
            subtitle = "Swipe 1 finger anywhere on the pad",
            description = "Move your finger across the trackpad surface to glide the cursor across your laptop screen with ultra-low latency."
        )

        HelpCard(
            icon = Icons.Default.Mouse,
            iconTint = Color(0xFF22C55E),
            title = "2. Left Click & Double Click",
            subtitle = "Single tap for Click • Rapid double-tap to Open",
            description = "Tap with 1 finger on the trackpad or tap the dedicated 'LEFT CLICK' button at the bottom. Quickly double-tap to open folders and launch apps."
        )

        HelpCard(
            icon = Icons.Default.TouchApp,
            iconTint = Color(0xFFA855F7),
            title = "3. Right Click (Context Menu)",
            subtitle = "Tap with 2 fingers simultaneously",
            description = "Tap two fingers at the same time anywhere on the trackpad, or tap the 'RIGHT CLICK' button to open context menus and file options."
        )

        HelpCard(
            icon = Icons.Default.Lock,
            iconTint = Color(0xFFF59E0B),
            title = "4. Click & Drag (Drag and Drop)",
            subtitle = "Use the Drag Lock button or Double-Tap & Hold",
            description = "• Option A (Drag Button): Tap the 'CLICK & DRAG' button above the pad. The left mouse button is locked down! Move your finger to drag windows, move files, or highlight text, then tap again to drop.\n\n• Option B (Gesture): Double-tap on the trackpad and keep your finger pressed down on the second tap to drag."
        )

        HelpCard(
            icon = Icons.Default.SwapVert,
            iconTint = Color(0xFF06B6D4),
            title = "5. Smooth Scrolling",
            subtitle = "Slide 2 fingers up or down",
            description = "Swipe vertically with two fingers anywhere on the trackpad to scroll webpages and documents. In Split Layout, you can also slide along the dedicated scroll strip on the right."
        )

        HelpCard(
            icon = Icons.Default.Keyboard,
            iconTint = Color(0xFFEC4899),
            title = "6. Live Keyboard & Extra Keys",
            subtitle = "Tap the keyboard icon in the top bar",
            description = "Your phone's keyboard will appear for live typing directly into your laptop. Tap the '+' button on the right of the keyboard bar to customize extra keys (Esc, Tab, Shift, Ctrl, Enter, Home, End, F-keys)."
        )

        HelpCard(
            icon = Icons.Default.Wifi,
            iconTint = Color(0xFF10B981),
            title = "7. Wi-Fi Connection & Troubleshooting",
            subtitle = "Same Wi-Fi network • Port 5055",
            description = "Make sure your phone and laptop are connected to the same Wi-Fi router or phone mobile hotspot. AirGlide discovers your laptop automatically. If Wi-Fi isolation is enabled on your router, check the laptop IP in the laptop app and connect directly in Settings."
        )
    }
}

@Composable
private fun HelpCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardDark,
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhitePrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = iconTint,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 12.5.sp,
                color = GraySecondary,
                lineHeight = 18.sp
            )
        }
    }
}
