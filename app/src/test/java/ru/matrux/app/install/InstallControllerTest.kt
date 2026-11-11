package ru.matrux.app.install

import com.github.luben.zstd.ZstdOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.matrux.app.TarBuilder
import ru.matrux.app.data.Failure
import ru.matrux.app.data.InMemoryLocalState
import ru.matrux.app.data.InMemorySecretStore
import ru.matrux.app.data.ManifestParser
import ru.matrux.app.data.MatruxApi
import ru.matrux.app.data.MatruxRepository
import ru.matrux.app.data.ResumableDownloader
import ru.matrux.app.imageJson
import ru.matrux.app.manifestBody
import ru.matrux.app.sha256
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random

class InstallControllerTest {
    @get:Rule val temp = TemporaryFolder()
    private val server = MockWebServer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val local = InMemoryLocalState()
    private var metered = false
    private var free = Long.MAX_VALUE
    private var dropFirst = false
    private var imageCode = 200
    private val archive = ByteArrayOutputStream().also { out ->
        ZstdOutputStream(out).use {
            it.write(
                TarBuilder()
                    .file("etc/os-release", "ID=alpine\n".toByteArray())
                    .file("usr/bin/blob", Random(3).nextBytes(400_000))
                    .build(),
            )
        }
    }.toByteArray()

    @Before fun start() {
        server.dispatcher = object : Dispatcher() {
            var served = 0
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (!request.path!!.startsWith("/api/images/")) return MockResponse().setResponseCode(404)
                if (imageCode != 200) return MockResponse().setResponseCode(imageCode).setBody("{\"error\":\"x\"}")
                served++
                val range = request.getHeader("Range")!!.removePrefix("bytes=").removeSuffix("-").toInt()
                val response = if (range == 0) {
                    MockResponse().setBody(Buffer().write(archive))
                } else {
                    MockResponse().setResponseCode(206)
                        .setHeader("Content-Range", "bytes $range-${archive.size - 1}/${archive.size}")
                        .setBody(Buffer().write(archive, range, archive.size - range))
                }
                if (dropFirst && served == 1) response.socketPolicy = SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY
                return response
            }
        }
        server.start()
    }

    @After fun stop() {
        scope.cancel()
        server.shutdown()
    }

    private fun image(sha: String = sha256(archive), size: Long = archive.size.toLong()) =
        ManifestParser.parse(manifestBody(imageJson("alpine", size, sha, distro = "alpine", desktop = "none", name = "Alpine"))).images.single()

    private fun controller(): InstallController {
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        val repository = MatruxRepository(MatruxApi(server.url("/").toString(), client), local, InMemorySecretStore(), null, null)
        val storage = object : InstallController.Storage {
            override val downloads = File(temp.root, "downloads")
            override val systems = File(temp.root, "systems")
            override fun freeBytes() = free
            override fun metered() = metered
        }
        return InstallController(repository, local, ResumableDownloader(client), storage, scope, retryDelaysMs = listOf(10, 10, 10))
    }

    private fun InstallController.awaitEnd(): InstallState = runBlocking {
        withTimeout(20_000) { state.first { it is InstallState.Done || it is InstallState.Failed } }
    }

    @Test
    fun installsVerifiesAndUnpacks() {
        val installs = controller()
        val seen = mutableListOf<InstallState>()
        val watch = scope.launchCollect(installs, seen)
        installs.request(image())
        val end = installs.awaitEnd()
        watch.cancel()
        assertTrue(end is InstallState.Done)
        assertTrue(installs.isInstalled("alpine"))
        assertEquals("ID=alpine\n", File(temp.root, "systems/alpine/etc/os-release").readText())
        assertEquals("alpine", local.installed!!.imageId)
        assertNull(local.pendingInstall)
        assertFalse(File(temp.root, "downloads/alpine.archive").exists())
        val running = seen.filterIsInstance<InstallState.Running>()
        assertTrue(running.isNotEmpty())
        val fractions = running.map { it.fraction }
        assertEquals(fractions.sorted(), fractions)
        assertTrue(running.any { it.unpacked > 0 })
    }

    @Test
    fun resumesAfterDropWithoutUserAction() {
        dropFirst = true
        val installs = controller()
        installs.request(image())
        assertTrue(installs.awaitEnd() is InstallState.Done)
        val ranges = (1..server.requestCount).map { server.takeRequest().getHeader("Range") }
        assertEquals("bytes=0-", ranges.first())
        assertTrue(ranges.last() != "bytes=0-")
    }

    @Test
    fun wrongChecksumFailsBeforeUnpack() {
        val installs = controller()
        installs.request(image(sha = "e".repeat(64)))
        val end = installs.awaitEnd() as InstallState.Failed
        assertEquals(Failure.ChecksumMismatch, end.failure)
        assertFalse(File(temp.root, "systems/alpine").exists())
        assertNull(local.installed)
    }

    @Test
    fun paywallIsReported() {
        imageCode = 402
        val installs = controller()
        installs.request(image())
        assertEquals(Failure.NeedSubscription, (installs.awaitEnd() as InstallState.Failed).failure)
    }

    @Test
    fun unavailableIsReported() {
        imageCode = 503
        val installs = controller()
        installs.request(image())
        assertEquals(Failure.ImageUnavailable, (installs.awaitEnd() as InstallState.Failed).failure)
    }

    @Test
    fun lowSpaceStopsEarly() {
        free = 10
        val installs = controller()
        installs.request(image())
        assertEquals(Failure.NotEnoughSpace, (installs.awaitEnd() as InstallState.Failed).failure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun meteredNetworkAsksFirst() {
        metered = true
        val installs = controller()
        installs.request(image())
        assertTrue(installs.state.value is InstallState.ConfirmMetered)
        assertEquals(0, server.requestCount)
        installs.request(image(), allowMetered = true)
        assertTrue(installs.awaitEnd() is InstallState.Done)
    }

    @Test
    fun cancelRemovesPartialDownload() {
        val installs = controller()
        installs.request(image(sha = "e".repeat(64), size = archive.size.toLong()))
        installs.awaitEnd()
        installs.cancel()
        assertEquals(InstallState.Idle, installs.state.value)
        assertFalse(File(temp.root, "downloads/alpine.archive.part").exists())
        assertNull(local.pendingInstall)
    }

    private fun CoroutineScope.launchCollect(installs: InstallController, into: MutableList<InstallState>) =
        launch(Dispatchers.Unconfined) { installs.state.collect { synchronized(into) { into += it } } }
}
