package ru.palma.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

@Immutable
data class PalmaMotion(
    val short: Int = 180,
    val medium: Int = 260,
    val long: Int = 420,
    val easing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f),
    val reduced: Boolean = false,
) {
    fun duration(value: Int): Int = if (reduced) 0 else value
}
