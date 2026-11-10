package ru.palma.app.ui.state

import ru.palma.app.learn.Access
import ru.palma.app.learn.Course
import ru.palma.app.learn.Lesson
import ru.palma.app.learn.Module
import ru.palma.app.learn.Progress
import ru.palma.app.learn.Task
import ru.palma.app.learn.Verdict
import ru.palma.app.ui.knowledge.KnowledgeSection

data class CourseCard(val course: Course, val done: Int, val locked: Boolean) {
    val total: Int get() = course.lessonCount
}

data class SectionRow(val section: KnowledgeSection, val locked: Boolean)

data class KnowledgeUi(
    val courses: List<CourseCard>,
    val rows: List<SectionRow>,
    val paid: Boolean,
    val offerOnEnter: Boolean,
)

fun knowledgeUi(courses: List<Course>, sections: List<KnowledgeSection>, paid: Boolean, progress: Progress): KnowledgeUi =
    KnowledgeUi(
        courses = courses.map { CourseCard(it, progress.count(it), Access.courseLocked(paid)) },
        rows = sections.map { SectionRow(it, Access.cheatSheetLocked()) },
        paid = paid,
        offerOnEnter = !paid && !progress.offerSeen,
    )

data class LessonRow(val lesson: Lesson, val number: Int, val done: Boolean, val locked: Boolean)

data class ModuleRows(val module: Module, val lessons: List<LessonRow>)

data class CourseUi(val course: Course, val paid: Boolean, val modules: List<ModuleRows>) {
    val total: Int get() = course.lessonCount
    val done: Int get() = modules.sumOf { module -> module.lessons.count { it.done } }
}

fun courseUi(course: Course, paid: Boolean, progress: Progress): CourseUi {
    var number = 0
    return CourseUi(
        course = course,
        paid = paid,
        modules = course.modules.map { module ->
            ModuleRows(
                module = module,
                lessons = module.lessons.map { lesson ->
                    number += 1
                    LessonRow(
                        lesson = lesson,
                        number = number,
                        done = progress.isDone(course.id, lesson.id),
                        locked = !Access.lessonOpen(course, lesson.id, paid),
                    )
                },
            )
        },
    )
}

sealed interface Attempt {
    data object Idle : Attempt
    data object Running : Attempt
    data class Result(val verdict: Verdict) : Attempt
}

data class LessonUi(
    val course: Course,
    val lesson: Lesson,
    val done: Boolean,
    val attempt: Attempt,
    val nextOpen: Boolean,
) {
    val number: Int get() = course.number(lesson.id)
    val total: Int get() = course.lessonCount
    val next: Lesson? get() = course.next(lesson.id)
    val verdict: Verdict? get() = (attempt as? Attempt.Result)?.verdict
    val reviewed: Boolean get() = verdict is Verdict.Pass || verdict is Verdict.Fail
    val passed: Boolean get() = verdict is Verdict.Pass
    val starter: String get() = (lesson.task as? Task.Run)?.starter.orEmpty()
}

fun lessonUi(course: Course, lesson: Lesson, paid: Boolean, progress: Progress, attempt: Attempt): LessonUi = LessonUi(
    course = course,
    lesson = lesson,
    done = progress.isDone(course.id, lesson.id),
    attempt = attempt,
    nextOpen = course.next(lesson.id)?.let { Access.lessonOpen(course, it.id, paid) } == true,
)
