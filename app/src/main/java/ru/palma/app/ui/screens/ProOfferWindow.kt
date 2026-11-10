package ru.palma.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import ru.palma.app.R
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.state.LockedItem
import ru.palma.app.ui.state.ProOfferUi
import ru.palma.app.ui.theme.Palma

@Composable
fun ProOfferOverlay(visible: Boolean, ui: ProOfferUi, onSubscribe: () -> Unit, onClose: () -> Unit) {
    val motion = Palma.motion
    val lift = with(LocalDensity.current) { Palma.space.lift.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(motion.duration(motion.medium), easing = motion.easing)) +
            slideInVertically(tween(motion.duration(motion.medium), easing = motion.easing)) { lift },
        exit = fadeOut(tween(motion.duration(motion.short), easing = motion.easing)),
    ) {
        ProOfferWindow(ui, onSubscribe, onClose)
    }
}

@Composable
fun ProOfferWindow(ui: ProOfferUi, onSubscribe: () -> Unit, onClose: () -> Unit) {
    val colors = Palma.colors
    val space = Palma.space
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.ink.copy(alpha = space.scrimAlpha))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            Modifier
                .padding(horizontal = space.page)
                .clickable(remember { MutableInteractionSource() }, null) {},
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.offer_title), style = Palma.type.title, color = colors.ink, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(space.touch)
                        .clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(Lucide.X, colors.muted)
                }
            }
            Spacer(Modifier.height(space.unit))
            Column(verticalArrangement = Arrangement.spacedBy(space.unit * 2)) {
                ui.items.forEach { item -> OfferLine(item, ui) }
            }
            Spacer(Modifier.height(space.block))
            PrimaryButton(stringResource(R.string.pro_buy), onSubscribe)
        }
    }
}

@Composable
private fun OfferLine(item: LockedItem, ui: ProOfferUi) {
    val (icon, title, detail) = when (item) {
        LockedItem.SYSTEMS -> Triple(Lucide.Terminal, stringResource(R.string.offer_systems), ui.systems.joinToString(" · "))
        LockedItem.COURSES -> Triple(
            Lucide.BookOpen,
            stringResource(R.string.offer_courses),
            ui.courses.joinToString(" · ") + " · " + pluralStringResource(R.plurals.offer_lessons, ui.lessons, ui.lessons),
        )
        LockedItem.SHEETS -> Triple(Lucide.List, stringResource(R.string.offer_sheets), ui.sheets.joinToString(" · "))
        LockedItem.OFFLINE -> Triple(Lucide.WifiOff, stringResource(R.string.offer_offline), stringResource(R.string.offer_offline_detail))
    }
    Row(verticalAlignment = Alignment.Top) {
        LucideIcon(icon, Palma.colors.inkSoft)
        Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
        Column(Modifier.weight(1f)) {
            Text(title, style = Palma.type.bodyStrong, color = Palma.colors.ink)
            Text(detail, style = Palma.type.caption, color = Palma.colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
