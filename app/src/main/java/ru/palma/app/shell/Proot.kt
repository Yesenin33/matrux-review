package ru.palma.app.shell

import java.io.File

enum class DisplayMode { X11, VNC }

data class ProotLayout(
    val binary: File,
    val loader: File,
    val rootfs: File,
    val tmp: File,
    val home: File? = null,
)

object ProotCommand {
    const val SESSION_SCRIPT = "/usr/local/bin/palma-session"
    const val X11_DISPLAY = ":0"
    const val VNC_PORT = 5901

    fun arguments(layout: ProotLayout, mode: DisplayMode, geometry: String, port: Int, dns: String = ""): List<String> {
        val shm = File(layout.tmp, "shm").apply { mkdirs() }
        val binds = mutableListOf(
            "-b", "${shm.absolutePath}:/dev/shm",
            "-b", "/dev",
            "-b", "/dev/pts",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "${layout.tmp.absolutePath}:/tmp",
        )
        layout.home?.let { home ->
            home.mkdirs()
            binds.addAll(listOf("-b", "${home.absolutePath}:/root"))
        }
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
        if (dns.isNotEmpty()) environment.add("PALMA_DNS=$dns")
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

    const val LESSON_DIR = "/tmp/palma-lesson"

    fun exec(layout: ProotLayout, work: File, command: List<String>): List<String> = listOf(
        layout.binary.absolutePath,
        "--kill-on-exit",
        "--link2symlink",
        "-0",
        "-r", layout.rootfs.absolutePath,
        "-b", "/dev",
        "-b", "/proc",
        "-b", "${layout.tmp.absolutePath}:/tmp",
        "-b", "${work.absolutePath}:$LESSON_DIR",
        "-w", LESSON_DIR,
        "/usr/bin/env", "-i",
        "HOME=/root",
        "USER=root",
        "LANG=C.UTF-8",
        "PYTHONIOENCODING=utf-8",
        "PYTHONDONTWRITEBYTECODE=1",
        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
    ) + command

    fun environment(layout: ProotLayout): Map<String, String> = mapOf(
        "PROOT_TMP_DIR" to layout.tmp.absolutePath,
        "PROOT_LOADER" to layout.loader.absolutePath,
        "LD_LIBRARY_PATH" to layout.binary.parentFile.absolutePath,
    )
}

interface Engine {
    fun available(): Boolean
    fun hasSessionScript(rootfs: File): Boolean
    fun installSessionScript(rootfs: File): Boolean = false
    fun start(rootfs: File, mode: DisplayMode, geometry: String, port: Int): Process
}

class ProotEngine(
    private val nativeLibraryDir: File,
    private val tmpDir: File,
    private val sessionScript: () -> ByteArray? = { null },
    private val homeDir: File? = null,
    private val dnsServers: () -> String = { "" },
) : Engine {
    private val binary get() = File(nativeLibraryDir, "libproot.so")
    private val loader get() = File(nativeLibraryDir, "libproot-loader.so")

    override fun available(): Boolean = binary.canExecute() && loader.exists()

    override fun hasSessionScript(rootfs: File): Boolean =
        File(rootfs, ProotCommand.SESSION_SCRIPT.trimStart('/')).exists()

    override fun installSessionScript(rootfs: File): Boolean {
        val bundled = runCatching { sessionScript() }
            .onFailure { android.util.Log.w("palma-session", "bundled script unreadable", it) }
            .getOrNull() ?: return false
        val result = SessionScript.install(rootfs, bundled)
        result
            .onSuccess { android.util.Log.i("palma-session", if (it) "script updated in $rootfs" else "script up to date") }
            .onFailure { android.util.Log.w("palma-session", "script install failed in $rootfs", it) }
        return result.getOrDefault(false)
    }

    override fun start(rootfs: File, mode: DisplayMode, geometry: String, port: Int): Process {
        tmpDir.mkdirs()
        val home = homeDir?.let { dir ->
            File(dir, rootfs.name).also { seeded ->
                seedHome(rootfs, seeded)
                syncDesktopIcons(rootfs, seeded)
            }
        }
        val layout = ProotLayout(binary, loader, rootfs, tmpDir, home)
        val free = GuestProcesses.reclaim(port)
        android.util.Log.i("palma-session", "reclaim port $port: $free")
        return ProcessBuilder(ProotCommand.arguments(layout, mode, geometry, port, dnsServers()))
            .redirectErrorStream(true)
            .apply { environment().putAll(ProotCommand.environment(layout)) }
            .start()
    }

    private fun seedHome(rootfs: File, home: File) {
        if (home.exists()) return
        home.mkdirs()
        val source = File(rootfs, "root")
        if (!source.isDirectory) return
        runCatching {
            java.nio.file.Files.walkFileTree(
                source.toPath(),
                object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                    override fun preVisitDirectory(dir: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                        File(home, source.toPath().relativize(dir).toString()).mkdirs()
                        return java.nio.file.FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                        if (attrs.isRegularFile) {
                            val target = File(home, source.toPath().relativize(file).toString())
                            runCatching { java.nio.file.Files.copy(file, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
                        }
                        return java.nio.file.FileVisitResult.CONTINUE
                    }
                },
            )
        }.onFailure { android.util.Log.w("palma-session", "seed home failed for ${rootfs.name}", it) }
    }

    private fun syncDesktopIcons(rootfs: File, home: File) {
        val source = File(rootfs, "root/Desktop")
        if (!source.isDirectory) return
        val target = File(home, "Desktop")
        if (!target.isDirectory && !target.mkdirs()) return
        source.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            val dst = File(target, file.name)
            if (dst.exists()) return@forEach
            runCatching {
                java.nio.file.Files.copy(file.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                dst.setExecutable(true, false)
            }
        }
    }
}

object SessionScript {
    fun target(rootfs: File): File = File(rootfs, ProotCommand.SESSION_SCRIPT.trimStart('/'))

    fun install(rootfs: File, bundled: ByteArray): Result<Boolean> = runCatching {
        val target = target(rootfs)
        val path = target.toPath()
        val linked = java.nio.file.Files.isSymbolicLink(path)
        if (!linked && target.isFile && target.readBytes().contentEquals(bundled)) {
            makeExecutable(target)
            return@runCatching false
        }
        val dir = target.parentFile ?: error("no parent for $target")
        if (!dir.isDirectory && !dir.mkdirs()) error("cannot create $dir")
        val staged = File(dir, ".${target.name}.new")
        staged.writeBytes(bundled)
        makeExecutable(staged)
        if (linked) java.nio.file.Files.delete(path)
        java.nio.file.Files.move(
            staged.toPath(),
            path,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
        )
        true
    }

    private fun makeExecutable(file: File) {
        file.setReadable(true, false)
        file.setWritable(true, true)
        file.setExecutable(true, false)
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
