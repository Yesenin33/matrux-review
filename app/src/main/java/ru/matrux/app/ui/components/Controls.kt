package ru.matrux.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.ui.theme.Matrux
import ru.matrux.app.ui.theme.cardShadow

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Lucide? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = Matrux.colors
    val motion = Matrux.motion
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = tween(motion.duration(motion.short), easing = motion.easing),
        label = "press",
    )
    val background = when {
        !enabled -> colors.surfaceAlt
        pressed -> colors.actionPress
        else -> colors.action
    }
    val foreground = if (enabled) colors.onAction else colors.muted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Matrux.space.button)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(Matrux.shapes.control)
            .background(background)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == null) {
            Text(text, style = Matrux.type.bodyStrong, color = foreground, maxLines = 1)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LucideIcon(icon, foreground, size = Matrux.space.iconSmall)
                Spacer(Modifier.width(Matrux.space.unit))
                Text(text, style = Matrux.type.bodyStrong, color = foreground, maxLines = 1)
            }
        }
    }
}

/** Кнопка на терминальной панели: янтарная, с тёмной подписью. */
@Composable
fun TerminalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = Matrux.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Matrux.space.button)
            .clip(Matrux.shapes.control)
            .background(if (pressed) colors.accentPress else colors.terminalButton)
            .clickable(source, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Matrux.type.bodyStrong, color = colors.onTerminalButton, maxLines = 1)
    }
}

/** Серая кнопка с цветной подписью: второе действие в карточке. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    height: Dp = Matrux.space.touch,
) {
    val colors = Matrux.colors
    Box(
        modifier = modifier
            .height(height)
            .clip(Matrux.shapes.control)
            .background(colors.surfaceAlt)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Matrux.type.bodyStrong,
            color = if (danger) colors.danger else colors.accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Matrux.space.unit * 2),
        )
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = Matrux.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Matrux.space.touch)
            .clip(Matrux.shapes.control)
            .clickable(source, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Matrux.type.bodyStrong, color = if (pressed) colors.accentPress else colors.accent)
    }
}

/** Плоская карточка: слой отделён цветом поверхности, без теней. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    tint: Color = Matrux.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.card)
            .background(tint)
            .padding(Matrux.space.card),
        content = content,
    )
}

/** Сплошная плашка статуса с белым текстом. */
@Composable
fun Plaque(text: String, background: Color, modifier: Modifier = Modifier, icon: Lucide? = null) {
    Row(
        modifier = modifier
            .height(22.dp)
            .clip(Matrux.shapes.pill)
            .background(background)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) LucideIcon(icon, Color.White, size = 12.dp)
        Text(
            text,
            style = Matrux.type.captionStrong.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StatePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.card)
            .background(Matrux.colors.surface)
            .padding(Matrux.space.card),
        content = content,
    )
}

@Composable
fun Note(icon: Lucide, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.control)
            .background(Matrux.colors.surface)
            .padding(horizontal = Matrux.space.unit * 2, vertical = Matrux.space.unit * 2),
        horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit),
    ) {
        LucideIcon(icon, Matrux.colors.muted, size = Matrux.space.iconSmall)
        Text(text, style = Matrux.type.caption, color = Matrux.colors.muted)
    }
}

@Composable
fun StatusDot() {
    val colors = Matrux.colors
    Canvas(Modifier.size(Matrux.space.unit)) {
        drawCircle(colors.success)
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = Matrux.colors.bar) {
    val colors = Matrux.colors
    val motion = Matrux.motion
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(motion.duration(motion.medium), easing = motion.easing),
        label = "progress",
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Matrux.space.progress),
    ) {
        val radius = size.height / 2
        drawLine(colors.hairline, Offset(radius, radius), Offset(size.width - radius, radius), size.height, StrokeCap.Round)
        if (animated > 0f) {
            val end = radius + (size.width - 2 * radius) * animated
            drawLine(color, Offset(radius, radius), Offset(end, radius), size.height, StrokeCap.Round)
        }
    }
}

@Composable
fun ProgressRing(fraction: Float, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = Matrux.colors
    val motion = Matrux.motion
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(motion.duration(motion.long), easing = motion.easing),
        label = "ring",
    )
    val ringStroke = Matrux.space.ringStroke
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = ringStroke.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(colors.hairline, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(colors.bar, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun DistroLogo(resource: Int?, modifier: Modifier = Modifier, size: Dp = Matrux.space.logo) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(Color.White)
            .border(1.dp, Matrux.colors.hairline, RoundedCornerShape(size * 0.24f)),
        contentAlignment = Alignment.Center,
    ) {
        if (resource != null) {
            Image(painterResource(resource), contentDescription = null, modifier = Modifier.size(size * 0.64f))
        }
    }
}

@Composable
fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit), verticalAlignment = Alignment.Top) {
        LucideIcon(Lucide.Check, Matrux.colors.success, size = Matrux.space.iconSmall)
        Text(text, style = Matrux.type.body, color = Matrux.colors.ink)
    }
}

@Composable
fun ErrorWords(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2)) {
        Text(title, style = Matrux.type.title, color = Matrux.colors.danger)
        Text(body, style = Matrux.type.body, color = Matrux.colors.muted)
    }
}

@Composable
fun CenterCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Matrux.type.caption,
        color = Matrux.colors.muted,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun Tappable(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .heightIn(min = Matrux.space.touch)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

internal const val PRESS_SCALE = 0.98f
internal const val DISABLED_ALPHA = 0.5f

@Composable
fun Spinner(modifier: Modifier = Modifier) {
    val colors = Matrux.colors
    val motion = Matrux.motion
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (motion.reduced) 1500 else 900, easing = LinearEasing)),
        label = "spinnerAngle",
    )
    Canvas(modifier.size(28.dp)) {
        val stroke = 2.5f.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(colors.hairline, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(colors.bar, angle - 90f, 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun Pill(text: String, tint: Color, background: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Matrux.type.captionStrong,
        color = tint,
        maxLines = 1,
        modifier = modifier
            .clip(Matrux.shapes.pill)
            .background(background)
            .padding(horizontal = Matrux.space.unit * 2, vertical = Matrux.space.unit / 2),
    )
}

@Composable
fun BusyPanel(title: String, failed: Boolean = false, modifier: Modifier = Modifier) {
    val colors = Matrux.colors
    Row(
        modifier = modifier
            .clip(Matrux.shapes.card)
            .background(colors.surface)
            .padding(horizontal = Matrux.space.card, vertical = Matrux.space.unit * 2),
        horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (failed) {
            LucideIcon(Lucide.CircleAlert, colors.danger, size = Matrux.space.iconLarge)
        } else {
            Spinner()
        }
        Text(
            title,
            style = Matrux.type.title,
            color = if (failed) colors.danger else colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
