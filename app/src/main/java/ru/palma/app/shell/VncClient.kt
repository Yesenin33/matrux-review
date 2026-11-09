package ru.palma.app.shell

import android.graphics.Bitmap
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

class VncException(message: String) : IOException(message)

class Framebuffer(val width: Int, val height: Int) {
    val buffer: ByteBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
    val pixels: IntBuffer = buffer.asIntBuffer()
    private var image: Bitmap? = null

    fun frameBitmap(): Bitmap {
        val current = image ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { image = it }
        val source = buffer.duplicate()
        source.rewind()
        current.copyPixelsFromBuffer(source)
        return current
    }

    fun blank(): Boolean {
        if (pixels.capacity() == 0) return true
        var i = 0
        while (i < pixels.capacity()) {
            if (pixels.get(i) and 0x00FFFFFF != 0) return false
            i += 17
        }
        return true
    }

    private var raw = ByteArray(0)
    private var packed = ByteArray(0)

    fun rawRow(size: Int): ByteArray {
        if (raw.size < size) raw = ByteArray(size)
        return raw
    }

    fun packRow(size: Int): ByteArray {
        if (packed.size < size) packed = ByteArray(size)
        return packed
    }

    fun writeRow(y: Int, x: Int, row: ByteArray, size: Int) {
        val out = packRow(size)
        var i = 0
        while (i < size) {
            out[i] = row[i + 2]
            out[i + 1] = row[i + 1]
            out[i + 2] = row[i]
            out[i + 3] = -1
            i += 4
        }
        val view = buffer
        view.position((y * width + x) * 4)
        view.put(out, 0, size)
    }

    fun copyRect(srcX: Int, srcY: Int, x: Int, y: Int, w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        if (srcX + w > width || srcY + h > height || x + w > width || y + h > height) return
        val size = w * 4
        val row = packRow(size)
        val view = buffer
        val down = y > srcY
        for (n in 0 until h) {
            val r = if (down) h - 1 - n else n
            view.position(((srcY + r) * width + srcX) * 4)
            view.get(row, 0, size)
            view.position(((y + r) * width + x) * 4)
            view.put(row, 0, size)
        }
    }
}

class VncClient(private val input: DataInputStream, private val output: DataOutputStream) {
    lateinit var framebuffer: Framebuffer
        private set
    var name: String = ""
        private set
    private var sizeChanged = false

    fun handshake() {
        val version = ByteArray(12)
        input.readFully(version)
        val text = String(version, Charsets.US_ASCII)
        if (!text.startsWith("RFB ")) throw VncException("not_rfb")
        val minor = text.substring(8, 11).toIntOrNull() ?: throw VncException("bad_version")
        val use38 = minor >= 8
        output.write((if (use38) "RFB 003.008\n" else "RFB 003.003\n").toByteArray(Charsets.US_ASCII))
        output.flush()
        if (use38) {
            val count = input.readUnsignedByte()
            if (count == 0) throw VncException(readReason())
            val types = ByteArray(count).also { input.readFully(it) }
            if (types.none { it.toInt() == SECURITY_NONE }) throw VncException("auth_required")
            output.writeByte(SECURITY_NONE)
            output.flush()
            if (input.readInt() != 0) throw VncException(readReason())
        } else {
            when (input.readInt()) {
                SECURITY_NONE -> Unit
                0 -> throw VncException(readReason())
                else -> throw VncException("auth_required")
            }
        }
        output.writeByte(1)
        output.flush()
        val width = input.readUnsignedShort()
        val height = input.readUnsignedShort()
        input.skipBytes(16)
        name = String(ByteArray(input.readInt()).also { input.readFully(it) }, Charsets.UTF_8)
        framebuffer = Framebuffer(width, height)
        setPixelFormat()
        setEncodings()
        requestUpdate(incremental = false)
    }

    fun readMessage(): Boolean {
        when (val type = input.readUnsignedByte()) {
            0 -> {
                input.readUnsignedByte()
                repeat(input.readUnsignedShort()) { readRect() }
                val full = sizeChanged
                sizeChanged = false
                requestUpdate(incremental = !full)
                return true
            }
            1 -> {
                input.readUnsignedByte()
                input.readUnsignedShort()
                input.skipBytes(input.readUnsignedShort() * 6)
            }
            2 -> Unit
            3 -> {
                input.skipBytes(3)
                input.skipBytes(input.readInt())
            }
            else -> throw VncException("unknown_message_$type")
        }
        return false
    }

    fun pointer(x: Int, y: Int, buttons: Int) = synchronized(output) {
        output.writeByte(5)
        output.writeByte(buttons)
        output.writeShort(x.coerceIn(0, framebuffer.width - 1))
        output.writeShort(y.coerceIn(0, framebuffer.height - 1))
        output.flush()
    }

    fun key(keysym: Int, down: Boolean) = synchronized(output) {
        output.writeByte(4)
        output.writeByte(if (down) 1 else 0)
        output.writeShort(0)
        output.writeInt(keysym)
        output.flush()
    }

    private fun resize(w: Int, h: Int): Framebuffer {
        if (w == framebuffer.width && h == framebuffer.height) return framebuffer
        sizeChanged = true
        val next = Framebuffer(w, h)
        framebuffer = next
        return next
    }

    private fun readRect() {
        val x = input.readUnsignedShort()
        val y = input.readUnsignedShort()
        val w = input.readUnsignedShort()
        val h = input.readUnsignedShort()
        when (val encoding = input.readInt()) {
            ENCODING_RAW -> {
                val fb = framebuffer
                val size = w * 4
                if (x + w > fb.width || y + h > fb.height) {
                    var left = w.toLong() * h * 4
                    while (left > 0) {
                        val moved = input.skip(left)
                        if (moved <= 0L) throw java.io.EOFException()
                        left -= moved
                    }
                } else {
                    val row = fb.rawRow(size)
                    for (line in 0 until h) {
                        input.readFully(row, 0, size)
                        fb.writeRow(y + line, x, row, size)
                    }
                }
            }
            ENCODING_COPYRECT -> framebuffer.copyRect(input.readUnsignedShort(), input.readUnsignedShort(), x, y, w, h)
            ENCODING_DESKTOP_SIZE -> resize(w, h)
            ENCODING_EXT_DESKTOP_SIZE -> {
                val screens = input.readUnsignedByte()
                input.skipBytes(3 + SCREEN_BYTES * screens)
                resize(w, h)
            }
            else -> throw VncException("unsupported_encoding_$encoding")
        }
    }

    private fun setPixelFormat() = synchronized(output) {
        output.writeByte(0)
        output.write(ByteArray(3))
        output.writeByte(32)
        output.writeByte(24)
        output.writeByte(0)
        output.writeByte(1)
        output.writeShort(255)
        output.writeShort(255)
        output.writeShort(255)
        output.writeByte(16)
        output.writeByte(8)
        output.writeByte(0)
        output.write(ByteArray(3))
        output.flush()
    }

    private fun setEncodings() = synchronized(output) {
        output.writeByte(2)
        output.writeByte(0)
        output.writeShort(3)
        output.writeInt(ENCODING_COPYRECT)
        output.writeInt(ENCODING_RAW)
        output.writeInt(ENCODING_DESKTOP_SIZE)
        output.flush()
    }

    private fun requestUpdate(incremental: Boolean) = synchronized(output) {
        output.writeByte(3)
        output.writeByte(if (incremental) 1 else 0)
        output.writeShort(0)
        output.writeShort(0)
        output.writeShort(framebuffer.width)
        output.writeShort(framebuffer.height)
        output.flush()
    }

    private fun readReason(): String {
        val length = input.readInt()
        if (length !in 0..4096) return "rejected"
        return String(ByteArray(length).also { input.readFully(it) }, Charsets.UTF_8)
    }

    companion object {
        private const val SECURITY_NONE = 1
        private const val ENCODING_RAW = 0
        private const val ENCODING_COPYRECT = 1
        private const val ENCODING_DESKTOP_SIZE = -223
        private const val ENCODING_EXT_DESKTOP_SIZE = -308
        private const val SCREEN_BYTES = 16

        fun connect(host: String, port: Int, timeoutMs: Int): Pair<Socket, VncClient> {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.tcpNoDelay = true
            val client = VncClient(
                DataInputStream(BufferedInputStream(socket.getInputStream())),
                DataOutputStream(BufferedOutputStream(socket.getOutputStream())),
            )
            return socket to client
        }
    }
}