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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.palma.app.R
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.PenguinMark
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.knowledge.Branch
import ru.palma.app.ui.knowledge.KnowledgeEntry
import ru.palma.app.ui.knowledge.KnowledgeGroup
import ru.palma.app.ui.knowledge.KnowledgeSection
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

@Composable
fun SectionsBlock(sections: List<KnowledgeSection>, paid: Boolean, onOpen: (KnowledgeSection) -> Unit) {
    Text(stringResource(R.string.sections_title), style = Palma.type.title, color = Palma.colors.ink)
    Spacer(Modifier.height(Palma.space.block))
    Branch.entries.forEachIndexed { index, branch ->
        val part = sections.filter { it.branch == branch }
        if (part.isEmpty()) return@forEachIndexed
        if (index > 0) Spacer(Modifier.height(Palma.space.block))
        Text(
            stringResource(if (branch == Branch.START) R.string.sections_start else R.string.sections_deeper),
            style = Palma.type.bodyStrong,
            color = Palma.colors.muted,
        )
        Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 2))
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit + Palma.space.unit / 2)) {
            part.forEach { section -> SectionCard(section, paid, onOpen) }
        }
    }
}

@Composable
private fun SectionCard(section: KnowledgeSection, paid: Boolean, onOpen: (KnowledgeSection) -> Unit) {
    val colors = Palma.colors
    val locked = section.pro && !paid
    Card(Modifier.clickable(role = Role.Button) { onOpen(section) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(Palma.space.logoTile)
                    .clip(Palma.shapes.control)
                    .background(colors.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                LucideIcon(sectionIcon(section.id), colors.accent)
            }
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
            if (locked) {
                LucideIcon(Lucide.Lock, colors.muted, size = Palma.space.iconSmall)
            } else {
                LucideIcon(Lucide.ChevronRight, colors.muted, size = Palma.space.iconSmall)
            }
        }
    }
}

data class SectionActions(
    val onBack: () -> Unit = {},
    val onPro: () -> Unit = {},
)

@Composable
fun SectionScreen(section: KnowledgeSection, paid: Boolean, actions: SectionActions) {
    val colors = Palma.colors
    var query by remember(section.id) { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        BackRow(actions.onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Palma.space.page)
                .padding(bottom = Palma.space.block),
        ) {
            Text(section.title, style = Palma.type.display, color = colors.ink)
            Spacer(Modifier.height(Palma.space.block))
            ProNote(section, paid, actions)
            if (section.pro && !paid) {
                Spacer(Modifier.height(Palma.space.block))
                Locked()
            } else {
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
}

@Composable
private fun BackRow(onBack: () -> Unit) {
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
        Text(stringResource(R.string.section_back), style = Palma.type.body, color = Palma.colors.accent)
    }
}

@Composable
private fun ProNote(section: KnowledgeSection, paid: Boolean, actions: SectionActions) {
    if (!section.pro) return
    if (paid) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LucideIcon(Lucide.BadgeCheck, Palma.colors.success, size = Palma.space.iconSmall)
            Spacer(Modifier.width(Palma.space.unit / 2))
            Text(stringResource(R.string.section_pro), style = Palma.type.captionStrong, color = Palma.colors.success)
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = actions.onPro),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LucideIcon(Lucide.Lock, Palma.colors.accent, size = Palma.space.iconSmall)
            Spacer(Modifier.width(Palma.space.unit / 2))
            Text(stringResource(R.string.section_pro), style = Palma.type.captionStrong, color = Palma.colors.accent)
            Spacer(Modifier.weight(1f))
            LucideIcon(Lucide.ChevronRight, Palma.colors.accent, size = Palma.space.iconSmall)
        }
    }
}

@Composable
private fun Locked() {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PenguinMark(size = 44.dp, silhouette = true, tint = Palma.colors.ink)
            Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
            Text(stringResource(R.string.section_pro), style = Palma.type.bodyStrong, color = Palma.colors.ink)
        }
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
