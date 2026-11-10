package ru.palma.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProgressTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun marksByCourseAndLesson() {
        val progress = Progress().mark("demo", "a").mark("demo", "c").mark("other", "a").mark("demo", "a")
        assertTrue(progress.isDone("demo", "a"))
        assertFalse(progress.isDone("demo", "b"))
        assertTrue(progress.isDone("other", "a"))
        assertEquals(setOf("a", "c"), progress.doneIn("demo"))
        assertEquals(2, progress.count(Fixture.course))
    }

    @Test
    fun countIgnoresLessonsMissingFromCourse() {
        val progress = Progress().mark("demo", "removed").mark("demo", "b")
        assertEquals(1, progress.count(Fixture.course))
    }

    @Test
    fun fileStoreSurvivesRestart() {
        val file = File(folder.root, "learning.json")
        val store = FileProgressStore(file)
        store.mark("python", "print")
        store.mark("python", "input")
        store.mark("html", "skeleton")
        store.seeOffer()
        val reopened = FileProgressStore(file).progress.value
        assertEquals(setOf("print", "input"), reopened.doneIn("python"))
        assertEquals(setOf("skeleton"), reopened.doneIn("html"))
        assertTrue(reopened.offerSeen)
        assertFalse(File(folder.root, ".learning.json.new").exists())
    }

    @Test
    fun brokenOrMissingFileStartsEmpty() {
        assertEquals(Progress(), FileProgressStore(File(folder.root, "absent.json")).progress.value)
        val broken = File(folder.root, "broken.json").apply { writeText("{not json") }
        assertEquals(Progress(), FileProgressStore(broken).progress.value)
    }

    @Test
    fun repeatedMarkDoesNotRewriteFile() {
        val file = File(folder.root, "learning.json")
        val store = FileProgressStore(file)
        store.mark("go", "hello")
        file.writeText("{\"done\": {\"go\": [\"hello\"]}, \"offer_seen\": false, \"marker\": 1}")
        store.mark("go", "hello")
        assertTrue(file.readText().contains("marker"))
    }

    @Test
    fun storePublishesUpdates() {
        val store = InMemoryProgressStore()
        assertFalse(store.progress.value.offerSeen)
        store.seeOffer()
        store.mark("css", "grid")
        assertTrue(store.progress.value.offerSeen)
        assertTrue(store.progress.value.isDone("css", "grid"))
    }
}
