package ru.matrux.app.learn

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GuestProgramsTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun findsInterpreterInsideRootfs() {
        val root = folder.newFolder("rootfs")
        File(root, "usr/bin").mkdirs()
        File(root, "usr/bin/python3").writeText("")
        File(root, "bin").mkdirs()
        File(root, "bin/sh").writeText("")
        assertEquals("/usr/bin/python3", GuestPrograms.find(root, Runtime.PYTHON))
        assertEquals("/bin/sh", GuestPrograms.find(root, Runtime.SH))
        assertNull(GuestPrograms.find(root, Runtime.BASH))
        assertNull(GuestPrograms.find(root, Runtime.NODE))
    }

    @Test
    fun runnerReportsMissingSystemAndProgram() = runTest {
        val noSystem = GuestRunner(folder.root, folder.newFolder("tmp1"), rootfs = { null })
        assertEquals(RunOutcome.NoSystem, noSystem.run(Runtime.PYTHON, "print(1)", null))
        val empty = folder.newFolder("empty")
        val noPython = GuestRunner(folder.root, folder.newFolder("tmp2"), rootfs = { empty })
        assertEquals(RunOutcome.Missing("python3"), noPython.run(Runtime.PYTHON, "print(1)", null))
    }
}
