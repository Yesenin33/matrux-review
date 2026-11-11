package ru.matrux.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import ru.matrux.app.R
import ru.matrux.app.ui.components.DISABLED_ALPHA
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.PRESS_SCALE
import ru.matrux.app.ui.components.StatusDot
import ru.matrux.app.ui.state.GateSystem
import ru.matrux.app.ui.state.SignInUi
import ru.matrux.app.ui.theme.Matrux
import ru.matrux.app.ui.theme.cardShadow

@Composable
fun SignInOverlay(visible: Boolean, ui: SignInUi, onSignIn: () -> Unit) {
    val motion = Matrux.motion
    val lift = with(LocalDensity.current) { Matrux.space.lift.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(motion.duration(motion.medium), easing = motion.easing)) +
            slideInVertically(tween(motion.duration(motion.medium), easing = motion.easing)) { lift },
        exit = fadeOut(tween(motion.duration(motion.short), easing = motion.easing)),
    ) {
        SignInScreen(ui, onSignIn)
    }
}

@Composable
fun SignInScreen(ui: SignInUi, onSignIn: () -> Unit) {
    val space = Matrux.space
    Box(
        Modifier
            .fillMaxSize()
            .background(Matrux.colors.canvas)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    ) {
        Image(
            painterResource(R.drawable.penguin_hero),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .alpha(space.heroArtAlpha),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = space.page)
                .padding(bottom = space.block),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Avatar(ui.signedIn)
            Spacer(Modifier.height(space.block))
            SystemState(ui.system)
            Spacer(Modifier.weight(1f))
            VkButton(ui.enabled, onSignIn)
        }
    }
}

@Composable
private fun Avatar(signedIn: Boolean) {
    val colors = Matrux.colors
    val space = Matrux.space
    val ring = space.avatar + space.avatarRing * 2
    Box(Modifier.size(ring)) {
        Box(
            Modifier
                .size(ring)
                .cardShadow(ring / 2)
                .clip(Matrux.shapes.pill)
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.penguin_mark), contentDescription = null, modifier = Modifier.size(space.avatar))
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(space.avatarBadge)
                .clip(Matrux.shapes.pill)
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (signedIn) {
                LucideIcon(Lucide.BadgeCheck, colors.success, size = space.iconSmall)
            } else {
                LucideIcon(Lucide.Lock, colors.muted, size = space.iconSmall)
            }
        }
    }
}

@Composable
private fun SystemState(system: GateSystem) {
    val colors = Matrux.colors
    when (system) {
        is GateSystem.Running -> SystemLines(system.name) {
            StatusDot()
            Text(stringResource(R.string.sign_in_running), style = Matrux.type.body, color = colors.muted)
        }
        is GateSystem.Stopped -> SystemLines(system.name) {
            Text(stringResource(R.string.sign_in_stopped), style = Matrux.type.body, color = colors.muted)
        }
        is GateSystem.Installing -> SystemLines(system.name) {
            Text(stringResource(R.string.home_installing_state, system.percent), style = Matrux.type.body, color = colors.muted)
        }
        GateSystem.Absent -> Text(stringResource(R.string.home_empty_state), style = Matrux.type.body, color = colors.muted)
    }
}

@Composable
private fun SystemLines(name: String, status: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = Matrux.type.title, color = Matrux.colors.ink)
        Spacer(Modifier.height(Matrux.space.unit))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit),
        ) { status() }
    }
}

@Composable
private fun VkButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = Matrux.colors
    val space = Matrux.space
    val motion = Matrux.motion
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = tween(motion.duration(motion.short), easing = motion.easing),
        label = "press",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(space.button)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else DISABLED_ALPHA
            }
            .cardShadow(Matrux.shapes.radiusPill)
            .clip(Matrux.shapes.pill)
            .background(colors.surface)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.ic_vk), contentDescription = null, modifier = Modifier.size(space.iconSmall))
        Spacer(Modifier.width(space.unit + space.unit / 2))
        Text(stringResource(R.string.sign_in_vk), style = Matrux.type.bodyStrong, color = colors.ink)
    }
}
