package ru.matrux.app.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.matrux.app.data.LocalState

data class OnboardingFlags(val done: Boolean) {
    val visible: Boolean get() = !done
}

class Onboarding(private val local: LocalState, private val version: Int) {
    private val state = MutableStateFlow(OnboardingFlags(done = local.onboardVersion >= version))
    val flags: StateFlow<OnboardingFlags> = state.asStateFlow()

    fun finish() {
        local.onboardVersion = version
        state.value = OnboardingFlags(done = true)
    }
}
