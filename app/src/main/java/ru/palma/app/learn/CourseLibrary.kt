package ru.palma.app.learn

import ru.palma.app.ui.knowledge.KnowledgeSection

class CourseLibrary(private val read: (String) -> String) {

    val courses: List<Course> by lazy {
        CourseParser.index(read(COURSES + INDEX)).map { file -> CourseParser.course(read(COURSES + file)) }
    }

    val sources: List<Source> by lazy { CourseParser.sources(read(COURSES + SOURCES)) }

    val sheets: List<KnowledgeSection> by lazy {
        CourseParser.index(read(SHEETS + INDEX), "sheets").map { file -> CourseParser.sheet(read(SHEETS + file)) }
    }

    fun course(id: String): Course? = courses.firstOrNull { it.id == id }

    fun sheet(id: String): KnowledgeSection? = sheets.firstOrNull { it.id == id }

    companion object {
        const val COURSES = "courses/"
        const val SHEETS = "sheets/"
        const val INDEX = "index.json"
        const val SOURCES = "sources.json"
    }
}
