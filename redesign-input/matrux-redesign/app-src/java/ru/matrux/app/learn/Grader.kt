package ru.matrux.app.learn

sealed interface Answer {
    data class Code(val text: String) : Answer
    data class Pick(val index: Int) : Answer
    data class Text(val text: String) : Answer
    data class Quiz(val picks: List<Int>) : Answer
}

sealed interface RunOutcome {
    data class Done(val stdout: String, val exit: Int) : RunOutcome
    data class Checked(val passed: Boolean) : RunOutcome
    data object Timeout : RunOutcome
    data object NoSystem : RunOutcome
    data class Missing(val program: String) : RunOutcome
    data class Broken(val reason: String) : RunOutcome
}

sealed interface Verdict {
    data class Pass(val output: String?) : Verdict
    data class Fail(val expected: String?, val actual: String?) : Verdict
    data class CantRun(val outcome: RunOutcome) : Verdict
}

object Grader {

    fun normalize(text: String): String = text
        .replace("\r\n", "\n")
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .trim('\n')

    fun choice(task: Task.Choice, pick: Int): Verdict =
        if (pick == task.answer) Verdict.Pass(null) else Verdict.Fail(task.options[task.answer], task.options.getOrNull(pick))

    fun quiz(questions: List<QuizQuestion>, picks: List<Int>): Verdict {
        val wrong = questions.indices.firstOrNull { picks.getOrNull(it) != questions[it].answer }
        return if (wrong == null) {
            Verdict.Pass(null)
        } else {
            val q = questions[wrong]
            Verdict.Fail(q.options[q.answer], q.options.getOrNull(picks.getOrNull(wrong) ?: -1))
        }
    }

    fun output(task: Task.Output, text: String): Verdict {
        val actual = normalize(text)
        val expected = normalize(task.answer)
        return if (actual == expected) Verdict.Pass(null) else Verdict.Fail(expected, actual)
    }

    fun run(task: Task.Run, outcome: RunOutcome): Verdict = when (outcome) {
        is RunOutcome.Checked -> if (outcome.passed) Verdict.Pass(null) else Verdict.Fail(null, null)
        is RunOutcome.Done -> {
            val actual = normalize(outcome.stdout)
            val expected = task.expect?.let(::normalize)
            if (outcome.exit == 0 && expected != null && actual == expected) Verdict.Pass(actual) else Verdict.Fail(expected, actual)
        }
        else -> Verdict.CantRun(outcome)
    }
}
