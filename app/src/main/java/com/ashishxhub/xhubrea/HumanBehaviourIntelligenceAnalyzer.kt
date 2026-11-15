package com.ashishxhub.xhubrea

import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Heuristic scan of readable UI resource text and archive resource names.
 * This detects design clues only; it cannot establish intent or psychological effects.
 */
internal object HumanBehaviourIntelligenceAnalyzer {
    private const val MAX_ENTRIES = 100_000
    private const val MAX_XML_FILE_BYTES = 512 * 1024
    private const val MAX_TOTAL_XML_BYTES = 4 * 1024 * 1024
    private const val MAX_MATCHES_PER_PATTERN = 8

    private data class Pattern(
        val category: String,
        val label: String,
        val explanation: String,
        val terms: List<String>,
        val caution: String
    )

    private val patterns = listOf(
        Pattern("Subscription / continuity", "Trial and recurring-payment language",
            "Readable interface text may refer to trials, renewal, billing or subscription offers.",
            listOf("free trial", "auto-renew", "subscription", "renews", "billing period", "cancel anytime"),
            "Text presence does not show the actual checkout flow, price prominence or cancellation difficulty."),
        Pattern("Urgency / scarcity", "Urgency and limited-availability language",
            "Readable text may use countdown, limited-time or scarcity wording.",
            listOf("limited time", "ends soon", "only today", "hurry", "countdown", "few left"),
            "A phrase alone cannot establish whether urgency is genuine or misleading."),
        Pattern("Choice architecture", "Decline, skip and opt-out wording",
            "The interface contains language associated with accepting, declining or postponing an action.",
            listOf("not now", "maybe later", "skip", "decline", "no thanks", "continue"),
            "Static text cannot establish whether choices are equally visible or equally easy to use."),
        Pattern("Engagement / habit", "Streak, daily reward and reminder language",
            "The interface may contain wording associated with repeated daily engagement or rewards.",
            listOf("streak", "daily reward", "come back tomorrow", "daily bonus", "reminder", "keep your streak"),
            "These mechanics can be useful or pressuring depending on implementation and user context."),
        Pattern("Social influence", "Invite, referral and social-proof language",
            "Readable text may encourage inviting others or refer to popularity and social activity.",
            listOf("invite friends", "refer a friend", "people are viewing", "trending", "popular", "friends joined"),
            "The scan cannot verify whether popularity claims are accurate or how social prompts behave."),
        Pattern("Advertising", "Ad and sponsored-content wording",
            "Readable text may label advertising or sponsored content.",
            listOf("sponsored", "advertisement", "promoted", "watch an ad", "ad-free"),
            "The presence of ad labels does not prove that ads are disguised or deceptive.")
    )

    fun run(apk: File, networkReport: String = ""): String = runCatching {
        require(apk.isFile && apk.length() > 0L) { "Choose a valid APK first." }
        val evidence = mutableListOf<Pair<String, String>>()
        var scannedBytes = 0
        var scannedFiles = 0
        ZipFile(apk).use { zip ->
            val entries = zip.entries().asSequence().toList()
            require(entries.size <= MAX_ENTRIES) { "APK has too many entries for this bounded scan." }
            for (entry in entries) {
                if (scannedBytes >= MAX_TOTAL_XML_BYTES || scannedFiles >= 80) break
                val path = entry.name.replace('\\', '/')
                val candidate = path.startsWith("res/layout") && path.endsWith(".xml") ||
                    path.startsWith("res/values") && path.endsWith(".xml")
                if (!candidate || entry.isDirectory || entry.size <= 0L || entry.size > MAX_XML_FILE_BYTES) continue
                val remaining = MAX_TOTAL_XML_BYTES - scannedBytes
                val limit = minOf(MAX_XML_FILE_BYTES.toLong(), remaining.toLong()).toInt()
                val bytes = zip.getInputStream(entry).use { input ->
                    val out = ByteArray(limit)
                    var offset = 0
                    while (offset < out.size) {
                        val read = input.read(out, offset, out.size - offset)
                        if (read <= 0) break
                        offset += read
                    }
                    out.copyOf(offset)
                }
                scannedBytes += bytes.size
                scannedFiles++
                // Compiled binary XML is deliberately skipped; only plain UTF-8 XML is searched.
                if (bytes.isNotEmpty() && bytes[0] == '<'.code.toByte()) {
                    val text = bytes.toString(Charsets.UTF_8).lowercase(Locale.ROOT)
                    evidence += path to text
                }
            }
        }

        val readableText = evidence.joinToString("\n") { it.second }
        val sdkMarkers = listOf("firebase", "facebook", "appsflyer", "adjust", "amplitude", "mixpanel", "onesignal", "braze")
            .filter { marker -> networkReport.contains(marker, ignoreCase = true) }

        buildString {
            appendLine("XhubREA — HUMAN BEHAVIOUR INTELLIGENCE")
            appendLine("Mode: bounded static scan of readable layout/values XML and correlated SDK clues")
            appendLine("Readable XML files scanned: $scannedFiles • Bytes inspected: $scannedBytes")
            appendLine()
            appendLine("1) POSSIBLE DESIGN PATTERNS")
            var totalFindings = 0
            for (pattern in patterns) {
                val matches = pattern.terms.filter { term -> readableText.contains(term) }.take(MAX_MATCHES_PER_PATTERN)
                if (matches.isNotEmpty()) {
                    totalFindings++
                    appendLine()
                    appendLine("• ${pattern.category}: ${pattern.label}")
                    appendLine(pattern.explanation)
                    appendLine("Text indicators: ${matches.joinToString(", ")}")
                    appendLine("Evidence: readable text in inspected res/layout or res/values XML.")
                    appendLine("Confidence: LIKELY design clue; actual screen placement and interaction are UNKNOWN.")
                    appendLine("Caution: ${pattern.caution}")
                }
            }
            if (totalFindings == 0) {
                appendLine("No listed wording patterns were found in the readable XML files inspected.")
                appendLine("This is not evidence that such patterns are absent; compiled, remote or dynamically generated UI was not inspected.")
            }
            appendLine()
            appendLine("2) ENGAGEMENT / NOTIFICATION SDK CLUES")
            if (sdkMarkers.isEmpty()) appendLine("No selected engagement/analytics SDK marker was correlated from the Network Intelligence summary.")
            else {
                appendLine("Static SDK markers: ${sdkMarkers.joinToString(", ")}")
                appendLine("These markers may indicate an integration dependency; they do not prove notifications, tracking, profiling or user targeting.")
            }
            appendLine()
            appendLine("3) ATTENTION & CHOICE INTERPRETATION")
            appendLine("A UI phrase can suggest a design pattern, but cannot show how often it appears, when it appears, or whether a user can easily dismiss it.")
            appendLine("No psychological outcome or manipulation is asserted by this static scan.")
            appendLine()
            appendLine("4) WHAT WOULD CONFIRM THE USER EXPERIENCE?")
            appendLine("Use an authorized manual test session on an app you own or are allowed to test. Record screen flow, prompt timing, available choices and dismissal behaviour.")
            appendLine("Compare the visible flow with the wording clues; keep observations separate from static evidence.")
            appendLine()
            appendLine("5) LIMITATIONS")
            appendLine("Binary XML, Compose-generated UI, WebView/remote content, server-driven experiments, runtime state, actual notification frequency and A/B tests are not inspected.")
            appendLine("The scan is bounded to at most 80 readable XML files, 512 KiB per file and 4 MiB total.")
        }
    }.getOrElse { error ->
        "Human Behaviour Intelligence could not complete: ${error.message ?: error.javaClass.simpleName}"
    }
}
