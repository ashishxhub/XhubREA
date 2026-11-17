package com.ashishxhub.xhubrea

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bounded, read-only resources.arsc inventory parser.
 * Reports package/type/configuration metadata; it does not resolve runtime values.
 */
internal object ResourceTableDecoder {
    private const val TABLE = 0x0002
    private const val STRING_POOL = 0x0001
    private const val PACKAGE = 0x0200
    private const val TYPE = 0x0201
    private const val TYPE_SPEC = 0x0202
    private const val MAX_BYTES = 32 * 1024 * 1024
    private const val MAX_PACKAGES = 256
    private const val MAX_STRINGS = 100_000
    private const val MAX_ENTRIES = 65_536
    private const val NO_ENTRY = -1

    data class TypeInfo(val id: Int, val name: String?, val entryCount: Int, val configurations: Int)
    data class PackageInfo(val id: Long, val name: String, val types: List<TypeInfo>)
    data class Report(val packages: List<PackageInfo>, val globalStringCount: Int)

    fun decode(data: ByteArray): Report {
        require(data.size in 12..MAX_BYTES) { "resources.arsc size invalid or exceeds 32 MiB limit." }
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        require(u16(b, 0) == TABLE) { "File Android resource table format mein nahi hai." }
        val rootHeader = u16(b, 2)
        val total = u32(b, 4)
        val packageCount = i32(b, 8)
        require(rootHeader >= 12 && rootHeader <= total && total <= data.size.toLong()) { "Resource table header/size invalid." }
        require(packageCount in 0..MAX_PACKAGES) { "Package count limit exceed." }

        var globalStrings = emptyList<String>()
        val packages = mutableListOf<PackageInfo>()
        var offset = rootHeader
        while (offset + 8L <= total) {
            val chunk = chunkAt(b, offset, total.toInt())
            when (chunk.type) {
                STRING_POOL -> globalStrings = parseStringPool(b, chunk)
                PACKAGE -> packages += parsePackage(b, chunk)
            }
            offset += chunk.size
        }
        require(offset == total.toInt()) { "Trailing partial resource-table chunk." }
        require(packages.size == packageCount) { "Declared package count does not match parsed packages." }
        return Report(packages, globalStrings.size)
    }

    private data class Chunk(val type: Int, val headerSize: Int, val size: Int, val start: Int)

    private fun chunkAt(b: ByteBuffer, start: Int, limit: Int): Chunk {
        require(start >= 0 && start.toLong() + 8 <= limit.toLong()) { "Truncated resource chunk header." }
        val type = u16(b, start)
        val header = u16(b, start + 2)
        val sizeLong = u32(b, start + 4)
        require(header >= 8 && sizeLong >= header && sizeLong <= (limit - start).toLong()) {
            "Resource chunk boundary invalid."
        }
        return Chunk(type, header, sizeLong.toInt(), start)
    }

    private fun parsePackage(b: ByteBuffer, chunk: Chunk): PackageInfo {
        require(chunk.headerSize >= 284) { "Resource package header is truncated." }
        val start = chunk.start
        val id = u32(b, start + 8)
        val name = readPackageName(b, start + 12, start + chunk.headerSize)
        val typePoolOffset = u32(b, start + 268)
        val keyPoolOffset = u32(b, start + 276)
        val typeNames = packageStringPool(b, start, chunk, typePoolOffset)
        val keyNames = packageStringPool(b, start, chunk, keyPoolOffset)
        val types = linkedMapOf<Int, TypeInfo>()
        var offset = start + chunk.headerSize
        val end = start + chunk.size
        while (offset + 8 <= end) {
            val child = chunkAt(b, offset, end)
            when (child.type) {
                TYPE_SPEC -> {
                    require(child.headerSize >= 16 && child.size >= child.headerSize) { "Type-spec chunk invalid." }
                    val typeId = b.get(offset + 8).toInt() and 0xff
                    val count = i32(b, offset + 12)
                    require(typeId > 0 && count in 0..MAX_ENTRIES) { "Type-spec count/id invalid." }
                    require(child.headerSize.toLong() + count.toLong() * 4 <= child.size) { "Type-spec flags exceed chunk." }
                    val previous = types[typeId]
                    types[typeId] = TypeInfo(typeId, typeNames.getOrNull(typeId - 1), count, previous?.configurations ?: 0)
                }
                TYPE -> {
                    val parsed = parseType(b, child, typeNames, keyNames)
                    val previous = types[parsed.id]
                    types[parsed.id] = parsed.copy(
                        entryCount = maxOf(parsed.entryCount, previous?.entryCount ?: 0),
                        configurations = (previous?.configurations ?: 0) + 1
                    )
                }
            }
            offset += child.size
        }
        require(offset == end) { "Package contains a partial child chunk." }
        return PackageInfo(id, name, types.values.sortedBy { it.id })
    }

    private fun parseType(
        b: ByteBuffer, chunk: Chunk, typeNames: List<String>, keyNames: List<String>
    ): TypeInfo {
        val start = chunk.start
        require(chunk.headerSize >= 24 && chunk.size >= chunk.headerSize) { "Type chunk header invalid." }
        val id = b.get(start + 8).toInt() and 0xff
        val entryCount = i32(b, start + 12)
        val entriesStart = u32(b, start + 16)
        val configSize = u32(b, start + 20)
        require(id > 0 && entryCount in 0..MAX_ENTRIES) { "Type id/entry count invalid." }
        require(configSize >= 4 && 20L + configSize <= chunk.headerSize.toLong()) { "Type configuration bounds invalid." }
        require(entriesStart >= chunk.headerSize && entriesStart <= chunk.size.toLong()) { "Entry data offset invalid." }
        require(chunk.headerSize.toLong() + entryCount.toLong() * 4 <= entriesStart) { "Entry offset array exceeds its data region." }
        var present = 0
        for (index in 0 until entryCount) {
            val relative = i32(b, start + chunk.headerSize + index * 4)
            if (relative == NO_ENTRY) continue
            require(relative >= 0 && entriesStart + relative.toLong() + 8 <= chunk.size.toLong()) {
                "Resource entry offset outside type chunk."
            }
            val entry = start + entriesStart.toInt() + relative
            val entrySize = u16(b, entry)
            val keyIndex = u32(b, entry + 4)
            require(entrySize >= 8 && relative.toLong() + entrySize <= chunk.size - entriesStart) {
                "Resource entry header invalid."
            }
            require(keyIndex < keyNames.size.toLong()) { "Resource key index outside key string pool." }
            present++
        }
        return TypeInfo(id, typeNames.getOrNull(id - 1), present, 1)
    }

    private fun packageStringPool(b: ByteBuffer, packageStart: Int, parent: Chunk, relative: Long): List<String> {
        require(relative >= parent.headerSize && relative < parent.size.toLong()) { "Package string-pool offset invalid." }
        val poolStart = packageStart + relative.toInt()
        val poolChunk = chunkAt(b, poolStart, packageStart + parent.size)
        require(poolChunk.type == STRING_POOL) { "Package string-pool offset does not point to a string pool." }
        return parseStringPool(b, poolChunk)
    }

    private fun parseStringPool(b: ByteBuffer, chunk: Chunk): List<String> {
        require(chunk.headerSize >= 28) { "String-pool header invalid." }
        val start = chunk.start
        val count = i32(b, start + 8)
        val flags = i32(b, start + 16)
        val stringsStart = u32(b, start + 20)
        require(count in 0..MAX_STRINGS && stringsStart <= chunk.size.toLong()) { "String-pool bounds invalid." }
        require(chunk.headerSize.toLong() + count.toLong() * 4 <= chunk.size.toLong()) { "String offset array invalid." }
        val utf8 = flags and 0x100 != 0
        return (0 until count).map { index ->
            val relative = i32(b, start + chunk.headerSize + index * 4)
            val position = start.toLong() + stringsStart + relative.toLong()
            require(relative >= 0 && position >= start.toLong() && position < (start + chunk.size).toLong()) {
                "String offset outside pool."
            }
            if (utf8) readUtf8(b, position.toInt(), start + chunk.size)
            else readUtf16(b, position.toInt(), start + chunk.size)
        }
    }

    private fun readUtf8(b: ByteBuffer, position: Int, limit: Int): String {
        val (_, afterChars) = length8(b, position, limit)
        val (byteCount, afterLength) = length8(b, afterChars, limit)
        require(byteCount >= 0 && afterLength.toLong() + byteCount < limit) { "UTF-8 string exceeds pool." }
        val bytes = ByteArray(byteCount) { b.get(afterLength + it) }
        return String(bytes, Charsets.UTF_8)
    }

    private fun length8(b: ByteBuffer, position: Int, limit: Int): Pair<Int, Int> {
        require(position < limit) { "Truncated UTF-8 length." }
        val first = b.get(position).toInt() and 0xff
        return if (first and 0x80 == 0) first to position + 1 else {
            require(position + 1 < limit) { "Truncated UTF-8 length." }
            (((first and 0x7f) shl 8) or (b.get(position + 1).toInt() and 0xff)) to position + 2
        }
    }

    private fun readUtf16(b: ByteBuffer, position: Int, limit: Int): String {
        val (length, afterLength) = length16(b, position, limit)
        require(length >= 0 && afterLength.toLong() + length.toLong() * 2 + 2 <= limit) { "UTF-16 string exceeds pool." }
        return String(CharArray(length) { b.getShort(afterLength + it * 2).toInt().toChar() })
    }

    private fun length16(b: ByteBuffer, position: Int, limit: Int): Pair<Int, Int> {
        require(position.toLong() + 2 <= limit) { "Truncated UTF-16 length." }
        val first = u16(b, position)
        return if (first and 0x8000 == 0) first to position + 2 else {
            require(position.toLong() + 4 <= limit) { "Truncated UTF-16 length." }
            (((first and 0x7fff) shl 16) or u16(b, position + 2)) to position + 4
        }
    }

    private fun readPackageName(b: ByteBuffer, position: Int, limit: Int): String {
        require(position + 256 <= limit) { "Package name field truncated." }
        val chars = CharArray(128)
        for (index in chars.indices) chars[index] = b.getShort(position + index * 2).toInt().toChar()
        return String(chars).substringBefore('\u0000')
    }

    private fun u16(b: ByteBuffer, p: Int) = b.getShort(p).toInt() and 0xffff
    private fun i32(b: ByteBuffer, p: Int) = b.getInt(p)
    private fun u32(b: ByteBuffer, p: Int) = b.getInt(p).toLong() and 0xffffffffL
}
