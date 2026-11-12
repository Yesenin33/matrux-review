package ru.matrux.app.learn

import org.json.JSONArray
import org.json.JSONObject
import ru.matrux.app.ui.knowledge.Branch
import ru.matrux.app.ui.knowledge.KnowledgeEntry
import ru.matrux.app.ui.knowledge.KnowledgeGroup
import ru.matrux.app.ui.knowledge.KnowledgeSection

object CourseParser {

    fun index(json: String, list: String = "courses"): List<String> =
        JSONObject(json).getJSONArray(list).objects().map { it.getString("file") }

    fun course(json: String): Course {
        val root = JSONObject(json)
        return Course(
            id = root.getString("id"),
            title = root.getString("title"),
            outcome = root.text("outcome") ?: root.text("description").orEmpty(),
            note = root.text("note"),
            description = root.text("description"),
            distros = root.optJSONArray("distros")?.objects()?.mapNotNull { it.text("id") }.orEmpty(),
            level = root.text("level"),
            modules = root.getJSONArray("modules").objects().map(::module),
        ).also { course ->
            require(course.modules.isNotEmpty() && course.modules.all { it.lessons.isNotEmpty() }) { "empty module in ${course.id}" }
            require(course.lessons.map { it.id }.toSet().size == course.lessonCount) { "duplicate lesson id in ${course.id}" }
        }
    }

    fun sources(json: String): List<Source> = JSONObject(json).getJSONArray("sources").objects().map { item ->
        Source(
            course = item.getString("course"),
            author = item.getString("author"),
            title = item.getString("title"),
            url = item.getString("url"),
            license = item.getString("license"),
            accessed = item.getString("accessed"),
            attribution = item.text("attribution"),
        )
    }

    fun sheet(json: String): KnowledgeSection {
        val root = JSONObject(json)
        return KnowledgeSection(
            id = root.getString("id"),
            title = root.getString("title"),
            lead = root.getString("lead"),
            pro = root.optBoolean("pro", false),
            branch = Branch.of(root.optString("branch", Branch.DEEPER.key)),
            groups = root.getJSONArray("groups").objects().map { group ->
                KnowledgeGroup(
                    title = group.getString("title"),
                    entries = group.getJSONArray("entries").objects().map { entry ->
                        KnowledgeEntry(
                            title = entry.getString("title"),
                            summary = entry.getString("summary"),
                            detail = entry.text("detail"),
                            code = entry.text("code"),
                        )
                    },
                )
            },
        )
    }

    private fun module(json: JSONObject) = Module(
        id = json.getString("id"),
        title = json.getString("title"),
        lessons = json.getJSONArray("lessons").objects().map(::lesson),
    )

    private fun lesson(json: JSONObject): Lesson {
        val theory = when {
            json.has("theory") && !json.isNull("theory") && json.get("theory") is JSONArray ->
                json.getJSONArray("theory").strings()
            json.has("theory") && !json.isNull("theory") ->
                splitTheory(json.getString("theory"))
            else -> emptyList()
        }
        val review = json.text("review").orEmpty()
        val examples = json.optJSONArray("examples")?.objects()?.map { ex ->
            LessonExample(
                title = ex.getString("title"),
                lang = ex.getString("lang"),
                code = ex.getString("code"),
                explain = ex.text("explain"),
            )
        }.orEmpty()
        val fallback = Snippet(
            examples.firstOrNull()?.lang.orEmpty(),
            examples.firstOrNull()?.code.orEmpty(),
        )
        val practice = json.optJSONObject("practice")?.let { p ->
            Practice(
                task = p.getString("task"),
                steps = p.optJSONArray("steps")?.strings().orEmpty(),
                hint = p.text("hint"),
            )
        }
        val quiz = json.optJSONArray("quiz")?.objects()?.map { q ->
            QuizQuestion(
                question = q.getString("question"),
                options = q.getJSONArray("options").strings(),
                answer = q.getInt("answer"),
                explain = q.text("explain"),
            )
        }.orEmpty()
        return Lesson(
            id = json.getString("id"),
            title = json.getString("title"),
            theory = theory,
            example = json.optJSONObject("example")?.let(::snippet) ?: fallback,
            task = json.optJSONObject("task")?.let(::task) ?: Task.Choice(
                prompt = quiz.firstOrNull()?.question ?: review,
                code = null,
                options = quiz.firstOrNull()?.options.orEmpty(),
                answer = quiz.firstOrNull()?.answer ?: 0,
            ),
            review = review.ifEmpty { quiz.firstOrNull()?.explain.orEmpty() },
            examples = examples,
            practice = practice,
            quiz = quiz,
            durationMin = json.optInt("duration_min", 0),
            order = json.optInt("order", 0),
        )
    }

    private fun splitTheory(markdown: String): List<String> =
        markdown.split("\n\n").map(::cleanBlock).filter { it.isNotEmpty() }

    private fun cleanBlock(block: String): String {
        val text = block.trim()
        // таблица markdown | a | b | и разделитель |---|---| -> строки "a — b"
        if (text.lines().all { line -> line.startsWith("|") && line.endsWith("|") }) {
            val rows = text.lines()
                .filterNot { line -> line.contains("---") }
                .mapNotNull { line ->
                    val cells = line.removePrefix("|").removeSuffix("|")
                        .split("|").map { it.trim() }
                    cells.joinToString(" — ").takeIf { it.isNotBlank() }
                }
            return rows.joinToString("\n")
        }
        // **жирный** и `код` -> чистый текст
        return text
            .replace("**", "")
            .replace("`", "")
    }

    private fun snippet(json: JSONObject) = Snippet(json.getString("lang"), json.getString("code"))

    private fun task(json: JSONObject): Task = when (val kind = json.getString("kind")) {
        "run" -> Task.Run(
            prompt = json.getString("prompt"),
            runtime = Runtime.of(json.getString("runtime")),
            starter = json.optString("starter", ""),
            solution = json.getString("solution"),
            expect = json.text("expect"),
            check = json.text("check"),
            stdin = json.text("stdin"),
        ).also {
            require(if (it.runtime == Runtime.WEB) it.check != null else it.expect != null) { "run task needs expect or check" }
        }
        "choice" -> Task.Choice(
            prompt = json.getString("prompt"),
            code = json.optJSONObject("code")?.let(::snippet),
            options = json.getJSONArray("options").strings(),
            answer = json.getInt("answer"),
        ).also { require(it.answer in it.options.indices) { "choice answer out of range" } }
        "output" -> Task.Output(
            prompt = json.getString("prompt"),
            code = snippet(json.getJSONObject("code")),
            answer = json.getString("answer"),
        )
        else -> error("unknown task kind $kind")
    }

    private fun JSONObject.text(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
}
