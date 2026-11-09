package ru.palma.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.TimeUnit

const val DEVICE_KEY_HEADER = "X-Device-Key"
private const val SIGNATURE_HEADER = "X-Palma-Signature"
private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

sealed interface ManifestFetch {
    data class Fresh(val body: ByteArray, val signature: String?, val etag: String?) : ManifestFetch
    data class NotModified(val signature: String?) : ManifestFetch
}

data class Registration(val deviceId: String, val deviceKey: String?, val status: SubscriptionStatus)

class PalmaApi(baseUrl: String, private val client: OkHttpClient = defaultClient()) {
    private val base: HttpUrl = baseUrl.trimEnd('/').toHttpUrl()

    fun resolve(pathOrUrl: String): HttpUrl =
        base.resolve(pathOrUrl) ?: throw FailureException(Failure.BrokenManifest)

    suspend fun pubkey(): String = call(get("/api/pubkey")) { response ->
        JSONObject(response.body!!.string()).getString("public_key")
    }

    suspend fun manifest(etag: String?): ManifestFetch {
        val request = Request.Builder().url(resolve("/api/manifest")).apply {
            if (etag != null) header("If-None-Match", etag)
        }.build()
        return io {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 304 -> ManifestFetch.NotModified(response.header(SIGNATURE_HEADER))
                    response.isSuccessful -> ManifestFetch.Fresh(
                        body = response.body!!.bytes(),
                        signature = response.header(SIGNATURE_HEADER),
                        etag = response.header("ETag"),
                    )
                    else -> throw failure(response)
                }
            }
        }
    }

    suspend fun register(deviceId: String, name: String?, knownKey: String?): Registration {
        val payload = JSONObject().put("device_id", deviceId).put("name", name ?: JSONObject.NULL)
        val request = Request.Builder()
            .url(resolve("/api/devices/register"))
            .post(payload.toString().toRequestBody(JSON_TYPE))
            .apply { if (knownKey != null) header(DEVICE_KEY_HEADER, knownKey) }
            .build()
        return call(request) { response ->
            val json = JSONObject(response.body!!.string())
            Registration(
                deviceId = json.getString("device_id"),
                deviceKey = json.optStringOrNull("device_key"),
                status = SubscriptionStatus.parse(json.toString()),
            )
        }
    }

    suspend fun status(deviceId: String, key: String): SubscriptionStatus {
        val url = resolve("/api/sub/status").newBuilder().addQueryParameter("device_id", deviceId).build()
        val request = Request.Builder().url(url).header(DEVICE_KEY_HEADER, key).build()
        return call(request) { SubscriptionStatus.parse(it.body!!.string()) }
    }

    suspend fun verify(deviceId: String, key: String, purchaseId: String, productId: String): VerifyResult {
        val payload = JSONObject()
            .put("purchase_id", purchaseId)
            .put("product_id", productId)
            .put("device_id", deviceId)
        val request = Request.Builder()
            .url(resolve("/api/sub/verify"))
            .header(DEVICE_KEY_HEADER, key)
            .post(payload.toString().toRequestBody(JSON_TYPE))
            .build()
        return call(request) { response ->
            val body = response.body!!.string()
            VerifyResult(PurchaseState.valueOf(JSONObject(body).getString("state")), SubscriptionStatus.parse(body))
        }
    }

    fun imageRequest(image: ImageInfo, deviceId: String?, key: String?): Request {
        val url = resolve(image.url).newBuilder().apply {
            if (image.pro && deviceId != null) addQueryParameter("device_id", deviceId)
        }.build()
        return Request.Builder().url(url).apply {
            if (image.pro && key != null) header(DEVICE_KEY_HEADER, key)
        }.build()
    }

    val http: OkHttpClient get() = client

    private fun get(path: String) = Request.Builder().url(resolve(path)).build()

    private suspend fun <T> call(request: Request, read: (Response) -> T): T = io {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw failure(response)
            read(response)
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (error: FailureException) {
            throw error
        } catch (error: org.json.JSONException) {
            throw FailureException(Failure.Server(200, "bad_json"))
        } catch (error: java.io.IOException) {
            throw FailureException(Failures.fromThrowable(error))
        }
    }

    companion object {
        fun failure(response: Response): FailureException =
            FailureException(Failures.fromStatus(response.code, errorCode(response.body?.string())))

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
