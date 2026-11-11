package ru.matrux.app.store

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.AuthCodeData
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.matrux.app.account.AuthCode
import ru.matrux.app.account.AuthGateway
import ru.matrux.app.account.AuthRequest
import ru.matrux.app.data.Failure
import ru.matrux.app.data.FailureException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VkIdAuth : AuthGateway {
    override val available = true

    override suspend fun authorize(owner: LifecycleOwner, request: AuthRequest): AuthCode =
        suspendCancellableCoroutine { continuation ->
            val callback = object : VKIDAuthCallback {
                override fun onAuth(accessToken: AccessToken) {
                    if (continuation.isActive) continuation.resumeWithException(FailureException(Failure.SignInRejected))
                }

                override fun onAuthCode(data: AuthCodeData, isCompletion: Boolean) {
                    if (continuation.isActive) continuation.resume(AuthCode(data.code, data.deviceId))
                }

                override fun onFail(fail: VKIDAuthFail) {
                    if (!continuation.isActive) return
                    val failure = if (fail is VKIDAuthFail.Canceled) Failure.Cancelled else Failure.SignInRejected
                    continuation.resumeWithException(FailureException(failure))
                }
            }
            VKID.instance.authorize(
                owner,
                callback,
                VKIDAuthParams {
                    codeChallenge = request.challenge
                    state = request.state
                    scopes = setOf("phone", "email")
                },
            )
        }

    companion object {
        fun init(app: Application) = VKID.init(app)
    }
}
