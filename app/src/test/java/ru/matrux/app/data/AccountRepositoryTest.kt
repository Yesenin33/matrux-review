package ru.matrux.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.matrux.app.account.AccountInfo
import ru.matrux.app.account.AuthCode
import ru.matrux.app.account.AuthRequest
import ru.matrux.app.billing.PurchaseReceipt
import java.time.Duration
import java.time.Instant

class AccountRepositoryTest {
    private val server = MockWebServer()
    private val local = InMemoryLocalState()
    private val secrets = InMemorySecretStore()
    private val calls = mutableListOf<String>()
    private val bodies = mutableMapOf<String, JSONObject>()
    private var accountCode = 200
    private var logoutCode = 200
    private var receipts = listOf<PurchaseReceipt>()
    private var storeCalls = 0
    private var now = Instant.parse("2026-11-10T10:00:00Z")

    private val free = "\"tier\":\"free\",\"expires_at\":null,\"checked_at\":\"2026-11-10T10:00:00Z\""
    private val pro = "\"tier\":\"pro\",\"expires_at\":\"2026-12-10T10:00:00Z\",\"checked_at\":\"2026-11-10T10:00:00Z\""
    private val account = "\"account\":{\"id\":\"12\",\"phone\":\"+7 ··· 45-67\"}"

    @Before fun start() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!.substringBefore('?')
                calls += path
                request.body.readUtf8().takeIf { it.isNotBlank() }?.let { bodies[path] = JSONObject(it) }
                return when (path) {
                    "/api/devices/register" -> MockResponse().setResponseCode(201)
                        .setBody("{\"device_id\":\"a-1\",\"device_key\":\"dk\",$free}")
                    "/api/sub/status" -> MockResponse().setBody("{$free}")
                    "/api/sub/verify" -> MockResponse().setBody("{\"state\":\"ACTIVE\",$pro}")
                    "/api/account/vkid" -> MockResponse().setBody("{\"token\":\"tok\",$account,$pro}")
                    "/api/account" -> if (accountCode == 200) {
                        MockResponse().setBody("{$account,$pro}")
                    } else {
                        MockResponse().setResponseCode(accountCode).setBody("{\"error\":\"unauthorized\"}")
                    }
                    "/api/account/purchases" -> MockResponse().setBody("{\"state\":\"ACTIVE\",$account,$pro}")
                    "/api/account/logout" -> MockResponse().setResponseCode(logoutCode).setBody("{\"ok\":true}")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }

    @After fun stop() {
        runCatching { server.shutdown() }
    }

    private fun repository() = MatruxRepository(
        api = MatruxApi(server.url("/").toString(), pinnedKey = ""),
        local = local,
        secrets = secrets,
        pinnedKey = null,
        deviceName = "test",
        storePurchases = { storeCalls++; receipts },
        unlocked = false,
        clock = { now },
    )

    private suspend fun signedIn(): MatruxRepository = repository().also {
        it.signIn(AuthCode("code", "vkdev"), AuthRequest("v".repeat(43), "ch", "st"))
    }

    @Test
    fun signInKeepsTokenSecretAndLinksDevice() = runTest {
        val repo = signedIn()
        assertEquals("tok", secrets.read("account_token"))
        assertEquals(AccountInfo("12", "+7 ··· 45-67"), repo.account)
        assertEquals(Tier.PRO, local.cachedStatus!!.tier)
        assertEquals("a-1", bodies["/api/account/vkid"]!!.getString("device_id"))
    }

    @Test
    fun signedInStatusComesFromAccount() = runTest {
        val repo = signedIn()
        calls.clear()
        assertEquals(Tier.PRO, repo.refreshStatus().tier)
        assertTrue("/api/account" in calls)
        assertTrue("/api/sub/status" !in calls)
    }

    @Test
    fun expiredTokenFallsBackToDevice() = runTest {
        val repo = signedIn()
        accountCode = 401
        assertEquals(Tier.FREE, repo.refreshStatus().tier)
        assertNull(secrets.read("account_token"))
        assertNull(repo.account)
        assertTrue("/api/sub/status" in calls)
    }

    @Test
    fun serverErrorKeepsSession() = runTest {
        val repo = signedIn()
        accountCode = 500
        runCatching { repo.refreshStatus() }
        assertEquals("tok", secrets.read("account_token"))
    }

    @Test
    fun signedInPurchaseGoesToAccount() = runTest {
        val repo = signedIn()
        repo.verifyPurchase(PurchaseReceipt("p-1", "77", "palma_pro_month"))
        assertEquals("77", bodies["/api/account/purchases"]!!.getString("invoice_id"))
        assertTrue("/api/sub/verify" !in calls)
    }

    @Test
    fun anonymousPurchaseGoesToDeviceWithInvoice() = runTest {
        repository().verifyPurchase(PurchaseReceipt("p-1", "77", "palma_pro_month"))
        val body = bodies["/api/sub/verify"]!!
        assertEquals("77", body.getString("invoice_id"))
        assertEquals("a-1", body.getString("device_id"))
        assertTrue("/api/account/purchases" !in calls)
    }

    @Test
    fun launchResyncsStorePurchasesAtMostEverySixHours() = runTest {
        receipts = listOf(PurchaseReceipt("p-1", null, "palma_pro_month"))
        val repo = repository()
        repo.refreshStatus()
        assertEquals(1, storeCalls)
        assertEquals(1, calls.count { it == "/api/sub/verify" })
        now = now.plus(Duration.ofHours(5))
        repo.refreshStatus()
        assertEquals(1, storeCalls)
        now = now.plus(Duration.ofHours(2))
        repo.refreshStatus()
        assertEquals(2, storeCalls)
    }

    @Test
    fun brokenStoreDoesNotBreakStatus() = runTest {
        val repo = MatruxRepository(
            MatruxApi(server.url("/").toString(), pinnedKey = ""), local, secrets, null, "test",
            storePurchases = { error("sdk") }, unlocked = false, clock = { now },
        )
        assertEquals(Tier.FREE, repo.refreshStatus().tier)
        assertNull(local.storeCheckedAt)
    }

    @Test
    fun signInForcesFreshStoreCheck() = runTest {
        local.storeCheckedAt = now.toEpochMilli()
        val repo = signedIn()
        repo.refreshStatus()
        assertEquals(1, storeCalls)
    }

    @Test
    fun signOutForgetsLocallyEvenWhenServerFails() = runTest {
        val repo = signedIn()
        logoutCode = 500
        repo.signOut()
        assertNull(secrets.read("account_token"))
        assertNull(repo.account)
        assertEquals("a-1", bodies["/api/account/logout"]!!.getString("device_id"))
    }

    @Test
    fun unlockedBuildSkipsNetwork() = runTest {
        val repo = MatruxRepository(MatruxApi(server.url("/").toString(), pinnedKey = ""), local, secrets, null, "test", unlocked = true)
        assertEquals(Tier.PRO, repo.refreshStatus().tier)
        assertTrue(calls.isEmpty())
    }
}
