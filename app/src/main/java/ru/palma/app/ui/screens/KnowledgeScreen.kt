package ru.palma.app.ui.screens

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
import ru.palma.app.R
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.Tappable
import ru.palma.app.ui.knowledge.KnowledgeEntry
import ru.palma.app.ui.knowledge.KnowledgeGroup
import ru.palma.app.ui.knowledge.KnowledgeSection
import ru.palma.app.ui.components.ScreenTitle
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.components.TabScreen
import ru.palma.app.ui.state.KnowledgeUi
import ru.palma.app.ui.state.SectionRow
import ru.palma.app.ui.state.CourseCard
import ru.palma.app.ui.theme.Palma

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
        Spacer(Modifier.height(Palma.space.unit))
        ScreenTitle(stringResource(R.string.knowledge_title), null)
        Spacer(Modifier.height(Palma.space.block))
    }
}

@Composable
fun AccessMark(locked: Boolean, paid: Boolean) {
    when {
        locked -> LucideIcon(Lucide.Lock, Palma.colors.muted, size = Palma.space.iconSmall)
        paid -> LucideIcon(Lucide.BadgeCheck, Palma.colors.success, size = Palma.space.iconSmall)
        else -> LucideIcon(Lucide.ChevronRight, Palma.colors.muted, size = Palma.space.iconSmall)
    }
}

@Composable
fun IconTile(icon: Lucide) {
    Box(
        Modifier
            .size(Palma.space.logoTile)
            .clip(Palma.shapes.control)
            .background(Palma.colors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(icon, Palma.colors.inkSoft)
    }
}

@Composable
fun lessonsLine(total: Int, done: Int): String {
    val count = pluralStringResource(R.plurals.course_lessons, total, total)
    return if (done > 0) count + " · " + stringResource(R.string.course_progress, done, total) else count
}

@Composable
fun SectionCard(row: SectionRow, paid: Boolean, onOpen: () -> Unit) {
    val colors = Palma.colors
    val section = row.section
    Card(Modifier.clickable(role = Role.Button, onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(sectionIcon(section.id))
            Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
            Column(Modifier.weight(1f)) {
                Text(section.title, style = Palma.type.bodyStrong, color = colors.ink)
                Spacer(Modifier.height(Palma.space.unit / 2))
                Text(
                    pluralStringResource(R.plurals.section_count, section.entries().size, section.entries().size),
                    style = Palma.type.caption,
                    color = colors.muted,
                )
            }
            Spacer(Modifier.width(Palma.space.unit))
            AccessMark(row.locked, paid)
        }
    }
}

data class SectionActions(
    val onBack: () -> Unit = {},
)

@Composable
fun SectionScreen(section: KnowledgeSection, actions: SectionActions) {
    val colors = Palma.colors
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
                .padding(horizontal = Palma.space.page)
                .padding(bottom = Palma.space.block),
        ) {
            Text(section.title, style = Palma.type.display, color = colors.ink)
            Spacer(Modifier.height(Palma.space.block))
            SearchField(query) { query = it }
            Spacer(Modifier.height(Palma.space.block))
            val groups = filtered(section, query)
            if (groups.isEmpty()) {
                Text(stringResource(R.string.section_search_empty), style = Palma.type.body, color = colors.muted)
            } else {
                groups.forEachIndexed { index, group ->
                    if (index > 0) Spacer(Modifier.height(Palma.space.block))
                    Group(group)
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
            .height(Palma.space.touch)
            .clickable(role = Role.Button, onClick = onBack)
            .padding(horizontal = Palma.space.page),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.ChevronLeft, Palma.colors.accent)
        Spacer(Modifier.width(Palma.space.unit))
        Text(label, style = Palma.type.body, color = Palma.colors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val colors = Palma.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Palma.shapes.control)
            .background(colors.surfaceAlt)
            .padding(horizontal = Palma.space.unit + Palma.space.unit / 2, vertical = Palma.space.unit + Palma.space.unit / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.Search, colors.muted, size = Palma.space.iconSmall)
        Spacer(Modifier.width(Palma.space.unit))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(stringResource(R.string.section_search), style = Palma.type.body, color = colors.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Palma.type.body.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            LucideIcon(
                Lucide.X,
                colors.muted,
                size = Palma.space.iconSmall,
                modifier = Modifier.clickable(role = Role.Button) { onChange("") },
            )
        }
    }
}

@Composable
private fun Group(group: KnowledgeGroup) {
    val colors = Palma.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(group.title, style = Palma.type.bodyStrong, color = colors.ink)
        Spacer(Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.section_count, group.entries.size, group.entries.size),
            style = Palma.type.caption,
            color = colors.muted,
        )
    }
    Spacer(Modifier.height(Palma.space.unit))
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit * 2)) {
            group.entries.forEach { entry -> Entry(entry) }
        }
    }
}

@Composable
private fun Entry(entry: KnowledgeEntry) {
    val colors = Palma.colors
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
            },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.title,
                style = if (entry.code != null) Palma.type.code else Palma.type.bodyStrong,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Palma.space.unit))
            if (copied) {
                Text(stringResource(R.string.section_copied), style = Palma.type.captionStrong, color = colors.success)
            } else {
                LucideIcon(Lucide.Copy, colors.muted, size = Palma.space.iconSmall)
            }
        }
        Spacer(Modifier.height(Palma.space.unit / 2))
        Text(entry.summary, style = Palma.type.body, color = colors.inkSoft)
        entry.detail?.let {
            Spacer(Modifier.height(Palma.space.unit / 2))
            Text(it, style = Palma.type.caption, color = colors.muted)
        }
        entry.code?.let { code ->
            Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 2))
            Text(
                code,
                style = Palma.type.code,
                color = colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Palma.shapes.radiusControl))
                    .background(colors.accentTint)
                    .padding(horizontal = Palma.space.unit + Palma.space.unit / 2, vertical = Palma.space.unit),
            )
        }
    }
}
