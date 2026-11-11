package ru.matrux.app.ui.theme

import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

val LocalMatruxColors = staticCompositionLocalOf { MatruxColors() }
val LocalMatruxType = staticCompositionLocalOf { MatruxType() }
val LocalMatruxShapes = staticCompositionLocalOf { MatruxShapes() }
val LocalMatruxSpace = staticCompositionLocalOf { MatruxSpace() }
val LocalMatruxShadow = staticCompositionLocalOf { MatruxShadow() }
val LocalMatruxMotion = staticCompositionLocalOf { MatruxMotion() }

object Matrux {
    val colors: MatruxColors @Composable @ReadOnlyComposable get() = LocalMatruxColors.current
    val type: MatruxType @Composable @ReadOnlyComposable get() = LocalMatruxType.current
    val shapes: MatruxShapes @Composable @ReadOnlyComposable get() = LocalMatruxShapes.current
    val space: MatruxSpace @Composable @ReadOnlyComposable get() = LocalMatruxSpace.current
    val shadow: MatruxShadow @Composable @ReadOnlyComposable get() = LocalMatruxShadow.current
    val motion: MatruxMotion @Composable @ReadOnlyComposable get() = LocalMatruxMotion.current
}

fun MatruxColors.toScheme(): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = surface,
    primaryContainer = accentTint,
    onPrimaryContainer = accent,
    secondary = inkSoft,
    onSecondary = surface,
    background = canvas,
    onBackground = ink,
    surface = surface,
    onSurface = ink,
    surfaceVariant = surfaceAlt,
    onSurfaceVariant = muted,
    outline = hairline,
    outlineVariant = hairline,
    error = danger,
    onError = surface,
)

fun MatruxType.toTypography(): Typography = Typography(
    displaySmall = display,
    headlineSmall = display,
    titleLarge = title,
    titleMedium = title,
    bodyLarge = body,
    bodyMedium = body,
    labelLarge = bodyStrong,
    bodySmall = caption,
    labelSmall = caption,
)

fun MatruxShapes.toMaterial(): Shapes = Shapes(
    extraSmall = control,
    small = control,
    medium = control,
    large = card,
    extraLarge = card,
)

@Composable
fun MatruxTheme(content: @Composable () -> Unit) {
    val colors = MatruxColors()
    val type = MatruxType()
    val shapes = MatruxShapes()
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val reduced = !inspection && Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
    CompositionLocalProvider(
        LocalMatruxColors provides colors,
        LocalMatruxType provides type,
        LocalMatruxShapes provides shapes,
        LocalMatruxSpace provides MatruxSpace(),
        LocalMatruxShadow provides MatruxShadow(),
        LocalMatruxMotion provides MatruxMotion(reduced = reduced),
    ) {
        MaterialTheme(
            colorScheme = colors.toScheme(),
            typography = type.toTypography(),
            shapes = shapes.toMaterial(),
            content = content,
        )
    }
}
