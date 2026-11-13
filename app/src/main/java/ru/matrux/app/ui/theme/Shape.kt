package ru.matrux.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MatruxShapes(
    val radiusCard: Dp = 18.dp,
    val radiusControl: Dp = 14.dp,
    val radiusSegment: Dp = 9.dp,
    val radiusPill: Dp = 999.dp,
) {
    val card get() = RoundedCornerShape(radiusCard)
    val control get() = RoundedCornerShape(radiusControl)
    val segment get() = RoundedCornerShape(radiusSegment)
    val pill get() = RoundedCornerShape(radiusPill)
}

@Immutable
data class MatruxSpace(
    val unit: Dp = 8.dp,
    val page: Dp = 16.dp,
    val block: Dp = 24.dp,
    val card: Dp = 16.dp,
    val button: Dp = 52.dp,
    val touch: Dp = 48.dp,
    val iconSmall: Dp = 20.dp,
    val iconLarge: Dp = 24.dp,
    val iconStroke: Float = 1.75f,
    val progress: Dp = 8.dp,
    val ring: Dp = 120.dp,
    val ringStroke: Dp = 8.dp,
    val logoTile: Dp = 36.dp,
    val logoMark: Dp = 28.dp,
    val navBar: Dp = 64.dp,
    val panel: Dp = 56.dp,
    val avatar: Dp = 88.dp,
    val avatarRing: Dp = 8.dp,
    val avatarBadge: Dp = 28.dp,
    val heroArt: Dp = 320.dp,
    val heroArtAlpha: Float = 0.05f,
    val lift: Dp = 12.dp,
    val trackInset: Dp = 4.dp,
    val scrimAlpha: Float = 0.36f,
    val lessonMark: Dp = 28.dp,
    val preview: Dp = 240.dp,
    val onboardArt: Dp = 136.dp,
    val onboardMark: Dp = 96.dp,
    val onboardRow: Dp = 40.dp,
    val dot: Dp = 8.dp,
    val row: Dp = 48.dp,
    val rowInset: Dp = 64.dp,
    val tile: Dp = 32.dp,
    val logo: Dp = 56.dp,
)

@Immutable
data class MatruxShadow(
    val nearY: Dp = 1.dp,
    val nearBlur: Dp = 2.dp,
    val nearAlpha: Float = 0.04f,
    val farY: Dp = 8.dp,
    val farBlur: Dp = 24.dp,
    val farAlpha: Float = 0.06f,
)
