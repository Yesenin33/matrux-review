package ru.matrux.app.data

import okhttp3.Request
import ru.matrux.app.BuildConfig
import ru.matrux.app.account.AccountInfo
import ru.matrux.app.account.AuthCode
import ru.matrux.app.account.AuthRequest
import ru.matrux.app.billing.PurchaseReceipt
import ru.matrux.app.billing.ServerPlan
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class Catalog(val manifest: Manifest, val offline: Boolean)

class MatruxRepository(
    private val api: MatruxApi,
    private val local: LocalState,
    private val secrets: SecretStore,
    private val pinnedKey: String?,
    private val deviceName: String?,
    private val storePurchases: suspend () -> List<PurchaseReceipt> = { emptyList() },
    private val unlocked: Boolean = BuildConfig.TEST_UNLOCKED,
    private val clock: () -> Instant = Instant::now,
) {
    private val keyName = "device_key"
    private val tokenName = "account_token"

    val account: AccountInfo? get() = local.account?.takeIf { secrets.read(tokenName) != null }

    val deviceId: String? get() = local.deviceId
    val hasDeviceKey: Boolean get() = secrets.read(keyName) != null

    suspend fun ensureDevice(): Pair<String, String> {
        val id = local.deviceId
        val key = secrets.read(keyName)
        if (id != null && key != null) return id to key
        return registerFresh(id ?: newDeviceId())
    }

    private suspend fun registerFresh(id: String): Pair<String, String> {
        val registration = try {
            api.register(id, deviceName, null)
        } catch (error: FailureException) {
            val failure = error.failure
            if (failure is Failure.Server && failure.status == 409) return registerFresh(newDeviceId())
            throw error
        }
        val key = registration.deviceKey ?: throw FailureException(Failure.NoAccess)
        local.deviceId = registration.deviceId
        secrets.write(keyName, key)
        local.cachedStatus = registration.status
        return registration.deviceId to key
    }

    fun forgetDevice() {
        secrets.remove(keyName)
        local.deviceId = null
        local.cachedStatus = null
    }

    val cachedStatus: SubscriptionStatus? get() = local.cachedStatus

    suspend fun refreshStatus(): SubscriptionStatus {
        if (unlocked) {
            val status = SubscriptionStatus(Tier.LIFETIME, null, clock())
            local.cachedStatus = status
            return status
        }
        resyncStore()
        val status = accountStatus() ?: deviceStatus()
        local.cachedStatus = status
        return status
    }

    private suspend fun deviceStatus(): SubscriptionStatus {
        val (id, key) = ensureDevice()
        return api.status(id, key)
    }

    private suspend fun accountStatus(): SubscriptionStatus? {
        val token = secrets.read(tokenName) ?: return null
        return try {
            api.account(token).also { local.account = it.account }.status
        } catch (error: FailureException) {
            if (error.failure != Failure.SignedOut) throw error
            forgetAccount()
            null
        }
    }

    private suspend fun resyncStore() {
        val now = clock()
        val last = local.storeCheckedAt?.let(Instant::ofEpochMilli)
        if (last != null && Duration.between(last, now) < STORE_RECHECK && now >= last) return
        val receipts = runCatching { storePurchases() }.getOrNull() ?: return
        receipts.forEach { receipt ->
            try {
                verifyPurchase(receipt)
            } catch (error: FailureException) {
                if (error.failure == Failure.NoConnection) return
            }
        }
        local.storeCheckedAt = now.toEpochMilli()
    }

    suspend fun verifyPurchase(receipt: PurchaseReceipt): VerifyResult {
        val token = secrets.read(tokenName)
        val invoiceId = receipt.invoiceId
        val result = if (token != null && invoiceId != null) {
            api.verifyAccountPurchase(token, receipt, invoiceId)
        } else {
            val (id, key) = ensureDevice()
            api.verify(id, key, receipt, token)
        }
        local.cachedStatus = result.status
        return result
    }

    suspend fun signIn(code: AuthCode, request: AuthRequest): SubscriptionStatus {
        val (id, key) = ensureDevice()
        val state = api.signIn(code, request, id, key)
        val token = state.token ?: throw FailureException(Failure.SignInRejected)
        secrets.write(tokenName, token)
        local.account = state.account
        local.cachedStatus = state.status
        local.storeCheckedAt = null
        return state.status
    }

    suspend fun signOut() {
        val token = secrets.read(tokenName)
        forgetAccount()
        if (token == null) return
        runCatching { api.signOut(token, local.deviceId, secrets.read(keyName)) }
    }

    private fun forgetAccount() {
        secrets.remove(tokenName)
        local.account = null
    }

    suspend fun plans(): List<ServerPlan> = api.plans()

    suspend fun catalog(): Catalog {
        val key = try {
            signingKey()
        } catch (error: FailureException) {
            if (error.failure == Failure.NoConnection) return cachedCatalog() ?: throw error
            throw error
        }
        val cached = local.manifestCache
        val fetch = try {
            api.manifest(if (cached != null) local.manifestEtag else null)
        } catch (error: FailureException) {
            if (error.failure == Failure.NoConnection) return cachedCatalog() ?: throw error
            throw error
        }
        return when (fetch) {
            is ManifestFetch.NotModified -> {
                val (body, signature) = cached ?: throw FailureException(Failure.BrokenManifest)
                Catalog(verified(key, body, signature), offline = false)
            }
            is ManifestFetch.Fresh -> {
                val manifest = verified(key, fetch.body, fetch.signature)
                local.manifestCache = fetch.body to fetch.signature!!
                local.manifestEtag = fetch.etag
                Catalog(manifest, offline = false)
            }
        }
    }

    private fun cachedCatalog(): Catalog? {
        val key = pinnedKey?.takeIf { it.isNotBlank() } ?: local.signingKey ?: return null
        val (body, signature) = local.manifestCache ?: return null
        return runCatching { Catalog(verified(key, body, signature), offline = true) }.getOrNull()
    }

    private suspend fun signingKey(): String {
        val pinned = pinnedKey?.takeIf { it.isNotBlank() }
        val remembered = local.signingKey
        if (pinned == null && remembered != null) return remembered
        return when (val decision = SigningKeyPolicy.decide(api.pubkey(), pinned, remembered)) {
            is KeyDecision.Use -> decision.key.also { if (decision.remember) local.signingKey = it }
            KeyDecision.Reject -> throw FailureException(Failure.UnknownSigningKey)
        }
    }

    private fun verified(key: String, body: ByteArray, signature: String?): Manifest {
        if (ManifestSignature.check(key, signature, body) != SignatureCheck.VALID) {
            throw FailureException(Failure.BadSignature)
        }
        return try {
            ManifestParser.parse(body)
        } catch (error: ManifestFormatException) {
            throw FailureException(Failure.BrokenManifest)
        }
    }

    suspend fun imageRequest(image: ImageInfo): Request {
        if (!image.pro) return api.imageRequest(image, null, null)
        val (id, key) = ensureDevice()
        return api.imageRequest(image, id, key)
    }

    val http get() = api.http

    private fun newDeviceId() = "a-" + UUID.randomUUID().toString()

    private companion object {
        val STORE_RECHECK: Duration = Duration.ofHours(6)
    }
}
