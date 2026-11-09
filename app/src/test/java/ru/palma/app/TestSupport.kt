package ru.palma.app

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class TestSigner {
    private val pair = Ed25519KeyPairGenerator().apply { init(Ed25519KeyGenerationParameters(SecureRandom())) }.generateKeyPair()
    val publicKey: String = Base64.getEncoder().encodeToString((pair.public as Ed25519PublicKeyParameters).encoded)

    fun sign(body: ByteArray): String {
        val signer = Ed25519Signer()
        signer.init(true, pair.private as Ed25519PrivateKeyParameters)
        signer.update(body, 0, body.size)
        return Base64.getEncoder().encodeToString(signer.generateSignature())
    }
}

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

fun imageJson(
    id: String,
    size: Long,
    sha: String,
    pro: Boolean = false,
    distro: String = "debian",
    desktop: String = "xfce",
    name: String = id,
): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("distro", distro)
    .put("desktop", desktop)
    .put("size_bytes", size)
    .put("sha256", sha)
    .put("min_ram_mb", 2048)
    .put("pro", pro)
    .put("url", "/api/images/$id")

fun manifestBody(vararg images: JSONObject): ByteArray = JSONObject()
    .put("version", 1)
    .put("generated_at", "2026-11-08T10:00:00+00:00")
    .put("images", JSONArray(images.toList()))
    .toString()
    .toByteArray()

class TarBuilder {
    private val out = ByteArrayOutputStream()

    fun file(path: String, content: ByteArray, mode: Int = 0b110_100_100): TarBuilder = entry(path, '0', content, mode)
    fun dir(path: String): TarBuilder = entry(path, '5', ByteArray(0), 0b111_101_101)
    fun symlink(path: String, target: String): TarBuilder = entry(path, '2', ByteArray(0), 0b111_111_111, target)
    fun hardlink(path: String, target: String): TarBuilder = entry(path, '1', ByteArray(0), 0b110_100_100, target)

    fun longName(path: String, content: ByteArray): TarBuilder {
        val name = (path + "\u0000").toByteArray()
        entry("././@LongLink", 'L', name, 0)
        return entry(path.takeLast(99), '0', content, 0b110_100_100)
    }

    fun pax(path: String, content: ByteArray): TarBuilder {
        val record = "path=$path\n"
        var length = record.length + 3
        while ("$length $record".toByteArray().size != length) length = "$length $record".toByteArray().size
        entry("PaxHeader", 'x', "$length $record".toByteArray(), 0)
        return entry("short", '0', content, 0b110_100_100)
    }

    fun raw(header: ByteArray): TarBuilder {
        out.write(header)
        return this
    }

    fun entry(path: String, type: Char, content: ByteArray, mode: Int, link: String = ""): TarBuilder {
        out.write(header(path, type, content.size.toLong(), mode, link))
        out.write(content)
        val pad = (512 - content.size % 512) % 512
        out.write(ByteArray(pad))
        return this
    }

    fun build(): ByteArray {
        out.write(ByteArray(1024))
        return out.toByteArray()
    }

    companion object {
        fun header(path: String, type: Char, size: Long, mode: Int, link: String): ByteArray {
            val h = ByteArray(512)
            fun put(offset: Int, value: String) = value.toByteArray().copyInto(h, offset)
            put(0, path.take(100))
            put(100, "%07o".format(mode) + "\u0000")
            put(108, "0000000\u0000")
            put(116, "0000000\u0000")
            put(124, "%011o".format(size) + "\u0000")
            put(136, "00000000000\u0000")
            put(148, "        ")
            h[156] = type.code.toByte()
            put(157, link.take(100))
            put(257, "ustar\u0000")
            put(263, "00")
            val sum = h.sumOf { it.toInt() and 0xFF }
            put(148, "%06o".format(sum) + "\u0000 ")
            return h
        }
    }
}
