package ru.palma.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

data class DownloadTarget(
    val request: Request,
    val file: File,
    val sizeBytes: Long,
    val sha256: String,
)

private val CONTENT_RANGE = Regex("^bytes (\\d+)-(\\d+)/(\\d+|\\*)$")

class ResumableDownloader(private val client: OkHttpClient) {

    fun partFile(target: DownloadTarget) = File(target.file.path + ".part")
    private fun tagFile(target: DownloadTarget) = File(target.file.path + ".part.sha256")

    fun downloadedBytes(target: DownloadTarget): Long {
        val part = partFile(target)
        val tag = tagFile(target)
        return if (part.exists() && tag.exists() && tag.readText() == target.sha256) part.length() else 0L
    }

    suspend fun download(target: DownloadTarget, onProgress: (Long) -> Unit): File = withContext(Dispatchers.IO) {
        if (target.file.exists() && target.file.length() == target.sizeBytes) return@withContext target.file
        val part = partFile(target)
        val tag = tagFile(target)
        if (!tag.exists() || tag.readText() != target.sha256 || part.length() > target.sizeBytes) {
            part.delete()
            target.file.parentFile?.mkdirs()
            tag.writeText(target.sha256)
        }
        val digest = MessageDigest.getInstance("SHA-256")
        hashExisting(part, digest)
        var offset = part.length()
        onProgress(offset)
        if (offset < target.sizeBytes) {
            val request = target.request.newBuilder()
                .header("Range", "bytes=$offset-")
                .header("If-Range", "\"${target.sha256}\"")
                .build()
            client.newCall(request).execute().use { response ->
                val append = when (response.code) {
                    206 -> {
                        val start = CONTENT_RANGE.matchEntire(response.header("Content-Range").orEmpty())
                            ?.groupValues?.get(1)?.toLongOrNull()
                        if (start != offset) throw IOException("range_mismatch")
                        true
                    }
                    200 -> false
                    416 -> {
                        part.delete()
                        throw IOException("range_not_satisfiable")
                    }
                    else -> throw PalmaApi.failure(response)
                }
                if (!append) {
                    digest.reset()
                    offset = 0
                    onProgress(0)
                }
                val body = response.body ?: throw IOException("empty_body")
                FileOutputStream(part, append).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (offset + read > target.sizeBytes) throw FailureException(Failure.ChecksumMismatch)
                            out.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            offset += read
                            onProgress(offset)
                        }
                    }
                }
            }
        }
        if (offset != target.sizeBytes) throw IOException("incomplete")
        if (digest.digest().toHex() != target.sha256) {
            part.delete()
            tag.delete()
            throw FailureException(Failure.ChecksumMismatch)
        }
        if (!part.renameTo(target.file)) throw IOException("rename_failed")
        tag.delete()
        target.file
    }

    fun discard(target: DownloadTarget) {
        partFile(target).delete()
        tagFile(target).delete()
        target.file.delete()
    }

    private fun hashExisting(file: File, digest: MessageDigest) {
        if (!file.exists()) return
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
    }

    private companion object {
        const val BUFFER = 64 * 1024
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
