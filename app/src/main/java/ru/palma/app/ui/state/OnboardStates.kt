package ru.palma.app.ui.state

import ru.palma.app.billing.Terms
import ru.palma.app.data.Catalog
import ru.palma.app.data.ImageInfo
import ru.palma.app.learn.Course
import ru.palma.app.ui.knowledge.KnowledgeSection

enum class Slide { DESK, SHEETS, LANGUAGES, PRO }

private val SHEET_ORDER = listOf("files", "terms", "packages", "users", "services", "network", "disks", "logs", "scripts")

data class OnboardUi(
    val systems: List<ImageInfo>,
    val sheets: List<KnowledgeSection>,
    val courses: List<Course>,
    val terms: Terms,
    val offer: ProOfferUi,
) {
    val slides: List<Slide> get() = Slide.entries
    val last: Int get() = slides.lastIndex
}

fun onboardSheets(sections: List<KnowledgeSection>): List<KnowledgeSection> =
    SHEET_ORDER.mapNotNull { id -> sections.firstOrNull { it.id == id } }

fun onboardUi(catalog: Load<Catalog>, sections: List<KnowledgeSection>, courses: List<Course>, terms: Terms): OnboardUi {
    val images = (catalog as? Load.Ready)?.value?.manifest?.images.orEmpty()
    return OnboardUi(
        systems = images,
        sheets = onboardSheets(sections),
        courses = courses,
        terms = terms,
        offer = proOffer(images, courses, sections),
    )
}
