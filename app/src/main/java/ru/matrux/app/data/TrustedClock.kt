package ru.matrux.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings

object TrustedClock {
    private const val PREFS = "matrux_clock"
    private const val KEY_WALL = "anchor_wall"
    private const val KEY_ELAPSED = "anchor_elapsed"
    private const val KEY_BOOT = "anchor_boot"
    private const val KEY_LAST = "last_now"
    private const val ROLLBACK_PENALTY_MS = 86_400_000L

    const val SKEW_LIMIT_MS = 2L * 86_400_000L

    private var app: Context? = null
    private var prefs: SharedPreferences? = null

    fun install(context: Context) {
        if (prefs == null) {
            app = context.applicationContext
            prefs = app!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun now(): Long {
        val store = prefs ?: return System.currentTimeMillis()
        val wall = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val boot = bootId(store)
        val anchorWall = store.getLong(KEY_WALL, 0L)
        if (anchorWall <= 0L) {
            store.edit().putLong(KEY_WALL, wall).putLong(KEY_ELAPSED, elapsed).putInt(KEY_BOOT, boot).apply()
            return wall
        }
        val anchorElapsed = store.getLong(KEY_ELAPSED, elapsed)
        val anchorBoot = store.getInt(KEY_BOOT, boot)
        val passed = if (anchorBoot == boot && elapsed >= anchorElapsed) {
            elapsed - anchorElapsed
        } else {
            val delta = wall - anchorWall
            if (delta >= 0L) delta else ROLLBACK_PENALTY_MS
        }
        val last = store.getLong(KEY_LAST, 0L)
        val estimate = maxOf(anchorWall + passed, last, anchorWall)
        if (estimate > last) store.edit().putLong(KEY_LAST, estimate).apply()
        return estimate
    }

    fun wallBehind(): Long = now() - System.currentTimeMillis()

    fun anchor(serverMillis: Long?) {
        val store = prefs ?: return
        val value = serverMillis ?: System.currentTimeMillis()
        store.edit()
            .putLong(KEY_WALL, value)
            .putLong(KEY_ELAPSED, SystemClock.elapsedRealtime())
            .putInt(KEY_BOOT, bootId(store))
            .putLong(KEY_LAST, value)
            .apply()
    }

    private fun bootId(store: SharedPreferences): Int {
        val context = app ?: return store.getInt(KEY_BOOT, 0)
        return runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(store.getInt(KEY_BOOT, 0))
    }
}
