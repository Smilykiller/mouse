package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = WhitePrimary,
    onPrimary = TrueBlack,
    primaryContainer = ElevatedSurface,
    onPrimaryContainer = WhitePrimary,
    secondary = GraySecondary,
    onSecondary = TrueBlack,
    secondaryContainer = CardDark,
    onSecondaryContainer = WhitePrimary,
    tertiary = WhitePrimary,
    onTertiary = TrueBlack,
    background = TrueBlack,
    onBackground = WhitePrimary,
    surface = DarkSurface,
    onSurface = WhitePrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = GraySecondary,
    outline = BorderDark,
    outlineVariant = BorderHighlight,
    error = DotRedMild,
    onError = WhitePrimary
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = TrueBlack,
    onPrimary = WhitePrimary,
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = TrueBlack,
    secondary = Color(0xFF52525B),
    onSecondary = WhitePrimary,
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = TrueBlack,
    tertiary = TrueBlack,
    onTertiary = WhitePrimary,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurf,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFE4E4E7),
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderColor,
    outlineVariant = Color(0xFFD4D4D8),
    error = DotRedMild,
    onError = WhitePrimary
)

@Composable
fun AirGlideTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MonochromeDarkColorScheme
        else -> MonochromeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
