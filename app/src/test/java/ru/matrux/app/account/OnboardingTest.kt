package ru.matrux.app.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.matrux.app.data.InMemoryLocalState

class OnboardingTest {

    @Test
    fun firstLaunchShowsSlides() {
        val local = InMemoryLocalState()
        assertTrue(Onboarding(local, 65).flags.value.visible)
    }

    @Test
    fun slidesDoNotRepeatOnSameVersion() {
        val local = InMemoryLocalState()
        val onboarding = Onboarding(local, 65)
        onboarding.finish()
        assertEquals(65, local.onboardVersion)
        assertFalse(onboarding.flags.value.visible)
        assertFalse(Onboarding(local, 65).flags.value.visible)
    }

    @Test
    fun updateBringsSlidesBack() {
        val local = InMemoryLocalState()
        Onboarding(local, 65).finish()
        assertTrue(Onboarding(local, 66).flags.value.visible)
        assertFalse(Onboarding(local, 64).flags.value.visible)
    }
}
