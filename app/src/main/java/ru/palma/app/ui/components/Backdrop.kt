package ru.palma.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import ru.palma.app.R
import ru.palma.app.ui.theme.Palma

private const val PATTERN_ALPHA = 0.22f

@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val colors = Palma.colors
    val tile = ImageBitmap.imageResource(R.drawable.pattern_circuit)
    val pattern = remember(tile) {
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(brush = pattern, alpha = PATTERN_ALPHA)
        drawRect(
            brush = Brush.verticalGradient(
                0f to colors.canvas.copy(alpha = 0f),
                0.42f to colors.canvas.copy(alpha = 0.30f),
                0.72f to colors.canvas.copy(alpha = 0.85f),
                1f to colors.canvas,
            ),
        )
    }
}
