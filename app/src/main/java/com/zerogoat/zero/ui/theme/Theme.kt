package com.zerogoat.zero.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZeroDarkColorScheme = darkColorScheme(
    primary = ZeroColors.AccentCyan,
    onPrimary = ZeroColors.BgPrimary,
    secondary = ZeroColors.AccentTeal,
    onSecondary = ZeroColors.BgPrimary,
    tertiary = ZeroColors.AccentBlue,
    background = ZeroColors.BgPrimary,
    onBackground = ZeroColors.TextPrimary,
    surface = ZeroColors.Surface,
    onSurface = ZeroColors.TextPrimary,
    surfaceVariant = ZeroColors.SurfaceElevated,
    onSurfaceVariant = ZeroColors.TextSecondary,
    error = ZeroColors.Error,
    onError = Color.White,
    outline = ZeroColors.TextMuted,
)

@Composable
fun ZeroGoatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZeroDarkColorScheme,
        content = content
    )
}
