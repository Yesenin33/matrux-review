package ru.matrux.app.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.matrux.app.data.ImageInfo

class RateMeterTest {
    @Test
    fun etaFromMeasuredRate() {
        var now = 0L
        val meter = RateMeter({ now })
        meter.sample(0)
        assertNull(meter.etaSeconds(1000))
        now = 1000
        meter.sample(1_000_000)
        assertEquals(1_000_000.0, meter.unitsPerSecond, 0.1)
        assertEquals(3L, meter.etaSeconds(3_000_000))
    }

    @Test
    fun estimateNeedsMeasurement() {
        assertNull(InstallEstimate.seconds(300_000_000, null))
        assertEquals(120L, InstallEstimate.seconds(300_000_000, 5_000_000))
    }

    @Test
    fun progressCombinesDownloadAndUnpack() {
        val image = ImageInfo("d", "D", "debian", "xfce", 100, "a".repeat(64), 1, false, "/x")
        assertEquals(0, InstallState.Running(image, 0, 0, null).percent)
        assertEquals(50, InstallState.Running(image, 100, 0, null).percent)
        assertEquals(75, InstallState.Running(image, 100, 50, null).percent)
        assertEquals(100, InstallState.Running(image, 100, 100, null).percent)
    }
}
