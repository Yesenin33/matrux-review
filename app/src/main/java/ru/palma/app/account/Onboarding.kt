package ru.palma.app.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.palma.app.data.LocalState

data class OnboardingFlags(val done: Boolean, val replay: Boolean = false) {
    fun visible(signedIn: Boolean): Boolean = replay || (signedIn && !done)
}

class Onboarding(private val local: LocalState) {
    private val state = MutableStateFlow(OnboardingFlags(done = local.onboarded))
    val flags: StateFlow<OnboardingFlags> = state.asStateFlow()

    fun replay() {
        state.update { it.copy(replay = true) }
    }

    fun finish() {
        local.onboarded = true
        state.value = OnboardingFlags(done = true)
    }
}
