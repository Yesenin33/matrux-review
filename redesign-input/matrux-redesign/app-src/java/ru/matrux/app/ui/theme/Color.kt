package ru.matrux.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MatruxColors(
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
    val onAccent: Color = Color(0xFFFFFFFF),
)

val LightColors = MatruxColors()

val DarkColors = MatruxColors(
    canvas = Color(0xFF0E1116),
    surface = Color(0xFF171C22),
    surfaceAlt = Color(0xFF1D232B),
    ink = Color(0xFFF2F4F7),
    inkSoft = Color(0xFFC3CAD4),
    muted = Color(0xFF939CA7),
    accent = Color(0xFF3F90F7),
    accentPress = Color(0xFF2F7CE2),
    accentTint = Color(0xFF1A2634),
    success = Color(0xFF3FBF74),
    successTint = Color(0xFF16301F),
    warning = Color(0xFFE0913F),
    hairline = Color(0xFF2C333C),
    danger = Color(0xFFFF5D62),
    onAccent = Color(0xFFFFFFFF),
)
