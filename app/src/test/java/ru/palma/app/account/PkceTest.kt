package ru.palma.app.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {
    @Test
    fun challengeMatchesRfc7636Example() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test
    fun requestIsFreshAndWellFormed() {
        val first = Pkce.request()
        val second = Pkce.request()
        assertNotEquals(first.verifier, second.verifier)
        assertNotEquals(first.state, second.state)
        assertTrue(first.verifier.length in 43..128)
        assertTrue(first.verifier.matches(Regex("^[A-Za-z0-9_-]+$")))
        assertEquals(Pkce.challenge(first.verifier), first.challenge)
    }
}
