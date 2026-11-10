package ru.palma.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ru.palma.app.R
import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanOffer
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.DistroLogo
import ru.palma.app.ui.components.ErrorWords
import ru.palma.app.ui.components.GhostButton
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.components.TabScreen
import ru.palma.app.ui.components.Tappable
import ru.palma.app.ui.components.TermPicker
import ru.palma.app.ui.imageLogo
import ru.palma.app.ui.state.HomeBlock
import ru.palma.app.ui.state.HomeUi
import ru.palma.app.ui.state.LockedItem
import ru.palma.app.ui.state.ProOfferUi
import ru.palma.app.ui.state.homeBlocks
import ru.palma.app.ui.subLineText
import ru.palma.app.ui.theme.Palma

private const val LOGO_LIMIT = 6

data class HomeActions(
    val onBuy: (PlanOffer) -> Unit = {},
    val onSelect: (Plan) -> Unit = {},
    val onLocked: () -> Unit = {},
    val onReRegister: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
)

@Composable
fun HomeScreen(ui: HomeUi, actions: HomeActions) {
    TabScreen(Tab.HOME, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Palma.space.unit * 2))
        homeBlocks(ui).forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(Palma.space.block))
            when (block) {
                HomeBlock.REVOKED -> Revoked(actions.onReRegister)
                HomeBlock.BUY -> PrimaryButton(stringResource(R.string.home_buy), onClick = { ui.terms.selectedOffer?.let(actions.onBuy) })
                HomeBlock.STATUS -> Status(ui)
                HomeBlock.TERMS -> Card { TermPicker(ui.terms, actions.onSelect) }
                HomeBlock.INSIDE -> Inside(ui)
                HomeBlock.LOCKED -> LockedLine(ui.locked, actions.onLocked)
            }
        }
        Spacer(Modifier.height(Palma.space.block))
    }
}

@Composable
private fun Revoked(onReRegister: () -> Unit) {
    Column {
        ErrorWords(stringResource(R.string.home_revoked_title), stringResource(R.string.home_revoked_detail))
        GhostButton(stringResource(R.string.home_reregister), onReRegister)
    }
}

@Composable
private fun Status(ui: HomeUi) {
    Row(Modifier.fillMaxWidth().heightIn(min = Palma.space.touch), verticalAlignment = Alignment.CenterVertically) {
        LucideIcon(Lucide.BadgeCheck, Palma.colors.success)
        Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
        Text(subLineText(ui.sub), style = Palma.type.title, color = Palma.colors.ink)
    }
}

@Composable
private fun Inside(ui: HomeUi) {
    val colors = Palma.colors
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit * 2)) {
        Text(stringResource(R.string.home_inside), style = Palma.type.title, color = colors.ink)
        if (ui.systems.isNotEmpty()) SystemsCard(ui)
        if (ui.courses.isNotEmpty()) KnowledgeCard(ui)
        Column {
            Feature(Lucide.Terminal, R.string.home_feature_linux, R.string.home_feature_linux_detail)
            Feature(Lucide.WifiOff, R.string.home_feature_offline, R.string.home_feature_offline_detail)
        }
    }
}

@Composable
private fun SystemsCard(ui: HomeUi) {
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
            ui.systems.take(LOGO_LIMIT).forEach { DistroLogo(imageLogo(it)) }
        }
        Spacer(Modifier.height(Palma.space.unit * 2))
        Text(
            pluralStringResource(R.plurals.home_systems, ui.systems.size, ui.systems.size),
            style = Palma.type.bodyStrong,
            color = Palma.colors.ink,
        )
        Spacer(Modifier.height(Palma.space.unit / 2))
        Text(ui.systems.joinToString(" · ") { it.name }, style = Palma.type.body, color = Palma.colors.muted)
    }
}

@Composable
private fun KnowledgeCard(ui: HomeUi) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LucideIcon(Lucide.BookOpen, Palma.colors.inkSoft)
            Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
            Text(
                pluralStringResource(R.plurals.home_courses, ui.courses.size, ui.courses.size) + " · " +
                    pluralStringResource(R.plurals.course_lessons, ui.lessons, ui.lessons),
                style = Palma.type.bodyStrong,
                color = Palma.colors.ink,
            )
        }
        Spacer(Modifier.height(Palma.space.unit))
        Text(
            ui.courses.joinToString(" · ") { it.title },
            style = Palma.type.body,
            color = Palma.colors.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (ui.sheets > 0) {
            Spacer(Modifier.height(Palma.space.unit / 2))
            Text(pluralStringResource(R.plurals.home_sheets, ui.sheets, ui.sheets), style = Palma.type.caption, color = Palma.colors.muted)
        }
    }
}

@Composable
private fun Feature(icon: Lucide, title: Int, detail: Int) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Palma.space.button),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(icon, Palma.colors.inkSoft)
        Spacer(Modifier.width(Palma.space.unit * 2))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = Palma.type.bodyStrong, color = Palma.colors.ink)
            Text(stringResource(detail), style = Palma.type.caption, color = Palma.colors.muted)
        }
    }
}

@Composable
private fun LockedLine(locked: ProOfferUi, onOpen: () -> Unit) {
    val summary = lockedSummary(locked)
    Tappable(onOpen, Modifier.fillMaxWidth()) {
        LucideIcon(Lucide.Lock, Palma.colors.muted, size = Palma.space.iconSmall)
        Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_locked), style = Palma.type.body, color = Palma.colors.ink)
            if (summary.isNotEmpty()) {
                Text(summary, style = Palma.type.caption, color = Palma.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        LucideIcon(Lucide.ChevronRight, Palma.colors.muted, size = Palma.space.iconSmall)
    }
}

@Composable
private fun lockedSummary(locked: ProOfferUi): String = locked.items.map { item ->
    when (item) {
        LockedItem.COURSES -> stringResource(R.string.locked_courses)
        LockedItem.SYSTEMS -> pluralStringResource(R.plurals.locked_systems, locked.systems.size, locked.systems.size)
        LockedItem.SHEETS -> stringResource(R.string.locked_sheets)
        LockedItem.OFFLINE -> stringResource(R.string.locked_offline)
    }
}.joinToString(" · ")
