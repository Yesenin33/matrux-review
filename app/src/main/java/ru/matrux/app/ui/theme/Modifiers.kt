package ru.matrux.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Тени в новом оформлении не используются: слои разделяются цветом поверхности.
 * Функция оставлена, чтобы код, который её вызывает, продолжал собираться.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun Modifier.cardShadow(radius: Dp = Matrux.shapes.radiusCard): Modifier = this
