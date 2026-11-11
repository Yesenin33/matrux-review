package ru.matrux.app.learn

enum class Runtime(val key: String) {
    PYTHON("python"),
    NODE("node"),
    SH("sh"),
    BASH("bash"),
    WEB("web");

    companion object {
        fun of(key: String): Runtime = entries.firstOrNull { it.key == key } ?: error("unknown runtime $key")
    }
}

data class Snippet(val lang: String, val code: String)

sealed interface Task {
    val prompt: String

    data class Run(
        override val prompt: String,
        val runtime: Runtime,
        val starter: String,
        val solution: String,
        val expect: String?,
        val check: String?,
        val stdin: String?,
    ) : Task

    data class Choice(
        override val prompt: String,
        val code: Snippet?,
        val options: List<String>,
        val answer: Int,
    ) : Task

    data class Output(
        override val prompt: String,
        val code: Snippet,
        val answer: String,
    ) : Task
}

data class Lesson(
    val id: String,
    val title: String,
    val theory: List<String>,
    val example: Snippet,
    val task: Task,
    val review: String,
)

data class Module(val id: String, val title: String, val lessons: List<Lesson>)

data class Course(
    val id: String,
    val title: String,
    val outcome: String,
    val note: String?,
    val modules: List<Module>,
) {
    val lessons: List<Lesson> get() = modules.flatMap { it.lessons }
    val lessonCount: Int get() = modules.sumOf { it.lessons.size }

    fun lesson(id: String): Lesson? = lessons.firstOrNull { it.id == id }

    fun next(id: String): Lesson? {
        val all = lessons
        val index = all.indexOfFirst { it.id == id }
        return if (index < 0) null else all.getOrNull(index + 1)
    }

    fun number(id: String): Int = lessons.indexOfFirst { it.id == id } + 1
}

data class Source(
    val course: String,
    val author: String,
    val title: String,
    val url: String,
    val license: String,
    val accessed: String,
    val attribution: String?,
)
