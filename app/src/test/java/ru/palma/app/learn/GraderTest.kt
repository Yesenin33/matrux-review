package ru.palma.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraderTest {
    private val run = Fixture.course.lesson("a")!!.task as Task.Run
    private val choice = Fixture.course.lesson("c")!!.task as Task.Choice
    private val output = Fixture.course.lesson("d")!!.task as Task.Output

    @Test
    fun normalizeIgnoresLineEndsAndTrailingSpaces() {
        assertEquals("7 4 4\n2", Grader.normalize("7 4 4 \r\n2\n\n"))
        assertEquals("a\n\nb", Grader.normalize("\na  \n\nb\n"))
    }

    @Test
    fun runPassesOnlyWithZeroExitAndSameOutput() {
        assertEquals(Verdict.Pass("2"), Grader.run(run, RunOutcome.Done("2\n", 0)))
        assertEquals(Verdict.Fail("2", "2"), Grader.run(run, RunOutcome.Done("2\n", 1)))
        assertEquals(Verdict.Fail("2", "3"), Grader.run(run, RunOutcome.Done("3", 0)))
    }

    @Test
    fun webCheckAndRunProblemsAreReported() {
        assertEquals(Verdict.Pass(null), Grader.run(run, RunOutcome.Checked(true)))
        assertEquals(Verdict.Fail(null, null), Grader.run(run, RunOutcome.Checked(false)))
        listOf(RunOutcome.Timeout, RunOutcome.NoSystem, RunOutcome.Missing("python3"), RunOutcome.Broken("proot")).forEach {
            assertEquals(Verdict.CantRun(it), Grader.run(run, it))
        }
    }

    @Test
    fun choiceComparesIndex() {
        assertEquals(Verdict.Pass(null), Grader.choice(choice, 1))
        assertEquals(Verdict.Fail("y", "z"), Grader.choice(choice, 2))
    }

    @Test
    fun outputToleratesTrailingWhitespace() {
        assertTrue(Grader.output(output, "1  \n2\n") is Verdict.Pass)
        assertTrue(Grader.output(output, "1 2") is Verdict.Fail)
    }

    @Test
    fun everyRealOutputAnswerMatchesItself() {
        AssetLibrary.library.courses.flatMap { it.lessons }.map { it.task }.filterIsInstance<Task.Output>().forEach { task ->
            assertTrue(Grader.output(task, task.answer + "\n") is Verdict.Pass)
        }
    }
}
