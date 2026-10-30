package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
fun MediaRemoteScreen(
    state: AirGlideUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onMute: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onLaunchSpotify: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                text = "🎵 MEDIA PLAYER REMOTE",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF10B981)
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CardDark,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Text(
                    text = if (state.isLiveConnected) "● Online" else "Offline",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isLiveConnected) DotGreenMild else GraySecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // --- Centerpiece: Playback Surface ---
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardDark,
            border = BorderStroke(1.5.dp, Color(0xFF1F2937)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Large Play/Pause Glow Button
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF064E3B))
                            )
                        )
                        .border(BorderStroke(3.dp, Color(0xFF34D399)), CircleShape)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }

                // Skip & Seek Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Track
                    MediaControlButton(
                        icon = Icons.Default.SkipPrevious,
                        label = "Prev",
                        onClick = onPrev
                    )

                    // 10s Rewind
                    MediaControlButton(
                        icon = Icons.Default.FastRewind,
                        label = "-10s",
                        onClick = onRewind
                    )

                    // 10s Fast-Forward
                    MediaControlButton(
                        icon = Icons.Default.FastForward,
                        label = "+10s",
                        onClick = onForward
                    )

                    // Next Track
                    MediaControlButton(
                        icon = Icons.Default.SkipNext,
                        label = "Next",
                        onClick = onNext
                    )
                }
            }
        }

        // --- Volume Slider & Quick Adjustment ---
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOLUME CONTROL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GraySecondary
                    )

                    // Mute Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF374151),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMute() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeMute,
                                contentDescription = "Mute",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mute", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Volume Down
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSurface)
                            .border(BorderStroke(1.dp, BorderDark), RoundedCornerShape(12.dp))
                            .clickable { onVolumeDown() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeDown, null, tint = WhitePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Volume -", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                        }
                    }

                    // Volume Up
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedSurface)
                            .border(BorderStroke(1.dp, BorderDark), RoundedCornerShape(12.dp))
                            .clickable { onVolumeUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Volume +", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                        }
                    }
                }
            }
        }

        // --- Fast App Quick Launcher ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Spotify on Laptop", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                        Text("Launch or bring to focus", fontSize = 11.sp, color = GraySecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLaunchSpotify() }
                ) {
                    Text(
                        text = "Open Spotify",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF1F2937))
                .border(BorderStroke(1.dp, Color(0xFF374151)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = WhitePrimary, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = GraySecondary)
    }
}
