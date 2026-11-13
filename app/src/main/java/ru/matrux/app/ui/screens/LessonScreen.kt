package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import ru.matrux.app.R
import ru.matrux.app.learn.Answer
import ru.matrux.app.learn.RunOutcome
import ru.matrux.app.learn.Runtime
import ru.matrux.app.learn.Task
import ru.matrux.app.learn.Verdict
import ru.matrux.app.ui.components.GhostButton
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.learn.CodeBlock
import ru.matrux.app.ui.learn.CodeEditor
import ru.matrux.app.ui.learn.WebPreview
import ru.matrux.app.ui.state.Attempt
import ru.matrux.app.ui.state.LessonUi
import ru.matrux.app.ui.theme.Matrux

data class LessonActions(
    val onBack: () -> Unit = {},
    val onCheck: (Answer) -> Unit = {},
    val onNext: () -> Unit = {},
    val onPro: () -> Unit = {},
)

private const val NO_PICK = -1

@Composable
fun LessonScreen(ui: LessonUi, actions: LessonActions, draft: String? = null, pick: Int = NO_PICK) {
    val colors = Matrux.colors
    val lesson = ui.lesson
    val key = ui.course.id + "/" + lesson.id
    var code by rememberSaveable(key) { mutableStateOf(draft ?: ui.starter) }
    var text by rememberSaveable(key) { mutableStateOf(draft.orEmpty()) }
    var chosen by rememberSaveable(key) { mutableIntStateOf(pick) }
    var picks by rememberSaveable(key) { mutableStateOf(emptyList<Int>()) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
    ) {
        BackRow(ui.course.title, actions.onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Matrux.space.page)
                .padding(bottom = Matrux.space.block),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.lesson_number, ui.number, ui.total), style = Matrux.type.caption, color = colors.muted)
                if (ui.done) {
                    Spacer(Modifier.width(Matrux.space.unit / 2))
                    LucideIcon(Lucide.Check, colors.success, size = Matrux.space.iconSmall)
                }
            }
            Spacer(Modifier.height(Matrux.space.unit / 2))
            Text(lesson.title, style = Matrux.type.title, color = colors.ink)
            Spacer(Modifier.height(Matrux.space.unit * 2))
            Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit + Matrux.space.unit / 2)) {
                lesson.theory.forEach { Text(it, style = Matrux.type.body, color = colors.ink) }
            }
            if (lesson.examples.isNotEmpty()) {
                Spacer(Modifier.height(Matrux.space.block))
                Heading(stringResource(R.string.lesson_example))
                lesson.examples.forEachIndexed { index, example ->
                    if (index > 0) Spacer(Modifier.height(Matrux.space.block))
                    if (example.title.isNotBlank()) {
                        Text(example.title, style = Matrux.type.bodyStrong, color = colors.ink)
                        Spacer(Modifier.height(Matrux.space.unit / 2))
                    }
                    CodeBlock(example.code, example.lang)
                    example.explain?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(Matrux.space.unit / 2))
                        Text(it, style = Matrux.type.body, color = colors.inkSoft)
                    }
                }
            } else {
                Spacer(Modifier.height(Matrux.space.block))
                Heading(stringResource(R.string.lesson_example))
                CodeBlock(lesson.example.code, lesson.example.lang)
            }
            Spacer(Modifier.height(Matrux.space.block))
            Heading(stringResource(R.string.lesson_task))
            Text(lesson.task.prompt, style = Matrux.type.body, color = colors.ink)
            lesson.practice?.let { practice ->
                Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
                Text(stringResource(R.string.lesson_steps), style = Matrux.type.caption, color = colors.muted)
                Spacer(Modifier.height(Matrux.space.unit / 2))
                Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2)) {
                    practice.steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "${index + 1}.",
                                style = Matrux.type.bodyStrong,
                                color = colors.ink,
                                modifier = Modifier.width(Matrux.space.unit * 3),
                            )
                            Text(step, style = Matrux.type.body, color = colors.inkSoft, modifier = Modifier.weight(1f))
                        }
                    }
                }
                practice.hint?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(Matrux.space.unit))
                    Text(it, style = Matrux.type.body, color = colors.muted)
                }
            }
            Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
            when (val task = lesson.task) {
                is Task.Run -> {
                    CodeEditor(code, task.runtime.key, onChange = { code = it })
                    if (code != task.starter) {
                        Text(
                            stringResource(R.string.lesson_reset),
                            style = Matrux.type.caption,
                            color = colors.accent,
                            modifier = Modifier
                                .heightIn(min = Matrux.space.touch)
                                .clickable(role = Role.Button) { code = task.starter }
                                .padding(top = Matrux.space.unit),
                        )
                    }
                }
                is Task.Choice -> {
                    if (lesson.quiz.size > 1) {
                        lesson.quiz.forEachIndexed { qIndex, question ->
                            if (qIndex > 0) Spacer(Modifier.height(Matrux.space.block))
                            Text(question.question, style = Matrux.type.bodyStrong, color = colors.ink)
                            Spacer(Modifier.height(Matrux.space.unit))
                            Options(question.options, picks.getOrNull(qIndex) ?: NO_PICK) { pick ->
                                val next = picks.toMutableList()
                                while (next.size <= qIndex) next.add(NO_PICK)
                                next[qIndex] = pick
                                picks = next
                            }
                        }
                    } else {
                        task.code?.let {
                            CodeBlock(it.code, it.lang)
                            Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
                        }
                        Options(task.options, chosen) { chosen = it }
                    }
                }
                is Task.Output -> {
                    CodeBlock(task.code.code, task.code.lang)
                    Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
                    Text(stringResource(R.string.lesson_answer), style = Matrux.type.caption, color = colors.muted)
                    Spacer(Modifier.height(Matrux.space.unit / 2))
                    CodeEditor(text, "", onChange = { text = it }, lines = 2)
                }
            }
            Spacer(Modifier.height(Matrux.space.unit * 2))
            val running = ui.attempt == Attempt.Running
            val ready = when (lesson.task) {
                is Task.Run -> code.isNotBlank()
                is Task.Choice -> if (lesson.quiz.size > 1) picks.none { it == NO_PICK } else chosen != NO_PICK
                is Task.Output -> text.isNotBlank()
            }
            PrimaryButton(
                stringResource(if (running) R.string.lesson_running else R.string.lesson_check),
                onClick = {
                    actions.onCheck(
                        when (lesson.task) {
                            is Task.Run -> Answer.Code(code)
                            is Task.Choice -> if (lesson.quiz.size > 1) Answer.Quiz(picks) else Answer.Pick(chosen)
                            is Task.Output -> Answer.Text(text)
                        },
                    )
                },
                enabled = ready && !running,
            )
            ui.verdict?.let { verdict ->
                Spacer(Modifier.height(Matrux.space.block))
                Result(verdict, lesson.task)
            }
            val task = lesson.task
            if (task is Task.Run && task.runtime == Runtime.WEB && ui.attempt is Attempt.Result) {
                Spacer(Modifier.height(Matrux.space.block))
                Heading(stringResource(R.string.lesson_preview))
                WebPreview(code)
            }
            if (ui.reviewed) {
                Spacer(Modifier.height(Matrux.space.block))
                Heading(stringResource(R.string.lesson_review))
                Text(lesson.review, style = Matrux.type.body, color = colors.ink)
                if (task is Task.Run) {
                    Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
                    Text(stringResource(R.string.lesson_solution), style = Matrux.type.caption, color = colors.muted)
                    Spacer(Modifier.height(Matrux.space.unit / 2))
                    CodeBlock(task.solution, task.runtime.key)
                }
            }
            if (ui.passed && ui.next != null) {
                Spacer(Modifier.height(Matrux.space.block))
                if (ui.nextOpen) {
                    GhostButton(stringResource(R.string.lesson_next), actions.onNext)
                } else {
                    GhostButton(stringResource(R.string.lesson_next_pro), actions.onPro)
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = Matrux.type.title, color = Matrux.colors.ink)
    Spacer(Modifier.height(Matrux.space.unit))
}

@Composable
private fun Options(options: List<String>, chosen: Int, onPick: (Int) -> Unit) {
    val colors = Matrux.colors
    Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit)) {
        options.forEachIndexed { index, option ->
            val selected = index == chosen
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Matrux.space.touch)
                    .clip(Matrux.shapes.control)
                    .background(colors.surface)
                    .clickable(role = Role.RadioButton) { onPick(index) }
                    .padding(horizontal = Matrux.space.unit + Matrux.space.unit / 2, vertical = Matrux.space.unit),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    option,
                    style = if (selected) Matrux.type.bodyStrong else Matrux.type.body,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Spacer(Modifier.width(Matrux.space.unit))
                    LucideIcon(Lucide.Check, colors.ink, size = Matrux.space.iconSmall)
                }
            }
        }
    }
}

@Composable
private fun Result(verdict: Verdict, task: Task) {
    val colors = Matrux.colors
    val lang = when (task) {
        is Task.Run -> task.runtime.key
        else -> ""
    }
    when (verdict) {
        is Verdict.Pass -> {
            Plaque(stringResource(R.string.lesson_pass), colors.successSolid, icon = Lucide.Check)
            verdict.output?.takeIf { it.isNotEmpty() }?.let {
                Spacer(Modifier.height(Matrux.space.unit))
                CodeBlock(it, "")
            }
        }
        is Verdict.Fail -> {
            Plaque(stringResource(R.string.lesson_fail), colors.dangerSolid, icon = Lucide.CircleAlert)
            verdict.actual?.let {
                Spacer(Modifier.height(Matrux.space.unit))
                Text(stringResource(R.string.lesson_got), style = Matrux.type.caption, color = colors.muted)
                Spacer(Modifier.height(Matrux.space.unit / 2))
                CodeBlock(it.ifEmpty { stringResource(R.string.lesson_empty) }, if (task is Task.Choice) "" else lang)
            }
            verdict.expected?.let {
                Spacer(Modifier.height(Matrux.space.unit))
                Text(stringResource(R.string.lesson_expected), style = Matrux.type.caption, color = colors.muted)
                Spacer(Modifier.height(Matrux.space.unit / 2))
                CodeBlock(it, if (task is Task.Choice) "" else lang)
            }
        }
        is Verdict.CantRun -> Text(cantRunText(verdict.outcome), style = Matrux.type.body, color = colors.inkSoft)
    }
}

@Composable
private fun cantRunText(outcome: RunOutcome): String = when (outcome) {
    RunOutcome.NoSystem -> stringResource(R.string.run_no_system)
    is RunOutcome.Missing -> stringResource(R.string.run_missing, outcome.program)
    RunOutcome.Timeout -> stringResource(R.string.run_timeout)
    is RunOutcome.Broken -> stringResource(R.string.run_broken, outcome.reason)
    is RunOutcome.Done, is RunOutcome.Checked -> ""
}
