package ru.palma.app.data

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.util.Base64

enum class SignatureCheck { VALID, INVALID, MISSING, MALFORMED_KEY, MALFORMED_SIGNATURE }

object ManifestSignature {
    private const val KEY_BYTES = 32
    private const val SIGNATURE_BYTES = 64

    fun check(publicKeyB64: String, signatureB64: String?, body: ByteArray): SignatureCheck {
        if (signatureB64.isNullOrBlank()) return SignatureCheck.MISSING
        val key = decode(publicKeyB64) ?: return SignatureCheck.MALFORMED_KEY
        if (key.size != KEY_BYTES) return SignatureCheck.MALFORMED_KEY
        val signature = decode(signatureB64) ?: return SignatureCheck.MALFORMED_SIGNATURE
        if (signature.size != SIGNATURE_BYTES) return SignatureCheck.MALFORMED_SIGNATURE
        val verifier = Ed25519Signer()
        verifier.init(false, Ed25519PublicKeyParameters(key, 0))
        verifier.update(body, 0, body.size)
        return if (verifier.verifySignature(signature)) SignatureCheck.VALID else SignatureCheck.INVALID
    }

    private fun decode(value: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(value.trim()) }.getOrNull()
}

sealed interface KeyDecision {
    data class Use(val key: String, val remember: Boolean) : KeyDecision
    data object Reject : KeyDecision
}

object SigningKeyPolicy {
    fun decide(served: String, pinned: String?, remembered: String?): KeyDecision {
        val key = served.trim()
        return when {
            !pinned.isNullOrBlank() -> if (pinned.trim() == key) KeyDecision.Use(key, false) else KeyDecision.Reject
            remembered.isNullOrBlank() -> KeyDecision.Use(key, true)
            remembered == key -> KeyDecision.Use(key, false)
            else -> KeyDecision.Reject
        }
    }
}
