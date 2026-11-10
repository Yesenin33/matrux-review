package ru.palma.app.ui.knowledge

enum class Branch { START, DEEPER }

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
)

val knowledge: List<KnowledgeSection> = listOf(
    firstSteps,
    files,
    linux,
    terms,
    packages,
    users,
    services,
    network,
    disks,
    logs,
    scripts,
    git,
    hardware,
    languages,
)