package com.ashishxhub.xhubrea

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceTableDecoderTest {
    @Test fun decodesPackageAndTypeMetadata() {
        val report = ResourceTableDecoder.decode(minimalTable())
        assertEquals(1, report.packages.size)
        assertEquals("com.example.demo", report.packages[0].name)
        assertEquals(0x7fL, report.packages[0].id)
        assertEquals(1, report.packages[0].types.size)
        assertEquals("string", report.packages[0].types[0].name)
        assertEquals(1, report.packages[0].types[0].entryCount)
        assertTrue(report.packages[0].types[0].configurations >= 1)
    }

    @Test fun rejectsWrongRootChunk() {
        val bytes = minimalTable()
        bytes[0] = 0x03
        assertFails { ResourceTableDecoder.decode(bytes) }
    }

    @Test fun rejectsRootSizeBeyondInput() {
        val bytes = minimalTable()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, bytes.size + 50)
        assertFails { ResourceTableDecoder.decode(bytes) }
    }

    @Test fun rejectsPackageCountMismatch() {
        val bytes = minimalTable()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(8, 2)
        assertFails { ResourceTableDecoder.decode(bytes) }
    }

    @Test fun rejectsInvalidTypeEntryOffset() {
        val bytes = minimalTable()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val packageStart = 12
        val packageHeader = 288
        val typePool = stringPool(listOf("string"))
        val keyPool = stringPool(listOf("app_name"))
        val typeChunkStart = packageStart + packageHeader + typePool.size + keyPool.size
        buffer.putInt(typeChunkStart + 24, Int.MAX_VALUE)
        assertFails { ResourceTableDecoder.decode(bytes) }
    }

    private fun assertFails(block: () -> Unit) {
        try { block(); throw AssertionError("Expected invalid resource table to fail") }
        catch (_: IllegalArgumentException) { }
        catch (_: IndexOutOfBoundsException) { throw AssertionError("Parser read outside validated bounds") }
    }

    private fun minimalTable(): ByteArray {
        val typePool = stringPool(listOf("string"))
        val keyPool = stringPool(listOf("app_name"))
        val spec = ByteArrayOutputStream().apply {
            writeShortLE(0x0202); writeShortLE(16); writeIntLE(20)
            write(1); write(0); writeShortLE(0); writeIntLE(1); writeIntLE(0)
        }.toByteArray()
        val type = ByteArrayOutputStream().apply {
            writeShortLE(0x0201); writeShortLE(24); writeIntLE(36)
            write(1); write(0); writeShortLE(0); writeIntLE(1); writeIntLE(28)
            writeIntLE(4); writeIntLE(0)
            writeShortLE(8); writeShortLE(0); writeIntLE(0)
        }.toByteArray()
        val packageSize = 288 + typePool.size + keyPool.size + spec.size + type.size
        val pkg = ByteArrayOutputStream().apply {
            writeShortLE(0x0200); writeShortLE(288); writeIntLE(packageSize)
            writeIntLE(0x7f)
            val name = "com.example.demo".toCharArray()
            repeat(128) { index -> writeShortLE(if (index < name.size) name[index].code else 0) }
            writeIntLE(288); writeIntLE(0)
            writeIntLE(288 + typePool.size); writeIntLE(0); writeIntLE(0)
            write(typePool); write(keyPool); write(spec); write(type)
        }.toByteArray()
        val total = 12 + pkg.size
        return ByteArrayOutputStream().apply {
            writeShortLE(0x0002); writeShortLE(12); writeIntLE(total); writeIntLE(1)
            write(pkg)
        }.toByteArray()
    }

    private fun stringPool(values: List<String>): ByteArray {
        val payload = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        values.forEach { value ->
            offsets += payload.size()
            val bytes = value.toByteArray(Charsets.UTF_8)
            payload.write(bytes.size); payload.write(bytes.size); payload.write(bytes); payload.write(0)
        }
        while (payload.size() % 4 != 0) payload.write(0)
        val stringsStart = 28 + values.size * 4
        return ByteArrayOutputStream().apply {
            writeShortLE(1); writeShortLE(28); writeIntLE(stringsStart + payload.size())
            writeIntLE(values.size); writeIntLE(0); writeIntLE(0x100)
            writeIntLE(stringsStart); writeIntLE(0)
            offsets.forEach { writeIntLE(it) }
            write(payload.toByteArray())
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeShortLE(value: Int) {
        write(value and 0xff); write((value ushr 8) and 0xff)
    }
    private fun ByteArrayOutputStream.writeIntLE(value: Int) {
        write(value and 0xff); write((value ushr 8) and 0xff)
        write((value ushr 16) and 0xff); write((value ushr 24) and 0xff)
    }
}
