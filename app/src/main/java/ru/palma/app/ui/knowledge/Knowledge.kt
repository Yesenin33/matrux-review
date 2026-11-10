package ru.palma.app.ui.knowledge

enum class Branch(val key: String) {
    START("start"),
    DEEPER("deeper");

    companion object {
        fun of(key: String): Branch = entries.firstOrNull { it.key == key } ?: DEEPER
    }
}

data class KnowledgeEntry(
    val title: String,
    val summary: String,
    val detail: String? = null,
    val code: String? = null,
)

data class KnowledgeGroup(
    val title: String,
    val entries: List<KnowledgeEntry>,
)

data class KnowledgeSection(
    val id: String,
    val title: String,
    val lead: String,
    val pro: Boolean,
    val groups: List<KnowledgeGroup>,
    val branch: Branch = Branch.DEEPER,
) {
    val size: Int get() = groups.sumOf { it.entries.size }
}
