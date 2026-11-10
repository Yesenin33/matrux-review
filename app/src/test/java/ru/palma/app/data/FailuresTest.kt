package ru.palma.app.data

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class FailuresTest {
    @Test
    fun mapsRequiredCodes() {
        assertEquals(Failure.NeedSubscription, Failures.fromStatus(402, "subscription_required"))
        assertEquals(Failure.NoAccess, Failures.fromStatus(403, "forbidden"))
        assertEquals(Failure.ImageUnavailable, Failures.fromStatus(503, "file_unavailable"))
        assertEquals(Failure.ImageUnavailable, Failures.fromStatus(503, "file_corrupted"))
        assertEquals(Failure.ImageUnavailable, Failures.fromStatus(503, null))
        assertEquals(Failure.NoConnection, Failures.fromStatus(0, null))
    }

    @Test
    fun mapsSpecificServerErrors() {
        assertEquals(Failure.BillingUnavailable, Failures.fromStatus(503, "store_unavailable"))
        assertEquals(Failure.PurchaseNotFound, Failures.fromStatus(404, "purchase_not_found"))
        assertEquals(Failure.ImageNotFound, Failures.fromStatus(404, "image_not_found"))
        assertEquals(Failure.PurchaseOnOtherDevice, Failures.fromStatus(409, "purchase_bound_to_other_device"))
        assertEquals(Failure.Server(409, "device_exists"), Failures.fromStatus(409, "device_exists"))
        assertEquals(Failure.PurchaseOnOtherAccount, Failures.fromStatus(409, "purchase_bound_to_other_account"))
        assertEquals(Failure.SignedOut, Failures.fromStatus(401, "unauthorized"))
        assertEquals(Failure.SignInRejected, Failures.fromStatus(401, "vkid_rejected"))
        assertEquals(Failure.SignInUnavailable, Failures.fromStatus(503, "vkid_unavailable"))
        assertEquals(Failure.Server(500, "internal_error"), Failures.fromStatus(500, "internal_error"))
    }

    @Test
    fun mapsNetworkExceptionsToNoConnection() {
        listOf(
            SocketTimeoutException(),
            UnknownHostException(),
            ConnectException(),
            IOException("reset", ConnectException()),
            IOException("unexpected end of stream"),
        ).forEach { assertEquals(it.toString(), Failure.NoConnection, Failures.fromThrowable(it)) }
    }

    @Test
    fun keepsWrappedFailure() {
        assertEquals(Failure.NoAccess, Failures.fromThrowable(FailureException(Failure.NoAccess)))
    }

    @Test
    fun errorBodyIsRead() {
        assertEquals("subscription_required", errorCode("{\"error\":\"subscription_required\"}"))
        assertEquals(null, errorCode("<html>"))
        assertEquals(null, errorCode(null))
    }

    @Test
    fun realHttpCodesFromServer() = runTest {
        val server = MockWebServer()
        server.start()
        val api = PalmaApi(server.url("/").toString())
        val cases = listOf(
            402 to Failure.NeedSubscription,
            403 to Failure.NoAccess,
            503 to Failure.ImageUnavailable,
        )
        for ((code, expected) in cases) {
            server.enqueue(MockResponse().setResponseCode(code).setBody("{\"error\":\"x\"}"))
            val failure = runCatching { api.status("d", "k") }.exceptionOrNull() as FailureException
            assertEquals(expected, failure.failure)
        }
        server.shutdown()
    }

    @Test
    fun timeoutBecomesNoConnection() = runTest {
        val server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val client = OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
        val api = PalmaApi(server.url("/").toString(), client)
        val failure = runCatching { api.pubkey() }.exceptionOrNull() as FailureException
        assertEquals(Failure.NoConnection, failure.failure)
        server.shutdown()
    }

    @Test
    fun refusedConnectionBecomesNoConnection() = runTest {
        val server = MockWebServer()
        server.start()
        val url = server.url("/").toString()
        server.shutdown()
        val failure = runCatching { PalmaApi(url).pubkey() }.exceptionOrNull() as FailureException
        assertEquals(Failure.NoConnection, failure.failure)
    }
}
