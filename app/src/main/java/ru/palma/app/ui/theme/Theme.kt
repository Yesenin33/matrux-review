package ru.palma.app.ui.theme

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

val LocalPalmaColors = staticCompositionLocalOf { PalmaColors() }
val LocalPalmaType = staticCompositionLocalOf { PalmaType() }
val LocalPalmaShapes = staticCompositionLocalOf { PalmaShapes() }
val LocalPalmaSpace = staticCompositionLocalOf { PalmaSpace() }
val LocalPalmaShadow = staticCompositionLocalOf { PalmaShadow() }
val LocalPalmaMotion = staticCompositionLocalOf { PalmaMotion() }

object Palma {
    val colors: PalmaColors @Composable @ReadOnlyComposable get() = LocalPalmaColors.current
    val type: PalmaType @Composable @ReadOnlyComposable get() = LocalPalmaType.current
    val shapes: PalmaShapes @Composable @ReadOnlyComposable get() = LocalPalmaShapes.current
    val space: PalmaSpace @Composable @ReadOnlyComposable get() = LocalPalmaSpace.current
    val shadow: PalmaShadow @Composable @ReadOnlyComposable get() = LocalPalmaShadow.current
    val motion: PalmaMotion @Composable @ReadOnlyComposable get() = LocalPalmaMotion.current
}

fun PalmaColors.toScheme(): ColorScheme = lightColorScheme(
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

fun PalmaType.toTypography(): Typography = Typography(
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

fun PalmaShapes.toMaterial(): Shapes = Shapes(
    extraSmall = control,
    small = control,
    medium = control,
    large = card,
    extraLarge = card,
)

@Composable
fun PalmaTheme(content: @Composable () -> Unit) {
    val colors = PalmaColors()
    val type = PalmaType()
    val shapes = PalmaShapes()
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val reduced = !inspection && Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
    CompositionLocalProvider(
        LocalPalmaColors provides colors,
        LocalPalmaType provides type,
        LocalPalmaShapes provides shapes,
        LocalPalmaSpace provides PalmaSpace(),
        LocalPalmaShadow provides PalmaShadow(),
        LocalPalmaMotion provides PalmaMotion(reduced = reduced),
    ) {
        MaterialTheme(
            colorScheme = colors.toScheme(),
            typography = type.toTypography(),
            shapes = shapes.toMaterial(),
            content = content,
        )
    }
}
