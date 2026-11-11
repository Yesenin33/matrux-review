package ru.matrux.app.learn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.matrux.app.shell.ProotCommand
import ru.matrux.app.shell.ProotLayout
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

interface CodeRunner {
    suspend fun run(runtime: Runtime, code: String, stdin: String?): RunOutcome
}

object GuestPrograms {
    private val DIRS = listOf("usr/local/bin", "usr/bin", "bin")

    fun names(runtime: Runtime): List<String> = when (runtime) {
        Runtime.PYTHON -> listOf("python3")
        Runtime.NODE -> listOf("node", "nodejs")
        Runtime.SH -> listOf("sh")
        Runtime.BASH -> listOf("bash")
        Runtime.WEB -> emptyList()
    }

    fun file(runtime: Runtime): String = when (runtime) {
        Runtime.PYTHON -> "main.py"
        Runtime.NODE -> "main.js"
        Runtime.SH, Runtime.BASH -> "main.sh"
        Runtime.WEB -> "index.html"
    }

    fun find(rootfs: File, runtime: Runtime): String? = names(runtime).firstNotNullOfOrNull { name ->
        DIRS.firstOrNull { dir -> Files.exists(File(rootfs, "$dir/$name").toPath(), LinkOption.NOFOLLOW_LINKS) }
            ?.let { "/$it/$name" }
    }
}

class GuestRunner(
    private val nativeLibraryDir: File,
    private val tmpDir: File,
    private val rootfs: () -> File?,
    private val timeoutMs: Long = TIMEOUT_MS,
) : CodeRunner {

    override suspend fun run(runtime: Runtime, code: String, stdin: String?): RunOutcome = withContext(Dispatchers.IO) {
        if (runtime == Runtime.WEB) return@withContext RunOutcome.Broken(runtime.key)
        val root = rootfs()?.takeIf { it.isDirectory } ?: return@withContext RunOutcome.NoSystem
        val program = GuestPrograms.find(root, runtime)
            ?: return@withContext RunOutcome.Missing(GuestPrograms.names(runtime).first())
        runCatching { execute(root, runtime, program, code, stdin) }
            .getOrElse { RunOutcome.Broken(it.message ?: it.javaClass.simpleName) }
    }

    private fun execute(root: File, runtime: Runtime, program: String, code: String, stdin: String?): RunOutcome {
        val binary = File(nativeLibraryDir, "libproot.so")
        val loader = File(nativeLibraryDir, "libproot-loader.so")
        if (!binary.canExecute()) return RunOutcome.Broken("proot")
        tmpDir.mkdirs()
        val work = File(tmpDir, "matrux-lesson").apply {
            deleteRecursively()
            mkdirs()
        }
        val name = GuestPrograms.file(runtime)
        File(work, name).writeText(code)
        val layout = ProotLayout(binary, loader, root, tmpDir)
        val process = ProcessBuilder(ProotCommand.exec(layout, work, listOf(program, name)))
            .redirectErrorStream(true)
            .apply { environment().putAll(ProotCommand.environment(layout)) }
            .start()
        val output = StringBuilder()
        val reader = thread(isDaemon = true) {
            process.inputStream.bufferedReader().use { input ->
                val buffer = CharArray(4096)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    synchronized(output) { if (output.length < OUTPUT_LIMIT) output.append(buffer, 0, read) }
                }
            }
        }
        runCatching { process.outputStream.use { it.write(stdin.orEmpty().toByteArray()) } }
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            return RunOutcome.Timeout
        }
        reader.join(JOIN_MS)
        val text = synchronized(output) { output.toString() }
        return RunOutcome.Done(text, process.exitValue())
    }

    companion object {
        const val TIMEOUT_MS = 10_000L
        private const val OUTPUT_LIMIT = 64 * 1024
        private const val JOIN_MS = 1_000L
    }
}
