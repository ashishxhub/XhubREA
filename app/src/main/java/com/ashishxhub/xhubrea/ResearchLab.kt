package com.ashishxhub.xhubrea

import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Bounded, read-only APK research suite.
 * It inventories archive metadata and reports structural hints only.
 * It never loads, installs, launches, decompiles, or modifies the inspected APK.
 */
internal object ResearchLab {
    private const val MAX_ENTRIES = 120_000
    private const val MAX_SCAN_BYTES = 48L * 1024L * 1024L
    private const val MAX_ENTRY_BYTES = 16L * 1024L * 1024L

    fun run(apk: File): String = runCatching {
        require(apk.isFile && apk.length() > 0L) { "Choose a valid APK first." }
        ZipFile(apk).use { zip ->
            val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
            require(entries.size <= MAX_ENTRIES) { "APK contains too many archive entries for this safe scan." }

            val paths = entries.map { it.name }
            val lower = paths.map { it.lowercase(Locale.ROOT) }
            val dexEntries = entries.filter { Regex("""classes(\d*)\.dex""").matches(it.name) }
            val resourceEntries = paths.filter {
                it.startsWith("res/") || it.startsWith("assets/") ||
                    it == "resources.arsc" || it.endsWith(".xml")
            }
            val layoutEntries = paths.filter {
                it.startsWith("res/layout") && it.endsWith(".xml")
            }
            val drawableEntries = paths.filter {
                it.startsWith("res/drawable") || it.startsWith("res/mipmap")
            }
            val nativeEntries = paths.filter { it.startsWith("lib/") && it.endsWith(".so") }
            val totalUncompressed = entries.sumOf { if (it.size > 0) it.size else 0L }
            val largest = entries.sortedByDescending { it.size }.take(8)

            val engineHints = linkedMapOf(
                "Unity" to listOf("libunity.so", "assets/bin/data/"),
                "Unreal Engine" to listOf("libue4.so", "libunreal.so", "assets/ue4game/"),
                "Godot" to listOf("libgodot_android.so", "assets/.godot/"),
                "Flutter" to listOf("libflutter.so", "assets/flutter_assets/"),
                "React Native" to listOf("libreactnative.so", "index.android.bundle"),
                "Xamarin / .NET" to listOf("monodroid.dll", "assemblies/")
            )
            val engineMatches = engineHints.mapNotNull { (name, markers) ->
                val evidence = lower.filter { path -> markers.any { path.contains(it) } }.take(5)
                if (evidence.isEmpty()) null else "$name: " + evidence.joinToString()
            }

            val scannedDex = StringBuilder()
            var scanBudget = MAX_SCAN_BYTES
            dexEntries.forEach { entry ->
                if (scanBudget > 0 && entry.size in 1..MAX_ENTRY_BYTES) {
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    scanBudget -= bytes.size
                    // Only collect printable ASCII for endpoint host matching; never print raw strings.
                    bytes.forEach { b ->
                        val n = b.toInt() and 0xff
                        if (n in 32..126) scannedDex.append(n.toChar()) else scannedDex.append(' ')
                    }
                }
            }
            val hosts = Regex("""https?://([a-zA-Z0-9.-]+)(?::[0-9]{1,5})?""")
                .findAll(scannedDex)
                .map { it.groupValues[1].lowercase(Locale.ROOT) }
                .filter { it.contains('.') && !it.endsWith(".png") && !it.endsWith(".jpg") }
                .distinct()
                .take(30)
                .toList()

            buildString {
                appendLine("XhubREA FULL RESEARCH SCAN")
                appendLine("Mode: bounded, read-only archive inspection")
                appendLine("APK: ${apk.name}")
                appendLine("APK size: ${apk.length()} bytes")
                appendLine("Archive entries: ${entries.size}")
                appendLine("Known uncompressed size total: ${totalUncompressed} bytes")
                appendLine()
                appendLine("1) CODE / DEX STRUCTURE")
                appendLine("DEX files: ${dexEntries.size}")
                dexEntries.forEach { appendLine(" • ${it.name} | ${it.size} bytes") }
                appendLine("This scan does not decompile code, expose string tables, or execute bytecode.")
                appendLine()
                appendLine("2) RESOURCE & UI INVENTORY")
                appendLine("Resource/assets entries: ${resourceEntries.size}")
                appendLine("Layout XML files: ${layoutEntries.size}")
                appendLine("Drawable / mipmap entries: ${drawableEntries.size}")
                appendLine("Layout samples:")
                appendLine(layoutEntries.take(25).joinToString("\n").ifBlank { "No conventional res/layout XML files found." })
                appendLine()
                appendLine("3) GAME / FRAMEWORK DETECTION")
                appendLine(engineMatches.joinToString("\n").ifBlank { "No known framework fingerprint matched; this is not proof that no engine is used." })
                appendLine()
                appendLine("4) NATIVE LIBRARY INVENTORY")
                appendLine("Native libraries: ${nativeEntries.size}")
                appendLine(nativeEntries.take(40).joinToString("\n").ifBlank { "No .so files found." })
                appendLine()
                appendLine("5) NETWORK / API HINTS")
                appendLine("Distinct URL hostnames found in bounded DEX text scan: ${hosts.size}")
                appendLine(hosts.joinToString("\n").ifBlank { "No hostnames found in scanned DEX bytes." })
                appendLine("Only hostnames are shown. Paths, query parameters, credentials and raw strings are intentionally omitted.")
                appendLine()
                appendLine("6) ARCHIVE / TESTING CHECKS")
                appendLine("Empty files: ${entries.count { it.size == 0L }}")
                appendLine("Largest entries:")
                appendLine(largest.joinToString("\n") { " • ${it.name} — ${it.size} bytes" })
                appendLine("ZIP archive opened successfully: YES")
                appendLine("APK installation / runtime behavior tested: NO")
                appendLine()
                appendLine("7) ORIGINAL PROJECT PLANNING")
                appendLine("Suggested workflow:")
                appendLine(" • Map visible screens and user journeys manually.")
                appendLine(" • Record feature inputs, outputs, loading and error states.")
                appendLine(" • Design an original data model and API contract.")
                appendLine(" • Implement fresh UI, logic, and assets; do not copy proprietary code or branding.")
                appendLine(" • Add unit, UI and accessibility tests for the new implementation.")
                appendLine()
                appendLine("8) LIMITATIONS")
                appendLine("This is static metadata research, not a full reverse-engineering/decompiler or AI model.")
                appendLine("Manifest details are available in the separate PackageManager-based APK inspector.")
                appendLine("Encrypted, obfuscated, split, dynamic or unusual APK layouts may hide useful evidence.")
                appendLine("Use only APKs you own, open-source APKs, or files you have permission to inspect.")
            }
        }
    }.getOrElse { error ->
        "Research scan could not complete: ${error.message ?: "Unknown error"}"
    }
}
