package ru.matrux.app.shell

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.matrux.app.data.Failure
import ru.matrux.app.data.FailureException
import java.io.File

class LaunchSequenceTest {
    private val calls = mutableListOf<String>()

    private fun sequence(
        online: Boolean = true,
        engine: () -> Unit = {},
        display: () -> DisplayMode = { DisplayMode.X11 },
    ) = LaunchSequence(
        checkSubscription = { calls += "sub"; online },
        prepareRootfs = { calls += "rootfs" },
        startEngine = { calls += "proot"; engine() },
        startDisplay = { calls += "display"; display() },
    )

    @Test
    fun runsAllStepsInOrder() = runTest {
        val states = sequence().run().toList()
        assertEquals(listOf("sub", "rootfs", "proot", "display"), calls)
        assertEquals(
            LaunchStep.entries.map { LaunchState.Running(it) } + LaunchState.Ready(DisplayMode.X11, offline = false),
            states,
        )
    }

    @Test
    fun stopsAtFailedStepWithReason() = runTest {
        val states = sequence(engine = { throw FailureException(Failure.EngineMissing) }).run().toList()
        assertEquals(LaunchState.Failed(LaunchStep.PROOT, Failure.EngineMissing), states.last())
        assertEquals(listOf("sub", "rootfs", "proot"), calls)
    }

    @Test
    fun offlineLaunchIsMarked() = runTest {
        val last = sequence(online = false, display = { DisplayMode.VNC }).run().toList().last()
        assertEquals(LaunchState.Ready(DisplayMode.VNC, offline = true), last)
    }

    @Test
    fun noAccessStopsLaunchBeforeRootfs() = runTest {
        val sequence = LaunchSequence(
            checkSubscription = { throw FailureException(Failure.NoAccess) },
            prepareRootfs = { calls += "rootfs" },
            startEngine = { calls += "proot" },
            startDisplay = { calls += "display"; DisplayMode.X11 },
        )
        val states = sequence.run().toList()
        assertEquals(LaunchState.Failed(LaunchStep.SUBSCRIPTION, Failure.NoAccess), states.last())
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun unexpectedErrorIsReported() = runTest {
        val last = sequence(display = { throw java.net.ConnectException() }).run().toList().last()
        assertEquals(LaunchState.Failed(LaunchStep.DISPLAY, Failure.NoConnection), last)
    }

    @Test
    fun prootCommandBindsRootfsAndRunsSessionScript() {
        val layout = ProotLayout(File("/lib/libproot.so"), File("/lib/libproot-loader.so"), File("/data/systems/debian"), File("/cache/proot"))
        val args = ProotCommand.arguments(layout, DisplayMode.VNC, "720x1600", 5901)
        assertEquals("/lib/libproot.so", args.first())
        assertEquals("/data/systems/debian", args[args.indexOf("-r") + 1])
        assertEquals(listOf("/bin/sh", ProotCommand.SESSION_SCRIPT, "vnc"), args.takeLast(3))
        assertEquals(true, args.contains("--link2symlink"))
        assertEquals("/lib/libproot-loader.so", ProotCommand.environment(layout)["PROOT_LOADER"])
        assertEquals(true, args.contains("MATRUX_GEOMETRY=720x1600"))
    }

    @Test
    fun engineIsMissingWithoutBinary() {
        val dir = kotlin.io.path.createTempDirectory().toFile()
        assertEquals(false, ProotEngine(dir, dir).available())
        assertEquals(false, NativeX11Server(dir).available())
    }
}
