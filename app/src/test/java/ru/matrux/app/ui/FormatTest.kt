package ru.matrux.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    @Test
    fun sizes() {
        assertEquals(Format.Size.Megabytes("300"), Format.size(300_000_000))
        assertEquals(Format.Size.Megabytes("25"), Format.size(25_000_000))
        assertEquals(Format.Size.Megabytes("2,5"), Format.size(2_500_000))
        assertEquals(Format.Size.Gigabytes("24,1"), Format.size(24_100_000_000))
    }

    @Test
    fun durations() {
        assertEquals(Format.Duration.Seconds(40), Format.roughDuration(40))
        assertEquals(Format.Duration.Minutes(2), Format.roughDuration(100))
        assertEquals(Format.Duration.MinSec(1, 20), Format.eta(80))
        assertEquals(Format.Duration.Seconds(1), Format.eta(0))
    }

    @Test
    fun russianDate() {
        assertEquals("15 декабря", Format.dayMonth(LocalDate.of(2026, 12, 15)))
    }
}
