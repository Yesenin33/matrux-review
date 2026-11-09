package ru.palma.app.data

import okhttp3.Request
import ru.palma.app.BuildConfig
import java.time.Instant
import java.util.UUID

data class Catalog(val manifest: Manifest, val offline: Boolean)

class PalmaRepository(
    private val api: PalmaApi,
    private val local: LocalState,
    private val secrets: SecretStore,
    private val pinnedKey: String?,
    private val deviceName: String?,
) {
    private val keyName = "device_key"

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
        if (BuildConfig.TEST_UNLOCKED) {
            val status = SubscriptionStatus(Tier.LIFETIME, null, Instant.now())
            local.cachedStatus = status
            return status
        }
        val (id, key) = ensureDevice()
        val status = api.status(id, key)
        local.cachedStatus = status
        return status
    }

    suspend fun verifyPurchase(purchaseId: String, productId: String): VerifyResult {
        val (id, key) = ensureDevice()
        val result = api.verify(id, key, purchaseId, productId)
        local.cachedStatus = result.status
        return result
    }

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
}
