package com.ashishxhub.xhubrea

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/** Bounded read-only DEX identifier inspection. No bytecode is executed or disassembled. */
internal object DexIntelligenceEngine {
    private const val MAX_DEX = 64L * 1024 * 1024
    private const val MAX_PREVIEW = 500
    private val dexPattern = Regex("""classes(\d*)\.dex""")

    fun inspect(apk: File): String = ZipFile(apk).use { zip ->
        val entries = zip.entries().asSequence()
            .filter { !it.isDirectory && dexPattern.matches(it.name) }
            .sortedBy { it.name }.take(33).toList()
        require(entries.size <= 32) { "DEX files count safe limit (32) se zyada hai." }
        if (entries.isEmpty()) return@use "No DEX files were found in this APK."
        buildString {
            appendLine("DEX Intelligence Engine — read-only")
            appendLine("DEX files found: ${entries.size}")
            entries.forEach { entry ->
                appendLine()
                appendLine("===== ${entry.name} =====")
                if (entry.size !in 112..MAX_DEX) {
                    appendLine("Skipped: file size is outside the 112-byte to 64 MiB limit.")
                } else {
                    val bytes = zip.getInputStream(entry).use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            total += n
                            require(total <= MAX_DEX) { "DEX expanded beyond safe limit." }
                            out.write(buf, 0, n)
                        }
                        out.toByteArray()
                    }
                    append(inspectDex(bytes))
                }
            }
            appendLine()
            append("Identifiers are metadata clues only; this does not show method instructions or actual runtime behaviour.")
        }
    }

    private fun inspectDex(bytes: ByteArray): String {
        require(bytes.size >= 112) { "DEX header truncated." }
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun u32(p: Int): Long {
            require(p >= 0 && p.toLong() + 4 <= bytes.size) { "Field offset out of range." }
            return b.getInt(p).toLong() and 0xffffffffL
        }
        val magic = bytes.copyOfRange(0, 8).toString(Charsets.US_ASCII)
        require(magic.startsWith("dex\n") && magic[7] == '\u0000') { "Invalid DEX magic." }
        val fileSize = u32(32)
        val headerSize = u32(36)
        val endian = u32(40)
        require(headerSize == 112L && fileSize in 112..bytes.size.toLong() &&
            endian == 0x12345678L) { "DEX header validation failed." }

        fun table(sizeAt: Int, offAt: Int, width: Long): Pair<Long, Long> {
            val count = u32(sizeAt)
            val off = u32(offAt)
            val end = off + count * width
            require(count <= 1_000_000 && (count == 0L ||
                (off >= 112 && end >= off && end <= bytes.size.toLong()))) {
                "DEX table bounds invalid."
            }
            return count to off
        }
        val (stringCount, stringOff) = table(56, 60, 4)
        val (typeCount, typeOff) = table(64, 68, 4)
        val (protoCount, protoOff) = table(72, 76, 12)
        val (fieldCount, fieldOff) = table(80, 84, 8)
        val (methodCount, methodOff) = table(88, 92, 8)
        val (classCount, classOff) = table(96, 100, 32)

        val stringCache = HashMap<Int, String>()
        fun readString(index: Int): String {
            require(index >= 0 && index.toLong() < stringCount) { "String index out of range." }
            val offset = u32((stringOff + index * 4L).toInt())
            require(offset in 112 until bytes.size.toLong()) { "String data offset invalid." }
            var p = offset.toInt()
            var shift = 0
            while (true) {
                require(p < bytes.size && shift <= 28) { "Malformed ULEB128 string length." }
                val v = bytes[p++].toInt() and 255
                if (v and 128 == 0) break
                shift += 7
            }
            val start = p
            while (p < bytes.size && bytes[p].toInt() != 0) p++
            require(p < bytes.size && p - start <= 65536) { "Unterminated or oversized DEX string." }
            return bytes.copyOfRange(start, p).toString(Charsets.UTF_8).take(512)
        }
        fun str(index: Int) = stringCache.getOrPut(index) { readString(index) }
        fun type(index: Int): String {
            require(index >= 0 && index.toLong() < typeCount) { "Type index out of range." }
            return str(u32((typeOff + index * 4L).toInt()).toInt())
        }

        val strings = (0 until minOf(stringCount.toInt(), MAX_PREVIEW)).map { str(it) }
        val classes = (0 until minOf(classCount.toInt(), MAX_PREVIEW)).map { i ->
            type(u32((classOff + i * 32L).toInt()).toInt())
        }
        val fields = (0 until minOf(fieldCount.toInt(), MAX_PREVIEW)).map { i ->
            val p = (fieldOff + i * 8L).toInt()
            "${type(b.getShort(p).toInt() and 65535)} -> ${str(u32(p + 4).toInt())} : ${type(b.getShort(p + 2).toInt() and 65535)}"
        }
        val methods = (0 until minOf(methodCount.toInt(), MAX_PREVIEW)).map { i ->
            val p = (methodOff + i * 8L).toInt()
            val proto = b.getShort(p + 2).toInt() and 65535
            require(proto.toLong() < protoCount) { "Method prototype index out of range." }
            "${type(b.getShort(p).toInt() and 65535)} -> ${str(u32(p + 4).toInt())} (prototype #$proto)"
        }
        val duplicates = strings.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.take(50)

        return buildString {
            appendLine("DEX version: ${magic.substring(4, 7)}")
            appendLine("Actual ZIP entry bytes: ${bytes.size}")
            appendLine("Header file_size: $fileSize")
            appendLine("Header size: $headerSize")
            appendLine("Endian tag: 0x${endian.toString(16)}")
            appendLine("Adler32 checksum field: 0x${u32(8).toString(16)} (not verified)")
            appendLine("Map list offset: 0x${u32(52).toString(16)}")
            appendLine("Data section size: ${u32(104)}")
            appendLine("Data section offset: ${u32(108)}")
            appendLine()
            appendLine("DEX identifier tables")
            appendLine("String IDs: $stringCount | offset: 0x${stringOff.toString(16)} | bounds: OK")
            appendLine("Type IDs: $typeCount | offset: 0x${typeOff.toString(16)} | bounds: OK")
            appendLine("Prototype IDs: $protoCount | offset: 0x${protoOff.toString(16)} | bounds: OK")
            appendLine("Field IDs: $fieldCount | offset: 0x${fieldOff.toString(16)} | bounds: OK")
            appendLine("Method IDs: $methodCount | offset: 0x${methodOff.toString(16)} | bounds: OK")
            appendLine("Class definitions: $classCount | offset: 0x${classOff.toString(16)} | bounds: OK")
            appendLine("Header validation: basic checks passed")
            appendLine()
            appendLine("STRING IDENTIFIERS (preview ${strings.size}/$stringCount)")
            strings.forEach { appendLine(it.replace("\n", " ").take(200)) }
            appendLine()
            appendLine("CLASS DESCRIPTORS (preview ${classes.size}/$classCount)")
            classes.forEach { appendLine(it) }
            appendLine()
            appendLine("FIELD IDENTIFIERS (preview ${fields.size}/$fieldCount)")
            fields.forEach { appendLine(it) }
            appendLine()
            appendLine("METHOD IDENTIFIERS (preview ${methods.size}/$methodCount)")
            methods.forEach { appendLine(it) }
            appendLine()
            appendLine("Duplicate string values in displayed preview: ${duplicates.size}")
            duplicates.forEach { appendLine("DUPLICATE: ${it.take(160)}") }
            if (stringCount > MAX_PREVIEW || classCount > MAX_PREVIEW ||
                fieldCount > MAX_PREVIEW || methodCount > MAX_PREVIEW) {
                appendLine("Preview capped at $MAX_PREVIEW items per table to limit memory use.")
            }
        }
    }
}
