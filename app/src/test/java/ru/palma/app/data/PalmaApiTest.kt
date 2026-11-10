package ru.palma.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.palma.app.account.AccountInfo
import ru.palma.app.account.AuthCode
import ru.palma.app.account.AuthRequest
import ru.palma.app.billing.PurchaseReceipt
import ru.palma.app.imageJson

private const val ACCOUNT_BODY =
    "{\"token\":\"tok\",\"account\":{\"id\":\"12\",\"phone\":\"+7 ··· 45-67\"},\"tier\":\"pro\",\"expires_at\":null,\"checked_at\":null}"

class PalmaApiTest {
    private val server = MockWebServer()
    private lateinit var api: PalmaApi

    @Before fun start() {
        server.start()
        api = PalmaApi(server.url("/").toString())
    }

    @After fun stop() = server.shutdown()

    @Test
    fun plansReadPricesAndOptionalDiscount() = runTest {
        server.enqueue(
            MockResponse().setBody(
                "{\"currency\":\"RUB\",\"plans\":[{\"product_id\":\"m\",\"months\":1,\"price_rub\":349}," +
                    "{\"product_id\":\"q\",\"months\":3,\"price_rub\":890,\"discount_percent\":15}]}",
            ),
        )
        val plans = api.plans()
        assertEquals(listOf(1, 3), plans.map { it.months })
        assertEquals(listOf(349L, 890L), plans.map { it.priceRub })
        assertEquals(listOf(null, 15), plans.map { it.discountPercent })
        assertEquals("/api/plans", server.takeRequest().path)
    }

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
        val result = api.verify("a-1", "secret", PurchaseReceipt("pause-1", null, "palma_pro_month"))
        assertEquals(PurchaseState.PAUSED, result.state)
        val request = server.takeRequest()
        assertEquals("secret", request.getHeader(DEVICE_KEY_HEADER))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("pause-1", body.getString("purchase_id"))
        assertEquals("palma_pro_month", body.getString("product_id"))
        assertEquals("a-1", body.getString("device_id"))
        assertTrue(!body.has("invoice_id"))
    }

    @Test
    fun verifyCarriesAccountTokenWhenSignedIn() = runTest {
        server.enqueue(MockResponse().setBody("{\"state\":\"ACTIVE\",\"tier\":\"pro\",\"expires_at\":null,\"checked_at\":null}"))
        api.verify("a-1", "secret", PurchaseReceipt("p-1", null, "palma_pro_month"), "tok")
        val request = server.takeRequest()
        assertEquals("Bearer tok", request.getHeader("Authorization"))
        assertEquals("secret", request.getHeader(DEVICE_KEY_HEADER))
    }

    @Test
    fun verifyWithoutTokenSendsNoAuthorization() = runTest {
        server.enqueue(MockResponse().setBody("{\"state\":\"ACTIVE\",\"tier\":\"pro\",\"expires_at\":null,\"checked_at\":null}"))
        api.verify("a-1", "secret", PurchaseReceipt("p-1", null, "palma_pro_month"))
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun verifyPassesInvoiceWhenKnown() = runTest {
        server.enqueue(MockResponse().setBody("{\"state\":\"ACTIVE\",\"tier\":\"pro\",\"expires_at\":null,\"checked_at\":null}"))
        api.verify("a-1", "secret", PurchaseReceipt("p-1", "77", "palma_pro_quarter"))
        assertEquals("77", JSONObject(server.takeRequest().body.readUtf8()).getString("invoice_id"))
    }

    @Test
    fun signInSendsCodeVerifierAndDevice() = runTest {
        server.enqueue(MockResponse().setBody(ACCOUNT_BODY))
        val state = api.signIn(AuthCode("c-1", "vkdev"), AuthRequest("v".repeat(43), "ch", "st"), "a-1", "secret")
        assertEquals("tok", state.token)
        assertEquals(AccountInfo("12", "+7 ··· 45-67"), state.account)
        assertEquals(Tier.PRO, state.status.tier)
        val request = server.takeRequest()
        assertEquals("/api/account/vkid", request.path)
        assertEquals("secret", request.getHeader(DEVICE_KEY_HEADER))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("c-1", body.getString("code"))
        assertEquals("v".repeat(43), body.getString("code_verifier"))
        assertEquals("vkdev", body.getString("vk_device_id"))
        assertEquals("st", body.getString("state"))
        assertEquals("a-1", body.getString("device_id"))
        assertTrue(!body.has("challenge"))
    }

    @Test
    fun accountCallsCarryBearer() = runTest {
        server.enqueue(MockResponse().setBody(ACCOUNT_BODY))
        server.enqueue(MockResponse().setBody("{\"state\":\"ACTIVE\",\"tier\":\"pro\",\"expires_at\":null,\"checked_at\":null}"))
        server.enqueue(MockResponse().setBody("{\"ok\":true}"))
        api.account("tok")
        api.verifyAccountPurchase("tok", PurchaseReceipt("p-1", "77", "palma_pro_month"), "77")
        api.signOut("tok", "a-1", "secret")
        val me = server.takeRequest()
        assertEquals("/api/account", me.path)
        assertEquals("Bearer tok", me.getHeader("Authorization"))
        val buy = server.takeRequest()
        assertEquals("/api/account/purchases", buy.path)
        assertEquals("Bearer tok", buy.getHeader("Authorization"))
        val payload = JSONObject(buy.body.readUtf8())
        assertEquals("77", payload.getString("invoice_id"))
        assertEquals("p-1", payload.getString("purchase_id"))
        val out = server.takeRequest()
        assertEquals("/api/account/logout", out.path)
        assertEquals("secret", out.getHeader(DEVICE_KEY_HEADER))
        assertEquals("a-1", JSONObject(out.body.readUtf8()).getString("device_id"))
    }

    @Test
    fun expiredTokenIsSignedOut() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"unauthorized\"}"))
        val error = runCatching { api.account("old") }.exceptionOrNull() as FailureException
        assertEquals(Failure.SignedOut, error.failure)
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
