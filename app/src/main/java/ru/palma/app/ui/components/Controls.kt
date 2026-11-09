package ru.palma.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import ru.palma.app.ui.theme.Palma
import ru.palma.app.ui.theme.cardShadow

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = Palma.colors
    val motion = Palma.motion
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = tween(motion.duration(motion.short), easing = motion.easing),
        label = "press",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Palma.space.button)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else DISABLED_ALPHA
            }
            .clip(Palma.shapes.control)
            .background(if (pressed) colors.accentPress else colors.accent)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Palma.type.bodyStrong, color = colors.surface)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = Palma.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Palma.space.button)
            .clip(Palma.shapes.control)
            .clickable(source, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Palma.type.bodyStrong, color = if (pressed) colors.accentPress else colors.accent)
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(Palma.shapes.card)
            .background(Palma.colors.surface)
            .padding(Palma.space.card),
        content = content,
    )
}

@Composable
fun StatePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Palma.shapes.card)
            .background(Palma.colors.surfaceAlt)
            .padding(Palma.space.card),
        content = content,
    )
}

@Composable
fun Note(icon: Lucide, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Palma.shapes.control)
            .background(Palma.colors.surfaceAlt)
            .padding(horizontal = Palma.space.unit * 2, vertical = Palma.space.unit * 2),
        horizontalArrangement = Arrangement.spacedBy(Palma.space.unit),
    ) {
        LucideIcon(icon, Palma.colors.muted, size = Palma.space.iconSmall)
        Text(text, style = Palma.type.caption, color = Palma.colors.muted)
    }
}

@Composable
fun StatusDot() {
    val colors = Palma.colors
    Canvas(Modifier.size(Palma.space.unit)) {
        drawCircle(colors.success)
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val colors = Palma.colors
    val motion = Palma.motion
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(motion.duration(motion.medium), easing = motion.easing),
        label = "progress",
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(Palma.space.progress),
    ) {
        val radius = size.height / 2
        drawLine(colors.hairline, Offset(radius, radius), Offset(size.width - radius, radius), size.height, StrokeCap.Round)
        if (animated > 0f) {
            val end = radius + (size.width - 2 * radius) * animated
            drawLine(colors.accent, Offset(radius, radius), Offset(end, radius), size.height, StrokeCap.Round)
        }
    }
}

@Composable
fun ProgressRing(fraction: Float, size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = Palma.colors
    val motion = Palma.motion
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(motion.duration(motion.long), easing = motion.easing),
        label = "ring",
    )
    val ringStroke = Palma.space.ringStroke
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = ringStroke.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(colors.hairline, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(colors.accent, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun DistroLogo(resource: Int?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Palma.space.logoTile)
            .clip(Palma.shapes.control)
            .background(Palma.colors.surfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        if (resource != null) {
            Image(painterResource(resource), contentDescription = null, modifier = Modifier.size(Palma.space.logoMark))
        }
    }
}

@Composable
fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Palma.space.unit), verticalAlignment = Alignment.Top) {
        LucideIcon(Lucide.Check, Palma.colors.success, size = Palma.space.iconSmall)
        Text(text, style = Palma.type.body, color = Palma.colors.inkSoft)
    }
}

@Composable
fun ErrorWords(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Palma.space.unit / 2)) {
        Text(title, style = Palma.type.title, color = Palma.colors.danger)
        Text(body, style = Palma.type.body, color = Palma.colors.muted)
    }
}

@Composable
fun CenterCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Palma.type.caption,
        color = Palma.colors.muted,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun Tappable(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .heightIn(min = Palma.space.touch)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private const val PRESS_SCALE = 0.98f
private const val DISABLED_ALPHA = 0.5f

@Composable
fun Spinner(modifier: Modifier = Modifier) {
    val colors = Palma.colors
    val motion = Palma.motion
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
        drawArc(colors.accent, angle - 90f, 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun Pill(text: String, tint: Color, background: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Palma.type.captionStrong,
        color = tint,
        maxLines = 1,
        modifier = modifier
            .clip(Palma.shapes.pill)
            .background(background)
            .padding(horizontal = Palma.space.unit * 2, vertical = Palma.space.unit / 2),
    )
}

@Composable
fun BusyPanel(title: String, failed: Boolean = false, modifier: Modifier = Modifier) {
    val colors = Palma.colors
    Row(
        modifier = modifier
            .clip(Palma.shapes.card)
            .background(colors.surfaceAlt)
            .padding(horizontal = Palma.space.card, vertical = Palma.space.unit * 2),
        horizontalArrangement = Arrangement.spacedBy(Palma.space.unit * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (failed) {
            LucideIcon(Lucide.CircleAlert, colors.danger, size = Palma.space.iconLarge)
        } else {
            Spinner()
        }
        Text(
            title,
            style = Palma.type.title,
            color = if (failed) colors.danger else colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
