package ru.palma.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class PalmaColors(
    val canvas: Color = Color(0xFFF5F5F7),
    val surface: Color = Color(0xFFFFFFFF),
    val surfaceAlt: Color = Color(0xFFFAFAFA),
    val ink: Color = Color(0xFF1D1D1F),
    val inkSoft: Color = Color(0xFF515154),
    val muted: Color = Color(0xFF6E6E73),
    val accent: Color = Color(0xFF0071E3),
    val accentPress: Color = Color(0xFF0062C4),
    val accentTint: Color = Color(0xFFEAF2FD),
    val success: Color = Color(0xFF1D8A4E),
    val successTint: Color = Color(0xFFE7F4EC),
    val warning: Color = Color(0xFFB25000),
    val hairline: Color = Color(0xFFD2D2D7),
    val danger: Color = Color(0xFFD70015),
)
