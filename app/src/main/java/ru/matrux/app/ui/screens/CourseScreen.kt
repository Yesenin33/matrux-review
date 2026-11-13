package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.R
import ru.matrux.app.ui.components.Group
import ru.matrux.app.ui.components.GroupDivider
import ru.matrux.app.ui.components.ListRow
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.SectionTitle
import ru.matrux.app.ui.components.SoftButton
import ru.matrux.app.ui.state.CourseUi
import ru.matrux.app.ui.state.LessonRow
import ru.matrux.app.ui.theme.Matrux

data class CourseActions(
    val onBack: () -> Unit = {},
    val onLesson: (LessonRow) -> Unit = {},
    val onPro: () -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseScreen(ui: CourseUi, actions: CourseActions) {
    val colors = Matrux.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
    ) {
        BackRow(stringResource(R.string.course_back), actions.onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Matrux.space.page)
                .padding(bottom = Matrux.space.block),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    ui.course.title,
                    style = Matrux.type.display.copy(fontSize = 28.sp, lineHeight = 34.sp),
                    color = colors.ink,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (ui.paid) {
                        Plaque(stringResource(R.string.course_open), colors.successSolid, icon = Lucide.Check)
                    } else {
                        Plaque(stringResource(R.string.course_free), colors.infoSolid)
                    }
                    Text(
                        lessonsLine(ui.total, ui.done),
                        style = Matrux.type.value.copy(fontSize = 13.sp),
                        color = colors.muted,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
            if (!ui.paid) {
                SoftButton(stringResource(R.string.pro_buy), actions.onPro, Modifier.fillMaxWidth())
            }
            ui.modules.forEach { module ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(module.module.title)
                    Group {
                        module.lessons.forEachIndexed { index, row ->
                            if (index > 0) GroupDivider(inset = 56.dp)
                            LessonLine(row) { actions.onLesson(row) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonLine(row: LessonRow, onOpen: () -> Unit) {
    val colors = Matrux.colors
    ListRow(
        title = row.lesson.title,
        leading = { LessonMark(row) },
        trailing = {
            if (row.locked) {
                LucideIcon(Lucide.Lock, colors.muted, size = Matrux.space.iconSmall)
            } else {
                LucideIcon(Lucide.ChevronRight, colors.muted, size = Matrux.space.iconSmall)
            }
        },
        titleColor = if (row.locked) colors.muted else colors.ink,
        onClick = onOpen,
    )
}

@Composable
private fun LessonMark(row: LessonRow) {
    val colors = Matrux.colors
    if (row.done) {
        LucideIcon(Lucide.Check, colors.success, size = Matrux.space.lessonMark)
    } else {
        Box(
            Modifier
                .size(Matrux.space.lessonMark)
                .clip(CircleShape)
                .background(colors.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                row.number.toString(),
                style = Matrux.type.value.copy(fontSize = 13.sp, lineHeight = 16.sp),
                color = if (row.locked) colors.muted else colors.ink,
            )
        }
    }
}
