package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.R
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Pill
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.state.HomeSection
import ru.matrux.app.ui.state.SectionRow
import ru.matrux.app.ui.theme.Matrux

data class HomeActions(
    val onSection: (SectionRow) -> Unit = {},
    val onSubscribe: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
)

private val LOCKED_TOPICS = listOf(
    R.string.home_topic_git,
    R.string.home_topic_files,
    R.string.home_topic_network,
    R.string.home_topic_scripts,
    R.string.home_topic_languages,
)

@Composable
fun HomeScreen(sections: List<HomeSection>, actions: HomeActions) {
    val basics = sections.firstOrNull { it.id == "basics" }
    val advanced = sections.firstOrNull { it.id == "advanced" }
    TabScreen(Tab.HOME, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Matrux.space.unit * 2))
        Header(stringResource(R.string.home_basics))
        Spacer(Modifier.height(Matrux.space.block))
        basics?.rows?.forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(Matrux.space.unit * 2))
            SectionCard(row) { actions.onSection(row) }
        }
        if (advanced != null) {
            Spacer(Modifier.height(Matrux.space.block))
            if (advanced.locked) {
                LockedCard(advanced, actions.onSubscribe)
            } else {
                Header(stringResource(advanced.title))
                Spacer(Modifier.height(Matrux.space.block))
                advanced.rows.forEachIndexed { index, row ->
                    if (index > 0) Spacer(Modifier.height(Matrux.space.unit * 2))
                    SectionCard(row) { actions.onSection(row) }
                }
            }
        }
        Spacer(Modifier.height(Matrux.space.block))
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = Matrux.type.display, color = Matrux.colors.ink)
    Spacer(Modifier.height(Matrux.space.unit))
    Box(
        Modifier
            .width(48.dp)
            .height(4.dp)
            .clip(Matrux.shapes.pill)
            .background(Matrux.colors.accent),
    )
}

@Composable
private fun SectionCard(row: SectionRow, onOpen: () -> Unit) {
    val colors = Matrux.colors
    val section = row.section
    Card(Modifier.clickable(role = Role.Button, onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionMark(section.id)
            Spacer(Modifier.width(Matrux.space.unit * 2))
            Column(Modifier.weight(1f)) {
                Text(
                    section.title,
                    style = Matrux.type.title,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Matrux.space.unit / 2))
                Text(
                    pluralStringResource(R.plurals.section_count, section.size, section.size),
                    style = Matrux.type.body,
                    color = colors.muted,
                )
            }
            Spacer(Modifier.width(Matrux.space.unit))
            LucideIcon(Lucide.ChevronRight, colors.muted, size = Matrux.space.iconSmall)
        }
    }
}

@Composable
private fun SectionMark(id: String) {
    Box(
        Modifier
            .size(Matrux.space.logoTile + Matrux.space.unit)
            .clip(CircleShape)
            .background(Matrux.colors.accentTint),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(sectionIcon(id), Matrux.colors.accent, size = Matrux.space.iconLarge)
    }
}

@Composable
private fun LockedCard(section: HomeSection, onSubscribe: () -> Unit) {
    val colors = Matrux.colors
    val topics = LOCKED_TOPICS.map { stringResource(it) }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(Matrux.space.logoTile)
                    .clip(CircleShape)
                    .background(Matrux.colors.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                LucideIcon(Lucide.Shield, Matrux.colors.accent, size = Matrux.space.iconLarge)
                LucideIcon(
                    Lucide.Crown,
                    Matrux.colors.accent,
                    size = 14.dp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.width(Matrux.space.unit + Matrux.space.unit / 2))
            Text(
                stringResource(section.title),
                style = Matrux.type.title.copy(fontSize = 18.sp, lineHeight = 22.sp),
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Matrux.space.unit))
            Pill(stringResource(R.string.section_pro), colors.accent, colors.accentTint)
        }
        Spacer(Modifier.height(Matrux.space.unit * 2))
        LockedRow(stringResource(R.string.home_pro_hint), colors.muted)
        topics.forEach { title -> LockedRow(title, colors.inkSoft) }
        Spacer(Modifier.height(Matrux.space.unit * 2))
        PrimaryButton(stringResource(R.string.pro_buy), onSubscribe, icon = Lucide.Crown)
    }
}

@Composable
private fun LockedRow(text: String, tint: Color) {
    val colors = Matrux.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LucideIcon(Lucide.Lock, colors.muted, size = Matrux.space.iconSmall - 2.dp)
        Spacer(Modifier.width(Matrux.space.unit + Matrux.space.unit / 2))
        Text(text, style = Matrux.type.body, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.hairline),
    )
    Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
}
