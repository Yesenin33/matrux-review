package ru.palma.app.learn

object Access {

    fun lessonOpen(course: Course, lessonId: String, paid: Boolean): Boolean =
        paid || course.modules.firstOrNull()?.lessons?.any { it.id == lessonId } == true

    fun openLessons(course: Course, paid: Boolean): List<Lesson> =
        course.lessons.filter { lessonOpen(course, it.id, paid) }

    fun courseLocked(paid: Boolean): Boolean = !paid

    fun cheatSheetLocked(): Boolean = false
}
