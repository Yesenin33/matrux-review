package ru.palma.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.palma.app.TestSigner

class SignatureTest {
    @Test
    fun acceptsSignatureMadeByBackend() {
        assertEquals(
            SignatureCheck.VALID,
            ManifestSignature.check(BackendFixture.publicKey, BackendFixture.signature, BackendFixture.body.toByteArray()),
        )
    }

    @Test
    fun rejectsBackendBodyWithOneByteChanged() {
        val tampered = BackendFixture.body.replace("2500000", "2500001").toByteArray()
        assertEquals(SignatureCheck.INVALID, ManifestSignature.check(BackendFixture.publicKey, BackendFixture.signature, tampered))
    }

    @Test
    fun rejectsReformattedJson() {
        val pretty = BackendFixture.body.replace(",", ", ").toByteArray()
        assertEquals(SignatureCheck.INVALID, ManifestSignature.check(BackendFixture.publicKey, BackendFixture.signature, pretty))
    }

    @Test
    fun rejectsOtherKey() {
        val other = TestSigner()
        assertEquals(SignatureCheck.INVALID, ManifestSignature.check(other.publicKey, BackendFixture.signature, BackendFixture.body.toByteArray()))
    }

    @Test
    fun roundTripWithFreshKey() {
        val signer = TestSigner()
        val body = "{}".toByteArray()
        assertEquals(SignatureCheck.VALID, ManifestSignature.check(signer.publicKey, signer.sign(body), body))
    }

    @Test
    fun reportsMissingAndMalformed() {
        val body = "{}".toByteArray()
        assertEquals(SignatureCheck.MISSING, ManifestSignature.check(BackendFixture.publicKey, null, body))
        assertEquals(SignatureCheck.MISSING, ManifestSignature.check(BackendFixture.publicKey, " ", body))
        assertEquals(SignatureCheck.MALFORMED_SIGNATURE, ManifestSignature.check(BackendFixture.publicKey, "%%%", body))
        assertEquals(SignatureCheck.MALFORMED_SIGNATURE, ManifestSignature.check(BackendFixture.publicKey, "AAAA", body))
        assertEquals(SignatureCheck.MALFORMED_KEY, ManifestSignature.check("AAAA", BackendFixture.signature, body))
        assertEquals(SignatureCheck.MALFORMED_KEY, ManifestSignature.check("не base64", BackendFixture.signature, body))
    }

    @Test
    fun keyPolicyTrustsFirstKeyAndRejectsRotation() {
        assertEquals(KeyDecision.Use("k1", true), SigningKeyPolicy.decide("k1", null, null))
        assertEquals(KeyDecision.Use("k1", false), SigningKeyPolicy.decide("k1", null, "k1"))
        assertEquals(KeyDecision.Reject, SigningKeyPolicy.decide("k2", null, "k1"))
    }

    @Test
    fun keyPolicyPrefersPinnedKey() {
        assertEquals(KeyDecision.Use("p", false), SigningKeyPolicy.decide("p", "p", "old"))
        assertEquals(KeyDecision.Reject, SigningKeyPolicy.decide("x", "p", null))
    }
}
