package ru.matrux.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.matrux.app.imageJson
import ru.matrux.app.manifestBody

class ManifestTest {
    private val sha = "b".repeat(64)

    @Test
    fun parsesServerManifest() {
        val manifest = ManifestParser.parse(
            manifestBody(
                imageJson("alpine", 25_000_000, sha, distro = "alpine", desktop = "none", name = "Alpine"),
                imageJson("debian-12", 300_000_000, sha, pro = true, name = "Debian 12"),
            ),
        )
        assertEquals(1, manifest.version)
        assertEquals(2, manifest.images.size)
        val debian = manifest.images[1]
        assertEquals("Debian 12", debian.name)
        assertEquals(300_000_000L, debian.sizeBytes)
        assertTrue(debian.pro)
        assertFalse(manifest.images[0].pro)
        assertEquals("/api/images/debian-12", debian.url)
    }

    @Test
    fun parsesBackendBytesExactly() {
        val manifest = ManifestParser.parse(BackendFixture.body.toByteArray())
        assertEquals(3, manifest.version)
        assertEquals("alpine", manifest.images.single().id)
        assertEquals(2_500_000L, manifest.images.single().sizeBytes)
    }

    @Test
    fun uppercaseHashIsNormalized() {
        val manifest = ManifestParser.parse(manifestBody(imageJson("a", 1, "AB".repeat(32))))
        assertEquals("ab".repeat(32), manifest.images.single().sha256)
    }

    @Test
    fun emptyCatalogIsValid() {
        assertTrue(ManifestParser.parse(manifestBody()).images.isEmpty())
    }

    private fun rejects(body: ByteArray, reason: String) {
        val error = runCatching { ManifestParser.parse(body) }.exceptionOrNull()
        assertTrue("expected $reason, got $error", error is ManifestFormatException)
        assertEquals(reason, error!!.message)
    }

    @Test
    fun rejectsNotJson() = rejects("<html>".toByteArray(), "not_json")

    @Test
    fun rejectsMissingField() {
        val image = imageJson("a", 1, sha).apply { remove("sha256") }
        rejects(manifestBody(image), "missing_field")
    }

    @Test
    fun rejectsBadHash() = rejects(manifestBody(imageJson("a", 1, "xyz")), "bad_sha256")

    @Test
    fun rejectsZeroSize() = rejects(manifestBody(imageJson("a", 0, sha)), "bad_size")

    @Test
    fun rejectsPathLikeId() = rejects(manifestBody(imageJson("../etc", 1, sha)), "bad_id")

    @Test
    fun rejectsDuplicateIds() = rejects(manifestBody(imageJson("a", 1, sha), imageJson("a", 2, sha)), "duplicate_id")
}
