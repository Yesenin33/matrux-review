package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.matrux.app.R
import ru.matrux.app.ui.components.DistroLogo
import ru.matrux.app.ui.components.GlyphTile
import ru.matrux.app.ui.components.Group
import ru.matrux.app.ui.components.GroupDivider
import ru.matrux.app.ui.components.LargeTitle
import ru.matrux.app.ui.components.ListRow
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.PromptLine
import ru.matrux.app.ui.components.SectionTitle
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.components.TerminalButton
import ru.matrux.app.ui.components.TerminalPanel
import ru.matrux.app.ui.components.sectionStyle
import ru.matrux.app.ui.state.HomeSection
import ru.matrux.app.ui.state.SectionRow
import ru.matrux.app.ui.theme.Matrux

/** Установленная система для панели на «Главной». [host] — короткое имя для приглашения оболочки. */
data class HomeSystem(val name: String, val host: String, val running: Boolean, val logo: Int? = null)

data class HomeActions(
    val onSection: (SectionRow) -> Unit = {},
    val onSubscribe: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
    val dark: Boolean = false,
    val onTheme: (Boolean) -> Unit = {},
    val onLaunch: () -> Unit = {},
    val onOpenSystem: () -> Unit = {},
)

@Composable
fun HomeScreen(sections: List<HomeSection>, actions: HomeActions, system: HomeSystem? = null) {
    val basics = sections.firstOrNull { it.id == "basics" }
    val advanced = sections.firstOrNull { it.id == "advanced" }
    val paid = advanced?.locked == false
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()

    TabScreen(Tab.HOME, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Matrux.space.unit))
        LargeTitle(stringResource(R.string.home_title), trailing = { ThemeSwitch(actions.dark, actions.onTheme) })
        Spacer(Modifier.height(18.dp))
        SearchField(query) { query = it }
        Spacer(Modifier.height(18.dp))

        if (needle.isEmpty()) {
            Hero(system, actions)
            Spacer(Modifier.height(18.dp))
            if (basics != null) {
                SectionTitle(
                    stringResource(basics.title),
                    trailing = { Plaque(stringResource(R.string.catalog_free), Matrux.colors.successSolid) },
                )
                Spacer(Modifier.height(10.dp))
                SectionGroup(basics.rows, locked = false) { actions.onSection(it) }
            }
            if (advanced != null) {
                Spacer(Modifier.height(18.dp))
                SectionTitle(
                    stringResource(advanced.title),
                    trailing = { Plaque(stringResource(R.string.section_pro), Matrux.colors.proSolid) },
                )
                Spacer(Modifier.height(10.dp))
                SectionGroup(advanced.rows, locked = !paid) { row ->
                    if (paid) actions.onSection(row) else actions.onSubscribe()
                }
            }
        } else {
            val pool = basics?.rows.orEmpty() + if (paid) advanced?.rows.orEmpty() else emptyList()
            val found = pool.mapNotNull { row -> matches(row, needle)?.let { row to it } }
            if (found.isEmpty()) {
                Text(stringResource(R.string.section_search_empty), style = Matrux.type.body, color = Matrux.colors.muted)
            } else {
                Group {
                    found.forEachIndexed { index, (row, hits) ->
                        if (index > 0) GroupDivider()
                        SectionRowItem(row, locked = false, found = hits) { actions.onSection(row) }
                    }
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
private fun Hero(system: HomeSystem?, actions: HomeActions) {
    val colors = Matrux.colors
    TerminalPanel {
        if (system != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DistroLogo(system.logo, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    system.name,
                    style = Matrux.type.title,
                    color = colors.terminalInk,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (system.running) {
                    Plaque(stringResource(R.string.catalog_running), colors.infoSolid)
                } else {
                    Plaque(stringResource(R.string.catalog_installed), colors.successSolid)
                }
            }
            Spacer(Modifier.height(14.dp))
            PromptLine(stringResource(R.string.terminal_host, system.host))
            Spacer(Modifier.height(14.dp))
            TerminalButton(
                stringResource(if (system.running) R.string.home_return else R.string.home_launch),
                if (system.running) actions.onOpenSystem else actions.onLaunch,
            )
        } else {
            Text(stringResource(R.string.home_empty_state), style = Matrux.type.title, color = colors.terminalInk)
            Spacer(Modifier.height(14.dp))
            PromptLine(stringResource(R.string.terminal_host_none))
            Spacer(Modifier.height(14.dp))
            TerminalButton(stringResource(R.string.home_open_catalog), { actions.onTab(Tab.CATALOG) })
        }
    }
}

@Composable
private fun SectionGroup(rows: List<SectionRow>, locked: Boolean, onOpen: (SectionRow) -> Unit) {
    Group {
        rows.forEachIndexed { index, row ->
            if (index > 0) GroupDivider()
            SectionRowItem(row, locked = locked, found = null) { onOpen(row) }
        }
    }
}

@Composable
private fun SectionRowItem(row: SectionRow, locked: Boolean, found: Int?, onOpen: () -> Unit) {
    val colors = Matrux.colors
    val section = row.section
    val style = sectionStyle(section.id)
    ListRow(
        title = section.title,
        subtitle = if (found == null) {
            pluralStringResource(R.plurals.section_count, section.size, section.size)
        } else {
            pluralStringResource(R.plurals.home_found, found, found)
        },
        leading = { GlyphTile(style.glyph, style.color, size = Matrux.space.tile) },
        trailing = {
            if (locked) {
                LucideIcon(Lucide.Lock, colors.muted, size = Matrux.space.iconSmall)
            } else {
                LucideIcon(Lucide.ChevronRight, colors.muted, size = Matrux.space.iconSmall)
            }
        },
        onClick = onOpen,
        titleMaxLines = 1,
    )
}

@Composable
private fun ThemeSwitch(dark: Boolean, onChange: (Boolean) -> Unit) {
    val colors = Matrux.colors
    Row(
        Modifier
            .clip(Matrux.shapes.control)
            .background(colors.surfaceAlt)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSegment(Lucide.Sun, stringResource(R.string.theme_light), selected = !dark) { onChange(false) }
        ThemeSegment(Lucide.Moon, stringResource(R.string.theme_dark), selected = dark) { onChange(true) }
    }
}

@Composable
private fun ThemeSegment(icon: Lucide, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = Matrux.colors
    val light = colors.canvas.luminance() > 0.5f
    val highlight = if (light) colors.canvas else Color(0xFF636366)
    Box(
        Modifier
            .size(width = 40.dp, height = 32.dp)
            .clip(Matrux.shapes.segment)
            .background(if (selected) highlight else Color.Transparent)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(icon, if (selected) colors.accent else colors.muted, size = 18.dp)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val colors = Matrux.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(Matrux.shapes.control)
            .background(colors.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.Search, colors.muted, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(stringResource(R.string.home_search), style = Matrux.type.body, color = colors.muted, maxLines = 1)
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
                size = 18.dp,
                modifier = Modifier.clickable(role = Role.Button) { onChange("") },
            )
        }
    }
}
