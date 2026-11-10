package ru.palma.app.learn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Progress(val done: Map<String, Set<String>> = emptyMap(), val offerSeen: Boolean = false) {
    fun doneIn(courseId: String): Set<String> = done[courseId].orEmpty()

    fun isDone(courseId: String, lessonId: String): Boolean = lessonId in doneIn(courseId)

    fun count(course: Course): Int = course.lessons.count { isDone(course.id, it.id) }

    fun mark(courseId: String, lessonId: String): Progress =
        copy(done = done + (courseId to (doneIn(courseId) + lessonId)))
}

interface ProgressStore {
    val progress: StateFlow<Progress>
    fun mark(courseId: String, lessonId: String)
    fun seeOffer()
}

open class InMemoryProgressStore(initial: Progress = Progress()) : ProgressStore {
    protected val state = MutableStateFlow(initial)
    override val progress: StateFlow<Progress> = state.asStateFlow()

    override fun mark(courseId: String, lessonId: String) {
        update(state.value.mark(courseId, lessonId))
    }

    override fun seeOffer() {
        update(state.value.copy(offerSeen = true))
    }

    protected open fun update(next: Progress) {
        state.value = next
    }
}

class FileProgressStore(private val file: File) : InMemoryProgressStore(read(file)) {

    override fun update(next: Progress) {
        if (next == state.value) return
        super.update(next)
        write(file, next)
    }

    companion object {
        fun read(file: File): Progress = runCatching {
            if (!file.isFile) return Progress()
            val root = JSONObject(file.readText())
            val done = root.optJSONObject("done") ?: JSONObject()
            Progress(
                done = done.keys().asSequence().associateWith { key ->
                    val lessons = done.getJSONArray(key)
                    List(lessons.length()) { lessons.getString(it) }.toSet()
                },
                offerSeen = root.optBoolean("offer_seen", false),
            )
        }.getOrDefault(Progress())

        fun write(file: File, progress: Progress) {
            val done = JSONObject()
            progress.done.toSortedMap().forEach { (course, lessons) -> done.put(course, JSONArray(lessons.sorted())) }
            val body = JSONObject().put("done", done).put("offer_seen", progress.offerSeen).toString()
            file.parentFile?.mkdirs()
            val staged = File(file.parentFile, ".${file.name}.new")
            staged.writeText(body)
            if (!staged.renameTo(file)) {
                file.writeText(body)
                staged.delete()
            }
        }
    }
}
