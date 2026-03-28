package com.ruthless.spendguard.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Design Token System ──────────────────────────────────────────────────────
// All color usage goes through SG — never hardcode Color(0xFF...) in UI code.

object SG {
    // Backgrounds — layered dark surfaces
    val Background   = Color(0xFF0D0D0D)
    val Surface      = Color(0xFF141414)
    val Card         = Color(0xFF1A1A1A)
    val CardBorder   = Color(0xFF2A2A2A)
    val Divider      = Color(0xFF1F1F1F)

    // Accent — green for good, red for bad
    val Green        = Color(0xFF00D97E)   // slightly softer than pure neon — less eye strain
    val GreenSubtle  = Color(0x1A00D97E)
    val GreenBorder  = Color(0x3300D97E)
    val Red          = Color(0xFFFF453A)
    val RedSubtle    = Color(0x1AFF453A)
    val RedBorder    = Color(0x33FF453A)
    val Amber        = Color(0xFFFFB340)   // warning state (was missing)
    val AmberSubtle  = Color(0x1AFFB340)

    // Text hierarchy — three levels
    val TextPrimary  = Color(0xFFF2F2F7)
    val TextBody     = Color(0xFFAAAAAA)
    val TextDim      = Color(0xFF4A4A4A)
    val TextOnAccent = Color(0xFF0D0D0D)
}

private val ColorScheme = darkColorScheme(
    primary          = SG.Green,
    onPrimary        = SG.TextOnAccent,
    primaryContainer = SG.GreenSubtle,
    secondary        = SG.Amber,
    onSecondary      = SG.TextOnAccent,
    error            = SG.Red,
    onError          = Color.White,
    background       = SG.Background,
    onBackground     = SG.TextPrimary,
    surface          = SG.Surface,
    onSurface        = SG.TextPrimary,
    surfaceVariant   = SG.Card,
    onSurfaceVariant = SG.TextBody,
    outline          = SG.CardBorder,
)

// ─── Typography ───────────────────────────────────────────────────────────────
// Two-family system: Monospace for data/numbers/labels, SansSerif for body copy.

val SGTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,    fontSize = 52.sp, letterSpacing = (-1).sp,   lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,     fontSize = 36.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,     fontSize = 18.sp, letterSpacing = 3.sp),
    headlineMedium= TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 2.sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,   fontSize = 13.sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,   fontSize = 11.sp, letterSpacing = 2.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,   fontSize = 11.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,   fontSize = 9.sp,  letterSpacing = 1.5.sp),
)

@Composable
fun SpendGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = SGTypography,
        content = content
    )
}
