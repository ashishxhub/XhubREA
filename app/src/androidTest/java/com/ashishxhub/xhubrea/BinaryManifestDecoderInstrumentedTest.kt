package com.ashishxhub.xhubrea

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Boundary and malformed-input fixtures for the read-only AXML decoder.
 * These tests intentionally use synthetic bytes; no APK is installed or executed.
 */
class BinaryManifestDecoderInstrumentedTest {

    @Test
    fun rejectsInputShorterThanXmlHeader() {
        assertRejected(ByteArray(7))
    }

    @Test
    fun rejectsNonXmlChunkType() {
        val data = xmlHeader(totalSize = 8)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putShort(0, 0x0001)
        assertRejected(data)
    }

    @Test
    fun rejectsDocumentSizeBeyondAvailableBytes() {
        assertRejected(xmlHeader(totalSize = 64))
    }

    @Test
    fun rejectsXmlHeaderSizeBelowMinimum() {
        val data = xmlHeader(totalSize = 8)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putShort(2, 4)
        assertRejected(data)
    }

    @Test
    fun rejectsChunkThatExtendsPastDocumentBoundary() {
        val data = xmlHeader(totalSize = 8)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 12)
        assertRejected(data)
    }

    @Test
    fun rejectsManifestAboveConfiguredMaximum() {
        val oversized = ByteArray(8 * 1024 * 1024 + 1)
        assertRejected(oversized)
    }


    @Test
    fun rejectsStringPoolWithOutOfRangeStringOffset() {
        // XML header (8 bytes) followed by a string-pool chunk whose sole
        // string offset points beyond the declared chunk boundary.
        val data = ByteArray(48)
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        b.putShort(0, 0x0003) // RES_XML_TYPE
        b.putShort(2, 8)      // XML header size
        b.putInt(4, 48)       // XML document size

        b.putShort(8, 0x0001) // RES_STRING_POOL_TYPE
        b.putShort(10, 28)    // string-pool header size
        b.putInt(12, 40)      // chunk size
        b.putInt(16, 1)       // string count
        b.putInt(24, 32)      // stringsStart relative to chunk
        b.putInt(36, 100)     // invalid string offset

        assertRejected(data)
    }
\n    private fun xmlHeader(totalSize: Int): ByteArray =
        ByteArray(8).also { data ->
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).apply {
                putShort(0, 0x0003)
                putShort(2, 8)
                putInt(4, totalSize)
            }
        }

    private fun assertRejected(data: ByteArray) {
        var rejected = false
        try {
            BinaryManifestDecoder.decode(data)
        } catch (expected: IllegalArgumentException) {
            rejected = true
        } catch (expected: Exception) {
            rejected = true
        }
        assertTrue("Malformed binary manifest input must be rejected.", rejected)
    }
}
