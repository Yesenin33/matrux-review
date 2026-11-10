package ru.palma.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.palma.app.ui.state.Attempt
import ru.palma.app.ui.state.courseUi
import ru.palma.app.ui.state.knowledgeUi
import ru.palma.app.ui.state.lessonUi

class LearnStatesTest {
    private val course = Fixture.course

    @Test
    fun courseRowsNumberLockAndMark() {
        val progress = Progress().mark("demo", "a")
        val free = courseUi(course, paid = false, progress = progress)
        val rows = free.modules.flatMap { it.lessons }
        assertEquals(listOf(1, 2, 3, 4), rows.map { it.number })
        assertEquals(listOf(false, false, true, true), rows.map { it.locked })
        assertEquals(listOf(true, false, false, false), rows.map { it.done })
        assertEquals(1, free.done)
        assertEquals(4, free.total)
        assertTrue(courseUi(course, paid = true, progress = progress).modules.flatMap { it.lessons }.none { it.locked })
    }

    @Test
    fun lessonKnowsWhetherNextIsOpen() {
        val lastFree = lessonUi(course, course.lesson("b")!!, paid = false, progress = Progress(), attempt = Attempt.Idle)
        assertFalse(lastFree.nextOpen)
        assertEquals(2, lastFree.number)
        val paid = lessonUi(course, course.lesson("b")!!, paid = true, progress = Progress(), attempt = Attempt.Idle)
        assertTrue(paid.nextOpen)
        val first = lessonUi(course, course.lesson("a")!!, paid = false, progress = Progress(), attempt = Attempt.Result(Verdict.Pass("2")))
        assertTrue(first.nextOpen)
        assertTrue(first.passed && first.reviewed)
        assertEquals("", first.starter)
    }

    @Test
    fun knowledgeCardsCountRealProgress() {
        val progress = Progress().mark("demo", "a").mark("demo", "d")
        val ui = knowledgeUi(listOf(course), emptyList(), paid = false, progress = progress)
        assertEquals(2, ui.courses.single().done)
        assertEquals(4, ui.courses.single().total)
        assertTrue(ui.courses.single().locked)
    }
}
