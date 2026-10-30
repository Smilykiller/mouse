package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DotGreenMild
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import com.example.viewmodel.AirGlideUiState
import kotlinx.coroutines.delay

@Composable
fun PresentationScreen(
    state: AirGlideUiState,
    onBack: () -> Unit,
    onNextSlide: () -> Unit,
    onPrevSlide: () -> Unit,
    onStartSlideshow: () -> Unit,
    onEndSlideshow: () -> Unit,
    onBlankB: () -> Unit,
    onBlankW: () -> Unit,
    onToggleLaserPointer: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Presentation Timer state
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var isLaserActive by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val laserPulse = rememberInfiniteTransition(label = "laser_pulse")
    val laserAlpha by laserPulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "laser_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrueBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header Row ---
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

            // Live Timer & Status Display
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElevatedSurface,
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isTimerRunning) DotGreenMild else GrayMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeFormatted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTimerRunning) DotGreenMild else WhitePrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Timer",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { isTimerRunning = !isTimerRunning }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = GraySecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                isTimerRunning = false
                                elapsedSeconds = 0L
                            }
                    )
                }
            }
        }

        // --- Ergonomic Giant Slide Cards ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Previous Slide
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .border(BorderStroke(1.5.dp, Color(0xFF334155)), RoundedCornerShape(20.dp))
                    .clickable { onPrevSlide() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⬅", fontSize = 48.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PREVIOUS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
                    Text("Page Up", fontSize = 11.sp, color = GraySecondary)
                }
            }

            // Next Slide
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0369A1), Color(0xFF0284C7))
                        )
                    )
                    .border(BorderStroke(2.dp, Color(0xFF38BDF8)), RoundedCornerShape(20.dp))
                    .clickable { onNextSlide() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("➔", fontSize = 54.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("NEXT SLIDE", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Page Down / Space", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        // --- Hold-to-Laser Pointer Button (Air Mouse Gyroscope) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isLaserActive) Brush.horizontalGradient(
                        listOf(Color(0xFFDC2626), Color(0xFFEF4444))
                    )
                    else Brush.horizontalGradient(
                        listOf(Color(0xFF7F1D1D).copy(alpha = 0.5f), Color(0xFF991B1B).copy(alpha = 0.5f))
                    )
                )
                .border(
                    BorderStroke(1.5.dp, if (isLaserActive) Color(0xFFFCA5A5) else Color(0xFFEF4444).copy(alpha = 0.6f)),
                    RoundedCornerShape(16.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isLaserActive = true
                            onToggleLaserPointer(true)
                            tryAwaitRelease()
                            isLaserActive = false
                            onToggleLaserPointer(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = if (isLaserActive) Color.White.copy(alpha = laserAlpha) else Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isLaserActive) "🔴 LASER POINTER ACTIVE (WAVE PHONE)" else "HOLD FOR GYRO LASER POINTER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Wave phone to point directly at screen / projector",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // --- Slideshow Controls Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start F5
            PresentationActionTile(
                title = "Start (F5)",
                icon = "▶",
                color = Color(0xFF10B981),
                onClick = onStartSlideshow,
                modifier = Modifier.weight(1f)
            )
            // Exit Esc
            PresentationActionTile(
                title = "Exit (Esc)",
                icon = "⏹",
                color = Color(0xFF64748B),
                onClick = onEndSlideshow,
                modifier = Modifier.weight(1f)
            )
            // Blank Black 'B'
            PresentationActionTile(
                title = "Black (B)",
                icon = "⬛",
                color = Color(0xFF1E293B),
                onClick = onBlankB,
                modifier = Modifier.weight(1f)
            )
            // Blank White 'W'
            PresentationActionTile(
                title = "White (W)",
                icon = "⬜",
                color = Color(0xFF475569),
                onClick = onBlankW,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PresentationActionTile(
    title: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDark,
        border = BorderStroke(1.dp, BorderDark),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WhitePrimary)
        }
    }
}
