package ru.matrux.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.R
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Pill
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.state.HomeSection
import ru.matrux.app.ui.state.SectionRow
import ru.matrux.app.ui.theme.Matrux
import ru.matrux.app.ui.theme.cardShadow

private fun sectionImage(id: String): Int = when (id) {
    "start" -> R.drawable.sec_start
    "linux" -> R.drawable.sec_linux
    "terms" -> R.drawable.sec_terms
    "files" -> R.drawable.sec_files
    "git" -> R.drawable.sec_git
    "hardware" -> R.drawable.sec_hardware
    "packages" -> R.drawable.sec_packages
    "users" -> R.drawable.sec_users
    "services" -> R.drawable.sec_services
    "network" -> R.drawable.sec_network
    "disks" -> R.drawable.sec_disks
    "logs" -> R.drawable.sec_logs
    "scripts" -> R.drawable.sec_scripts
    "languages" -> R.drawable.sec_languages
    else -> R.drawable.sec_linux
}

data class HomeActions(
    val onSection: (SectionRow) -> Unit = {},
    val onSubscribe: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
)

@Composable
fun HomeScreen(sections: List<HomeSection>, actions: HomeActions) {
    val basics = sections.firstOrNull { it.id == "basics" }
    val advanced = sections.firstOrNull { it.id == "advanced" }
    val paid = advanced?.locked == false
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()

    TabScreen(Tab.HOME, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Matrux.space.unit * 2))
        Text(stringResource(R.string.home_title), style = Matrux.type.display, color = Matrux.colors.ink)
        Spacer(Modifier.height(Matrux.space.unit))
        Box(
            Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(Matrux.shapes.pill)
                .background(Matrux.colors.accent),
        )
        Spacer(Modifier.height(Matrux.space.block))
        SearchField(query) { query = it }
        Spacer(Modifier.height(Matrux.space.block))

        val pool = basics?.rows.orEmpty() + if (paid) advanced?.rows.orEmpty() else emptyList()
        if (needle.isEmpty()) {
            Basics(basics, actions)
            if (advanced != null && !paid) Advanced(advanced, actions)
            if (advanced != null && paid) AdvancedOpen(advanced, actions)
        } else {
            val found = pool.mapNotNull { row -> matches(row, needle)?.let { row to it } }
            if (found.isEmpty()) {
                Text(stringResource(R.string.section_search_empty), style = Matrux.type.body, color = Matrux.colors.muted)
            } else {
                found.forEachIndexed { index, (row, hits) ->
                    if (index > 0) Spacer(Modifier.height(Matrux.space.unit * 2))
                    SectionCard(row, hits) { actions.onSection(row) }
                }
            }
        }
        Spacer(Modifier.height(Matrux.space.block))
    }
}

internal fun matches(row: SectionRow, raw: String): Int? {
    val needle = raw.trim().lowercase()
    if (needle.isEmpty()) return null
    val section = row.section
    if (section.title.lowercase().contains(needle)) return section.size
    val hits = section.groups.sumOf { group ->
        group.entries.count { entry ->
            entry.title.lowercase().contains(needle) ||
                entry.summary.lowercase().contains(needle) ||
                entry.detail?.lowercase()?.contains(needle) == true
        }
    }
    return hits.takeIf { it > 0 }
}

@Composable
private fun Basics(section: HomeSection?, actions: HomeActions) {
    if (section == null) return
    SectionHead(stringResource(section.title), R.string.home_free_badge, null, Matrux.colors.success, Matrux.colors.successTint, null)
    Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
    section.rows.forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(Matrux.space.unit))
        SectionCard(row, null) { actions.onSection(row) }
    }
}

@Composable
private fun Advanced(section: HomeSection, actions: HomeActions) {
    Spacer(Modifier.height(Matrux.space.block))
    SectionHead(
        stringResource(section.title),
        R.string.section_pro,
        stringResource(R.string.pro_buy),
        Matrux.colors.accent,
        Matrux.colors.accentTint,
        actions.onSubscribe,
    )
    Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
    section.rows.forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(Matrux.space.unit))
        SectionCard(row, null) { actions.onSubscribe() }
    }
}

@Composable
private fun AdvancedOpen(section: HomeSection, actions: HomeActions) {
    Spacer(Modifier.height(Matrux.space.block))
    SectionHead(stringResource(section.title), null, null, Matrux.colors.accent, Matrux.colors.accentTint, null)
    Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
    section.rows.forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(Matrux.space.unit))
        SectionCard(row, null) { actions.onSection(row) }
    }
}

@Composable
private fun SectionHead(
    title: String,
    badge: Int?,
    action: String?,
    tint: androidx.compose.ui.graphics.Color,
    badgeBackground: androidx.compose.ui.graphics.Color,
    onAction: (() -> Unit)?,
) {
    val colors = Matrux.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = Matrux.type.title.copy(fontSize = 17.sp, lineHeight = 22.sp),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (badge != null) {
                Spacer(Modifier.width(Matrux.space.unit))
                Badge(stringResource(badge), tint)
            }
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.width(Matrux.space.unit))
            Text(
                action,
                style = Matrux.type.caption,
                color = colors.surface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(Matrux.shapes.control)
                    .background(colors.accent)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = Matrux.space.unit, vertical = Matrux.space.unit + 2.dp),
            )
        }
    }
}

@Composable
private fun Badge(text: String, background: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = Matrux.type.captionStrong,
        color = Matrux.colors.surface,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(Matrux.shapes.pill)
            .background(background)
            .padding(horizontal = Matrux.space.unit + 2.dp, vertical = 3.dp),
    )
}

@Composable
private fun SectionCard(row: SectionRow, found: Int?, onOpen: () -> Unit) {
    val colors = Matrux.colors
    val section = row.section
    Column(
        Modifier
            .fillMaxWidth()
            .cardShadow()
            .clip(Matrux.shapes.control)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = Matrux.space.unit * 2, vertical = Matrux.space.unit + Matrux.space.unit / 2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionMark(section.id, row.locked)
            Spacer(Modifier.width(Matrux.space.unit * 2))
            Column(Modifier.weight(1f)) {
                Text(
                    section.title,
                    style = Matrux.type.title.copy(fontSize = 17.sp, lineHeight = 22.sp),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Matrux.space.unit / 2))
                Text(
                    if (found == null) {
                        pluralStringResource(R.plurals.section_count, section.size, section.size)
                    } else {
                        pluralStringResource(R.plurals.home_found, found, found)
                    },
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
private fun SectionMark(id: String, locked: Boolean) {
    val colors = Matrux.colors
    Box(
        Modifier
            .size(Matrux.space.logoTile + 4.dp)
            .clip(CircleShape)
            .background(if (locked) colors.muted else colors.accentTint),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(if (locked) R.drawable.sec_lock else sectionImage(id)),
            contentDescription = null,
            modifier = Modifier.size(if (locked) Matrux.space.iconLarge else Matrux.space.iconLarge + 2.dp),
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val colors = Matrux.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.control)
            .background(colors.surface)
            .border(1.dp, colors.hairline, Matrux.shapes.control)
            .padding(horizontal = Matrux.space.unit * 2, vertical = Matrux.space.unit + Matrux.space.unit / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.Search, colors.muted, size = Matrux.space.iconSmall)
        Spacer(Modifier.width(Matrux.space.unit))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(stringResource(R.string.home_search), style = Matrux.type.body, color = colors.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Matrux.type.body.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            LucideIcon(
                Lucide.X,
                colors.muted,
                size = Matrux.space.iconSmall,
                modifier = Modifier.clickable(role = Role.Button) { onChange("") },
            )
        }
    }
}
