package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CardDark
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.WhitePrimary

@Composable
fun ScrollStrip(
    onScroll: (Float) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "SCROLL"
) {
    var isDragging by remember { mutableStateOf(false) }
    var currentIndicatorY by remember { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .width(68.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF131D2E), Color(0xFF090D15))
                )
            )
            .testTag("scroll_strip_zone")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        currentIndicatorY = offset.y
                    },
                    onDragEnd = {
                        isDragging = false
                        currentIndicatorY = null
                    },
                    onDragCancel = {
                        isDragging = false
                        currentIndicatorY = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentIndicatorY = change.position.y
                        onScroll(-dragAmount.y)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cx = width / 2f

            // Outer border with smooth corner radius
            drawRoundRect(
                color = if (isDragging) Color(0xFF38BDF8) else Color(0xFF1E293B),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                style = Stroke(width = if (isDragging) 1.5.dp.toPx() else 1.dp.toPx())
            )

            // Center track guideline
            drawLine(
                color = if (isDragging) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF334155).copy(alpha = 0.4f),
                start = Offset(cx, 32.dp.toPx()),
                end = Offset(cx, height - 32.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Tactile Ribbed Notches along scroll wheel track
            val notchSpacing = 16.dp.toPx()
            var ny = 40.dp.toPx()
            val notchArm = 8.dp.toPx()
            while (ny < height - 40.dp.toPx()) {
                drawLine(
                    color = if (isDragging) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFF475569).copy(alpha = 0.25f),
                    start = Offset(cx - notchArm, ny),
                    end = Offset(cx + notchArm, ny),
                    strokeWidth = 1.2.dp.toPx()
                )
                ny += notchSpacing
            }

            // Glowing Dynamic Thumb
            currentIndicatorY?.let { yPos ->
                val clampedY = yPos.coerceIn(36.dp.toPx(), height - 36.dp.toPx())
                // Soft glow
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.30f),
                    radius = 18.dp.toPx(),
                    center = Offset(cx, clampedY)
                )
                // Solid thumb ring
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 11.dp.toPx(),
                    center = Offset(cx, clampedY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(cx, clampedY)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll Up",
                tint = if (isDragging) Color(0xFF38BDF8) else GrayMuted,
                modifier = Modifier.size(18.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Scroll Wheel",
                    tint = if (isDragging) Color(0xFF38BDF8) else GraySecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDragging) Color(0xFF38BDF8) else GrayMuted,
                    letterSpacing = 1.sp
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll Down",
                tint = if (isDragging) Color(0xFF38BDF8) else GrayMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
