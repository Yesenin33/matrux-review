package ru.matrux.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessTest {
    private val course = Fixture.course

    @Test
    fun freeOpensOnlyTheFirstModule() {
        assertTrue(Access.lessonOpen(course, "a", paid = false))
        assertTrue(Access.lessonOpen(course, "b", paid = false))
        assertFalse(Access.lessonOpen(course, "c", paid = false))
        assertFalse(Access.lessonOpen(course, "d", paid = false))
        assertEquals(listOf("a", "b"), Access.openLessons(course, paid = false).map { it.id })
        assertTrue(Access.courseLocked(paid = false))
    }

    @Test
    fun proOpensEverything() {
        assertEquals(course.lessons, Access.openLessons(course, paid = true))
        assertTrue(course.lessons.all { Access.lessonOpen(course, it.id, paid = true) })
        assertFalse(Access.courseLocked(paid = true))
    }

    @Test
    fun unknownLessonStaysClosedWithoutPro() {
        assertFalse(Access.lessonOpen(course, "missing", paid = false))
    }

    @Test
    fun cheatSheetsAreNeverLocked() {
        assertFalse(Access.cheatSheetLocked())
    }

    @Test
    fun realCoursesOpenTheirFirstModuleOnly() {
        AssetLibrary.library.courses.forEach { course ->
            val open = Access.openLessons(course, paid = false)
            assertEquals(course.id, course.modules.first().lessons, open)
            assertTrue(course.id, open.size < course.lessonCount)
            assertEquals(course.lessonCount, Access.openLessons(course, paid = true).size)
        }
    }
}
