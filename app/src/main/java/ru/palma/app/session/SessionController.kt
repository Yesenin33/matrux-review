package ru.palma.app.session

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.palma.app.data.Failure
import ru.palma.app.data.FailureException
import ru.palma.app.data.Failures
import ru.palma.app.data.LocalState
import ru.palma.app.data.PalmaRepository
import ru.palma.app.install.InstallController
import ru.palma.app.shell.DisplayMode
import ru.palma.app.shell.Engine
import ru.palma.app.shell.Framebuffer
import ru.palma.app.shell.GuestPorts
import ru.palma.app.shell.GuestProcesses
import ru.palma.app.shell.LaunchSequence
import ru.palma.app.shell.LaunchState
import ru.palma.app.shell.ProotCommand
import ru.palma.app.shell.VncClient
import ru.palma.app.shell.X11Server
import ru.palma.app.ui.Keys
import java.io.File
import java.net.Socket
import kotlin.math.abs

sealed interface SessionState {
    data object Stopped : SessionState
    data class Launching(val name: String, val launch: LaunchState) : SessionState
    data class Live(val name: String, val desktop: String, val mode: DisplayMode, val offline: Boolean) : SessionState
    data class Lost(val name: String, val failure: Failure) : SessionState
}

class SessionController(
    private val repository: PalmaRepository,
    private val local: LocalState,
    private val installs: InstallController,
    private val engine: Engine,
    private val x11: X11Server,
    private val tmpDir: File,
    private val scope: CoroutineScope,
    private val geometry: () -> String = { DEFAULT_GEOMETRY },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow<SessionState>(SessionState.Stopped)
    val state: StateFlow<SessionState> = mutable.asStateFlow()
    private val frames = MutableStateFlow<Pair<Framebuffer, Long>?>(null)
    val frame: StateFlow<Pair<Framebuffer, Long>?> = frames.asStateFlow()
    private var vncPort: Int = ProotCommand.VNC_PORT
    private val blankState = MutableStateFlow(false)
    val blank: StateFlow<Boolean> = blankState.asStateFlow()
    private val russianState = MutableStateFlow(false)
    val russian: StateFlow<Boolean> = russianState.asStateFlow()

    private var job: Job? = null
    private var inputJob: Job? = null
    private var process: Process? = null
    private var socket: Socket? = null
    private var vnc: VncClient? = null
    private var cursorX = 0
    private var cursorY = 0
    private var cursorPlaced = false
    private val engineOutput = StringBuilder()
    private var startedGeometry = ""
    private var lastGeometryAt = 0L
    private val GEOMETRY_COOLDOWN_MS = 12_000L
    private var restarting = false
    private var loggedFrame = ""

    fun start() {
        if (job?.isActive == true) return
        val system = local.installed
        if (system == null) {
            mutable.value = SessionState.Lost("", Failure.NotInstalled)
            return
        }
        val started = clock()
        restarting = false
        startedGeometry = geometry()
        job = scope.launch {
            val sequence = LaunchSequence(
                checkSubscription = ::checkSubscription,
                prepareRootfs = { prepareRootfs(system.imageId) },
                startEngine = { startEngine(system.imageId) },
                startDisplay = ::startDisplay,
            )
            sequence.run().collect { launch ->
                mutable.value = when (launch) {
                    is LaunchState.Ready -> {
                        local.installed = system.copy(lastLaunchMillis = clock() - started)
                        SessionState.Live(system.name, system.desktop, launch.mode, launch.offline)
                    }
                    is LaunchState.Failed -> {
                        teardown()
                        SessionState.Launching(system.name, launch)
                    }
                    is LaunchState.Running -> SessionState.Launching(system.name, launch)
                }
            }
            if (mutable.value is SessionState.Live) pump(system.name)
        }
    }

    fun stop() {
        restarting = false
        job?.cancel()
        job = null
        teardown()
        GuestProcesses.killLeftovers()
        mutable.value = SessionState.Stopped
    }

    fun syncGeometry() {
        val state = mutable.value
        if (state !is SessionState.Live && state !is SessionState.Launching) return
        val want = geometry()
        val frame = frames.value?.first
        android.util.Log.i("palma-geo", "want=" + want + " actual=" + (frame?.let { "" + it.width + "x" + it.height }))
        val actual = frame?.let { "${it.width}x${it.height}" }
        val mismatch = actual != null && actual != want
        if (want == startedGeometry && !mismatch) return
        val now = System.currentTimeMillis()
        if (now - lastGeometryAt < GEOMETRY_COOLDOWN_MS) return
        lastGeometryAt = now
        if (requestGeometry(want)) {
            startedGeometry = want
            frames.value = null
            scope.launch {
                delay(GEOMETRY_SETTLE_MS)
                reconnectDisplay()
                delay(GEOMETRY_WAIT_MS)
                val frame = frames.value?.first
                if (frame != null && "${frame.width}x${frame.height}" != want) restartGeometry()
            }
            return
        }
        restartGeometry()
    }

    private fun reconnectDisplay() {
        runCatching { socket?.close() }
    }

    private fun requestGeometry(size: String): Boolean = runCatching {
        val file = File(tmpDir.parentFile, "proot/geometry-request")
        file.parentFile?.mkdirs()
        file.writeText(size)
        true
    }.getOrDefault(false)

    private fun restartGeometry() {
        stop()
        restarting = true
        frames.value = null
        scope.launch {
            delay(RESTART_PAUSE_MS)
            if (restarting) start()
        }
    }

    fun pointer(x: Int, y: Int, buttons: Int) {
        val client = vnc ?: return
        scope.launch(Dispatchers.IO) { runCatching { client.pointer(x, y, buttons) } }
    }

    fun tap(x: Int, y: Int) {
        val client = vnc ?: return
        scope.launch(Dispatchers.IO) {
            runCatching {
                client.pointer(x, y, 1)
                delay(CLICK_MS)
                client.pointer(x, y, 0)
            }
        }
    }

    fun key(keysym: Int) {
        enqueue { client ->
            client.key(keysym, true)
            delay(TYPE_KEY_MS)
            client.key(keysym, false)
        }
    }

    fun typeChar(letter: Char) {
        val code = Keys.of(letter) ?: return
        val cyrillic = letter.code in CYRILLIC_FIRST..CYRILLIC_LAST
        val switching = cyrillic != russianState.value
        russianState.value = cyrillic
        enqueue { client ->
            if (switching) switchGroup(client)
            client.key(code, true)
            delay(TYPE_KEY_MS)
            client.key(code, false)
        }
    }

    fun toggleLayout() {
        russianState.value = !russianState.value
        enqueue { client -> switchGroup(client) }
    }

    fun combo(vararg keysyms: Int) {
        enqueue { client ->
            keysyms.forEach { client.key(it, true) }
            delay(TYPE_KEY_MS)
            keysyms.reversed().forEach { client.key(it, false) }
        }
    }

    private suspend fun switchGroup(client: VncClient) {
        client.key(Keys.ALT, true)
        client.key(Keys.SHIFT, true)
        delay(GROUP_SWITCH_MS)
        client.key(Keys.SHIFT, false)
        client.key(Keys.ALT, false)
        delay(GROUP_SETTLE_MS)
    }

    private fun enqueue(block: suspend (VncClient) -> Unit) {
        val client = vnc ?: return
        val previous = inputJob
        inputJob = scope.launch(Dispatchers.IO) {
            withTimeoutOrNull(QUEUE_WAIT_MS) { previous?.join() }
            runCatching { block(client) }
        }
    }

    fun moveCursor(dx: Int, dy: Int) {
        val client = vnc ?: return
        val width = client.framebuffer.width
        val height = client.framebuffer.height
        if (width <= 0 || height <= 0) return
        if (!cursorPlaced) {
            cursorX = width / 2
            cursorY = height / 2
            cursorPlaced = true
        }
        cursorX = (cursorX + dx).coerceIn(0, width - 1)
        cursorY = (cursorY + dy).coerceIn(0, height - 1)
        pointer(cursorX, cursorY, 0)
    }

    fun click(button: Int) {
        val client = vnc ?: return
        val width = client.framebuffer.width
        val height = client.framebuffer.height
        if (width <= 0 || height <= 0) return
        if (!cursorPlaced) {
            cursorX = width / 2
            cursorY = height / 2
            cursorPlaced = true
        }
        val x = cursorX
        val y = cursorY
        enqueue { client ->
            client.pointer(x, y, button)
            delay(CLICK_MS)
            client.pointer(x, y, 0)
        }
    }

    fun scroll(steps: Int) {
        val client = vnc ?: return
        if (steps == 0) return
        if (!cursorPlaced) return
        val button = if (steps < 0) WHEEL_UP else WHEEL_DOWN
        val x = cursorX
        val y = cursorY
        enqueue { client ->
            repeat(abs(steps)) {
                client.pointer(x, y, button)
                delay(CLICK_MS)
                client.pointer(x, y, 0)
            }
        }
    }

    private suspend fun checkSubscription(): Boolean = try {
        repository.refreshStatus()
        true
    } catch (error: FailureException) {
        if (error.failure == Failure.NoConnection) false else throw error
    }

    private fun prepareRootfs(imageId: String) {
        if (!installs.isInstalled(imageId)) throw FailureException(Failure.NotInstalled)
    }

    private suspend fun startEngine(imageId: String) = withContext(Dispatchers.IO) {
        if (!engine.available()) throw FailureException(Failure.EngineMissing)
        val rootfs = installs.rootfs(imageId)
        if (!engine.hasSessionScript(rootfs)) throw FailureException(Failure.SessionMissing)
        val mode = if (x11.available()) DisplayMode.X11 else DisplayMode.VNC
        vncPort = GuestPorts.free(ProotCommand.VNC_PORT)
        val started = engine.start(rootfs, mode, geometry(), vncPort)
        process = started
        scope.launch(Dispatchers.IO) {
            runCatching {
                started.inputStream.bufferedReader().forEachLine { line ->
                    synchronized(engineOutput) {
                        if (engineOutput.length < MAX_ENGINE_OUTPUT) engineOutput.append(line).append('\n')
                    }
                }
            }
        }
        delay(ENGINE_SETTLE_MS)
        if (!started.isAlive) {
            val output = runCatching { started.inputStream.bufferedReader().use { it.readText() } }
                .getOrNull().orEmpty().trim().take(MAX_ENGINE_OUTPUT)
            throw FailureException(Failure.Engine(output), output.ifEmpty { null })
        }
    }

    private suspend fun startDisplay(): DisplayMode = withContext(Dispatchers.IO) {
        if (x11.available() && x11.start(tmpDir)) return@withContext DisplayMode.X11
        val deadline = clock() + DISPLAY_TIMEOUT_MS
        while (isActive && clock() < deadline) {
            if (process?.isAlive == false) throw FailureException(Failure.EngineCrashed)
            val connected = runCatching { VncClient.connect(LOOPBACK, vncPort, CONNECT_TIMEOUT_MS) }.getOrNull()
            if (connected != null) {
                val (sock, client) = connected
                try {
                    client.handshake()
                    socket = sock
                    vnc = client
                    frames.value = client.framebuffer to clock()
                    return@withContext DisplayMode.VNC
                } catch (error: Exception) {
                    runCatching { sock.close() }
                }
            }
            delay(RETRY_MS)
        }
        val report = displayReport()
        throw FailureException(Failure.Display(report), report.ifEmpty { null })
    }

    fun diagnostics(): String {
        val engine = synchronized(engineOutput) { engineOutput.toString().trim().takeLast(LOG_TAIL) }
        val logs = DIAGNOSTIC_LOGS.mapNotNull { name ->
            val file = File(tmpDir, name)
            if (!file.isFile) return@mapNotNull null
            val text = runCatching { file.readText().trim().takeLast(LOG_TAIL) }.getOrNull().orEmpty()
            text.takeIf { it.isNotBlank() }?.let { "$name\n$it" }
        }.joinToString("\n")
        val blocks = listOf(engine, logs).filter { it.isNotBlank() }
        if (blocks.isEmpty()) return ""
        val headers = listOfNotNull(
            "— запуск —".takeIf { engine.isNotBlank() },
            "— логи —".takeIf { logs.isNotBlank() },
        )
        return (headers + blocks).joinToString("\n").takeLast(MAX_ENGINE_OUTPUT)
    }

    private fun displayReport(): String {
        val engine = synchronized(engineOutput) { engineOutput.toString().trim().takeLast(MAX_ENGINE_OUTPUT) }
        val logs = DISPLAY_LOGS.mapNotNull { name ->
            val file = File(tmpDir, name)
            if (!file.isFile) return@mapNotNull null
            val text = runCatching { file.readText().trim().takeLast(LOG_TAIL) }.getOrNull().orEmpty()
            text.takeIf { it.isNotBlank() }?.let { "$name\n$it" }
        }.joinToString("\n")
        return listOf(engine, logs).filter { it.isNotBlank() }.joinToString("\n").takeLast(MAX_ENGINE_OUTPUT)
    }

    private suspend fun pump(name: String) = withContext(Dispatchers.IO) {
        while (isActive) {
            val client = vnc ?: return@withContext
            try {
                while (isActive) {
                    if (client.readMessage()) {
                        frames.value = client.framebuffer to clock()
                        blankState.value = client.framebuffer.blank()
                        val size = "${client.framebuffer.width}x${client.framebuffer.height}"
                        if (size != loggedFrame) {
                            loggedFrame = size
                            Log.i(FRAME_TAG, "кадр $size пусто=${client.framebuffer.blank()}")
                        }
                    }
                }
                return@withContext
            } catch (error: Exception) {
                if (!isActive) return@withContext
                runCatching { socket?.close() }
                socket = null
                vnc = null
                if (reattach()) continue
                teardown()
                mutable.value = SessionState.Lost(name, Failures.fromThrowable(error).let {
                    if (it == Failure.NoConnection) Failure.EngineCrashed else it
                })
                return@withContext
            }
        }
    }

    private suspend fun reattach(): Boolean {
        var waited = 0L
        while (waited < REATTACH_TIMEOUT_MS) {
            if (process?.isAlive == false) return false
            val connected = runCatching { VncClient.connect(LOOPBACK, vncPort, CONNECT_TIMEOUT_MS) }.getOrNull()
            if (connected != null) {
                val (sock, client) = connected
                if (runCatching { client.handshake() }.isSuccess) {
                    socket = sock
                    vnc = client
                    frames.value = client.framebuffer to clock()
                    return true
                }
                runCatching { sock.close() }
            }
            delay(RETRY_MS)
            waited += RETRY_MS
        }
        return false
    }

    private fun teardown() {
        runCatching { socket?.close() }
        socket = null
        vnc = null
        cursorX = 0
        cursorY = 0
        cursorPlaced = false
        frames.value = null
        blankState.value = false
        process?.destroy()
        process = null
        x11.stop()
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val ENGINE_SETTLE_MS = 800L
        const val DISPLAY_TIMEOUT_MS = 60_000L
        const val CONNECT_TIMEOUT_MS = 1_000
        const val REATTACH_TIMEOUT_MS = 60_000L
        const val RETRY_MS = 500L
        const val RESTART_PAUSE_MS = 800L
        const val GEOMETRY_WAIT_MS = 20_000L
        const val FRAME_TAG = "palma-vnc"
        const val GEOMETRY_SETTLE_MS = 9_000L
        const val MAX_ENGINE_OUTPUT = 1200
        const val CLICK_MS = 40L
        const val TYPE_KEY_MS = 12L
        const val GROUP_SWITCH_MS = 30L
        const val GROUP_SETTLE_MS = 90L
        const val QUEUE_WAIT_MS = 2_000L
        const val CYRILLIC_FIRST = 0x0400
        const val CYRILLIC_LAST = 0x04FF
        const val WHEEL_UP = 4
        const val WHEEL_DOWN = 5
        const val LOG_TAIL = 500
        const val DEFAULT_GEOMETRY = "1280x800"
        val DISPLAY_LOGS = listOf("xvfb.log", "x11vnc.log")
        val DIAGNOSTIC_LOGS = listOf("palma-session.log", "xvfb-0.log", "x11vnc.log")
    }
}
