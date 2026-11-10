package ru.palma.app.session

import android.graphics.Bitmap
import android.util.Log
import java.net.SocketTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

sealed interface SessionState {
    data object Stopped : SessionState
    data class Launching(val name: String, val launch: LaunchState) : SessionState
    data class Live(val name: String, val desktop: String, val mode: DisplayMode, val offline: Boolean) : SessionState
    data class Lost(val name: String, val failure: Failure) : SessionState
}

data class Frame(val bitmap: Bitmap, val seq: Long)

private sealed interface Input {
    data class Move(val x: Int, val y: Int, val buttons: Int) : Input
    class Script(val block: suspend (VncClient) -> Unit) : Input
}

class SessionController(
    private val repository: PalmaRepository,
    private val local: LocalState,
    private val installs: InstallController,
    private val engine: Engine,
    private val x11: X11Server,
    private val tmpDir: File,
    private val guestTmp: File,
    private val scope: CoroutineScope,
    private val geometry: () -> String = { DEFAULT_GEOMETRY },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow<SessionState>(SessionState.Stopped)
    val state: StateFlow<SessionState> = mutable.asStateFlow()
    private val frames = MutableStateFlow<Frame?>(null)
    val frame: StateFlow<Frame?> = frames.asStateFlow()
    private val seq = AtomicLong()
    private val surfaces = arrayOfNulls<Bitmap>(2)
    private var back = 0
    private var vncPort: Int = ProotCommand.VNC_PORT
    private val blankState = MutableStateFlow(false)
    val blank: StateFlow<Boolean> = blankState.asStateFlow()
    private val russianState = MutableStateFlow(false)
    val russian: StateFlow<Boolean> = russianState.asStateFlow()

    private var job: Job? = null
    private var inputJob: Job? = null
    private var geometryJob: Job? = null
    private var inputs = Channel<Input>(INPUT_CAPACITY)
    @Volatile private var process: Process? = null
    @Volatile private var socket: Socket? = null
    @Volatile private var vnc: VncClient? = null
    @Volatile private var cursorX = 0
    @Volatile private var cursorY = 0
    @Volatile private var cursorPlaced = false
    private val engineOutput = StringBuilder()
    @Volatile private var startedGeometry = ""
    @Volatile private var restarting = false
    private var autoRestarts = 0
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
        autoRestarts = AUTO_RESTARTS
        startedGeometry = geometry()
        startInput()
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
        geometryJob?.cancel()
        geometryJob = null
        job?.cancel()
        job = null
        stopInput()
        teardown()
        scope.launch(Dispatchers.IO) { GuestProcesses.killLeftovers() }
        mutable.value = SessionState.Stopped
    }

    fun syncGeometry() {
        if (mutable.value !is SessionState.Live) return
        geometryJob?.cancel()
        geometryJob = scope.launch {
            delay(GEOMETRY_DEBOUNCE_MS)
            val want = geometry()
            if (want == startedGeometry) return@launch
            if (!requestGeometry(want)) return@launch restartGeometry()
            startedGeometry = want
            val size = { frame: Frame? -> frame != null && "${frame.bitmap.width}x${frame.bitmap.height}" == want }
            var reached = withTimeoutOrNull(GEOMETRY_LIVE_MS) { frames.first { size(it) } }
            if (reached == null) {
                dropConnection()
                reached = withTimeoutOrNull(GEOMETRY_WAIT_MS) { frames.first { size(it) } }
            }
            if (reached == null) restartGeometry()
        }
    }

    private fun dropConnection() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun requestGeometry(size: String): Boolean = runCatching {
        guestTmp.mkdirs()
        val part = File(guestTmp, "geometry-request.part")
        part.writeText(size)
        part.renameTo(File(guestTmp, "geometry-request"))
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
        if (vnc == null) return
        inputs.trySend(Input.Move(x, y, buttons))
    }

    fun tap(x: Int, y: Int) {
        enqueue { client ->
            client.pointer(x, y, 1)
            delay(CLICK_MS)
            client.pointer(x, y, 0)
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
        if (vnc == null) return
        inputs.trySend(Input.Script(block))
    }

    private fun startInput() {
        inputJob?.cancel()
        inputs.close()
        val queue = Channel<Input>(INPUT_CAPACITY)
        inputs = queue
        inputJob = scope.launch(Dispatchers.IO) {
            for (first in queue) {
                var next: Input = first
                while (next is Input.Move) {
                    val more = queue.tryReceive().getOrNull()
                    if (more !is Input.Move || more.buttons != next.buttons) {
                        send(next)
                        next = more ?: break
                        continue
                    }
                    next = more
                }
                if (next is Input.Script) send(next)
            }
        }
    }

    private fun stopInput() {
        inputJob?.cancel()
        inputJob = null
        inputs.close()
    }

    private suspend fun send(input: Input) {
        val client = vnc ?: return
        runCatching {
            when (input) {
                is Input.Move -> client.pointer(input.x, input.y, input.buttons)
                is Input.Script -> input.block(client)
            }
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
        if (vnc == null) return
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
        runCatching { engine.installSessionScript(rootfs) }
            .onFailure { Log.w("palma-session", "script install skipped", it) }
        if (!engine.hasSessionScript(rootfs)) throw FailureException(Failure.SessionMissing)
        val mode = if (x11.available()) DisplayMode.X11 else DisplayMode.VNC
        vncPort = GuestPorts.free(ProotCommand.VNC_PORT)
        val started = engine.start(rootfs, mode, startedGeometry, vncPort)
        process = started
        scope.launch(Dispatchers.IO) {
            runCatching {
                started.inputStream.bufferedReader().forEachLine { line ->
                    synchronized(engineOutput) {
                        engineOutput.append(line).append('\n')
                        if (engineOutput.length > MAX_ENGINE_OUTPUT * 2) {
                            engineOutput.delete(0, engineOutput.length - MAX_ENGINE_OUTPUT)
                        }
                    }
                }
            }
        }
        delay(ENGINE_SETTLE_MS)
        if (!started.isAlive) {
            delay(OUTPUT_DRAIN_MS)
            val output = synchronized(engineOutput) { engineOutput.toString().trim().takeLast(MAX_ENGINE_OUTPUT) }
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
            val file = File(guestTmp, name)
            if (!file.isFile) return@mapNotNull null
            val text = runCatching { file.readText().trim().takeLast(LOG_TAIL) }.getOrNull().orEmpty()
            text.takeIf { it.isNotBlank() }?.let { "$name\n$it" }
        }.joinToString("\n")
        val blocks = listOf(engine, logs).filter { it.isNotBlank() }
        if (blocks.isEmpty()) return ""
        val headers = listOfNotNull(
            "— launch —".takeIf { engine.isNotBlank() },
            "— logs —".takeIf { logs.isNotBlank() },
        )
        return (headers + blocks).joinToString("\n").takeLast(MAX_ENGINE_OUTPUT)
    }

    private fun displayReport(): String {
        val engine = synchronized(engineOutput) { engineOutput.toString().trim().takeLast(MAX_ENGINE_OUTPUT) }
        val logs = DISPLAY_LOGS.mapNotNull { name ->
            val file = File(guestTmp, name)
            if (!file.isFile) return@mapNotNull null
            val text = runCatching { file.readText().trim().takeLast(LOG_TAIL) }.getOrNull().orEmpty()
            text.takeIf { it.isNotBlank() }?.let { "$name\n$it" }
        }.joinToString("\n")
        return listOf(engine, logs).filter { it.isNotBlank() }.joinToString("\n").takeLast(MAX_ENGINE_OUTPUT)
    }

    private suspend fun pump(name: String) = withContext(Dispatchers.IO) {
        while (isActive) {
            val client = vnc ?: return@withContext
            var fresh = true
            try {
                while (isActive) {
                    if (client.readMessage()) {
                        val fb = client.framebuffer
                        publish(client, fb)
                        if (fresh || blankState.value) blankState.value = fb.blank()
                        fresh = false
                        val size = "${fb.width}x${fb.height}"
                        if (size != loggedFrame) {
                            loggedFrame = size
                            Log.i(FRAME_TAG, "frame $size blank=${blankState.value}")
                        }
                    }
                }
                return@withContext
            } catch (error: Exception) {
                if (!isActive) return@withContext
                if (error is SocketTimeoutException && client.keepAlive()) {
                    continue
                }
                blankState.value = true
                runCatching { socket?.close() }
                socket = null
                vnc = null
                if (reattach()) continue
                if (process?.isAlive != true && autoRestarts > 0) {
                    autoRestarts -= 1
                    Log.i(FRAME_TAG, "session lost, restarting ($autoRestarts left)")
                    teardown()
                    scope.launch {
                        delay(RESTART_PAUSE_MS)
                        start()
                    }
                    return@withContext
                }
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
            val connected = runCatching { VncClient.connect(LOOPBACK, vncPort, CONNECT_TIMEOUT_MS) }.getOrNull()
            if (connected != null) {
                val (sock, client) = connected
                if (runCatching { client.handshake() }.isSuccess) {
                    socket = sock
                    vnc = client
                    return true
                }
                runCatching { sock.close() }
            }
            delay(RETRY_MS)
            waited += RETRY_MS
        }
        return false
    }

    private fun publish(client: VncClient, fb: Framebuffer) {
        val bitmap = surface(back, fb.width, fb.height)
        bitmap.copyPixelsFromBuffer(fb.source())
        back = 1 - back
        if (vnc === client) frames.value = Frame(bitmap, seq.incrementAndGet())
    }

    private fun surface(slot: Int, width: Int, height: Int): Bitmap {
        val current = surfaces[slot]
        if (current != null && current.allocationByteCount >= width * height * 4) {
            if (current.width != width || current.height != height) {
                current.reconfigure(width, height, Bitmap.Config.ARGB_8888)
            }
            return current
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { surfaces[slot] = it }
    }

    private fun teardown() {
        runCatching { socket?.close() }
        socket = null
        vnc = null
        cursorX = 0
        cursorY = 0
        cursorPlaced = false
        frames.value = null
        surfaces.fill(null)
        blankState.value = false
        process?.destroy()
        process = null
        x11.stop()
        GuestProcesses.killGuest()
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val ENGINE_SETTLE_MS = 800L
        const val DISPLAY_TIMEOUT_MS = 60_000L
        const val CONNECT_TIMEOUT_MS = 1_000
        const val REATTACH_TIMEOUT_MS = 60_000L
        const val RETRY_MS = 500L
        const val RESTART_PAUSE_MS = 800L
        const val AUTO_RESTARTS = 2
        const val GEOMETRY_WAIT_MS = 30_000L
        const val GEOMETRY_LIVE_MS = 6_000L
        const val GEOMETRY_DEBOUNCE_MS = 400L
        const val FRAME_TAG = "palma-vnc"
        const val MAX_ENGINE_OUTPUT = 1200
        const val OUTPUT_DRAIN_MS = 200L
        const val INPUT_CAPACITY = 256
        const val CLICK_MS = 40L
        const val TYPE_KEY_MS = 12L
        const val GROUP_SWITCH_MS = 30L
        const val GROUP_SETTLE_MS = 90L
        const val CYRILLIC_FIRST = 0x0400
        const val CYRILLIC_LAST = 0x04FF
        const val WHEEL_UP = 4
        const val WHEEL_DOWN = 5
        const val LOG_TAIL = 500
        const val DEFAULT_GEOMETRY = "1280x800"
        val DISPLAY_LOGS = listOf("trace.log", "xvnc.log", "xvfb-0.log", "xvfb-1.log", "x11vnc.log")
        val DIAGNOSTIC_LOGS = listOf("trace.log", "xvnc.log", "xvfb-0.log", "x11vnc.log", "xfce.log")
    }
}
