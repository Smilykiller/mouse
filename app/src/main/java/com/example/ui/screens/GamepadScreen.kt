package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState

@Composable
fun GamepadScreen(
    state: AirGlideUiState,
    onBack: () -> Unit,
    onButtonPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardDark)
                    .border(BorderStroke(1.dp, BorderDark), RoundedCornerShape(10.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WhitePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
            }

            Text(
                text = "🎮 VIRTUAL ARCADE GAMEPAD",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF38BDF8)
            )

            // Telemetry indicator
            val telem = state.laptopTelemetry
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CardDark,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Text(
                    text = if (telem != null) "🔋 ${telem.batteryPct}%" else "🎮 Active",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DotGreenMild,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // --- Shoulder Bumpers (LB & RB) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GamepadTriggerButton(
                label = "LB",
                subtitle = "Bumper Left",
                color = Color(0xFF0284C7),
                onClick = { onButtonPress("GP_LB") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            GamepadTriggerButton(
                label = "RB",
                subtitle = "Bumper Right",
                color = Color(0xFF0284C7),
                onClick = { onButtonPress("GP_RB") },
                modifier = Modifier.weight(1f)
            )
        }

        // --- Main Controls: D-Pad on Left, Center Options, Action Buttons on Right ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Left: D-Pad ---
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                // Up
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(60.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color(0xFF1E293B))
                        .border(BorderStroke(1.5.dp, Color(0xFF334155)), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .clickable { onButtonPress("GP_UP") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▲", fontSize = 22.sp, color = Color(0xFF38BDF8))
                }

                // Down
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(60.dp)
                        .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                        .background(Color(0xFF1E293B))
                        .border(BorderStroke(1.5.dp, Color(0xFF334155)), RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                        .clickable { onButtonPress("GP_DOWN") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▼", fontSize = 22.sp, color = Color(0xFF38BDF8))
                }

                // Left
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(60.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(Color(0xFF1E293B))
                        .border(BorderStroke(1.5.dp, Color(0xFF334155)), RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .clickable { onButtonPress("GP_LEFT") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("◀", fontSize = 22.sp, color = Color(0xFF38BDF8))
                }

                // Right
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(60.dp)
                        .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                        .background(Color(0xFF1E293B))
                        .border(BorderStroke(1.5.dp, Color(0xFF334155)), RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                        .clickable { onButtonPress("GP_RIGHT") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", fontSize = 22.sp, color = Color(0xFF38BDF8))
                }

                // Center pivot
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                )
            }

            // --- Center: SELECT & START ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onButtonPress("GP_SELECT") }
                ) {
                    Text(
                        text = "SELECT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GraySecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onButtonPress("GP_START") }
                ) {
                    Text(
                        text = "START",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhitePrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // --- Right: Action Diamond (A, B, X, Y) ---
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                // Y (Top - Amber)
                GamepadActionButton(
                    label = "Y",
                    color = Color(0xFFF59E0B),
                    onClick = { onButtonPress("GP_Y") },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                // A (Bottom - Green)
                GamepadActionButton(
                    label = "A",
                    color = Color(0xFF10B981),
                    onClick = { onButtonPress("GP_A") },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                // X (Left - Blue)
                GamepadActionButton(
                    label = "X",
                    color = Color(0xFF3B82F6),
                    onClick = { onButtonPress("GP_X") },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                // B (Right - Red)
                GamepadActionButton(
                    label = "B",
                    color = Color(0xFFEF4444),
                    onClick = { onButtonPress("GP_B") },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // --- Bottom Hint ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Mapped to Arrow Keys, Space, Esc, Enter, Z, X, Q, E • Zero Latency",
                fontSize = 11.sp,
                color = GraySecondary
            )
        }
    }
}

@Composable
private fun GamepadTriggerButton(
    label: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.35f), Color(0xFF0F172A))
                )
            )
            .border(BorderStroke(1.5.dp, color.copy(alpha = 0.8f)), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = WhitePrimary)
            Text(subtitle, fontSize = 10.sp, color = GraySecondary)
        }
    }
}

@Composable
private fun GamepadActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.3f), Color(0xFF0F172A))
                )
            )
            .border(BorderStroke(2.dp, color), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}
