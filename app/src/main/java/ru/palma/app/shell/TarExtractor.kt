package ru.palma.app.shell

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

class ArchiveException(message: String) : IOException(message)

data class TarEntry(
    val path: String,
    val type: Char,
    val mode: Int,
    val size: Long,
    val linkTarget: String,
)

class TarExtractor(private val root: File, private val checkActive: () -> Unit = {}) {
    private val rootPath = root.canonicalFile.toPath()

    fun extract(input: InputStream): Int {
        root.mkdirs()
        var entries = 0
        var longName: String? = null
        var longLink: String? = null
        var pax: Map<String, String> = emptyMap()
        val header = ByteArray(BLOCK)
        while (true) {
            checkActive()
            val read = input.readNBytesCompat(header)
            if (read == 0) break
            if (read < BLOCK) throw ArchiveException("truncated_header")
            if (header.all { it.toInt() == 0 }) break
            verifyChecksum(header)
            val entry = parseHeader(header)
            when (entry.type) {
                'L' -> { longName = readString(input, entry.size); continue }
                'K' -> { longLink = readString(input, entry.size); continue }
                'x' -> { pax = parsePax(readString(input, entry.size)); continue }
                'g' -> { skip(input, padded(entry.size)); continue }
            }
            val resolved = entry.copy(
                path = pax["path"] ?: longName ?: entry.path,
                linkTarget = pax["linkpath"] ?: longLink ?: entry.linkTarget,
                size = pax["size"]?.toLongOrNull() ?: entry.size,
            )
            longName = null
            longLink = null
            pax = emptyMap()
            write(resolved, input)
            entries++
        }
        return entries
    }

    private fun write(entry: TarEntry, input: InputStream) {
        val target = safeTarget(entry.path)
        when (entry.type) {
            '0', '\u0000', '7' -> {
                target.parentFile?.mkdirs()
                deleteExisting(target)
                target.outputStream().use { out -> copyExact(input, out, entry.size) }
                skip(input, padded(entry.size) - entry.size)
                applyMode(target, entry.mode)
            }
            '5' -> {
                target.mkdirs()
                applyMode(target, entry.mode or 0b111_000_000)
                skip(input, padded(entry.size))
            }
            '2' -> {
                target.parentFile?.mkdirs()
                deleteExisting(target)
                Files.createSymbolicLink(target.toPath(), Paths.get(entry.linkTarget))
                skip(input, padded(entry.size))
            }
            '1' -> {
                val source = safeTarget(entry.linkTarget)
                target.parentFile?.mkdirs()
                deleteExisting(target)
                val linked = runCatching { Files.createLink(target.toPath(), source.toPath()) }.isSuccess
                if (!linked && source.exists()) source.copyTo(target, overwrite = true)
                skip(input, padded(entry.size))
            }
            else -> skip(input, padded(entry.size))
        }
    }

    fun safeTarget(path: String): File {
        val clean = path.trimStart('/').removePrefix("./")
        if (clean.isEmpty()) return root
        val resolved = rootPath.resolve(clean).normalize()
        if (!resolved.startsWith(rootPath)) throw ArchiveException("path_escape")
        return resolved.toFile()
    }

    private fun deleteExisting(target: File) {
        val path = target.toPath()
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(path)
        }
    }

    private fun applyMode(file: File, mode: Int) {
        val permissions = mutableSetOf<PosixFilePermission>()
        PERMISSION_BITS.forEach { (bit, permission) -> if (mode and bit != 0) permissions += permission }
        val applied = runCatching { Files.setPosixFilePermissions(file.toPath(), permissions) }.isSuccess
        if (!applied) {
            file.setReadable(true, mode and 0b000_100_100 == 0)
            file.setWritable(true, true)
            if (mode and 0b001_001_001 != 0) file.setExecutable(true, mode and 0b000_001_001 == 0)
        }
    }

    private fun copyExact(input: InputStream, out: java.io.OutputStream, size: Long) {
        val buffer = ByteArray(COPY_BUFFER)
        var remaining = size
        while (remaining > 0) {
            checkActive()
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException("truncated_entry")
            out.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun readString(input: InputStream, size: Long): String {
        if (size > MAX_META) throw ArchiveException("meta_too_large")
        val bytes = ByteArray(size.toInt())
        if (input.readNBytesCompat(bytes) != bytes.size) throw EOFException("truncated_meta")
        skip(input, padded(size) - size)
        return String(bytes, Charsets.UTF_8).trimEnd('\u0000')
    }

    private fun skip(input: InputStream, count: Long) {
        var remaining = count
        val buffer = ByteArray(BLOCK)
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(remaining, BLOCK.toLong()).toInt())
            if (read < 0) throw EOFException("truncated_padding")
            remaining -= read
        }
    }

    companion object {
        const val BLOCK = 512
        private const val COPY_BUFFER = 64 * 1024
        private const val MAX_META = 1L shl 20

        private val PERMISSION_BITS = listOf(
            0b100_000_000 to PosixFilePermission.OWNER_READ,
            0b010_000_000 to PosixFilePermission.OWNER_WRITE,
            0b001_000_000 to PosixFilePermission.OWNER_EXECUTE,
            0b000_100_000 to PosixFilePermission.GROUP_READ,
            0b000_010_000 to PosixFilePermission.GROUP_WRITE,
            0b000_001_000 to PosixFilePermission.GROUP_EXECUTE,
            0b000_000_100 to PosixFilePermission.OTHERS_READ,
            0b000_000_010 to PosixFilePermission.OTHERS_WRITE,
            0b000_000_001 to PosixFilePermission.OTHERS_EXECUTE,
        )

        fun padded(size: Long): Long = (size + BLOCK - 1) / BLOCK * BLOCK

        fun parseHeader(header: ByteArray): TarEntry {
            val name = field(header, 0, 100)
            val prefix = if (field(header, 257, 6).startsWith("ustar")) field(header, 345, 155) else ""
            return TarEntry(
                path = if (prefix.isNotEmpty()) "$prefix/$name" else name,
                type = header[156].toInt().toChar(),
                mode = octal(header, 100, 8).toInt(),
                size = number(header, 124, 12),
                linkTarget = field(header, 157, 100),
            )
        }

        fun verifyChecksum(header: ByteArray) {
            val stored = octal(header, 148, 8)
            var sum = 0L
            for (i in header.indices) sum += if (i in 148 until 156) 32 else header[i].toInt() and 0xFF
            if (sum != stored) throw ArchiveException("bad_checksum")
        }

        fun parsePax(text: String): Map<String, String> {
            val values = mutableMapOf<String, String>()
            text.lineSequence().filter { it.isNotBlank() }.forEach { line ->
                val record = line.substringAfter(' ', "")
                val key = record.substringBefore('=', "")
                if (key.isNotEmpty()) values[key] = record.substringAfter('=')
            }
            return values
        }

        private fun field(header: ByteArray, offset: Int, length: Int): String {
            var end = offset
            while (end < offset + length && header[end].toInt() != 0) end++
            return String(header, offset, end - offset, Charsets.UTF_8)
        }

        private fun octal(header: ByteArray, offset: Int, length: Int): Long {
            val text = field(header, offset, length).trim().trim('\u0000')
            return if (text.isEmpty()) 0 else text.toLongOrNull(8) ?: throw ArchiveException("bad_number")
        }

        private fun number(header: ByteArray, offset: Int, length: Int): Long {
            if (header[offset].toInt() and 0x80 != 0) {
                var value = 0L
                for (i in offset + 1 until offset + length) value = (value shl 8) or (header[i].toLong() and 0xFF)
                return value
            }
            return octal(header, offset, length)
        }
    }
}
