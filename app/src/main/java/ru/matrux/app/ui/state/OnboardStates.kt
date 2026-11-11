package ru.matrux.app.ui.state

enum class Slide { WELCOME, CATALOG, BASICS, LEVEL, START }

class OnboardUi {
    val slides: List<Slide> get() = Slide.entries
    val last: Int get() = slides.lastIndex
}

fun onboardUi(): OnboardUi = OnboardUi()
