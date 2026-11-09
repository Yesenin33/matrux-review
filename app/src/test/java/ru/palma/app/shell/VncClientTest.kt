package ru.palma.app.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class VncClientTest {
    private fun serverScript(write: DataOutputStream.() -> Unit): DataInputStream {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).write()
        return DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
    }

    private fun DataOutputStream.init(width: Int, height: Int) {
        writeShort(width)
        writeShort(height)
        write(ByteArray(16))
        val name = "palma:1".toByteArray()
        writeInt(name.size)
        write(name)
    }

    @Test
    fun handshake38AndRawUpdate() {
        val input = serverScript {
            write("RFB 003.008\n".toByteArray())
            writeByte(1)
            writeByte(1)
            writeInt(0)
            init(2, 2)
            writeByte(0)
            writeByte(0)
            writeShort(1)
            writeShort(0); writeShort(0); writeShort(2); writeShort(1); writeInt(0)
            write(byteArrayOf(0x30, 0x20, 0x10, 0, 0x01, 0x02, 0x03, 0))
        }
        val sent = ByteArrayOutputStream()
        val client = VncClient(input, DataOutputStream(sent))
        client.handshake()
        assertEquals("palma:1", client.name)
        assertEquals(2, client.framebuffer.width)
        assertTrue(client.readMessage())
        assertEquals(0xFF102030.toInt(), client.framebuffer.pixels[0])
        assertEquals(0xFF030201.toInt(), client.framebuffer.pixels[1])
        val out = sent.toByteArray()
        assertEquals("RFB 003.008\n", String(out, 0, 12))
        assertEquals(1, out[12].toInt())
        assertEquals(1, out[13].toInt())
    }

    @Test
    fun handshake33() {
        val input = serverScript {
            write("RFB 003.003\n".toByteArray())
            writeInt(1)
            init(4, 3)
        }
        val client = VncClient(input, DataOutputStream(ByteArrayOutputStream()))
        client.handshake()
        assertEquals(4, client.framebuffer.width)
        assertEquals(3, client.framebuffer.height)
    }

    @Test
    fun passwordServerIsRefused() {
        val input = serverScript {
            write("RFB 003.008\n".toByteArray())
            writeByte(1)
            writeByte(2)
        }
        val error = runCatching { VncClient(input, DataOutputStream(ByteArrayOutputStream())).handshake() }.exceptionOrNull()
        assertEquals("auth_required", error?.message)
    }

    @Test
    fun copyRectMovesPixels() {
        val fb = Framebuffer(3, 1)
        fb.pixels[0] = 7
        fb.copyRect(0, 0, 2, 0, 1, 1)
        assertEquals(7, fb.pixels[2])
    }

    @Test
    fun outOfBoundsRectIsRejected() {
        val input = serverScript {
            write("RFB 003.008\n".toByteArray())
            writeByte(1); writeByte(1); writeInt(0)
            init(1, 1)
            writeByte(0); writeByte(0); writeShort(1)
            writeShort(0); writeShort(0); writeShort(5); writeShort(5); writeInt(0)
        }
        val client = VncClient(input, DataOutputStream(ByteArrayOutputStream()))
        client.handshake()
        assertEquals("rect_out_of_bounds", runCatching { client.readMessage() }.exceptionOrNull()?.message)
    }
}
