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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
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

            // Outer border
            drawRoundRect(
                color = if (isDragging) BorderHighlight else BorderDark,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Center track guideline
            drawLine(
                color = if (isDragging) WhitePrimary else BorderDark,
                start = Offset(width / 2f, 24.dp.toPx()),
                end = Offset(width / 2f, height - 24.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )

            // Thumb
            currentIndicatorY?.let { yPos ->
                val clampedY = yPos.coerceIn(30.dp.toPx(), height - 30.dp.toPx())
                drawCircle(
                    color = WhitePrimary,
                    radius = 12.dp.toPx(),
                    center = Offset(width / 2f, clampedY)
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
                tint = if (isDragging) WhitePrimary else GrayMuted
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Scroll Wheel",
                    tint = if (isDragging) WhitePrimary else GrayMuted
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDragging) WhitePrimary else GrayMuted,
                    letterSpacing = 1.sp
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Scroll Down",
                tint = if (isDragging) WhitePrimary else GrayMuted
            )
        }
    }
}
