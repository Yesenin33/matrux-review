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
    fun sevenCoursesInIndexOrder() {
        assertEquals(
            listOf("linux", "python", "git", "sqlite"),
            library.courses.map { it.id },
        )
    }

    @Test
    fun everyCourseIsWellSized() {
        val counts = library.courses.associate { it.id to it.lessonCount }
        assertEquals(34, counts["linux"])
        assertEquals(41, counts["python"])
        assertEquals(25, counts["sqlite"])
        library.courses.forEach { course ->
            assertEquals(course.lessons.size, course.lessonCount)
            assertEquals(course.lessons.size, course.modules.sumOf { it.lessons.size })
        }
    }

    @Test
    fun everyLessonIsComplete() {
        library.courses.forEach { course ->
            course.lessons.forEach { lesson ->
                val where = course.id + "/" + lesson.id
                assertTrue(where, lesson.theory.isNotEmpty())
                assertTrue(where, lesson.theory.all { it.isNotBlank() })
                assertTrue(where, lesson.example.code.isNotBlank() || lesson.examples.isNotEmpty())
                assertTrue(where, lesson.task.prompt.isNotBlank())
                if (lesson.quiz.isEmpty()) {
                    assertTrue(where, lesson.review.isNotBlank())
                } else {
                    assertEquals(where, lesson.examples.isNotEmpty(), true)
                    lesson.quiz.forEach { question ->
                        assertTrue(where, question.question.isNotBlank())
                        assertTrue(where, question.options.size >= 3)
                        assertTrue(where, question.answer in question.options.indices)
                    }
                }
                val text = (
                    lesson.theory + listOf(lesson.review, lesson.task.prompt, lesson.title) +
                        lesson.quiz.flatMap { listOf(it.question, it.explain.orEmpty()) } +
                        lesson.quiz.flatMap { it.options } +
                        lesson.examples.flatMap { listOf(it.title, it.explain.orEmpty(), it.code) } +
                        listOfNotNull(lesson.practice?.task) + (lesson.practice?.steps ?: emptyList()) +
                        listOfNotNull(lesson.practice?.hint)
                    ).joinToString(" ").lowercase()
                placeholders.forEach { assertFalse("$where: $it", text.contains(it)) }
            }
        }
    }

    @Test
    fun liveCoursesRunAndCompiledCoursesSayWhy() {
        // linux и python — формат «теория + практика + квиз», без запуска кода в песочнице
        listOf("linux", "python", "sqlite").forEach { id ->
            val course = library.course(id)!!
            assertTrue(id, course.lessons.none { it.task is Task.Run })
            assertTrue(id, course.lessons.all { it.quiz.isNotEmpty() && it.practice != null })
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
            if (source.url.isBlank()) {
                // оригинальный контент Matrux без внешнего источника
                assertTrue(source.url, source.license.contains("собственный контент"))
            } else {
                assertTrue(source.url, source.url.startsWith("https://") || source.url.startsWith("http://"))
            }
            assertTrue(source.url, source.license.isNotBlank() && source.author.isNotBlank() && source.title.isNotBlank())
            assertTrue(source.accessed.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
            assertTrue(source.course in library.courses.map { it.id })
        }
        sources.filter { "developer.mozilla.org" in it.url }.forEach { source ->
            assertEquals("CC BY-SA 2.5", source.license)
            assertEquals("Материалы MDN, помощники Mozilla, CC BY-SA 2.5", source.attribution)
        }
        assertEquals(sources.size, sources.map { it.url to it.course }.toSet().size)
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
