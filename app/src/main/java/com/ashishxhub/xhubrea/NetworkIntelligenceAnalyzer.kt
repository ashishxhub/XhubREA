package com.ashishxhub.xhubrea

import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/** Bounded, read-only network configuration intelligence. */
internal object NetworkIntelligenceAnalyzer {
    private const val MAX_ENTRIES = 100_000
    private const val MAX_DEX_ENTRY_BYTES = 8L * 1024L * 1024L
    private const val MAX_TOTAL_SCAN_BYTES = 32L * 1024L * 1024L
    private const val MAX_XML_CANDIDATES = 40

    private val sdkMarkers = linkedMapOf(
        "Firebase / Google services" to listOf("com/google/firebase/", "com/google/android/gms/"),
        "Facebook SDK" to listOf("com/facebook/"),
        "AppsFlyer" to listOf("com/appsflyer/"),
        "Adjust" to listOf("com/adjust/sdk/"),
        "Amplitude" to listOf("com/amplitude/"),
        "Mixpanel" to listOf("com/mixpanel/"),
        "Branch" to listOf("io/branch/"),
        "OneSignal" to listOf("com/onesignal/"),
        "Braze" to listOf("com/appboy/", "com/braze/"),
        "Sentry" to listOf("io/sentry/"),
        "Bugsnag" to listOf("com/bugsnag/"),
        "Datadog" to listOf("com/datadog/")
    )

    fun run(apk: File): String = runCatching {
        require(apk.isFile && apk.length() > 0L) { "Choose a valid APK first." }
        ZipFile(apk).use { zip ->
            val entries = zip.entries().asSequence().toList()
            require(entries.size <= MAX_ENTRIES) { "APK has too many entries for this bounded scan." }

            val manifestText = BinaryManifestDecoder.decodeApk(apk)
            val appAttrs = readTagAttributes(manifestText, "application")
            val cleartext = appAttrs["usesCleartextTraffic"]
            val networkConfig = appAttrs["networkSecurityConfig"]
            val debuggable = appAttrs["debuggable"]

            val xmlCandidates = entries.asSequence().map { it.name }
                .filter { it.startsWith("res/xml/") && it.endsWith(".xml") }
                .take(MAX_XML_CANDIDATES).toList()

            val sdkHits = mutableListOf<String>()
            val hosts = linkedSetOf<String>()
            var scannedBytes = 0L
            val dexEntries = entries.filter {
                Regex("""classes(\d*)\.dex""").matches(it.name) &&
                    it.size in 1..MAX_DEX_ENTRY_BYTES
            }
            dexEntries.forEach { entry ->
                if (scannedBytes >= MAX_TOTAL_SCAN_BYTES) return@forEach
                val allowed = minOf(entry.size, MAX_TOTAL_SCAN_BYTES - scannedBytes).toInt()
                val bytes = zip.getInputStream(entry).use { input ->
                    val out = ByteArray(allowed)
                    var offset = 0
                    while (offset < allowed) {
                        val count = input.read(out, offset, allowed - offset)
                        if (count < 0) break
                        offset += count
                    }
                    out.copyOf(offset)
                }
                scannedBytes += bytes.size
                val printable = buildString(bytes.size) {
                    bytes.forEach { byte ->
                        val value = byte.toInt() and 0xff
                        append(if (value in 32..126) value.toChar() else ' ')
                    }
                }.lowercase(Locale.ROOT)

                sdkMarkers.forEach { (name, markers) ->
                    if (markers.any { printable.contains(it) } && name !in sdkHits) sdkHits.add(name)
                }
                Regex("""https?://([a-z0-9.-]+)(?::[0-9]{1,5})?""")
                    .findAll(printable)
                    .map { it.groupValues[1].trim('.') }
                    .filter { it.contains('.') && it.length <= 253 }
                    .forEach { hosts.add(it) }
            }

            buildString {
                appendLine("XhubREA — NETWORK INTELLIGENCE")
                appendLine("Mode: bounded, static, read-only APK inspection")
                appendLine()
                appendLine("1) CLEARtext TRAFFIC CONFIGURATION")
                when (cleartext?.lowercase(Locale.ROOT)) {
                    "true" -> appendLine("CONFIRMED DECLARATION: android:usesCleartextTraffic=true. The manifest explicitly permits cleartext traffic at application level; this does not prove HTTP requests actually occur.")
                    "false" -> appendLine("CONFIRMED DECLARATION: android:usesCleartextTraffic=false. The manifest explicitly disables cleartext traffic at application level.")
                    else -> appendLine("UNKNOWN: usesCleartextTraffic was not resolved. Android defaults depend on target SDK and platform rules; missing does not prove HTTP or HTTPS.")
                }
                appendLine()
                appendLine("2) NETWORK SECURITY CONFIG")
                if (!networkConfig.isNullOrBlank()) {
                    appendLine("Manifest reference found: android:networkSecurityConfig=$networkConfig")
                    appendLine("CONFIRMED: the manifest contains a network-security-config reference. Its resource ID/name is not resolved by this bounded analyzer.")
                } else {
                    appendLine("No networkSecurityConfig attribute was resolved in the application tag. This alone does not prove that every connection is secure or insecure.")
                }
                appendLine("Candidate res/xml files (not assumed to be the referenced config):")
                appendLine(xmlCandidates.joinToString("\n").ifBlank { "No conventional res/xml/*.xml candidates found." })
                appendLine("Compiled binary XML contents are not decoded here.")
                appendLine()
                appendLine("3) DEBUG / APP CONFIGURATION CLUE")
                appendLine("Manifest debuggable attribute: ${debuggable ?: "Unknown / not resolved"}")
                appendLine("This declaration alone is not proof of a remotely exploitable issue.")
                appendLine()
                appendLine("4) DOMAIN / HOSTNAME CLUES")
                appendLine("Distinct hostnames found in bounded DEX text scan: ${hosts.size}")
                appendLine(hosts.take(60).joinToString("\n").ifBlank { "No hostname clues found in the scanned DEX portion." })
                appendLine("Static string clue only: a hostname may be unused, stale, a library endpoint, or dynamically assembled.")
                appendLine()
                appendLine("5) THIRD-PARTY SDK FINGERPRINTS")
                appendLine(sdkHits.joinToString("\n").ifBlank { "No listed SDK fingerprints found in the scanned DEX portion." })
                appendLine("SDK/package fingerprints do not prove tracking, data collection, or a specific user-impacting action.")
                appendLine()
                appendLine("6) PRIVACY & USER-IMPACT INTERPRETATION")
                appendLine("• A hostname is not proof that personal data is sent there.")
                appendLine("• A declared SDK is not proof that it is initialized or active.")
                appendLine("• Correlate permissions, SDKs and network strings with code paths and authorized runtime evidence.")
                appendLine("• No traffic was intercepted; no APK code was executed.")
                appendLine()
                appendLine("7) SCAN BOUNDS")
                appendLine("DEX entries considered: ${dexEntries.size}")
                appendLine("DEX bytes scanned: $scannedBytes (maximum $MAX_TOTAL_SCAN_BYTES)")
                appendLine("XML candidate list limit: $MAX_XML_CANDIDATES")
                appendLine()
                appendLine("CONFIDENCE GUIDE")
                appendLine("CONFIRMED = directly decoded manifest declaration or literal static fingerprint.")
                appendLine("LIKELY = pattern-based clue that needs corroboration.")
                appendLine("UNKNOWN = unavailable, unresolved, obfuscated, or outside scan limits.")
                appendLine()
                appendLine("LIMITATIONS")
                appendLine("Static metadata only; not a live network monitor, full DEX decompiler, TLS verifier, backend audit, or proof of app manipulation.")
                appendLine("Inspect only apps you own, open-source apps, or apps you are authorized to research.")
            }
        }
    }.getOrElse { error ->
        "Network Intelligence could not complete: ${error.message ?: error.javaClass.simpleName}"
    }

    private fun readTagAttributes(report: String, tag: String): Map<String, String> {
        val lines = report.lineSequence().toList()
        val start = lines.indexOfFirst { it.trim() == "<$tag>" }
        if (start < 0) return emptyMap()
        val values = linkedMapOf<String, String>()
        for (index in start + 1 until lines.size) {
            val line = lines[index].trim()
            if (line.startsWith("<") || line.isBlank()) break
            val split = line.split(" = ", limit = 2)
            if (split.size == 2) values[split[0].substringAfterLast(':')] = split[1]
        }
        return values
    }
}
