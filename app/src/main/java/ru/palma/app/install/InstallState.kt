package ru.palma.app.install

import ru.palma.app.data.Failure
import ru.palma.app.data.ImageInfo

sealed interface InstallState {
    data object Idle : InstallState
    data class ConfirmMetered(val image: ImageInfo) : InstallState
    data class Running(
        val image: ImageInfo,
        val downloaded: Long,
        val unpacked: Long,
        val etaSeconds: Long?,
        val waitingForNetwork: Boolean = false,
    ) : InstallState {
        val total: Long get() = image.sizeBytes
        val fraction: Float get() = ((downloaded + unpacked).toDouble() / (2.0 * total)).coerceIn(0.0, 1.0).toFloat()
        val percent: Int get() = (fraction * 100).toInt()
    }
    data class Failed(val image: ImageInfo, val failure: Failure, val downloaded: Long) : InstallState
    data class Done(val image: ImageInfo) : InstallState
}

class RateMeter(private val now: () -> Long, private val smoothing: Double = 0.3) {
    private var lastTime = -1L
    private var lastUnits = 0L
    var unitsPerSecond: Double = 0.0
        private set

    fun reset() {
        lastTime = -1
        unitsPerSecond = 0.0
    }

    fun sample(units: Long) {
        val time = now()
        if (lastTime < 0) {
            lastTime = time
            lastUnits = units
            return
        }
        val elapsed = time - lastTime
        if (elapsed < MIN_WINDOW_MS) return
        val rate = (units - lastUnits) * 1000.0 / elapsed
        unitsPerSecond = if (unitsPerSecond == 0.0) rate else unitsPerSecond + smoothing * (rate - unitsPerSecond)
        lastTime = time
        lastUnits = units
    }

    fun etaSeconds(remaining: Long): Long? =
        if (unitsPerSecond <= 0.0) null else kotlin.math.ceil(remaining / unitsPerSecond).toLong()

    private companion object {
        const val MIN_WINDOW_MS = 500L
    }
}

object InstallEstimate {
    fun seconds(sizeBytes: Long, bytesPerSecond: Long?): Long? {
        if (bytesPerSecond == null || bytesPerSecond <= 0) return null
        return kotlin.math.ceil(sizeBytes * 2.0 / bytesPerSecond).toLong()
    }
}
