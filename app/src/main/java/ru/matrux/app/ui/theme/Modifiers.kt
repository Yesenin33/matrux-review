package ru.matrux.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.cardShadow(radius: Dp = Matrux.shapes.radiusCard): Modifier {
    val shadow = Matrux.shadow
    return drawBehind {
        val corner = radius.toPx()
        drawIntoCanvas { canvas ->
            listOf(
                Triple(shadow.nearY, shadow.nearBlur, shadow.nearAlpha),
                Triple(shadow.farY, shadow.farBlur, shadow.farAlpha),
            ).forEach { (dy, blur, alpha) ->
                val paint = Paint()
                paint.asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = Color.Black.copy(alpha = alpha).toArgb()
                    maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawRoundRect(0f, dy.toPx(), size.width, size.height + dy.toPx(), corner, corner, paint)
            }
        }
    }
}
