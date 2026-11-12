package ru.matrux.app.ui.screens

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
import ru.matrux.app.R
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Tappable
import ru.matrux.app.ui.state.CourseUi
import ru.matrux.app.ui.state.LessonRow
import ru.matrux.app.ui.theme.Matrux

data class CourseActions(
    val onBack: () -> Unit = {},
    val onLesson: (LessonRow) -> Unit = {},
    val onPro: () -> Unit = {},
)

@Composable
fun CourseScreen(ui: CourseUi, actions: CourseActions) {
    val colors = Matrux.colors
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
                .padding(horizontal = Matrux.space.page)
                .padding(bottom = Matrux.space.block),
        ) {
            Text(ui.course.title, style = Matrux.type.display, color = colors.ink)
            Spacer(Modifier.height(Matrux.space.unit))
            Text(ui.course.outcome, style = Matrux.type.body, color = colors.inkSoft)
            Spacer(Modifier.height(Matrux.space.unit))
            Text(lessonsLine(ui.total, ui.done), style = Matrux.type.caption, color = colors.muted)
            ui.course.note?.let {
                Spacer(Modifier.height(Matrux.space.unit))
                Text(it, style = Matrux.type.caption, color = colors.muted)
            }
            Spacer(Modifier.height(Matrux.space.unit * 2))
            AccessLine(ui.paid, actions.onPro)
            ui.modules.forEach { module ->
                Spacer(Modifier.height(Matrux.space.block))
                Text(module.module.title, style = Matrux.type.bodyStrong, color = colors.ink)
                Spacer(Modifier.height(Matrux.space.unit))
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2)) {
                        module.lessons.forEach { row -> LessonLine(row) { actions.onLesson(row) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessLine(paid: Boolean, onPro: () -> Unit) {
    val colors = Matrux.colors
    if (paid) {
        Row(Modifier.heightIn(min = Matrux.space.touch), verticalAlignment = Alignment.CenterVertically) {
            LucideIcon(Lucide.BadgeCheck, colors.success, size = Matrux.space.iconSmall)
            Spacer(Modifier.width(Matrux.space.unit))
            Text(stringResource(R.string.course_open), style = Matrux.type.bodyStrong, color = colors.success)
        }
    } else {
        Tappable(onPro, Modifier.fillMaxWidth()) {
            LucideIcon(Lucide.Lock, colors.accent, size = Matrux.space.iconSmall)
            Spacer(Modifier.width(Matrux.space.unit))
            Text(stringResource(R.string.course_free), style = Matrux.type.bodyStrong, color = colors.accent, modifier = Modifier.weight(1f))
            LucideIcon(Lucide.ChevronRight, colors.accent, size = Matrux.space.iconSmall)
        }
    }
}

@Composable
private fun LessonLine(row: LessonRow, onOpen: () -> Unit) {
    val colors = Matrux.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Matrux.space.touch)
            .clickable(role = Role.Button, onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LessonMark(row)
        Spacer(Modifier.width(Matrux.space.unit + Matrux.space.unit / 2))
        Text(
            row.lesson.title,
            style = Matrux.type.body,
            color = if (row.locked) colors.muted else colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (row.locked) {
            Spacer(Modifier.width(Matrux.space.unit))
            LucideIcon(Lucide.Lock, colors.muted, size = Matrux.space.iconSmall)
        }
    }
}

@Composable
private fun LessonMark(row: LessonRow) {
    val colors = Matrux.colors
    Box(
        Modifier
            .size(Matrux.space.lessonMark)
            .clip(CircleShape)
            .background(colors.canvas),
        contentAlignment = Alignment.Center,
    ) {
        if (row.done) {
            LucideIcon(Lucide.Check, colors.success, size = Matrux.space.iconSmall)
        } else {
            Text(row.number.toString(), style = Matrux.type.caption, color = colors.muted)
        }
    }
}
