package ru.palma.app.data

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

sealed interface Failure {
    data object NoConnection : Failure
    data object NeedSubscription : Failure
    data object NoAccess : Failure
    data object ImageUnavailable : Failure
    data object ImageNotFound : Failure
    data object BadSignature : Failure
    data object UnknownSigningKey : Failure
    data object BrokenManifest : Failure
    data object ChecksumMismatch : Failure
    data object NotEnoughSpace : Failure
    data object BrokenArchive : Failure
    data object PurchaseNotFound : Failure
    data object PurchaseOnOtherDevice : Failure
    data object BillingUnavailable : Failure
    data object EngineMissing : Failure
    data object EngineCrashed : Failure
    data class Engine(val output: String) : Failure
    data object SessionMissing : Failure
    data object DisplayUnavailable : Failure
    data class Display(val output: String) : Failure
    data object NotInstalled : Failure
    data class Server(val status: Int, val code: String?) : Failure
}

class FailureException(val failure: Failure, override val message: String? = null) : IOException(message ?: failure.toString())

object Failures {
    fun fromStatus(status: Int, code: String?): Failure = when (status) {
        0 -> Failure.NoConnection
        402 -> Failure.NeedSubscription
        403 -> Failure.NoAccess
        404 -> when (code) {
            "purchase_not_found" -> Failure.PurchaseNotFound
            "image_not_found" -> Failure.ImageNotFound
            else -> Failure.Server(status, code)
        }
        409 -> if (code == "purchase_bound_to_other_device") Failure.PurchaseOnOtherDevice else Failure.Server(status, code)
        503 -> if (code == "store_unavailable") Failure.BillingUnavailable else Failure.ImageUnavailable
        else -> Failure.Server(status, code)
    }

    fun fromThrowable(error: Throwable): Failure = when (error) {
        is FailureException -> error.failure
        is SocketTimeoutException,
        is InterruptedIOException,
        is UnknownHostException,
        is ConnectException,
        is NoRouteToHostException,
        is SSLException,
        is SocketException -> Failure.NoConnection
        else -> if (error is IOException && error.cause != null) fromThrowable(error.cause!!) else Failure.NoConnection
    }
}
