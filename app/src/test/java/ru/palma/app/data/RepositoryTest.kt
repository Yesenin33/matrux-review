package ru.palma.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.palma.app.TestSigner
import ru.palma.app.imageJson
import ru.palma.app.manifestBody

class RepositoryTest {
    private val server = MockWebServer()
    private val signer = TestSigner()
    private val local = InMemoryLocalState()
    private val secrets = InMemorySecretStore()
    private val body = manifestBody(imageJson("alpine", 10, "a".repeat(64), distro = "alpine"))
    private var signature: String? = signer.sign(body)
    private var pubkey = signer.publicKey
    private var registerCalls = mutableListOf<String>()
    private var statusCode = 200

    @Before fun start() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path!!.substringBefore('?')) {
                "/api/pubkey" -> MockResponse().setBody("{\"algorithm\":\"ed25519\",\"public_key\":\"$pubkey\"}")
                "/api/manifest" -> if (request.getHeader("If-None-Match") == "\"e1\"") {
                    MockResponse().setResponseCode(304)
                } else {
                    MockResponse().setBody(String(body)).setHeader("ETag", "\"e1\"").apply {
                        signature?.let { setHeader("X-Palma-Signature", it) }
                    }
                }
                "/api/devices/register" -> {
                    val id = org.json.JSONObject(request.body.readUtf8()).getString("device_id")
                    registerCalls += id
                    if (registerCalls.size == 1 && id == "taken") {
                        MockResponse().setResponseCode(409).setBody("{\"error\":\"device_exists\"}")
                    } else {
                        MockResponse().setResponseCode(201)
                            .setBody("{\"device_id\":\"$id\",\"device_key\":\"key-$id\",\"tier\":\"free\",\"expires_at\":null}")
                    }
                }
                "/api/sub/status" -> if (statusCode == 200) {
                    MockResponse().setBody("{\"tier\":\"lifetime\",\"expires_at\":null,\"checked_at\":\"2026-11-08T10:00:00+00:00\"}")
                } else {
                    MockResponse().setResponseCode(statusCode).setBody("{\"error\":\"forbidden\"}")
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    @After fun stop() {
        runCatching { server.shutdown() }
    }

    private fun repository(pinned: String? = null) =
        PalmaRepository(PalmaApi(server.url("/").toString()), local, secrets, pinned, "test")

    @Test
    fun verifiedCatalogIsCachedWithSignature() = runTest {
        val catalog = repository().catalog()
        assertEquals("alpine", catalog.manifest.images.single().id)
        assertEquals(false, catalog.offline)
        assertEquals(signer.publicKey, local.signingKey)
        assertArrayEquals(body, local.manifestCache!!.first)
        assertEquals("\"e1\"", local.manifestEtag)
    }

    @Test
    fun badSignatureStopsAndCachesNothing() = runTest {
        signature = TestSigner().sign(body)
        val error = runCatching { repository().catalog() }.exceptionOrNull() as FailureException
        assertEquals(Failure.BadSignature, error.failure)
        assertNull(local.manifestCache)
    }

    @Test
    fun missingSignatureStops() = runTest {
        signature = null
        val error = runCatching { repository().catalog() }.exceptionOrNull() as FailureException
        assertEquals(Failure.BadSignature, error.failure)
    }

    @Test
    fun pinnedKeyMismatchStops() = runTest {
        val error = runCatching { repository(pinned = TestSigner().publicKey).catalog() }.exceptionOrNull() as FailureException
        assertEquals(Failure.UnknownSigningKey, error.failure)
    }

    @Test
    fun rememberedKeyIsUsedNotServed() = runTest {
        local.signingKey = TestSigner().publicKey
        val error = runCatching { repository().catalog() }.exceptionOrNull() as FailureException
        assertEquals(Failure.BadSignature, error.failure)
    }

    @Test
    fun notModifiedUsesVerifiedCache() = runTest {
        repository().catalog()
        val again = repository().catalog()
        assertEquals("alpine", again.manifest.images.single().id)
    }

    @Test
    fun offlineFallsBackToCache() = runTest {
        repository().catalog()
        server.shutdown()
        val catalog = repository().catalog()
        assertTrue(catalog.offline)
    }

    @Test
    fun offlineWithoutCacheIsNoConnection() = runTest {
        server.shutdown()
        val error = runCatching { repository().catalog() }.exceptionOrNull() as FailureException
        assertEquals(Failure.NoConnection, error.failure)
    }

    @Test
    fun registersOnceAndKeepsKeySecret() = runTest {
        val repo = repository()
        val (id, key) = repo.ensureDevice()
        assertEquals("key-$id", key)
        assertEquals(key, secrets.read("device_key"))
        repo.ensureDevice()
        assertEquals(1, registerCalls.size)
        assertTrue(id.matches(Regex("^[A-Za-z0-9._-]{1,128}$")))
    }

    @Test
    fun lostKeyRegistersUnderNewId() = runTest {
        local.deviceId = "taken"
        val (id, _) = repository().ensureDevice()
        assertNotEquals("taken", id)
        assertEquals(listOf("taken", id), registerCalls)
    }

    @Test
    fun statusIsCached() = runTest {
        val status = repository().refreshStatus()
        assertEquals(Tier.LIFETIME, status.tier)
        assertEquals(Tier.LIFETIME, local.cachedStatus!!.tier)
    }

    @Test
    fun revokedKeyIsNoAccess() = runTest {
        if (ru.palma.app.BuildConfig.TEST_UNLOCKED) return@runTest
        statusCode = 403
        val error = runCatching { repository().refreshStatus() }.exceptionOrNull() as FailureException
        assertEquals(Failure.NoAccess, error.failure)
    }

    @Test
    fun forgetDeviceClearsKey() = runTest {
        val repo = repository()
        repo.ensureDevice()
        repo.forgetDevice()
        assertNull(secrets.read("device_key"))
        assertNull(local.deviceId)
    }
}
