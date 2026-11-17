package com.ashishxhub.xhubrea

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BinaryManifestDecoderTest {
    @Test
    fun decodesMinimalManifestStringPoolAndElement() {
        val decoded = BinaryManifestDecoder.decode(minimalManifest())
        assertTrue(decoded.contains("<manifest>"))
        assertTrue(decoded.contains("</manifest>"))
        assertTrue(decoded.contains("App ki identity"))
    }

    @Test
    fun rejectsInputShorterThanXmlHeader() {
        assertFails { BinaryManifestDecoder.decode(byteArrayOf(3, 0, 8, 0)) }
    }

    @Test
    fun rejectsWrongDocumentType() {
        val data = minimalManifest()
        data[0] = 0x02
        assertFails { BinaryManifestDecoder.decode(data) }
    }

    @Test
    fun rejectsDocumentSizeBeyondInput() {
        val data = minimalManifest()
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putInt(4, data.size + 100)
        assertFails { BinaryManifestDecoder.decode(data) }
    }

    @Test
    fun rejectsChunkWhoseDeclaredSizeCrossesDocumentBoundary() {
        val data = minimalManifest()
        val poolSize = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(12)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putInt(8 + poolSize + 4, Int.MAX_VALUE)
        assertFails { BinaryManifestDecoder.decode(data) }
    }

    @Test
    fun rejectsStringPoolOffsetOutsideChunk() {
        val data = minimalManifest()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val poolStart = 8
        buffer.putInt(poolStart + 28, Int.MAX_VALUE)
        assertFails { BinaryManifestDecoder.decode(data) }
    }

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected malformed manifest to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: malformed input must be rejected before out-of-bounds access.
        } catch (_: IndexOutOfBoundsException) {
            fail("Parser accessed data outside validated bounds")
        }
    }

    private fun minimalManifest(): ByteArray {
        val pool = stringPool(listOf("manifest"))
        val start = startElement(0)
        val end = endElement(0)
        val totalSize = 8 + pool.size + start.size + end.size
        val out = ByteArrayOutputStream()
        out.writeShortLE(0x0003)
        out.writeShortLE(8)
        out.writeIntLE(totalSize)
        out.write(pool)
        out.write(start)
        out.write(end)
        return out.toByteArray()
    }

    private fun stringPool(values: List<String>): ByteArray {
        val payload = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        values.forEach { value ->
            offsets += payload.size()
            val bytes = value.toByteArray(Charsets.UTF_8)
            payload.write(bytes.size)
            payload.write(bytes.size)
            payload.write(bytes)
            payload.write(0)
        }
        while (payload.size() % 4 != 0) payload.write(0)
        val stringsStart = 28 + values.size * 4
        val size = stringsStart + payload.size()
        val out = ByteArrayOutputStream()
        out.writeShortLE(0x0001)
        out.writeShortLE(28)
        out.writeIntLE(size)
        out.writeIntLE(values.size)
        out.writeIntLE(0)
        out.writeIntLE(0x100)
        out.writeIntLE(stringsStart)
        out.writeIntLE(0)
        offsets.forEach(out::writeIntLE)
        out.write(payload.toByteArray())
        return out.toByteArray()
    }

    private fun startElement(nameIndex: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeShortLE(0x0102)
        out.writeShortLE(16)
        out.writeIntLE(36)
        out.writeIntLE(1)
        out.writeIntLE(-1)
        out.writeIntLE(-1)
        out.writeIntLE(nameIndex)
        out.writeShortLE(20)
        out.writeShortLE(20)
        out.writeShortLE(0)
        out.writeShortLE(0)
        out.writeShortLE(0)
        out.writeShortLE(0)
        return out.toByteArray()
    }

    private fun endElement(nameIndex: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.writeShortLE(0x0103)
        out.writeShortLE(16)
        out.writeIntLE(24)
        out.writeIntLE(2)
        out.writeIntLE(-1)
        out.writeIntLE(-1)
        out.writeIntLE(nameIndex)
        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.writeShortLE(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
    }

    private fun ByteArrayOutputStream.writeIntLE(value: Int) {
        write(value and 0xff)
        write((value ushr 8) and 0xff)
        write((value ushr 16) and 0xff)
        write((value ushr 24) and 0xff)
    }
}
