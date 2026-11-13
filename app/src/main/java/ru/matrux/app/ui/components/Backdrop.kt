package ru.matrux.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.matrux.app.ui.theme.Matrux

/** Узор-плата убран: фон сплошной. Функция оставлена для совместимости с прежними вызовами. */
@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val canvas = Matrux.colors.canvas
    Canvas(modifier.fillMaxSize()) { drawRect(canvas) }
}
