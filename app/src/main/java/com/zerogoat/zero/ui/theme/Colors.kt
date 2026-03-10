package com.zerogoat.zero.ui.theme

import androidx.compose.ui.graphics.Color

// JARVIS-inspired dark palette with cyan accent
object ZeroColors {
    val BgPrimary = Color(0xFF0D1117)
    val BgSecondary = Color(0xFF161B22)
    val BgTertiary = Color(0xFF21262D)
    val Surface = Color(0xFF1C2128)
    val SurfaceElevated = Color(0xFF2D333B)

    val AccentCyan = Color(0xFF00E5FF)
    val AccentTeal = Color(0xFF00BFA5)
    val AccentBlue = Color(0xFF2979FF)
    val AccentGlow = Color(0x3300E5FF)

    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF8B949E)
    val TextMuted = Color(0xFF484F58)

    val Success = Color(0xFF3FB950)
    val Warning = Color(0xFFD29922)
    val Error = Color(0xFFF85149)

    // Aliases for dashboard
    val AccentGreen = Success
    val AccentRed = Error

    val UserBubble = Color(0xFF1F3A5F)
    val AgentBubble = Color(0xFF1C2128)
    val AgentBubbleBorder = Color(0xFF00E5FF)
}
