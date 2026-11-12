package ru.matrux.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/**
 * Достаёт иконку из PE-файла (.exe) без сторонних библиотек:
 * идём по каталогу ресурсов PE → RT_GROUP_ICON → самая крупная запись → RT_ICON.
 * PNG-иконки возвращаются как есть, DIB-иконки оборачиваются в BMP и пережимаются в PNG.
 */
object PeIcon {

    private const val RT_ICON = 3
    private const val RT_GROUP_ICON = 14

    fun extractPng(exe: File): ByteArray? = runCatching { extract(exe) }.getOrNull()

    private fun extract(exe: File): ByteArray? {
        RandomAccessFile(exe, "r").use { raf ->
            if (raf.length() < 0x40) return null
            if (readU16(raf, 0) != 0x5A4D) return null // MZ

            val peOffset = readU32(raf, 0x3C)
            if (peOffset <= 0 || peOffset + 24 > raf.length()) return null
            if (readU32(raf, peOffset) != 0x00004550) return null // PE\0\0

            val sectionsCount = readU16(raf, peOffset + 6)
            val optSize = readU16(raf, peOffset + 20)
            val optStart = peOffset + 24L
            val magic = readU16(raf, optStart)
            val ddStart = optStart + if (magic == 0x10B) 96 else 112
            val resRva = readU32(raf, ddStart + 2 * 8L)
            if (resRva == 0L) return null

            val sections = (0 until sectionsCount).map { i ->
                val base = optStart + optSize + i * 40L
                Section(
                    virtualAddress = readU32(raf, base + 12),
                    virtualSize = readU32(raf, base + 8),
                    rawSize = readU32(raf, base + 16),
                    rawPointer = readU32(raf, base + 20),
                )
            }

            fun offsetOf(rva: Long): Long? {
                val size = sections.size
                for (s in sections) {
                    val span = maxOf(s.virtualSize, s.rawSize)
                    if (rva >= s.virtualAddress && rva < s.virtualAddress + span) {
                        return rva - s.virtualAddress + s.rawPointer
                    }
                }
                return if (size == 0) null else null
            }

            val rootOffset = offsetOf(resRva) ?: return null
            val groupId = firstIdOfType(raf, rootOffset, RT_GROUP_ICON) ?: return null
            val groupData = dataEntry(raf, rootOffset, RT_GROUP_ICON, groupId) ?: return null
            val groupBytes = readBytes(raf, offsetOf(groupData.first) ?: return null, groupData.second)
            val iconId = bestIconId(groupBytes) ?: return null
            val iconData = dataEntry(raf, rootOffset, RT_ICON, iconId) ?: return null
            val raw = readBytes(raf, offsetOf(iconData.first) ?: return null, iconData.second)
            return toPng(raw)
        }
    }

    private data class Section(val virtualAddress: Long, val virtualSize: Long, val rawSize: Long, val rawPointer: Long)

    /** Ресурс верхнего уровня: набор записей «тип → имя → язык → данные». */
    private fun firstIdOfType(raf: RandomAccessFile, rootOffset: Long, type: Int): Int? =
        childIds(raf, rootOffset, rootOffset, type).firstOrNull()

    private fun childIds(raf: RandomAccessFile, rootOffset: Long, dirOffset: Long, wantedId: Int): List<Int> {
        val named = readU16(raf, dirOffset + 12).toInt()
        val ids = readU16(raf, dirOffset + 14).toInt()
        val entries = dirOffset + 16
        val result = mutableListOf<Int>()
        for (i in 0 until named + ids) {
            val entry = entries + i * 8L
            val nameOrId = readU32(raf, entry)
            if (nameOrId > 0x7FFF_FFFF) continue // строковое имя — пропускаем
            if (nameOrId.toInt() != wantedId) continue
            val sub = readU32(raf, entry + 4)
            if (sub and 0x8000_0000L == 0L) continue
            val childDir = rootOffset + (sub and 0x7FFF_FFFFL)
            val childNamed = readU16(raf, childDir + 12).toInt()
            val childIds = readU16(raf, childDir + 14).toInt()
            for (j in 0 until childNamed + childIds) {
                val id = readU32(raf, childDir + 16 + j * 8L)
                if (id <= 0x7FFF_FFFF) result += id.toInt()
            }
        }
        return result
    }

    /** Идёт по уровням «тип → имя → язык» и возвращает (RVA, размер) данных. */
    private fun dataEntry(raf: RandomAccessFile, rootOffset: Long, type: Int, name: Int): Pair<Long, Long>? {
        val dir = descend(raf, rootOffset, rootOffset, type) ?: return null
        val nameDir = descend(raf, rootOffset, dir, name) ?: return null
        val langs = readU16(raf, nameDir + 12).toInt() + readU16(raf, nameDir + 14).toInt()
        if (langs == 0) return null
        val sub = readU32(raf, nameDir + 16 + 4)
        if (sub and 0x8000_0000L != 0L) return null
        val dataEntry = rootOffset + sub
        val rva = readU32(raf, dataEntry)
        val size = readU32(raf, dataEntry + 4)
        return rva to size
    }

    /** Возвращает смещение дочернего каталога с указанным id. */
    private fun descend(raf: RandomAccessFile, rootOffset: Long, dirOffset: Long, id: Int): Long? {
        val named = readU16(raf, dirOffset + 12).toInt()
        val ids = readU16(raf, dirOffset + 14).toInt()
        for (i in 0 until named + ids) {
            val entry = dirOffset + 16 + i * 8L
            val nameOrId = readU32(raf, entry)
            if (nameOrId > 0x7FFF_FFFF || nameOrId.toInt() != id) continue
            val sub = readU32(raf, entry + 4)
            if (sub and 0x8000_0000L == 0L) return null
            return rootOffset + (sub and 0x7FFF_FFFFL)
        }
        return null
    }

    /** В GRPICONDIR выбирает запись с наибольшей площадью и возвращает её nID. */
    private fun bestIconId(group: ByteArray): Int? {
        if (group.size < 6) return null
        val count = u16(group, 4)
        var bestId: Int? = null
        var bestArea = -1
        for (i in 0 until count) {
            val base = 6 + i * 14
            if (base + 14 > group.size) break
            val w = group[base].toInt() and 0xFF
            val h = group[base + 1].toInt() and 0xFF
            val area = (if (w == 0) 256 else w) * (if (h == 0) 256 else h)
            val id = u16(group, base + 12)
            if (area > bestArea) {
                bestArea = area
                bestId = id
            }
        }
        return bestId
    }

    private fun toPng(raw: ByteArray): ByteArray? {
        if (raw.size > 8 && raw[0] == 0x89.toByte() && raw[1] == 'P'.code.toByte()) return raw

        // DIB: BITMAPINFOHEADER + палитра + XOR-битмап + AND-маска
        if (raw.size < 40 + 14) return null
        val headerSize = u32(raw, 0)
        if (headerSize < 40 || headerSize > raw.size) return null
        val bitCount = u16(raw, 14)
        val palette = if (bitCount <= 8) (1 shl bitCount) * 4 else 0
        val bitmapSize = raw.size + 14

        val bmp = ByteArray(bitmapSize)
        bmp[0] = 'B'.code.toByte()
        bmp[1] = 'M'.code.toByte()
        writeU32(bmp, 2, bitmapSize)
        val dataOffset = 14 + headerSize + palette
        writeU32(bmp, 10, dataOffset)
        raw.copyInto(bmp, 14)

        // высота DIB = XOR + AND, для BMP её надо удвоить
        val height = u32(bmp, 14 + 8)
        writeU32(bmp, 14 + 8, height * 2)

        val bitmap = BitmapFactory.decodeByteArray(bmp, 0, bmp.size) ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun readBytes(raf: RandomAccessFile, offset: Long, size: Long): ByteArray {
        if (size <= 0 || size > 4_000_000 || offset < 0 || offset + size > raf.length()) {
            return ByteArray(0)
        }
        val bytes = ByteArray(size.toInt())
        raf.seek(offset)
        raf.readFully(bytes)
        return bytes
    }

    private fun readU16(raf: RandomAccessFile, offset: Long): Int {
        raf.seek(offset)
        return raf.readUnsignedByte() or (raf.readUnsignedByte() shl 8)
    }

    private fun readU32(raf: RandomAccessFile, offset: Long): Long {
        raf.seek(offset)
        val b0 = raf.readUnsignedByte().toLong()
        val b1 = raf.readUnsignedByte().toLong()
        val b2 = raf.readUnsignedByte().toLong()
        val b3 = raf.readUnsignedByte().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun u32(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun writeU32(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
}
