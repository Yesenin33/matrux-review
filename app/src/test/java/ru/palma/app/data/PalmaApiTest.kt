package ru.palma.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.palma.app.imageJson

class PalmaApiTest {
    private val server = MockWebServer()
    private lateinit var api: PalmaApi

    @Before fun start() {
        server.start()
        api = PalmaApi(server.url("/").toString())
    }

    @After fun stop() = server.shutdown()

    @Test
    fun registerSendsDeviceIdAndReadsKey() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setBody("{\"device_id\":\"a-1\",\"device_key\":\"KEY\",\"tier\":\"free\",\"expires_at\":null}"),
        )
        val registration = api.register("a-1", "Pixel", null)
        assertEquals("KEY", registration.deviceKey)
        assertEquals(Tier.FREE, registration.status.tier)
        val request = server.takeRequest()
        assertEquals("/api/devices/register", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("a-1", body.getString("device_id"))
        assertEquals("Pixel", body.getString("name"))
        assertNull(request.getHeader(DEVICE_KEY_HEADER))
    }

    @Test
    fun statusCarriesDeviceKey() = runTest {
        server.enqueue(MockResponse().setBody("{\"tier\":\"pro\",\"expires_at\":\"2026-12-15T00:00:00+00:00\",\"checked_at\":\"2026-11-08T10:00:00+00:00\"}"))
        val status = api.status("a-1", "secret")
        assertEquals(Tier.PRO, status.tier)
        assertEquals("2026-12-15T00:00:00Z", status.expiresAt.toString())
        val request = server.takeRequest()
        assertEquals("/api/sub/status?device_id=a-1", request.path)
        assertEquals("secret", request.getHeader(DEVICE_KEY_HEADER))
    }

    @Test
    fun verifyCarriesKeyAndBody() = runTest {
        server.enqueue(MockResponse().setBody("{\"state\":\"PAUSED\",\"tier\":\"pro\",\"expires_at\":null,\"checked_at\":null}"))
        val result = api.verify("a-1", "secret", "pause-1", "palma_pro_month")
        assertEquals(PurchaseState.PAUSED, result.state)
        val request = server.takeRequest()
        assertEquals("secret", request.getHeader(DEVICE_KEY_HEADER))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("pause-1", body.getString("purchase_id"))
        assertEquals("palma_pro_month", body.getString("product_id"))
        assertEquals("a-1", body.getString("device_id"))
    }

    @Test
    fun proImageRequestCarriesDeviceAndKey() {
        val pro = ManifestParser.parse(ru.palma.app.manifestBody(imageJson("debian", 1, "a".repeat(64), pro = true))).images.single()
        val request = api.imageRequest(pro, "a-1", "secret")
        assertEquals("/api/images/debian?device_id=a-1", request.url.encodedPath + "?" + request.url.encodedQuery)
        assertEquals("secret", request.header(DEVICE_KEY_HEADER))
    }

    @Test
    fun freeImageRequestCarriesNothing() {
        val free = ManifestParser.parse(ru.palma.app.manifestBody(imageJson("alpine", 1, "a".repeat(64)))).images.single()
        val request = api.imageRequest(free, "a-1", "secret")
        assertNull(request.url.query)
        assertNull(request.header(DEVICE_KEY_HEADER))
    }

    @Test
    fun absoluteImageUrlIsKept() {
        val json = imageJson("alpine", 1, "a".repeat(64)).put("url", "https://cdn.example/x.tar.zst")
        val image = ManifestParser.parse(ru.palma.app.manifestBody(json)).images.single()
        assertEquals("https://cdn.example/x.tar.zst", api.imageRequest(image, null, null).url.toString())
    }

    @Test
    fun manifestNotModified() = runTest {
        server.enqueue(MockResponse().setResponseCode(304).setHeader("X-Palma-Signature", "sig"))
        val fetch = api.manifest("\"etag\"")
        assertEquals(ManifestFetch.NotModified("sig"), fetch)
        assertEquals("\"etag\"", server.takeRequest().getHeader("If-None-Match"))
    }
}
