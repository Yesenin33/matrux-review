package ru.matrux.app.account

import androidx.lifecycle.LifecycleOwner
import ru.matrux.app.data.Failure
import ru.matrux.app.data.FailureException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class AccountInfo(val id: String, val phone: String?)

data class AuthCode(val code: String, val deviceId: String)

data class AuthRequest(val verifier: String, val challenge: String, val state: String)

object Pkce {
    private const val VERIFIER_BYTES = 48
    private const val STATE_BYTES = 16

    fun request(random: SecureRandom = SecureRandom()): AuthRequest {
        val verifier = token(random, VERIFIER_BYTES)
        return AuthRequest(verifier, challenge(verifier), token(random, STATE_BYTES))
    }

    fun challenge(verifier: String): String =
        encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    private fun token(random: SecureRandom, bytes: Int): String =
        encoder.encodeToString(ByteArray(bytes).also(random::nextBytes))

    private val encoder = Base64.getUrlEncoder().withoutPadding()
}

interface AuthGateway {
    val available: Boolean
    suspend fun authorize(owner: LifecycleOwner, request: AuthRequest): AuthCode
}

class AuthNotConnected : AuthGateway {
    override val available = false
    override suspend fun authorize(owner: LifecycleOwner, request: AuthRequest): AuthCode =
        throw FailureException(Failure.SignInUnavailable)
}
