package com.ashishxhub.xhubrea

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * Read-only structural inspection of DEX files inside an APK.
 * It reports table sizes and offsets; it does not execute or modify APK content.
 */
internal object AdvancedDexAnalyzer {
    private const val HEADER_SIZE = 112
    private const val MAX_DEX_BYTES = 64L * 1024L * 1024L
    private val dexName = Regex("""classes(\d*)\.dex""")

    fun inspect(apkFile: File): String = runCatching {
        ZipFile(apkFile).use { zip ->
            val dexEntries = zip.entries().asSequence()
                .filter { !it.isDirectory && dexName.matches(it.name) }
                .sortedBy { it.name }
                .toList()

            if (dexEntries.isEmpty()) return@use "No DEX files were found in this APK."

            buildString {
                appendLine("Advanced DEX Explorer — read-only")
                appendLine("DEX files found: ${dexEntries.size}")
                appendLine("Inspection: structural metadata only; no code is executed or modified.")
                dexEntries.forEachIndexed { index, entry ->
                    appendLine()
                    appendLine("===== ${entry.name} =====")
                    if (entry.size > MAX_DEX_BYTES) {
                        appendLine("Skipped: file exceeds the 64 MiB inspection limit.")
                    } else {
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        append(inspectDexBytes(bytes))
                    }
                }
            }
        }
    }.getOrElse { error ->
        "DEX inspection failed: ${error.message ?: "Unknown error"}"
    }

    private fun inspectDexBytes(bytes: ByteArray): String {
        if (bytes.size < HEADER_SIZE) return "Invalid DEX: file is smaller than the 112-byte header."
        val magic = bytes.copyOfRange(0, 8).toString(Charsets.US_ASCII)
        if (!magic.startsWith("dex\n") || magic[7] != '\u0000') {
            return "Invalid or unrecognized DEX magic: ${magic.replace("\u0000", "")}"
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun u32(offset: Int): Long = buffer.getInt(offset).toLong() and 0xffffffffL
        fun table(label: String, sizeOffset: Int, dataOffset: Int, itemWidth: Long): String {
            val count = u32(sizeOffset)
            val offset = u32(dataOffset)
            if (count == 0L) return "$label: 0"
            val end = offset + count * itemWidth
            val valid = offset >= HEADER_SIZE && end >= offset && end <= bytes.size.toLong()
            return "$label: $count | offset: 0x${offset.toString(16)} | bounds: ${if (valid) "OK" else "OUT OF RANGE"}"
        }

        val fileSize = u32(32)
        val headerSize = u32(36)
        val endianTag = u32(40)
        val checksum = u32(8)
        val mapOffset = u32(52)
        val dataSize = u32(104)
        val dataOffset = u32(108)

        return buildString {
            appendLine("DEX version: ${magic.substring(4, 7)}")
            appendLine("Actual ZIP entry bytes: ${bytes.size}")
            appendLine("Header file_size: $fileSize")
            appendLine("Header size: $headerSize")
            appendLine("Endian tag: 0x${endianTag.toString(16)}")
            appendLine("Adler32 checksum field: 0x${checksum.toString(16)} (not verified)")
            appendLine("Map list offset: 0x${mapOffset.toString(16)}")
            appendLine("Data section size: $dataSize")
            appendLine("Data section offset: 0x${dataOffset.toString(16)}")
            appendLine()
            appendLine("DEX identifier tables")
            appendLine(table("String IDs", 56, 60, 4))
            appendLine(table("Type IDs", 64, 68, 4))
            appendLine(table("Prototype IDs", 72, 76, 12))
            appendLine(table("Field IDs", 80, 84, 8))
            appendLine(table("Method IDs", 88, 92, 8))
            appendLine(table("Class definitions", 96, 100, 32))
            appendLine()
            val headerValid = headerSize == HEADER_SIZE.toLong() &&
                fileSize <= bytes.size.toLong() && fileSize >= HEADER_SIZE &&
                (endianTag == 0x12345678L || endianTag == 0x78563412L)
            appendLine("Header validation: ${if (headerValid) "basic checks passed" else "one or more basic checks failed"}")
            if (fileSize != bytes.size.toLong()) {
                appendLine("Note: header file_size differs from the ZIP entry's uncompressed byte count.")
            }
            append("This view reports table counts and bounds only; it does not disassemble methods or expose string contents.")
        }
    }
}
