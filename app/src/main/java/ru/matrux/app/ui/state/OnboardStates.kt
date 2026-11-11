package ru.matrux.app.ui.state

import ru.matrux.app.data.Catalog
import ru.matrux.app.data.ImageInfo
import ru.matrux.app.ui.knowledge.KnowledgeSection

enum class Slide { DESK, COMMANDS }

data class OnboardUi(
    val systems: List<ImageInfo>,
    val sheets: List<KnowledgeSection>,
) {
    val slides: List<Slide> get() = Slide.entries
    val last: Int get() = slides.lastIndex
}

fun onboardUi(catalog: Load<Catalog>, sections: List<KnowledgeSection>): OnboardUi {
    val images = (catalog as? Load.Ready)?.value?.manifest?.images.orEmpty()
    return OnboardUi(systems = images, sheets = sections)
}
