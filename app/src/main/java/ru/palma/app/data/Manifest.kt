package ru.palma.app.data

import org.json.JSONException
import org.json.JSONObject

data class ImageInfo(
    val id: String,
    val name: String,
    val distro: String,
    val desktop: String,
    val sizeBytes: Long,
    val sha256: String,
    val minRamMb: Int,
    val pro: Boolean,
    val url: String,
)

data class Manifest(
    val version: Int,
    val generatedAt: String,
    val images: List<ImageInfo>,
)

class ManifestFormatException(message: String) : Exception(message)

private val SHA256 = Regex("^[0-9a-f]{64}$")
private val IMAGE_ID = Regex("^[A-Za-z0-9._-]{1,64}$")

object ManifestParser {
    fun parse(body: ByteArray): Manifest {
        val root = try {
            JSONObject(String(body, Charsets.UTF_8))
        } catch (error: JSONException) {
            throw ManifestFormatException("not_json")
        }
        try {
            val images = root.getJSONArray("images")
            val parsed = (0 until images.length()).map { image(images.getJSONObject(it)) }
            if (parsed.map { it.id }.toSet().size != parsed.size) throw ManifestFormatException("duplicate_id")
            return Manifest(
                version = root.getInt("version"),
                generatedAt = root.getString("generated_at"),
                images = parsed,
            )
        } catch (error: JSONException) {
            throw ManifestFormatException("missing_field")
        }
    }

    private fun image(json: JSONObject): ImageInfo {
        val info = ImageInfo(
            id = json.getString("id"),
            name = json.getString("name"),
            distro = json.getString("distro"),
            desktop = json.getString("desktop"),
            sizeBytes = json.getLong("size_bytes"),
            sha256 = json.getString("sha256").lowercase(),
            minRamMb = json.getInt("min_ram_mb"),
            pro = json.getBoolean("pro"),
            url = json.getString("url"),
        )
        if (!IMAGE_ID.matches(info.id)) throw ManifestFormatException("bad_id")
        if (info.sizeBytes <= 0) throw ManifestFormatException("bad_size")
        if (!SHA256.matches(info.sha256)) throw ManifestFormatException("bad_sha256")
        if (info.url.isBlank()) throw ManifestFormatException("bad_url")
        return info
    }
}
