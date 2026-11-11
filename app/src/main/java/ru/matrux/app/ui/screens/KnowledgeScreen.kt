package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.matrux.app.R
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Tappable
import ru.matrux.app.ui.knowledge.KnowledgeEntry
import ru.matrux.app.ui.knowledge.KnowledgeGroup
import ru.matrux.app.ui.knowledge.KnowledgeSection
import ru.matrux.app.ui.components.ScreenTitle
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.state.KnowledgeUi
import ru.matrux.app.ui.state.SectionRow
import ru.matrux.app.ui.state.CourseCard
import ru.matrux.app.ui.theme.Matrux

private const val COPY_MS = 1600L

fun sectionIcon(id: String): Lucide = when (id) {
    "start" -> Lucide.Rocket
    "files" -> Lucide.Folder
    "linux" -> Lucide.Terminal
    "terms" -> Lucide.BookOpen
    "packages" -> Lucide.Package
    "users" -> Lucide.Users
    "services" -> Lucide.Activity
    "network" -> Lucide.Globe
    "disks" -> Lucide.HardDrive
    "logs" -> Lucide.FileText
    "scripts" -> Lucide.FileCode
    "hardware" -> Lucide.Cpu
    "git" -> Lucide.GitBranch
    "languages" -> Lucide.Code
    else -> Lucide.List
}

private fun KnowledgeSection.entries(): List<KnowledgeEntry> = groups.flatMap { it.entries }

fun courseIcon(id: String): Lucide = when (id) {
    "python" -> Lucide.FileCode
    "go" -> Lucide.Code
    "html" -> Lucide.Globe
    "css" -> Lucide.Sliders
    "cpp" -> Lucide.Cpu
    "scripts" -> Lucide.Terminal
    else -> Lucide.BookOpen
}

data class KnowledgeActions(
    val onTab: (Tab) -> Unit = {},
)

@Composable
fun KnowledgeScreen(actions: KnowledgeActions) {
    TabScreen(Tab.KNOWLEDGE, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Matrux.space.unit))
        ScreenTitle(stringResource(R.string.knowledge_title), null)
        Spacer(Modifier.height(Matrux.space.block))
    }
}

@Composable
fun AccessMark(locked: Boolean, paid: Boolean) {
    when {
        locked -> LucideIcon(Lucide.Lock, Matrux.colors.muted, size = Matrux.space.iconSmall)
        paid -> LucideIcon(Lucide.BadgeCheck, Matrux.colors.success, size = Matrux.space.iconSmall)
        else -> LucideIcon(Lucide.ChevronRight, Matrux.colors.muted, size = Matrux.space.iconSmall)
    }
}

@Composable
fun IconTile(icon: Lucide) {
    Box(
        Modifier
            .size(Matrux.space.logoTile)
            .clip(Matrux.shapes.control)
            .background(Matrux.colors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(icon, Matrux.colors.inkSoft)
    }
}

@Composable
fun lessonsLine(total: Int, done: Int): String {
    val count = pluralStringResource(R.plurals.course_lessons, total, total)
    return if (done > 0) count + " · " + stringResource(R.string.course_progress, done, total) else count
}

@Composable
fun SectionCard(row: SectionRow, paid: Boolean, onOpen: () -> Unit) {
    val colors = Matrux.colors
    val section = row.section
    Card(Modifier.clickable(role = Role.Button, onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(sectionIcon(section.id))
            Spacer(Modifier.width(Matrux.space.unit + Matrux.space.unit / 2))
            Column(Modifier.weight(1f)) {
                Text(section.title, style = Matrux.type.bodyStrong, color = colors.ink)
                Spacer(Modifier.height(Matrux.space.unit / 2))
                Text(
                    pluralStringResource(R.plurals.section_count, section.entries().size, section.entries().size),
                    style = Matrux.type.caption,
                    color = colors.muted,
                )
            }
            Spacer(Modifier.width(Matrux.space.unit))
            AccessMark(row.locked, paid)
        }
    }
}

data class SectionActions(
    val onBack: () -> Unit = {},
    val onRead: (String) -> Unit = {},
)

@Composable
fun SectionScreen(section: KnowledgeSection, actions: SectionActions) {
    val colors = Matrux.colors
    var query by remember(section.id) { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        BackRow(stringResource(R.string.section_back), actions.onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Matrux.space.page)
                .padding(bottom = Matrux.space.block),
        ) {
            Text(section.title, style = Matrux.type.display, color = colors.ink)
            Spacer(Modifier.height(Matrux.space.block))
            SearchField(query) { query = it }
            Spacer(Modifier.height(Matrux.space.block))
            val groups = filtered(section, query)
            if (groups.isEmpty()) {
                Text(stringResource(R.string.section_search_empty), style = Matrux.type.body, color = colors.muted)
            } else {
                groups.forEachIndexed { index, group ->
                    if (index > 0) Spacer(Modifier.height(Matrux.space.block))
                    Group(group, actions.onRead)
                }
            }
        }
    }
}

@Composable
fun BackRow(label: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(Matrux.space.touch)
            .clickable(role = Role.Button, onClick = onBack)
            .padding(horizontal = Matrux.space.page),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.ChevronLeft, Matrux.colors.accent)
        Spacer(Modifier.width(Matrux.space.unit))
        Text(label, style = Matrux.type.body, color = Matrux.colors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun filtered(section: KnowledgeSection, query: String): List<KnowledgeGroup> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return section.groups
    return section.groups.mapNotNull { group ->
        val entries = group.entries.filter { entry ->
            entry.title.lowercase().contains(needle) ||
                entry.summary.lowercase().contains(needle) ||
                entry.detail?.lowercase()?.contains(needle) == true
        }
        if (entries.isEmpty()) null else group.copy(entries = entries)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val colors = Matrux.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.control)
            .background(colors.surfaceAlt)
            .padding(horizontal = Matrux.space.unit + Matrux.space.unit / 2, vertical = Matrux.space.unit + Matrux.space.unit / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.Search, colors.muted, size = Matrux.space.iconSmall)
        Spacer(Modifier.width(Matrux.space.unit))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(stringResource(R.string.section_search), style = Matrux.type.body, color = colors.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Matrux.type.body.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
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

@Composable
private fun Group(group: KnowledgeGroup, onRead: (String) -> Unit) {
    val colors = Matrux.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(group.title, style = Matrux.type.bodyStrong, color = colors.ink)
        Spacer(Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.section_count, group.entries.size, group.entries.size),
            style = Matrux.type.caption,
            color = colors.muted,
        )
    }
    Spacer(Modifier.height(Matrux.space.unit))
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit * 2)) {
            group.entries.forEach { entry -> Entry(entry, onRead) }
        }
    }
}

@Composable
private fun Entry(entry: KnowledgeEntry, onRead: (String) -> Unit) {
    val colors = Matrux.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember(entry.title) { mutableStateOf(false) }
    val text = entry.code ?: entry.title
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPY_MS)
            copied = false
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                clipboard.setText(AnnotatedString(text))
                copied = true
                onRead(entry.title)
            },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.title,
                style = if (entry.code != null) Matrux.type.code else Matrux.type.bodyStrong,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Matrux.space.unit))
            if (copied) {
                Text(stringResource(R.string.section_copied), style = Matrux.type.captionStrong, color = colors.success)
            } else {
                LucideIcon(Lucide.Copy, colors.muted, size = Matrux.space.iconSmall)
            }
        }
        Spacer(Modifier.height(Matrux.space.unit / 2))
        Text(entry.summary, style = Matrux.type.body, color = colors.inkSoft)
        entry.detail?.let {
            Spacer(Modifier.height(Matrux.space.unit / 2))
            Text(it, style = Matrux.type.caption, color = colors.muted)
        }
        entry.code?.let { code ->
            Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
            Text(
                code,
                style = Matrux.type.code,
                color = colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Matrux.shapes.radiusControl))
                    .background(colors.accentTint)
                    .padding(horizontal = Matrux.space.unit + Matrux.space.unit / 2, vertical = Matrux.space.unit),
            )
        }
    }
}
