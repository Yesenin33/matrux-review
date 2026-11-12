package ru.matrux.app.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    val russian: Locale = Locale("ru", "RU")
    private const val MB = 1_000_000.0
    private const val GB = 1_000_000_000.0

    sealed interface Size {
        val number: String
        data class Megabytes(override val number: String) : Size
        data class Gigabytes(override val number: String) : Size
    }

    fun size(bytes: Long): Size {
        val symbols = DecimalFormatSymbols(russian)
        return if (bytes >= GB) {
            Size.Gigabytes(DecimalFormat("0.#", symbols).format(bytes / GB))
        } else {
            val mb = bytes / MB
            Size.Megabytes(if (mb < 10) DecimalFormat("0.#", symbols).format(mb) else DecimalFormat("0", symbols).format(mb))
        }
    }

    fun rubles(amount: Long): String =
        amount.toString().reversed().chunked(3).joinToString("\u00A0").reversed()

    sealed interface Duration {
        data class Seconds(val seconds: Int) : Duration
        data class Minutes(val minutes: Int) : Duration
        data class MinSec(val minutes: Int, val seconds: Int) : Duration
    }

    fun roughDuration(seconds: Long): Duration = when {
        seconds < 60 -> Duration.Seconds(seconds.toInt().coerceAtLeast(1))
        else -> Duration.Minutes(((seconds + 59) / 60).toInt())
    }

    fun eta(seconds: Long): Duration =
        if (seconds < 60) Duration.Seconds(seconds.toInt().coerceAtLeast(1))
        else Duration.MinSec((seconds / 60).toInt(), (seconds % 60).toInt())

    fun dayMonth(date: LocalDate): String = DateTimeFormatter.ofPattern("d MMMM", russian).format(date)
}
