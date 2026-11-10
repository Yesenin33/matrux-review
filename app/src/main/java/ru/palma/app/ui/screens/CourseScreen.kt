package ru.palma.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import ru.palma.app.R
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.Tappable
import ru.palma.app.ui.state.CourseUi
import ru.palma.app.ui.state.LessonRow
import ru.palma.app.ui.theme.Palma

data class CourseActions(
    val onBack: () -> Unit = {},
    val onLesson: (LessonRow) -> Unit = {},
    val onPro: () -> Unit = {},
)

@Composable
fun CourseScreen(ui: CourseUi, actions: CourseActions) {
    val colors = Palma.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        BackRow(stringResource(R.string.course_back), actions.onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Palma.space.page)
                .padding(bottom = Palma.space.block),
        ) {
            Text(ui.course.title, style = Palma.type.display, color = colors.ink)
            Spacer(Modifier.height(Palma.space.unit))
            Text(ui.course.outcome, style = Palma.type.body, color = colors.inkSoft)
            Spacer(Modifier.height(Palma.space.unit))
            Text(lessonsLine(ui.total, ui.done), style = Palma.type.caption, color = colors.muted)
            ui.course.note?.let {
                Spacer(Modifier.height(Palma.space.unit))
                Text(it, style = Palma.type.caption, color = colors.muted)
            }
            Spacer(Modifier.height(Palma.space.unit * 2))
            AccessLine(ui.paid, actions.onPro)
            ui.modules.forEach { module ->
                Spacer(Modifier.height(Palma.space.block))
                Text(module.module.title, style = Palma.type.bodyStrong, color = colors.ink)
                Spacer(Modifier.height(Palma.space.unit))
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit / 2)) {
                        module.lessons.forEach { row -> LessonLine(row) { actions.onLesson(row) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessLine(paid: Boolean, onPro: () -> Unit) {
    val colors = Palma.colors
    if (paid) {
        Row(Modifier.heightIn(min = Palma.space.touch), verticalAlignment = Alignment.CenterVertically) {
            LucideIcon(Lucide.BadgeCheck, colors.success, size = Palma.space.iconSmall)
            Spacer(Modifier.width(Palma.space.unit))
            Text(stringResource(R.string.course_open), style = Palma.type.bodyStrong, color = colors.success)
        }
    } else {
        Tappable(onPro, Modifier.fillMaxWidth()) {
            LucideIcon(Lucide.Lock, colors.accent, size = Palma.space.iconSmall)
            Spacer(Modifier.width(Palma.space.unit))
            Text(stringResource(R.string.course_free), style = Palma.type.bodyStrong, color = colors.accent, modifier = Modifier.weight(1f))
            LucideIcon(Lucide.ChevronRight, colors.accent, size = Palma.space.iconSmall)
        }
    }
}

@Composable
private fun LessonLine(row: LessonRow, onOpen: () -> Unit) {
    val colors = Palma.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Palma.space.touch)
            .clickable(role = Role.Button, onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LessonMark(row)
        Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
        Text(
            row.lesson.title,
            style = Palma.type.body,
            color = if (row.locked) colors.muted else colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (row.locked) {
            Spacer(Modifier.width(Palma.space.unit))
            LucideIcon(Lucide.Lock, colors.muted, size = Palma.space.iconSmall)
        }
    }
}

@Composable
private fun LessonMark(row: LessonRow) {
    val colors = Palma.colors
    Box(
        Modifier
            .size(Palma.space.lessonMark)
            .clip(CircleShape)
            .background(colors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        if (row.done) {
            LucideIcon(Lucide.Check, colors.success, size = Palma.space.iconSmall)
        } else {
            Text(row.number.toString(), style = Palma.type.caption, color = colors.muted)
        }
    }
}
