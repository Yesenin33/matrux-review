package ru.matrux.app.data

import android.content.Context
import org.json.JSONObject
import ru.matrux.app.account.AccountInfo
import java.io.File

data class InstalledSystem(
    val imageId: String,
    val name: String,
    val distro: String,
    val desktop: String,
    val sizeBytes: Long,
    val sha256: String,
    val lastLaunchMillis: Long?,
)

interface LocalState {
    var deviceId: String?
    var signingKey: String?
    var manifestEtag: String?
    var pendingInstall: String?
    var cachedStatus: SubscriptionStatus?
    var installed: InstalledSystem?
    var bytesPerSecond: Long?
    var manifestCache: Pair<ByteArray, String>?
    var account: AccountInfo?
    var storeCheckedAt: Long?
    var onboardVersion: Int
    var theme: String?
}

class InMemoryLocalState : LocalState {
    override var deviceId: String? = null
    override var signingKey: String? = null
    override var manifestEtag: String? = null
    override var pendingInstall: String? = null
    override var cachedStatus: SubscriptionStatus? = null
    override var installed: InstalledSystem? = null
    override var bytesPerSecond: Long? = null
    override var manifestCache: Pair<ByteArray, String>? = null
    override var account: AccountInfo? = null
    override var storeCheckedAt: Long? = null
    override var onboardVersion: Int = 0
    override var theme: String? = null
}

class PrefsLocalState(context: Context) : LocalState {
    private val prefs = context.getSharedPreferences("matrux_state", Context.MODE_PRIVATE)
    private val manifestBody = File(context.filesDir, "manifest.json")

    override var deviceId by stringPref("device_id")
    override var signingKey by stringPref("signing_key")
    override var manifestEtag by stringPref("manifest_etag")
    override var pendingInstall by stringPref("pending_install")

    override var storeCheckedAt: Long?
        get() = prefs.getLong("store_checked_at", 0L).takeIf { it > 0 }
        set(value) = prefs.edit().putLong("store_checked_at", value ?: 0L).apply()

    override var onboardVersion: Int
        get() = prefs.getInt("onboard_version", 0)
        set(value) = prefs.edit().putInt("onboard_version", value).apply()

    override var theme: String?
        get() = prefs.getString("theme", null)
        set(value) = prefs.edit().putString("theme", value).apply()

    override var account: AccountInfo?
        get() = prefs.getString("account", null)?.let { raw ->
            runCatching {
                val json = JSONObject(raw)
                AccountInfo(json.getString("id"), json.optStringOrNull("phone"))
            }.getOrNull()
        }
        set(value) {
            val json = value?.let { JSONObject().put("id", it.id).put("phone", it.phone ?: JSONObject.NULL).toString() }
            prefs.edit().putString("account", json).apply()
        }

    override var bytesPerSecond: Long?
        get() = prefs.getLong("bytes_per_second", 0L).takeIf { it > 0 }
        set(value) = prefs.edit().putLong("bytes_per_second", value ?: 0L).apply()

    override var cachedStatus: SubscriptionStatus?
        get() = prefs.getString("status", null)?.let { runCatching { SubscriptionStatus.parse(it) }.getOrNull() }
        set(value) {
            val json = value?.let {
                JSONObject()
                    .put("tier", it.tier.name.lowercase())
                    .put("expires_at", it.expiresAt?.toString() ?: JSONObject.NULL)
                    .put("checked_at", it.checkedAt?.toString() ?: JSONObject.NULL)
                    .toString()
            }
            prefs.edit().putString("status", json).apply()
        }

    override var installed: InstalledSystem?
        get() = prefs.getString("installed", null)?.let { raw ->
            runCatching {
                val json = JSONObject(raw)
                InstalledSystem(
                    imageId = json.getString("image_id"),
                    name = json.getString("name"),
                    distro = json.getString("distro"),
                    desktop = json.getString("desktop"),
                    sizeBytes = json.getLong("size_bytes"),
                    sha256 = json.getString("sha256"),
                    lastLaunchMillis = json.optLong("last_launch_ms", 0L).takeIf { it > 0 },
                )
            }.getOrNull()
        }
        set(value) {
            val json = value?.let {
                JSONObject()
                    .put("image_id", it.imageId)
                    .put("name", it.name)
                    .put("distro", it.distro)
                    .put("desktop", it.desktop)
                    .put("size_bytes", it.sizeBytes)
                    .put("sha256", it.sha256)
                    .put("last_launch_ms", it.lastLaunchMillis ?: 0L)
                    .toString()
            }
            prefs.edit().putString("installed", json).apply()
        }

    override var manifestCache: Pair<ByteArray, String>?
        get() {
            val signature = prefs.getString("manifest_signature", null) ?: return null
            if (!manifestBody.exists()) return null
            return manifestBody.readBytes() to signature
        }
        set(value) {
            if (value == null) {
                manifestBody.delete()
                prefs.edit().remove("manifest_signature").apply()
            } else {
                manifestBody.writeBytes(value.first)
                prefs.edit().putString("manifest_signature", value.second).apply()
            }
        }

    private fun stringPref(name: String) = object : kotlin.properties.ReadWriteProperty<Any, String?> {
        override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>) = prefs.getString(name, null)
        override fun setValue(thisRef: Any, property: kotlin.reflect.KProperty<*>, value: String?) {
            prefs.edit().putString(name, value).apply()
        }
    }
}
