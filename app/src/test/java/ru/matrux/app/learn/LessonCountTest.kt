package ru.matrux.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LessonCountTest {
    private val course = Fixture.course

    @Test
    fun countsAcrossModules() {
        assertEquals(4, course.lessonCount)
        assertEquals(listOf("a", "b", "c", "d"), course.lessons.map { it.id })
    }

    @Test
    fun numbersAreContinuousAcrossModules() {
        assertEquals(1, course.number("a"))
        assertEquals(3, course.number("c"))
        assertEquals(0, course.number("missing"))
    }

    @Test
    fun nextCrossesModuleBoundaryAndEnds() {
        assertEquals("c", course.next("b")?.id)
        assertNull(course.next("d"))
        assertNull(course.next("missing"))
    }
}
