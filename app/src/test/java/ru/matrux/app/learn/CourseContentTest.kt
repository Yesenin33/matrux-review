package ru.matrux.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseContentTest {
    private val library = AssetLibrary.library
    private val placeholders = listOf("заглушк", "здесь будет", "todo:", "lorem", "скоро появится", "в разработке")

    @Test
    fun sixCoursesInIndexOrder() {
        assertEquals(listOf("python", "go", "html", "css", "cpp", "scripts"), library.courses.map { it.id })
    }

    @Test
    fun everyCourseHasTenToFourteenLessons() {
        library.courses.forEach { course ->
            assertTrue(course.id + " " + course.lessonCount, course.lessonCount in 10..14)
            assertEquals(course.lessons.size, course.lessonCount)
            assertEquals(course.lessons.size, course.modules.sumOf { it.lessons.size })
        }
        assertEquals(78, library.courses.sumOf { it.lessonCount })
    }

    @Test
    fun everyLessonIsComplete() {
        library.courses.forEach { course ->
            course.lessons.forEach { lesson ->
                val where = course.id + "/" + lesson.id
                assertTrue(where, lesson.theory.size in 2..4)
                assertTrue(where, lesson.theory.all { it.isNotBlank() })
                assertTrue(where, lesson.example.code.isNotBlank())
                assertTrue(where, lesson.task.prompt.isNotBlank())
                assertTrue(where, lesson.review.isNotBlank())
                val text = (lesson.theory + lesson.review + lesson.task.prompt + lesson.title).joinToString(" ").lowercase()
                placeholders.forEach { assertFalse("$where: $it", text.contains(it)) }
            }
        }
    }

    @Test
    fun liveCoursesRunAndCompiledCoursesSayWhy() {
        val live = mapOf(
            "python" to setOf(Runtime.PYTHON),
            "scripts" to setOf(Runtime.SH),
            "html" to setOf(Runtime.WEB),
            "css" to setOf(Runtime.WEB),
        )
        live.forEach { (id, runtimes) ->
            val course = library.course(id)!!
            val runs = course.lessons.map { it.task }.filterIsInstance<Task.Run>()
            assertTrue(id, runs.size >= course.lessonCount - 2)
            assertTrue(id, runs.all { it.runtime in runtimes })
            runs.forEach { task ->
                if (task.runtime == Runtime.WEB) assertNotNull(task.check) else assertNotNull(task.expect)
                assertTrue(task.solution.isNotBlank())
                assertFalse(task.solution == task.starter)
            }
            assertNull(id, course.note)
        }
        listOf("go", "cpp").forEach { id ->
            val course = library.course(id)!!
            assertTrue(id, course.lessons.none { it.task is Task.Run })
            assertTrue(id, course.lessons.count { it.task is Task.Output } >= course.lessonCount / 2)
            assertFalse(id, course.note.isNullOrBlank())
        }
    }

    @Test
    fun choiceAnswersPointAtOptions() {
        library.courses.flatMap { it.lessons }.map { it.task }.filterIsInstance<Task.Choice>().forEach { task ->
            assertTrue(task.options.size >= 3)
            assertTrue(task.answer in task.options.indices)
            assertEquals(task.options.size, task.options.toSet().size)
        }
    }

    @Test
    fun sourcesCoverEveryCourseWithLicenses() {
        val sources = library.sources
        library.courses.forEach { course -> assertTrue(course.id, sources.any { it.course == course.id }) }
        sources.forEach { source ->
            assertTrue(source.url, source.url.startsWith("https://") || source.url.startsWith("http://"))
            assertTrue(source.url, source.license.isNotBlank() && source.author.isNotBlank() && source.title.isNotBlank())
            assertTrue(source.accessed.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
            assertTrue(source.course in library.courses.map { it.id })
        }
        sources.filter { "developer.mozilla.org" in it.url }.forEach { source ->
            assertEquals("CC BY-SA 2.5", source.license)
            assertEquals("Материалы MDN, помощники Mozilla, CC BY-SA 2.5", source.attribution)
        }
        assertEquals(sources.size, sources.map { it.url }.toSet().size)
    }

    @Test
    fun cheatSheetsLoadFromAssets() {
        val sheets = library.sheets
        assertEquals(14, sheets.size)
        assertEquals(sheets.size, sheets.map { it.id }.toSet().size)
        assertTrue(sheets.all { it.size > 0 && it.title.isNotBlank() })
        assertEquals("start", sheets.first().id)
        assertNotNull(library.sheet("git"))
    }
}
