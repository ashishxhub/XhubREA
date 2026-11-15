package com.ashishxhub.xhubrea

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * Bounded, read-only Android binary XML (AXML) manifest reader.
 * It reports decoded declarations, not security verdicts.
 */
internal object BinaryManifestDecoder {
    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102
    private const val RES_XML_END_ELEMENT_TYPE = 0x0103
    private const val RES_XML_RESOURCE_MAP_TYPE = 0x0180
    private const val RES_XML_TYPE = 0x0003
    private const val MAX_MANIFEST_BYTES = 8 * 1024 * 1024
    private const val NO_INDEX = -1

    fun decodeApk(apk: java.io.File): String {
        val bytes = ZipFile(apk).use { zip ->
            val entry = zip.getEntry("AndroidManifest.xml")
                ?: error("APK ke andar AndroidManifest.xml nahi mila.")
            require(entry.size in 1..MAX_MANIFEST_BYTES.toLong()) {
                "Manifest size limit se bada ya empty hai."
            }
            zip.getInputStream(entry).use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var totalRead = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    totalRead += read
                    require(totalRead <= MAX_MANIFEST_BYTES) { "Manifest size limit exceed." }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
        }
        return decode(bytes)
    }

    fun decode(data: ByteArray): String {
        require(data.size >= 8 && data.size <= MAX_MANIFEST_BYTES) { "Invalid manifest size." }
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        require(u16(b, 0) == RES_XML_TYPE) { "File binary Android XML format mein nahi hai." }
        val total = u32(b, 4)
        require(total >= 8L && total <= data.size.toLong()) { "XML document size invalid hai." }

        var strings = emptyList<String>()
        val elements = mutableListOf<String>()
        val output = StringBuilder()
        var offset = u16(b, 2)
        require(offset >= 8 && offset <= total) { "XML header invalid hai." }

        while (offset + 8 <= total) {
            val type = u16(b, offset)
            val headerSize = u16(b, offset + 2)
            val chunkSize = u32(b, offset + 4)
            require(headerSize >= 8 && chunkSize >= headerSize && offset + chunkSize <= total) {
                "Manifest chunk boundary invalid hai."
            }
            when (type) {
                RES_STRING_POOL_TYPE -> strings = parseStringPool(b, offset, headerSize, chunkSize)
                RES_XML_RESOURCE_MAP_TYPE -> Unit
                RES_XML_START_ELEMENT_TYPE -> {
                    val node = parseStartElement(b, offset, headerSize, chunkSize, strings)
                    elements.add(node.first)
                    output.appendLine("<${node.first}>")
                    node.second.forEach { (name, value) ->
                        output.appendLine("  $name = $value")
                    }
                    output.appendLine("  Meaning: ${explain(node.first)}")
                    output.appendLine()
                }
                RES_XML_END_ELEMENT_TYPE -> {
                    val nameIndex = i32(b, offset + 20)
                    val name = strings.getOrNull(nameIndex) ?: "unknown"
                    if (elements.isNotEmpty()) elements.removeAt(elements.lastIndex)
                    output.appendLine("</$name>")
                }
            }
            offset += chunkSize.toInt()
        }
        require(strings.isNotEmpty()) { "String pool nahi mila; manifest decode nahi ho saka." }
        require(output.isNotBlank()) { "Koi readable XML element nahi mila." }
        return buildString {
            appendLine("BINARY ANDROID MANIFEST — READ-ONLY DECODE")
            appendLine("Elements decoded: ${output.lineSequence().count { it.startsWith("<") }}")
            appendLine("Note: resource references ko har APK mein resolve nahi kiya ja sakta.")
            appendLine()
            append(output)
        }
    }

    private fun parseStringPool(b: ByteBuffer, start: Int, header: Int, size: Int): List<String> {
        require(header >= 28) { "String pool header invalid." }
        val count = i32(b, start + 8)
        val flags = i32(b, start + 16)
        val stringsStart = u32(b, start + 20)
        require(count in 0..100_000 && stringsStart < size.toLong()) { "String pool bounds invalid." }
        val offsetsStart = start + header
        require(offsetsStart + count * 4L <= start + size) { "String offsets invalid." }
        val utf8 = (flags and 0x100) != 0
        return (0 until count).map { index ->
            val relative = i32(b, offsetsStart + index * 4)
            val posLong = start.toLong() + stringsStart + relative.toLong()
            require(relative >= 0 && posLong >= start.toLong() && posLong < (start + size).toLong()) { "String offset out of range." }
            val pos = posLong.toInt()
            if (utf8) readUtf8(b, pos, start + size) else readUtf16(b, pos, start + size)
        }
    }

    private fun readUtf8(b: ByteBuffer, pos: Int, limit: Int): String {
        val (chars, p1) = length8(b, pos, limit)
        val (bytes, p2) = length8(b, p1, limit)
        require(bytes >= 0 && p2 + bytes < limit) { "UTF-8 string bounds invalid." }
        val raw = ByteArray(bytes)
        for (i in raw.indices) raw[i] = b.get(p2 + i)
        return String(raw, Charsets.UTF_8)
    }

    private fun length8(b: ByteBuffer, p: Int, limit: Int): Pair<Int, Int> {
        require(p < limit)
        val first = b.get(p).toInt() and 0xff
        return if (first and 0x80 == 0) first to p + 1 else {
            require(p + 1 < limit)
            (((first and 0x7f) shl 8) or (b.get(p + 1).toInt() and 0xff)) to p + 2
        }
    }

    private fun readUtf16(b: ByteBuffer, pos: Int, limit: Int): String {
        val (length, p) = length16(b, pos, limit)
        require(length >= 0 && p + length * 2L + 2 <= limit)
        val chars = CharArray(length) { b.getShort(p + it * 2).toInt().toChar() }
        return String(chars)
    }

    private fun length16(b: ByteBuffer, p: Int, limit: Int): Pair<Int, Int> {
        require(p + 2 <= limit)
        val first = u16(b, p)
        return if (first and 0x8000 == 0) first to p + 2 else {
            require(p + 4 <= limit)
            (((first and 0x7fff) shl 16) or u16(b, p + 2)) to p + 4
        }
    }

    private fun parseStartElement(
        b: ByteBuffer, start: Int, header: Int, size: Int, strings: List<String>
    ): Pair<String, List<Pair<String, String>>> {
        require(header >= 16 && size >= 36) { "Start-element chunk invalid." }
        val nameIndex = i32(b, start + 20)
        val tag = strings.getOrNull(nameIndex) ?: "unknown"
        val attributeStart = u16(b, start + 24)
        val attributeSize = u16(b, start + 26)
        val count = u16(b, start + 28)
        require(attributeSize >= 20 && count <= 4096) { "Attribute table invalid." }
        val first = start + 16 + attributeStart
        require(first.toLong() + count.toLong() * attributeSize <= start + size) {
            "Attribute bounds invalid."
        }
        val attrs = (0 until count).map { i ->
            val p = first + i * attributeSize
            val name = strings.getOrNull(i32(b, p + 4)) ?: "attribute"
            val rawIndex = i32(b, p + 8)
            val valueType = b.get(p + 15).toInt() and 0xff
            val valueData = i32(b, p + 16)
            val value = when {
                rawIndex != NO_INDEX -> strings.getOrNull(rawIndex) ?: "(string index invalid)"
                valueType == 0x03 -> strings.getOrNull(valueData) ?: "(string index invalid)"
                valueType == 0x12 -> if (valueData != 0) "true" else "false"
                valueType == 0x10 -> valueData.toString()
                valueType == 0x11 -> "0x${valueData.toUInt().toString(16)}"
                else -> "type=0x${valueType.toString(16)}, data=$valueData"
            }
            name to value
        }
        return tag to attrs
    }

    private fun explain(tag: String): String = when (tag) {
        "manifest" -> "App ki identity aur package-level declarations."
        "uses-permission", "uses-permission-sdk-23" -> "App dwara maangi gayi permission; declaration actual use ka proof nahi."
        "application" -> "Application-level configuration aur app components ka parent."
        "activity", "activity-alias" -> "Android UI entry/screen declaration."
        "service" -> "Background ya bound service declaration."
        "receiver" -> "Broadcast event receive karne wala component."
        "provider" -> "Structured data share karne wala content provider."
        "intent-filter" -> "Component kis type ke intents handle kar sakta hai."
        "action" -> "Intent action declaration."
        "category" -> "Intent category declaration."
        "uses-sdk" -> "Minimum aur target Android API declarations."
        else -> "Manifest declaration; exact behaviour app implementation par depend karta hai."
    }

    private fun u16(b: ByteBuffer, p: Int) = b.getShort(p).toInt() and 0xffff
    private fun i32(b: ByteBuffer, p: Int) = b.getInt(p)
    private fun u32(b: ByteBuffer, p: Int) = b.getInt(p).toLong() and 0xffffffffL
}
