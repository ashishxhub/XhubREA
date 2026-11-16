package com.ashishxhub.xhubrea

import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Static security-configuration review. Findings are indicators for manual review,
 * not proof of exploitability or malicious activity.
 */
internal object SecurityIntelligenceAnalyzer {
    private const val MAX_ENTRIES = 100_000
    private const val MAX_DEX_ENTRY_BYTES = 4L * 1024L * 1024L
    private const val MAX_TOTAL_DEX_BYTES = 16L * 1024L * 1024L
    private const val MAX_COMPONENTS_REPORTED = 100

    private val dexIndicators = linkedMapOf(
        "Custom TLS trust handling" to listOf("javax/net/ssl/x509trustmanager", "checkServerTrusted"),
        "Hostname verification customization" to listOf("javax/net/ssl/hostnameverifier", "verify("),
        "WebView JavaScript bridge API" to listOf("addjavascriptinterface"),
        "WebView JavaScript setting API" to listOf("setjavascriptenabled"),
        "WebView SSL error callback" to listOf("onreceivedsslerror"),
        "Legacy world-readable / world-writable file flags" to listOf("mode_world_readable", "mode_world_writeable")
    )

    fun run(apk: File): String = runCatching {
        require(apk.isFile && apk.length() > 0L) { "Choose a valid APK first." }
        val entries = ZipFile(apk).use { zip ->
            val all = zip.entries().asSequence().toList()
            require(all.size <= MAX_ENTRIES) { "APK has too many entries for this bounded scan." }
            all
        }
        val manifest = BinaryManifestDecoder.decodeApk(apk)
        val declarations = parseDeclarations(manifest)
        val app = declarations.firstOrNull { it.first == "application" }?.second.orEmpty()
        val permissions = declarations.filter { it.first.startsWith("uses-permission") }
        val components = declarations.filter { it.first in setOf("activity", "activity-alias", "service", "receiver", "provider") }
        val customPermissions = declarations.filter { it.first == "permission" || it.first == "permission-tree" }
        val sdk = declarations.firstOrNull { it.first == "uses-sdk" }?.second.orEmpty()

        // The flattened manifest report does not preserve intent-filter nesting,
        // so only explicit exported=true declarations are counted as confirmed.
        val exported = components.filter { (_, attrs) ->
            attrs["exported"]?.equals("true", ignoreCase = true) == true
        }
        val dexFindings = scanDex(entries, apk)
        val targetSdk = sdk["targetSdkVersion"] ?: "not resolved"
        val minSdk = sdk["minSdkVersion"] ?: "not resolved"

        buildString {
            appendLine("XhubREA — SECURITY INTELLIGENCE")
            appendLine("Mode: static, bounded, read-only APK review")
            appendLine("This report identifies configuration and code-reference clues; it is not a penetration test.")
            appendLine()
            appendLine("1) APPLICATION SECURITY CONFIGURATION")
            append(reportFlag("Debuggable", app["debuggable"], "A true declaration can expose debugging capabilities in applicable build/runtime contexts."))
            append(reportFlag("Test-only", app["testOnly"], "A true declaration marks the package as test-only."))
            append(reportFlag("Allow backup", app["allowBackup"], "Backup policy is declared here; actual backup exposure depends on Android version, device policy and backup rules."))
            append(reportFlag("Cleartext traffic", app["usesCleartextTraffic"], "A true declaration permits cleartext traffic at application level unless more specific platform configuration changes the outcome."))
            appendLine("Network security config reference: ${app["networkSecurityConfig"] ?: "not resolved"}")
            appendLine("Minimum SDK: $minSdk | Target SDK: $targetSdk")
            appendLine("Evidence: decoded AndroidManifest.xml application / uses-sdk declarations.")
            appendLine()
            appendLine("2) EXPORTED COMPONENT REVIEW")
            appendLine("Components declared: ${components.size}")
            appendLine("Components explicitly marked exported=true: ${components.count { it.second["exported"]?.equals("true", true) == true }}")
            appendLine("Explicitly exported components requiring review: ${exported.size}")
            appendLine("Legacy implicit-export status: UNKNOWN (intent-filter nesting is not preserved by the current manifest report).")
            if (exported.isEmpty()) appendLine("No exported or legacy-default candidate components were identified by this parser.")
            exported.take(MAX_COMPONENTS_REPORTED).forEach { (tag, attrs) ->
                appendLine("• $tag ${attrs["name"] ?: "(name unresolved)"} | exported=${attrs["exported"] ?: "implicit/unknown"} | permission=${attrs["permission"] ?: "none declared"}")
                appendLine("  Review whether the component is intended to be externally reachable and whether caller permissions are appropriate.")
            }
            if (exported.size > MAX_COMPONENTS_REPORTED) appendLine("Additional components omitted from display: ${exported.size - MAX_COMPONENTS_REPORTED}")
            appendLine("Caution: this is a static declaration review. Actual reachability also depends on Android version, target SDK, permissions and runtime behavior.")
            appendLine()
            appendLine("3) CUSTOM PERMISSION PROTECTION")
            if (customPermissions.isEmpty()) appendLine("No custom permission declarations were decoded.")
            customPermissions.forEach { (tag, attrs) ->
                appendLine("• $tag ${attrs["name"] ?: "(name unresolved)"} | protectionLevel=${attrs["protectionLevel"] ?: "not resolved"}")
                appendLine("  Review whether the protection level matches the intended caller trust boundary.")
            }
            appendLine()
            appendLine("4) DEX SECURITY-RELATED API CLUES")
            if (dexFindings.isEmpty()) appendLine("No selected API-name clues were found in the bounded DEX text scan.")
            dexFindings.forEach { (label, evidence) ->
                appendLine("• $label")
                appendLine("  Static marker: $evidence")
                appendLine("  Confidence: LIKELY code-reference clue; call sites, configuration and runtime use are UNKNOWN.")
            }
            appendLine()
            appendLine("5) PERMISSION DECLARATION SNAPSHOT")
            appendLine("Declared permissions: ${permissions.size}")
            permissions.take(40).forEach { (_, attrs) -> appendLine("• ${attrs["name"] ?: "permission name unresolved"}") }
            if (permissions.size > 40) appendLine("Additional permissions omitted: ${permissions.size - 40}")
            appendLine("Permission declarations do not prove that Android granted access or that the app used it.")
            appendLine()
            appendLine("6) PRIORITY MANUAL REVIEW")
            appendLine("• Check exported components for intended exposure, caller validation and permission requirements.")
            appendLine("• Review debug/test flags and backup rules for the intended release build.")
            appendLine("• Review cleartext and network-security configuration against the app's documented network needs.")
            appendLine("• Review custom TLS/WebView API call sites in source code; API presence alone is not a vulnerability.")
            appendLine()
            appendLine("7) LIMITATIONS & CONFIDENCE")
            appendLine("CONFIRMED = a decoded manifest declaration or literal DEX marker was found.")
            appendLine("LIKELY = a heuristic or API-reference clue that needs source/runtime corroboration.")
            appendLine("UNKNOWN = exploitability, actual execution, backend security, server configuration and user-data exposure.")
            appendLine("No APK code was executed, no network traffic was intercepted, no secrets were extracted, and no security control was bypassed.")
            appendLine("Only inspect apps you own, open-source apps, or apps you are authorized to research.")
        }
    }.getOrElse { error ->
        "Security Intelligence could not complete: ${error.message ?: error.javaClass.simpleName}"
    }

    private fun reportFlag(label: String, value: String?, explanation: String): String = buildString {
        appendLine("$label: ${value ?: "not resolved"}")
        if (value.equals("true", true)) appendLine("  Review: $explanation")
    }

    private fun parseDeclarations(report: String): List<Pair<String, Map<String, String>>> {
        val lines = report.lineSequence().toList()
        val result = mutableListOf<Pair<String, Map<String, String>>>()
        var index = 0
        while (index < lines.size) {
            val tag = Regex("""^<([A-Za-z0-9_.-]+)>$""").matchEntire(lines[index].trim())?.groupValues?.get(1)
            if (tag == null) { index++; continue }
            val attrs = linkedMapOf<String, String>()
            index++
            while (index < lines.size) {
                val line = lines[index].trim()
                if (line.isBlank() || line.startsWith("<")) break
                val split = line.split(" = ", limit = 2)
                if (split.size == 2 && !split[0].startsWith("Meaning")) attrs[split[0].substringAfterLast(':')] = split[1]
                index++
            }
            result += tag to attrs
        }
        return result
    }

    private fun scanDex(
        entries: List<java.util.zip.ZipEntry>,
        apk: File
    ): List<Pair<String, String>> {
        val found = linkedMapOf<String, String>()
        var total = 0L
        ZipFile(apk).use { zip ->
            entries.filter { Regex("""classes(\d*)\.dex""").matches(it.name) && it.size in 1..MAX_DEX_ENTRY_BYTES }
                .forEach { entry ->
                    if (total >= MAX_TOTAL_DEX_BYTES) return@forEach
                    val allowed = minOf(entry.size, MAX_TOTAL_DEX_BYTES - total).toInt()
                    val bytes = zip.getInputStream(entry).use { input ->
                        val buffer = ByteArray(allowed)
                        var offset = 0
                        while (offset < buffer.size) {
                            val count = input.read(buffer, offset, buffer.size - offset)
                            if (count <= 0) break
                            offset += count
                        }
                        buffer.copyOf(offset)
                    }
                    total += bytes.size
                    val printable = buildString(bytes.size) {
                        bytes.forEach { byte ->
                            val ch = byte.toInt() and 0xff
                            append(if (ch in 32..126) ch.toChar() else ' ')
                        }
                    }.lowercase(Locale.ROOT)
                    dexIndicators.forEach { (label, markers) ->
                        val hit = markers.firstOrNull { printable.contains(it.lowercase(Locale.ROOT)) }
                        if (hit != null) found.putIfAbsent(label, hit)
                    }
                }
        }
        return found.entries.map { it.key to it.value }
    }
}
