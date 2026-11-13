package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.matrux.app.R
import ru.matrux.app.learn.Course
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.Glyph
import ru.matrux.app.ui.components.GlyphTile
import ru.matrux.app.ui.components.Group
import ru.matrux.app.ui.components.GroupDivider
import ru.matrux.app.ui.components.LargeTitle
import ru.matrux.app.ui.components.ListRow
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.SectionTitle
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.components.courseStyle
import ru.matrux.app.ui.components.sectionStyle
import ru.matrux.app.ui.knowledge.KnowledgeEntry
import ru.matrux.app.ui.knowledge.KnowledgeGroup
import ru.matrux.app.ui.knowledge.KnowledgeSection
import ru.matrux.app.ui.state.CourseCard
import ru.matrux.app.ui.state.KnowledgeUi
import ru.matrux.app.ui.state.SectionRow
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
    val onCourse: (Course) -> Unit = {},
    val onTab: (Tab) -> Unit = {},
    val onSources: (() -> Unit)? = null,
)

@Composable
fun KnowledgeScreen(ui: KnowledgeUi, actions: KnowledgeActions) {
    val colors = Matrux.colors
    TabScreen(Tab.KNOWLEDGE, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Matrux.space.unit))
        LargeTitle(
            stringResource(R.string.knowledge_title),
            trailing = if (ui.courses.any { it.locked }) {
                { Plaque(stringResource(R.string.section_pro), colors.proSolid, icon = Lucide.Lock) }
            } else {
                null
            },
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Group {
                ui.courses.forEachIndexed { index, card ->
                    if (index > 0) GroupDivider(inset = 68.dp)
                    CourseRow(card) { actions.onCourse(card.course) }
                }
            }
            actions.onSources?.let { open ->
                Group {
                    ListRow(
                        title = stringResource(R.string.sources_title),
                        leading = { GlyphTile(Glyph.Doc, colors.graySolid, size = Matrux.space.tile) },
                        trailing = { LucideIcon(Lucide.ChevronRight, colors.muted, size = Matrux.space.iconSmall) },
                        onClick = open,
                    )
                }
            }
        }
        Spacer(Modifier.height(Matrux.space.block))
    }
}

@Composable
private fun CourseRow(card: CourseCard, onOpen: () -> Unit) {
    val colors = Matrux.colors
    val style = courseStyle(card.course.id)
    ListRow(
        title = card.course.title,
        subtitle = lessonsLine(card.total, card.done),
        leading = { GlyphTile(style.glyph, style.color, size = 40.dp, corner = 10.dp) },
        trailing = {
            if (card.locked) {
                LucideIcon(Lucide.Lock, colors.muted, size = Matrux.space.iconSmall)
            } else {
                LucideIcon(Lucide.ChevronRight, colors.muted, size = Matrux.space.iconSmall)
            }
        },
        onClick = onOpen,
    )
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
            .size(Matrux.space.tile)
            .clip(Matrux.shapes.segment)
            .background(Matrux.colors.surfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(icon, Matrux.colors.inkSoft, size = Matrux.space.iconSmall)
    }
}

@Composable
fun lessonsLine(total: Int, done: Int): String {
    val count = pluralStringResource(R.plurals.course_lessons, total, total)
    return if (done > 0) count + " · " + stringResource(R.string.course_progress, done, total) else count
}

@Composable
fun SectionCard(row: SectionRow, paid: Boolean, onOpen: () -> Unit) {
    val section = row.section
    val style = sectionStyle(section.id)
    Group {
        ListRow(
            title = section.title,
            subtitle = pluralStringResource(R.plurals.section_count, section.entries().size, section.entries().size),
            leading = { GlyphTile(style.glyph, style.color, size = Matrux.space.tile) },
            trailing = { AccessMark(row.locked, paid) },
            onClick = onOpen,
        )
    }
}

data class SectionActions(
    val onBack: () -> Unit = {},
)

@Composable
fun SectionScreen(section: KnowledgeSection, actions: SectionActions) {
    val colors = Matrux.colors
    var query by remember(section.id) { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
    ) {
        BackRow(stringResource(R.string.tab_home), actions.onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Matrux.space.page)
                .padding(bottom = Matrux.space.block),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LargeTitle(
                section.title,
                trailing = {
                    Text(
                        pluralStringResource(R.plurals.section_count, section.entries().size, section.entries().size),
                        style = Matrux.type.value.copy(fontSize = 13.sp),
                        color = colors.muted,
                        maxLines = 1,
                    )
                },
            )
            SearchField(query) { query = it }
            val groups = filtered(section, query)
            if (groups.isEmpty()) {
                Text(stringResource(R.string.section_search_empty), style = Matrux.type.body, color = colors.muted)
            } else {
                groups.forEach { group -> EntryGroup(group) }
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
            .padding(horizontal = Matrux.space.page - 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(Lucide.ChevronLeft, Matrux.colors.accent)
        Spacer(Modifier.width(2.dp))
        Text(label, style = Matrux.type.lead, color = Matrux.colors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                Text(stringResource(R.string.section_search), style = Matrux.type.body, color = colors.muted, maxLines = 1)
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

@Composable
private fun EntryGroup(group: KnowledgeGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            group.title,
            trailing = {
                Text(
                    pluralStringResource(R.plurals.section_count, group.entries.size, group.entries.size),
                    style = Matrux.type.value.copy(fontSize = 12.sp),
                    color = Matrux.colors.muted,
                    maxLines = 1,
                )
            },
        )
        Group {
            group.entries.forEachIndexed { index, entry ->
                if (index > 0) GroupDivider(inset = Matrux.space.card)
                Entry(entry)
            }
        }
    }
}

@Composable
private fun Entry(entry: KnowledgeEntry) {
    val colors = Matrux.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Matrux.space.card, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            entry.title,
            style = if (entry.code != null) Matrux.type.code.copy(fontSize = 15.sp, lineHeight = 21.sp) else Matrux.type.bodyStrong,
            color = colors.ink,
        )
        if (entry.summary.isNotBlank()) {
            Text(entry.summary, style = Matrux.type.caption.copy(fontSize = 14.sp, lineHeight = 19.sp), color = colors.muted)
        }
        entry.detail?.let { Text(it, style = Matrux.type.caption, color = colors.muted) }
        entry.code?.let { code ->
            Spacer(Modifier.height(6.dp))
            CodeChip(code)
        }
    }
}

/** Команда на тёмной подложке и кнопка копирования в один тап. */
@Composable
private fun CodeChip(code: String) {
    val colors = Matrux.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }
    val label = stringResource(R.string.section_copied)
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPY_MS)
            copied = false
        }
    }
    val copy = {
        clipboard.setText(AnnotatedString(code))
        copied = true
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            code,
            style = Matrux.type.code.copy(fontSize = 13.sp),
            color = colors.terminalInk,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clip(Matrux.shapes.segment)
                .background(colors.terminal)
                .border(1.dp, colors.terminalEdge, Matrux.shapes.segment)
                .clickable(role = Role.Button, onClick = copy)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Box(
            Modifier
                .size(width = 44.dp, height = 36.dp)
                .clip(Matrux.shapes.segment)
                .background(colors.surfaceAlt)
                .semantics { contentDescription = label }
                .clickable(role = Role.Button, onClick = copy),
            contentAlignment = Alignment.Center,
        ) {
            if (copied) {
                LucideIcon(Lucide.CopyCheck, colors.success, size = 18.dp)
            } else {
                LucideIcon(Lucide.Copy, colors.accent, size = 18.dp)
            }
        }
    }
}
