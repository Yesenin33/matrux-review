package ru.palma.app.learn

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseParserTest {

    @Test
    fun parsesEveryTaskKind() {
        val course = Fixture.course
        assertEquals("demo", course.id)
        assertEquals("Без запуска", course.note)
        assertEquals(listOf("m1", "m2"), course.modules.map { it.id })
        val run = course.lesson("a")!!.task as Task.Run
        assertEquals(Runtime.PYTHON, run.runtime)
        assertEquals("2", run.expect)
        assertEquals("x\n", run.stdin)
        assertEquals("", run.starter)
        val web = course.lesson("b")!!.task as Task.Run
        assertEquals(Runtime.WEB, web.runtime)
        assertNull(web.expect)
        assertEquals("!!document.querySelector('p')", web.check)
        val choice = course.lesson("c")!!.task as Task.Choice
        assertEquals(1, choice.answer)
        assertEquals("go", choice.code?.lang)
        val output = course.lesson("d")!!.task as Task.Output
        assertEquals("1\n2", output.answer)
        assertEquals(listOf("Один", "Два", "Три"), course.lesson("c")!!.theory)
    }

    @Test
    fun rejectsBrokenContent() {
        fun mutate(change: (JSONObject) -> Unit): String {
            val root = JSONObject(Fixture.json)
            change(root)
            return root.toString()
        }
        val lessons = { root: JSONObject -> root.getJSONArray("modules").getJSONObject(1).getJSONArray("lessons") }
        assertThrows(IllegalArgumentException::class.java) {
            CourseParser.course(mutate { lessons(it).getJSONObject(0).getJSONObject("task").put("answer", 7) })
        }
        assertThrows(IllegalArgumentException::class.java) {
            CourseParser.course(mutate { it.getJSONArray("modules").getJSONObject(0).getJSONArray("lessons").getJSONObject(1).getJSONObject("task").remove("check") })
        }
        assertThrows(IllegalArgumentException::class.java) {
            CourseParser.course(mutate { lessons(it).getJSONObject(1).put("id", "a") })
        }
        assertThrows(IllegalStateException::class.java) {
            CourseParser.course(mutate { lessons(it).getJSONObject(0).getJSONObject("task").put("kind", "quiz") })
        }
        assertThrows(IllegalStateException::class.java) {
            CourseParser.course(mutate { it.getJSONArray("modules").getJSONObject(0).getJSONArray("lessons").getJSONObject(0).getJSONObject("task").put("runtime", "ruby") })
        }
    }

    @Test
    fun indexListsFilesInOrder() {
        val index = """{"courses": [{"id": "b", "file": "b.json"}, {"id": "a", "file": "a.json"}]}"""
        assertEquals(listOf("b.json", "a.json"), CourseParser.index(index))
        val sheets = """{"sheets": [{"id": "git", "file": "git.json"}]}"""
        assertEquals(listOf("git.json"), CourseParser.index(sheets, "sheets"))
    }

    @Test
    fun parsesSheetWithOptionalFields() {
        val sheet = CourseParser.sheet(
            """{"id": "git", "title": "git", "lead": "Ветки", "pro": true, "branch": "start",
               "groups": [{"title": "Начало", "entries": [{"title": "git init", "summary": "Создаёт", "code": "git init"}, {"title": "HEAD", "summary": "Указатель"}]}]}""",
        )
        assertEquals(2, sheet.size)
        assertEquals("git init", sheet.groups[0].entries[0].code)
        assertNull(sheet.groups[0].entries[1].detail)
        assertTrue(sheet.pro)
        assertEquals(ru.palma.app.ui.knowledge.Branch.START, sheet.branch)
    }
}
