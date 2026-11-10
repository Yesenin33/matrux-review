package ru.palma.app.learn

import java.io.File

object AssetLibrary {
    private val root = File("src/main/assets")

    fun read(name: String): String = File(root, name).readText()

    val library: CourseLibrary by lazy { CourseLibrary(::read) }
}
