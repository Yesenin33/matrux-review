package ru.palma.app.shell

import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket

object GuestPorts {

    fun free(start: Int, attempts: Int = 20): Int {
        for (port in start until start + attempts) {
            if (available(port)) return port
        }
        return start
    }

    fun available(port: Int): Boolean {
        val probe = ServerSocket()
        return try {
            probe.reuseAddress = false
            probe.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 1)
            true
        } catch (error: Exception) {
            false
        } finally {
            runCatching { probe.close() }
        }
    }
}

object GuestProcesses {

    private val NAMES = listOf(
        "x11vnc", "Xvfb", "Xvnc", "Xtigervnc", "xfce4-session", "xfwm4", "xfce4-panel", "xfdesktop",
        "xfsettingsd", "xfconfd", "palma-session", "libproot.so",
    )

    fun reclaim(port: Int, timeoutMs: Long = 8000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            killLeftovers()
            if (GuestPorts.available(port)) return true
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(400)
        }
    }

    fun killLeftovers(): Int {
        val me = android.os.Process.myPid()
        var killed = 0
        for (entry in File("/proc").list() ?: return 0) {
            val pid = entry.toIntOrNull() ?: continue
            if (pid == me) continue
            val line = cmdline(pid) ?: continue
            if (line.isEmpty()) continue
            if (line.contains("ru.palma.app")) continue
            if (!NAMES.any { line.contains(it) }) continue
            runCatching {
                android.os.Process.sendSignal(pid, 9)
                killed++
            }
        }
        return killed
    }

    private fun cmdline(pid: Int): String? =
        runCatching { File("/proc/$pid/cmdline").readText().replace('\u0000', ' ').trim() }
            .getOrNull()

    fun killGuest(): Int {
        val me = android.os.Process.myPid()
        val uid = android.os.Process.myUid()
        var killed = 0
        for (entry in File("/proc").list() ?: return 0) {
            val pid = entry.toIntOrNull() ?: continue
            if (pid == me) continue
            if (uidOf(pid) != uid) continue
            runCatching {
                android.os.Process.sendSignal(pid, 9)
                killed++
            }
        }
        return killed
    }

    private fun uidOf(pid: Int): Int? = runCatching {
        File("/proc/$pid/status").useLines { lines ->
            lines.firstOrNull { it.startsWith("Uid:") }
                ?.split('\t')
                ?.getOrNull(1)
                ?.trim()
                ?.toIntOrNull()
        }
    }.getOrNull()
}
