package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MousePadTheme
import com.example.model.TouchpadLayout
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import java.io.File

@Composable
fun ThemesScreen(
    currentTheme: MousePadTheme,
    customImagePath: String?,
    currentLayout: TouchpadLayout,
    onSelectTheme: (MousePadTheme) -> Unit,
    onSetCustomImage: (String?) -> Unit,
    onSelectLayout: (TouchpadLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Gallery Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val destFile = File(context.filesDir, "custom_trackpad_bg.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onSetCustomImage(destFile.absolutePath)
            } catch (e: Exception) {
                // handle error gracefully
            }
        }
    }

    // Decode preview bitmap of custom image
    val customBitmap = remember(customImagePath) {
        if (!customImagePath.isNullOrBlank()) {
            val file = File(customImagePath)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

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
                    .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Layout & Pad Designs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePrimary
                )
                Text(
                    text = "Configure control ergonomics and choose unique surface styles",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
            }
        }

        // ==============================================================
        // 1. TOUCHPAD CONTROL LAYOUT (Moved from trackpad for clean screen)
        // ==============================================================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "TOUCHPAD CONTROL LAYOUT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.sp
            )
        }

        // Layout Cards
        LayoutSelectorCard(
            title = "Split Layout",
            subtitle = "Large trackpad + dedicated scroll strip + twin bottom click boxes",
            isSelected = currentLayout == TouchpadLayout.SPLIT,
            icon = Icons.Default.Mouse,
            accentColor = Color(0xFF38BDF8),
            onSelect = { onSelectLayout(TouchpadLayout.SPLIT) }
        )

        LayoutSelectorCard(
            title = "Classic Laptop",
            subtitle = "Traditional laptop trackpad with dual bottom left & right click buttons",
            isSelected = currentLayout == TouchpadLayout.CLASSIC,
            icon = Icons.Default.Laptop,
            accentColor = Color(0xFF34D399),
            onSelect = { onSelectLayout(TouchpadLayout.CLASSIC) }
        )

        LayoutSelectorCard(
            title = "Gesture Surface",
            subtitle = "100% full-screen gesture pad with tap-to-click, 2-finger scroll & drag lock",
            isSelected = currentLayout == TouchpadLayout.GESTURE,
            icon = Icons.Default.Gesture,
            accentColor = Color(0xFFF472B6),
            onSelect = { onSelectLayout(TouchpadLayout.GESTURE) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ==============================================================
        // 2. CUSTOM PHOTO BACKGROUND
        // ==============================================================
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "PERSONAL GALLERY PHOTO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(
                1.5.dp,
                if (currentTheme == MousePadTheme.CUSTOM_PHOTO) Color(0xFF38BDF8) else BorderDark
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Use any photo from your phone's gallery as your trackpad surface with automatic contrast optimization.",
                    fontSize = 12.sp,
                    color = GraySecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (customBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = customBitmap,
                            contentDescription = "Custom preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "✓ Custom Photo Active",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (customBitmap != null) "Change Photo" else "Pick from Gallery",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (customBitmap != null) {
                        OutlinedButton(
                            onClick = { onSetCustomImage(null) },
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==============================================================
        // 3. MOUSE PAD THEMES
        // ==============================================================
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MOUSE PAD DESIGNS (13 UNIQUE STYLES)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.sp
            )
        }

        val allThemes = listOf(
            MousePadTheme.STEALTH,
            MousePadTheme.CYBER_NEON,
            MousePadTheme.TOKYO_MIDNIGHT,
            MousePadTheme.AURORA_BOREALIS,
            MousePadTheme.RETRO_SYNTHWAVE,
            MousePadTheme.DEEP_SPACE,
            MousePadTheme.AMOLED_PITCH,
            MousePadTheme.FROSTED_GLASS,
            MousePadTheme.CARBON_FIBER,
            MousePadTheme.SUNSET_AURA,
            MousePadTheme.EMERALD_MATRIX,
            MousePadTheme.NORDIC_MINIMALIST,
            MousePadTheme.TITANIUM_METAL
        )

        allThemes.forEach { theme ->
            val isSelected = currentTheme == theme
            ThemeCardItem(
                theme = theme,
                isSelected = isSelected,
                onSelect = { onSelectTheme(theme) }
            )
        }
    }
}

@Composable
private fun LayoutSelectorCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF131A26) else CardDark,
        border = BorderStroke(
            if (isSelected) 1.8.dp else 1.dp,
            if (isSelected) accentColor else BorderDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF1E222D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else GraySecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accentColor else WhitePrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = GraySecondary,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCardItem(
    theme: MousePadTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val (cardBorderColor, accentColor, previewBrush) = remember(theme) {
        when (theme) {
            MousePadTheme.CYBER_NEON -> Triple(
                Color(0xFF06B6D4),
                Color(0xFF06B6D4),
                Brush.linearGradient(listOf(Color(0xFF070B14), Color(0xFF082F49)))
            )
            MousePadTheme.TOKYO_MIDNIGHT -> Triple(
                Color(0xFFEC4899),
                Color(0xFFF43F5E),
                Brush.linearGradient(listOf(Color(0xFF0D0C1D), Color(0xFF260D38), Color(0xFF080614)))
            )
            MousePadTheme.AURORA_BOREALIS -> Triple(
                Color(0xFF14B8A6),
                Color(0xFF10B981),
                Brush.linearGradient(listOf(Color(0xFF042421), Color(0xFF093931), Color(0xFF031015)))
            )
            MousePadTheme.AMOLED_PITCH -> Triple(
                Color(0xFF52525B),
                Color(0xFFFFFFFF),
                Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF18181B)))
            )
            MousePadTheme.RETRO_SYNTHWAVE -> Triple(
                Color(0xFFD946EF),
                Color(0xFFF472B6),
                Brush.linearGradient(listOf(Color(0xFF16062A), Color(0xFF2C0A47), Color(0xFF0D031A)))
            )
            MousePadTheme.DEEP_SPACE -> Triple(
                Color(0xFF6366F1),
                Color(0xFFA5B4FC),
                Brush.radialGradient(listOf(Color(0xFF1E1B4B), Color(0xFF060914)))
            )
            MousePadTheme.FROSTED_GLASS -> Triple(
                Color(0xFF94A3B8),
                Color(0xFFCBD5E1),
                Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
            )
            MousePadTheme.CARBON_FIBER -> Triple(
                Color(0xFF71717A),
                Color(0xFFA1A1AA),
                Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFF27272A)))
            )
            MousePadTheme.SUNSET_AURA -> Triple(
                Color(0xFFA855F7),
                Color(0xFFF472B6),
                Brush.linearGradient(listOf(Color(0xFF2E1065), Color(0xFF4C0519), Color(0xFF0F172A)))
            )
            MousePadTheme.EMERALD_MATRIX -> Triple(
                Color(0xFF10B981),
                Color(0xFF34D399),
                Brush.linearGradient(listOf(Color(0xFF021B10), Color(0xFF064E3B)))
            )
            MousePadTheme.NORDIC_MINIMALIST -> Triple(
                Color(0xFF94A3B8),
                Color(0xFFE2E8F0),
                Brush.linearGradient(listOf(Color(0xFF1E2229), Color(0xFF334155)))
            )
            MousePadTheme.TITANIUM_METAL -> Triple(
                Color(0xFF64748B),
                Color(0xFF94A3B8),
                Brush.linearGradient(listOf(Color(0xFF252A34), Color(0xFF191D24), Color(0xFF2C333F)))
            )
            else -> Triple(
                BorderDark,
                WhitePrimary,
                Brush.linearGradient(listOf(Color(0xFF16181F), Color(0xFF1E222D)))
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardDark,
        border = BorderStroke(
            if (isSelected) 1.8.dp else 1.dp,
            if (isSelected) cardBorderColor else BorderDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Theme Swatch
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(previewBrush)
                    .border(1.dp, cardBorderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (theme == MousePadTheme.CYBER_NEON || theme == MousePadTheme.EMERALD_MATRIX || theme == MousePadTheme.RETRO_SYNTHWAVE) {
                    Text("+", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accentColor else WhitePrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = theme.subtitle,
                    fontSize = 11.5.sp,
                    color = GraySecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = cardBorderColor,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
