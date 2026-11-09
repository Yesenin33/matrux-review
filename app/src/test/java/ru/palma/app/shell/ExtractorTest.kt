package ru.palma.app.shell

import com.github.luben.zstd.ZstdOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import ru.palma.app.TarBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

class ExtractorTest {
    @get:Rule val temp = TemporaryFolder()

    private fun sample() = TarBuilder()
        .dir("usr/")
        .dir("usr/bin/")
        .file("usr/bin/hello", "#!/bin/sh\necho hi\n".toByteArray(), 0b111_101_101)
        .file("etc/os-release", "ID=debian\n".toByteArray())
        .symlink("bin", "usr/bin")
        .hardlink("usr/bin/hello2", "usr/bin/hello")
        .build()

    private fun zstd(bytes: ByteArray) = ByteArrayOutputStream().also { out -> ZstdOutputStream(out).use { it.write(bytes) } }.toByteArray()
    private fun xz(bytes: ByteArray) = ByteArrayOutputStream().also { out -> XZOutputStream(out, LZMA2Options()).use { it.write(bytes) } }.toByteArray()
    private fun gzip(bytes: ByteArray) = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()

    private fun extract(archive: ByteArray): java.io.File {
        val root = temp.newFolder("rootfs")
        Decompressors.open(ByteArrayInputStream(archive)).use { TarExtractor(root).extract(it) }
        return root
    }

    private fun assertSample(root: java.io.File) {
        assertEquals("ID=debian\n", root.resolve("etc/os-release").readText())
        val hello = root.resolve("usr/bin/hello")
        assertTrue(hello.canExecute())
        assertTrue(Files.isSymbolicLink(root.resolve("bin").toPath()))
        assertEquals("usr/bin", Files.readSymbolicLink(root.resolve("bin").toPath()).toString())
        assertArrayEquals(hello.readBytes(), root.resolve("usr/bin/hello2").readBytes())
    }

    @Test
    fun detectsFormats() {
        val tar = sample()
        assertEquals(Compression.ZSTD, Decompressors.detect(zstd(tar).copyOf(6)))
        assertEquals(Compression.XZ, Decompressors.detect(xz(tar).copyOf(6)))
        assertEquals(Compression.GZIP, Decompressors.detect(gzip(tar).copyOf(6)))
        assertEquals(Compression.NONE, Decompressors.detect(tar.copyOf(6)))
    }

    @Test fun extractsTarZst() = assertSample(extract(zstd(sample())))
    @Test fun extractsTarXz() = assertSample(extract(xz(sample())))
    @Test fun extractsTarGz() = assertSample(extract(gzip(sample())))
    @Test fun extractsPlainTar() = assertSample(extract(sample()))

    @Test
    fun longGnuNames() {
        val name = "usr/share/" + "deep/".repeat(30) + "file.txt"
        val root = extract(TarBuilder().longName(name, "x".toByteArray()).build())
        assertEquals("x", root.resolve(name).readText())
    }

    @Test
    fun paxPath() {
        val name = "opt/" + "p".repeat(150) + "/file"
        val root = extract(TarBuilder().pax(name, "y".toByteArray()).build())
        assertEquals("y", root.resolve(name).readText())
        assertFalse(root.resolve("short").exists())
    }

    @Test
    fun rejectsPathEscape() {
        val archive = TarBuilder().file("../../evil", "x".toByteArray()).build()
        val error = runCatching { extract(archive) }.exceptionOrNull()
        assertTrue(error is ArchiveException)
        assertFalse(temp.root.resolve("evil").exists())
    }

    @Test
    fun absolutePathsStayInside() {
        val root = extract(TarBuilder().file("/etc/hostname", "palma".toByteArray()).build())
        assertEquals("palma", root.resolve("etc/hostname").readText())
    }

    @Test
    fun brokenChecksumIsRejected() {
        val header = TarBuilder.header("a", '0', 0, 0b110_100_100, "")
        header[0] = 'b'.code.toByte()
        val error = runCatching { extract(TarBuilder().raw(header).build()) }.exceptionOrNull()
        assertTrue(error is ArchiveException)
    }

    @Test
    fun truncatedArchiveFails() {
        val tar = TarBuilder().file("big", ByteArray(5000)).build().copyOf(2048)
        val error = runCatching { extract(tar) }.exceptionOrNull()
        assertTrue(error is java.io.IOException)
    }

    @Test
    fun countingStreamReportsCompressedBytes() {
        val archive = zstd(sample())
        var last = 0L
        val counting = CountingInputStream(ByteArrayInputStream(archive)) { last = it }
        Decompressors.open(counting).use { TarExtractor(temp.newFolder("r")).extract(it) }
        assertTrue(last > 0)
        assertTrue(last <= archive.size)
    }
}
