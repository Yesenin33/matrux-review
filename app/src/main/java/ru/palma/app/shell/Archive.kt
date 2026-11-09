package ru.palma.app.shell

import com.github.luben.zstd.ZstdInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

enum class Compression { ZSTD, XZ, GZIP, NONE }

class CountingInputStream(input: InputStream, private val onRead: (Long) -> Unit) : FilterInputStream(input) {
    var count = 0L
        private set

    override fun read(): Int = super.read().also { if (it >= 0) advance(1) }

    override fun read(b: ByteArray, off: Int, len: Int): Int =
        super.read(b, off, len).also { if (it > 0) advance(it.toLong()) }

    override fun skip(n: Long): Long = super.skip(n).also { advance(it) }

    private fun advance(n: Long) {
        count += n
        onRead(count)
    }
}

object Decompressors {
    private val ZSTD_MAGIC = byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte())
    private val XZ_MAGIC = byteArrayOf(0xFD.toByte(), 0x37, 0x7A, 0x58, 0x5A, 0x00)
    private val GZIP_MAGIC = byteArrayOf(0x1F, 0x8B.toByte())

    fun detect(head: ByteArray): Compression = when {
        head.startsWith(ZSTD_MAGIC) -> Compression.ZSTD
        head.startsWith(XZ_MAGIC) -> Compression.XZ
        head.startsWith(GZIP_MAGIC) -> Compression.GZIP
        else -> Compression.NONE
    }

    fun open(raw: InputStream, zstd: (InputStream) -> InputStream = ::ZstdInputStream): InputStream {
        val buffered = BufferedInputStream(raw, BUFFER)
        buffered.mark(8)
        val head = ByteArray(6)
        val read = buffered.readNBytesCompat(head)
        buffered.reset()
        return when (detect(head.copyOf(read))) {
            Compression.ZSTD -> zstd(buffered)
            Compression.XZ -> XZInputStream(buffered)
            Compression.GZIP -> GZIPInputStream(buffered, BUFFER)
            Compression.NONE -> buffered
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray) =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private const val BUFFER = 64 * 1024
}

internal fun InputStream.readNBytesCompat(target: ByteArray): Int {
    var total = 0
    while (total < target.size) {
        val read = read(target, total, target.size - total)
        if (read < 0) break
        total += read
    }
    return total
}
