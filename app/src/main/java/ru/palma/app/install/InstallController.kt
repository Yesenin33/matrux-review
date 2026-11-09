package ru.palma.app.install

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.palma.app.data.DownloadTarget
import ru.palma.app.data.Failure
import ru.palma.app.data.FailureException
import ru.palma.app.data.Failures
import ru.palma.app.data.ImageInfo
import ru.palma.app.data.InstalledSystem
import ru.palma.app.data.LocalState
import ru.palma.app.data.PalmaRepository
import ru.palma.app.data.ResumableDownloader
import ru.palma.app.shell.ArchiveException
import ru.palma.app.shell.CountingInputStream
import ru.palma.app.shell.Decompressors
import ru.palma.app.shell.TarExtractor
import java.io.File
import java.io.IOException

class InstallController(
    private val repository: PalmaRepository,
    private val local: LocalState,
    private val downloader: ResumableDownloader,
    private val storage: Storage,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val retryDelaysMs: List<Long> = listOf(2_000, 4_000, 8_000, 16_000, 30_000),
) {
    interface Storage {
        val downloads: File
        val systems: File
        fun freeBytes(): Long
        fun metered(): Boolean
    }

    private val mutable = MutableStateFlow<InstallState>(InstallState.Idle)
    val state: StateFlow<InstallState> = mutable.asStateFlow()
    private var job: Job? = null

    fun rootfs(imageId: String) = File(storage.systems, imageId)
    fun isInstalled(imageId: String) = File(rootfs(imageId), READY_MARKER).exists()

    fun isCurrent(imageId: String, sha256: String?): Boolean {
        val marker = File(rootfs(imageId), READY_MARKER)
        if (!marker.isFile) return false
        if (sha256.isNullOrBlank()) return true
        return runCatching { marker.readText().trim().equals(sha256, ignoreCase = true) }.getOrDefault(false)
    }

    fun request(image: ImageInfo, allowMetered: Boolean = false) {
        if (job?.isActive == true) return
        if (!allowMetered && storage.metered()) {
            mutable.value = InstallState.ConfirmMetered(image)
            return
        }
        local.pendingInstall = image.id
        job = scope.launch { run(image) }
    }

    fun cancel() {
        val current = mutable.value
        job?.cancel()
        job = null
        local.pendingInstall = null
        val image = when (current) {
            is InstallState.Running -> current.image
            is InstallState.Failed -> current.image
            is InstallState.ConfirmMetered -> current.image
            else -> null
        }
        if (image != null) downloader.discard(target(image, null))
        mutable.value = InstallState.Idle
    }

    fun dismiss() {
        if (job?.isActive != true) mutable.value = InstallState.Idle
    }

    private fun target(image: ImageInfo, request: okhttp3.Request?) = DownloadTarget(
        request = request ?: okhttp3.Request.Builder().url("http://localhost/").build(),
        file = File(storage.downloads, "${image.id}.archive"),
        sizeBytes = image.sizeBytes,
        sha256 = image.sha256,
    )

    private suspend fun run(image: ImageInfo) {
        val meter = RateMeter(clock)
        try {
            if (storage.freeBytes() < image.sizeBytes * SPACE_FACTOR) throw FailureException(Failure.NotEnoughSpace)
            val archive = downloadWithRetry(image, meter)
            extract(image, archive, meter)
            archive.delete()
            local.installed = InstalledSystem(
                imageId = image.id,
                name = image.name,
                distro = image.distro,
                desktop = image.desktop,
                sizeBytes = image.sizeBytes,
                sha256 = image.sha256,
                lastLaunchMillis = null,
            )
            local.pendingInstall = null
            mutable.value = InstallState.Done(image)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failure = when (error) {
                is ArchiveException -> Failure.BrokenArchive
                is IOException -> if (error.message?.contains("ENOSPC") == true) Failure.NotEnoughSpace else Failures.fromThrowable(error)
                else -> Failure.BrokenArchive
            }
            val downloaded = downloader.downloadedBytes(target(image, null))
            mutable.value = InstallState.Failed(image, failure, downloaded)
        }
    }

    private suspend fun downloadWithRetry(image: ImageInfo, meter: RateMeter): File {
        val request = repository.imageRequest(image)
        val target = target(image, request)
        var attempt = 0
        val started = clock()
        var startBytes = -1L
        while (true) {
            try {
                val file = downloader.download(target) { done ->
                    if (startBytes < 0) startBytes = done
                    meter.sample(done)
                    publish(image, done, 0, meter, waiting = false)
                }
                val elapsed = clock() - started
                if (elapsed > 0 && startBytes >= 0 && image.sizeBytes > startBytes) {
                    local.bytesPerSecond = (image.sizeBytes - startBytes) * 1000 / elapsed
                }
                return file
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val failure = Failures.fromThrowable(error)
                if (failure != Failure.NoConnection || attempt >= retryDelaysMs.size) throw error
                publish(image, downloader.downloadedBytes(target), 0, meter, waiting = true)
                delay(retryDelaysMs[attempt++])
                meter.reset()
            }
        }
    }

    private suspend fun extract(image: ImageInfo, archive: File, meter: RateMeter) = withContext(Dispatchers.IO) {
        val destination = rootfs(image.id)
        val staging = File(storage.systems, "${image.id}.partial")
        staging.deleteRecursively()
        meter.reset()
        val context = coroutineContext
        archive.inputStream().use { raw ->
            val counting = CountingInputStream(raw) { read ->
                meter.sample(image.sizeBytes + read)
                publish(image, image.sizeBytes, read, meter, waiting = false)
            }
            Decompressors.open(counting).use { stream ->
                TarExtractor(staging) { context.ensureActive() }.extract(stream)
            }
        }
        File(staging, READY_MARKER).writeText(image.sha256)
        destination.deleteRecursively()
        if (!staging.renameTo(destination)) throw IOException("rename_failed")
    }

    private fun publish(image: ImageInfo, downloaded: Long, unpacked: Long, meter: RateMeter, waiting: Boolean) {
        val remaining = image.sizeBytes * 2 - downloaded - unpacked
        mutable.value = InstallState.Running(
            image = image,
            downloaded = downloaded,
            unpacked = unpacked.coerceAtMost(image.sizeBytes),
            etaSeconds = if (waiting) null else meter.etaSeconds(remaining),
            waitingForNetwork = waiting,
        )
    }

    companion object {
        const val READY_MARKER = ".palma-ready"
        private const val SPACE_FACTOR = 3
    }
}
