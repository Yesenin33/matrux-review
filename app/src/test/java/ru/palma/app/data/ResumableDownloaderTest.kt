package ru.palma.app.data

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.palma.app.sha256
import kotlin.random.Random

class ResumableDownloaderTest {
    @get:Rule val temp = TemporaryFolder()
    private val server = MockWebServer()
    private val payload = Random(7).nextBytes(300_000)
    private val sha = sha256(payload)
    private val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
    private val downloader = ResumableDownloader(client)

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    private fun target(size: Long = payload.size.toLong(), hash: String = sha) = DownloadTarget(
        request = Request.Builder().url(server.url("/api/images/debian")).build(),
        file = temp.root.resolve("debian.archive"),
        sizeBytes = size,
        sha256 = hash,
    )

    private fun partial(from: Int) = MockResponse()
        .setResponseCode(206)
        .setHeader("Content-Range", "bytes $from-${payload.size - 1}/${payload.size}")
        .setBody(Buffer().write(payload, from, payload.size - from))

    @Test
    fun downloadsWholeFileAndChecksHash() = runTest {
        server.enqueue(MockResponse().setBody(Buffer().write(payload)))
        val progress = mutableListOf<Long>()
        val file = downloader.download(target()) { progress += it }
        assertArrayEquals(payload, file.readBytes())
        assertEquals(payload.size.toLong(), progress.last())
        val request = server.takeRequest()
        assertEquals("bytes=0-", request.getHeader("Range"))
        assertEquals("\"$sha\"", request.getHeader("If-Range"))
        assertFalse(downloader.partFile(target()).exists())
    }

    @Test
    fun resumesAfterDroppedConnection() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(Buffer().write(payload))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )
        val first = runCatching { downloader.download(target()) {} }
        assertTrue(first.isFailure)
        val kept = downloader.downloadedBytes(target())
        assertTrue("kept $kept bytes", kept in 1 until payload.size)

        server.enqueue(partial(kept.toInt()))
        val file = downloader.download(target()) {}
        assertArrayEquals(payload, file.readBytes())
        server.takeRequest()
        assertEquals("bytes=$kept-", server.takeRequest().getHeader("Range"))
    }

    @Test
    fun resumesFromExistingPartFile() = runTest {
        val t = target()
        downloader.partFile(t).apply { parentFile!!.mkdirs(); writeBytes(payload.copyOf(120_000)) }
        temp.root.resolve("debian.archive.part.sha256").writeText(sha)
        server.enqueue(partial(120_000))
        val progress = mutableListOf<Long>()
        val file = downloader.download(t) { progress += it }
        assertArrayEquals(payload, file.readBytes())
        assertEquals(120_000L, progress.first())
        assertEquals("bytes=120000-", server.takeRequest().getHeader("Range"))
    }

    @Test
    fun restartsWhenServerIgnoresRange() = runTest {
        val t = target()
        downloader.partFile(t).apply { parentFile!!.mkdirs(); writeBytes(payload.copyOf(50_000)) }
        temp.root.resolve("debian.archive.part.sha256").writeText(sha)
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(payload)))
        val file = downloader.download(t) {}
        assertArrayEquals(payload, file.readBytes())
    }

    @Test
    fun discardsPartOfOtherImageVersion() = runTest {
        val t = target()
        downloader.partFile(t).apply { parentFile!!.mkdirs(); writeBytes(ByteArray(10_000)) }
        temp.root.resolve("debian.archive.part.sha256").writeText("c".repeat(64))
        assertEquals(0L, downloader.downloadedBytes(t))
        server.enqueue(MockResponse().setBody(Buffer().write(payload)))
        downloader.download(t) {}
        assertEquals("bytes=0-", server.takeRequest().getHeader("Range"))
    }

    @Test
    fun rejectsMismatchedContentRange() = runTest {
        val t = target()
        downloader.partFile(t).apply { parentFile!!.mkdirs(); writeBytes(payload.copyOf(1000)) }
        temp.root.resolve("debian.archive.part.sha256").writeText(sha)
        server.enqueue(partial(2000))
        assertTrue(runCatching { downloader.download(t) {} }.isFailure)
        assertEquals(1000L, downloader.downloadedBytes(t))
    }

    @Test
    fun wrongHashDeletesFile() = runTest {
        val t = target(hash = "d".repeat(64))
        server.enqueue(MockResponse().setBody(Buffer().write(payload)))
        val error = runCatching { downloader.download(t) {} }.exceptionOrNull()
        assertEquals(Failure.ChecksumMismatch, (error as FailureException).failure)
        assertFalse(downloader.partFile(t).exists())
        assertFalse(t.file.exists())
    }

    @Test
    fun oversizedBodyIsRejected() = runTest {
        val t = target(size = 1000)
        server.enqueue(MockResponse().setBody(Buffer().write(payload)))
        val error = runCatching { downloader.download(t) {} }.exceptionOrNull()
        assertEquals(Failure.ChecksumMismatch, (error as FailureException).failure)
    }

    @Test
    fun paywallStopsDownload() = runTest {
        server.enqueue(MockResponse().setResponseCode(402).setBody("{\"error\":\"subscription_required\"}"))
        val error = runCatching { downloader.download(target()) {} }.exceptionOrNull()
        assertEquals(Failure.NeedSubscription, (error as FailureException).failure)
    }

    @Test
    fun unavailableImage() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("{\"error\":\"file_unavailable\"}"))
        val error = runCatching { downloader.download(target()) {} }.exceptionOrNull()
        assertEquals(Failure.ImageUnavailable, (error as FailureException).failure)
    }
}
