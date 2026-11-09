package ru.palma.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PalmaShapes(
    val radiusCard: Dp = 20.dp,
    val radiusControl: Dp = 12.dp,
    val radiusPill: Dp = 999.dp,
) {
    val card get() = RoundedCornerShape(radiusCard)
    val control get() = RoundedCornerShape(radiusControl)
    val pill get() = RoundedCornerShape(radiusPill)
}

@Immutable
data class PalmaSpace(
    val unit: Dp = 8.dp,
    val page: Dp = 20.dp,
    val block: Dp = 24.dp,
    val card: Dp = 20.dp,
    val button: Dp = 56.dp,
    val touch: Dp = 48.dp,
    val iconSmall: Dp = 20.dp,
    val iconLarge: Dp = 24.dp,
    val iconStroke: Float = 1.75f,
    val progress: Dp = 8.dp,
    val ring: Dp = 120.dp,
    val ringStroke: Dp = 8.dp,
    val logoTile: Dp = 44.dp,
    val logoMark: Dp = 28.dp,
    val navBar: Dp = 72.dp,
    val panel: Dp = 56.dp,
)

@Immutable
data class PalmaShadow(
    val nearY: Dp = 1.dp,
    val nearBlur: Dp = 2.dp,
    val nearAlpha: Float = 0.04f,
    val farY: Dp = 8.dp,
    val farBlur: Dp = 24.dp,
    val farAlpha: Float = 0.06f,
)
