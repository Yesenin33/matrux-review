package ru.palma.app.shell

import java.io.File

enum class DisplayMode { X11, VNC }

data class ProotLayout(
    val binary: File,
    val loader: File,
    val rootfs: File,
    val tmp: File,
)

object ProotCommand {
    const val SESSION_SCRIPT = "/usr/local/bin/palma-session"
    const val X11_DISPLAY = ":0"
    const val VNC_PORT = 5901

    fun arguments(layout: ProotLayout, mode: DisplayMode, geometry: String, port: Int): List<String> {
        val shm = File(layout.tmp, "shm").apply { mkdirs() }
        val binds = mutableListOf(
            "-b", "${shm.absolutePath}:/dev/shm",
            "-b", "/dev",
            "-b", "/dev/pts",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "${layout.tmp.absolutePath}:/tmp",
        )
        val environment = mutableListOf(
            "HOME=/root",
            "USER=root",
            "LANG=ru_RU.UTF-8",
            "TERM=xterm-256color",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "DISPLAY=$X11_DISPLAY",
            "PALMA_VNC_PORT=$port",
            "PALMA_GEOMETRY=$geometry",
        )
        return listOf(
            layout.binary.absolutePath,
            "--kill-on-exit",
            "--link2symlink",
            "--sysvipc",
            "-0",
            "-r", layout.rootfs.absolutePath,
        ) + binds + listOf(
            "-w", "/root",
            "/usr/bin/env", "-i",
        ) + environment + listOf(
            "/bin/sh", SESSION_SCRIPT, mode.name.lowercase(),
        )
    }

    fun environment(layout: ProotLayout): Map<String, String> = mapOf(
        "PROOT_TMP_DIR" to layout.tmp.absolutePath,
        "PROOT_LOADER" to layout.loader.absolutePath,
        "LD_LIBRARY_PATH" to layout.binary.parentFile.absolutePath,
    )
}

interface Engine {
    fun available(): Boolean
    fun hasSessionScript(rootfs: File): Boolean
    fun start(rootfs: File, mode: DisplayMode, geometry: String, port: Int): Process
}

class ProotEngine(private val nativeLibraryDir: File, private val tmpDir: File) : Engine {
    private val binary get() = File(nativeLibraryDir, "libproot.so")
    private val loader get() = File(nativeLibraryDir, "libproot-loader.so")

    override fun available(): Boolean = binary.canExecute() && loader.exists()

    override fun hasSessionScript(rootfs: File): Boolean =
        File(rootfs, ProotCommand.SESSION_SCRIPT.trimStart('/')).exists()

    override fun start(rootfs: File, mode: DisplayMode, geometry: String, port: Int): Process {
        tmpDir.mkdirs()
        val layout = ProotLayout(binary, loader, rootfs, tmpDir)
        val free = GuestProcesses.reclaim(port)
        android.util.Log.i("palma-session", "reclaim port $port: $free")
        return ProcessBuilder(ProotCommand.arguments(layout, mode, geometry, port))
            .redirectErrorStream(true)
            .apply { environment().putAll(ProotCommand.environment(layout)) }
            .start()
    }
}

interface X11Server {
    fun available(): Boolean
    fun start(socketDir: File): Boolean
    fun stop()
}

class NativeX11Server(private val nativeLibraryDir: File) : X11Server {
    private var loaded = false

    override fun available(): Boolean = File(nativeLibraryDir, "libpalmax11.so").exists()

    override fun start(socketDir: File): Boolean {
        if (!available()) return false
        if (!loaded) {
            System.loadLibrary("palmax11")
            loaded = true
        }
        return true
    }

    override fun stop() = Unit
}
