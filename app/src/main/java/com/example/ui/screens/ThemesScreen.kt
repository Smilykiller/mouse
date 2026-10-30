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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
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
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.TrueBlack
import com.example.ui.theme.WhitePrimary
import java.io.File

@Composable
fun ThemesScreen(
    currentTheme: MousePadTheme,
    customImagePath: String?,
    onSelectTheme: (MousePadTheme) -> Unit,
    onSetCustomImage: (String?) -> Unit,
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
                    text = "Mouse Pad Designs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhitePrimary
                )
                Text(
                    text = "Customize the look and feel of your touchpad surface",
                    fontSize = 12.sp,
                    color = GraySecondary
                )
            }
        }

        // 1. Custom Photo Section (Featured Card)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOM PHOTO PAD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhitePrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    if (currentTheme == MousePadTheme.CUSTOM_PHOTO) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Use any wallpaper, picture, or photo from your phone's gallery as your trackpad surface.",
                    fontSize = 12.sp,
                    color = GraySecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Preview Thumbnail if photo is set
                if (customBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = customBitmap,
                            contentDescription = "Custom Pad Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "✓ Custom Photo Loaded",
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
                            Text("Remove", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Preset Themes Section
        Text(
            text = "PRESET STYLES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GrayMuted,
            letterSpacing = 1.sp
        )

        val presetThemes = listOf(
            MousePadTheme.CYBER_NEON,
            MousePadTheme.FROSTED_GLASS,
            MousePadTheme.CARBON_FIBER,
            MousePadTheme.SUNSET_AURA,
            MousePadTheme.EMERALD_MATRIX,
            MousePadTheme.STEALTH
        )

        presetThemes.forEach { theme ->
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
                if (theme == MousePadTheme.CYBER_NEON || theme == MousePadTheme.EMERALD_MATRIX) {
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
