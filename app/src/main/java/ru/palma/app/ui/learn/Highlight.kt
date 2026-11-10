package ru.palma.app.ui.learn

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

object Highlight {
    private val PYTHON = words(
        "def", "return", "if", "elif", "else", "for", "while", "in", "import", "from", "as", "class", "try", "except",
        "finally", "raise", "with", "lambda", "pass", "break", "continue", "and", "or", "not", "None", "True", "False", "is",
    )
    private val GO = words(
        "package", "import", "func", "return", "if", "else", "for", "range", "var", "const", "type", "struct", "interface",
        "map", "chan", "go", "defer", "select", "case", "switch", "default", "break", "continue", "nil", "true", "false",
    )
    private val CPP = words(
        "int", "double", "char", "bool", "void", "return", "if", "else", "for", "while", "do", "class", "struct", "public",
        "private", "const", "auto", "new", "delete", "nullptr", "true", "false", "using", "namespace", "std",
    )
    private val SHELL = words(
        "if", "then", "else", "elif", "fi", "for", "in", "do", "done", "while", "case", "esac", "function", "echo", "read",
        "local", "export", "return", "exit",
    )
    private val CPP_DIRECTIVE = Regex("#\\w+")
    private val MARKUP = Regex("</?[A-Za-z][A-Za-z0-9-]*|/?>|<!DOCTYPE")
    private val CSS = Regex("[a-z-]+(?=\\s*:[^:])|@[a-z-]+")

    private fun words(vararg list: String) = Regex("\\b(" + list.joinToString("|") + ")\\b")

    fun patterns(lang: String): List<Regex> = when (lang) {
        "python" -> listOf(PYTHON)
        "go" -> listOf(GO)
        "cpp" -> listOf(CPP, CPP_DIRECTIVE)
        "sh", "bash" -> listOf(SHELL)
        "html", "web" -> listOf(MARKUP)
        "css" -> listOf(CSS)
        else -> emptyList()
    }

    fun ranges(code: String, lang: String): List<IntRange> = patterns(lang)
        .flatMap { pattern -> pattern.findAll(code).map { it.range } }
        .sortedBy { it.first }
        .fold(mutableListOf()) { acc, range ->
            if (acc.isEmpty() || range.first > acc.last().last) acc.add(range)
            acc
        }

    fun annotate(code: String, lang: String, accent: Color): AnnotatedString = buildAnnotatedString {
        append(code)
        ranges(code, lang).forEach { addStyle(SpanStyle(color = accent), it.first, it.last + 1) }
    }
}
