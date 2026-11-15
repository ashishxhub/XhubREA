package com.ashishxhub.xhubrea

import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Permission and manifest-based privacy intelligence.
 * Findings describe declared capabilities and static indicators only; they do not prove collection.
 */
internal object PrivacyIntelligenceAnalyzer {
    private const val MAX_ENTRIES = 100_000

    private data class PermissionInfo(
        val category: String,
        val explanation: String,
        val possibleData: String,
        val userNote: String
    )

    private val permissionInfo = mapOf(
        "android.permission.ACCESS_COARSE_LOCATION" to PermissionInfo("Location", "Approximate location access is declared.", "Approximate device location", "Check whether location is needed for the app's main feature."),
        "android.permission.ACCESS_FINE_LOCATION" to PermissionInfo("Location", "Precise location access is declared.", "More precise device location", "Use location only while the relevant feature needs it, where controls allow."),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionInfo("Location", "Background location access is declared.", "Location while the app is not in the foreground", "This deserves extra attention; review the app's explanation and Android permission settings."),
        "android.permission.CAMERA" to PermissionInfo("Camera", "Camera access is declared.", "Camera imagery or video when camera features are used", "A camera permission does not mean the camera is always active."),
        "android.permission.RECORD_AUDIO" to PermissionInfo("Microphone", "Microphone access is declared.", "Audio captured when microphone features are used", "A microphone permission does not prove recording is occurring."),
        "android.permission.READ_CONTACTS" to PermissionInfo("Contacts", "Read access to contacts is declared.", "Names, phone numbers and other contact fields exposed by Android", "Check whether contact discovery or sharing is an expected feature."),
        "android.permission.WRITE_CONTACTS" to PermissionInfo("Contacts", "Write access to contacts is declared.", "Ability to add or change contact records", "Consider whether modifying contacts is necessary for the app."),
        "android.permission.READ_CALENDAR" to PermissionInfo("Calendar", "Calendar read access is declared.", "Calendar event details", "Review whether calendar integration is an expected feature."),
        "android.permission.WRITE_CALENDAR" to PermissionInfo("Calendar", "Calendar write access is declared.", "Ability to add or change calendar events", "Review whether calendar editing is expected."),
        "android.permission.READ_PHONE_STATE" to PermissionInfo("Phone", "Phone-state access is declared.", "Some device or telephony state", "The exact fields depend on Android version and permission grants."),
        "android.permission.READ_CALL_LOG" to PermissionInfo("Call history", "Call-log read access is declared.", "Call history metadata", "This is sensitive; verify the app has a clear feature-based reason."),
        "android.permission.SEND_SMS" to PermissionInfo("SMS", "SMS sending permission is declared.", "Ability to send text messages", "A declaration does not prove messages are sent."),
        "android.permission.READ_SMS" to PermissionInfo("SMS", "SMS read permission is declared.", "SMS message content and metadata", "This is highly sensitive; verify necessity and granted access."),
        "android.permission.POST_NOTIFICATIONS" to PermissionInfo("Notifications", "Notification posting permission is declared.", "Ability to show notifications", "Notifications can be useful; review channel and frequency controls."),
        "android.permission.READ_MEDIA_IMAGES" to PermissionInfo("Photos and media", "Image media read access is declared.", "Selected or accessible image files, depending on Android version and access model", "Prefer Android's photo picker when supported."),
        "android.permission.READ_MEDIA_VIDEO" to PermissionInfo("Photos and media", "Video media read access is declared.", "Selected or accessible video files, depending on Android version and access model", "Check whether video access matches the app's feature."),
        "android.permission.READ_MEDIA_AUDIO" to PermissionInfo("Photos and media", "Audio media read access is declared.", "Accessible audio files", "Check whether audio-library access is expected."),
        "android.permission.READ_EXTERNAL_STORAGE" to PermissionInfo("Storage", "Legacy external-storage read access is declared.", "Files exposed by the Android storage model for the target OS", "Actual access is restricted by Android version and scoped-storage rules."),
        "android.permission.WRITE_EXTERNAL_STORAGE" to PermissionInfo("Storage", "Legacy external-storage write access is declared.", "Ability to write to some shared storage locations on older Android versions", "Actual access is restricted by Android version and scoped-storage rules."),
        "android.permission.BLUETOOTH_SCAN" to PermissionInfo("Nearby devices", "Bluetooth scan permission is declared.", "Nearby Bluetooth device signals and identifiers", "Check whether nearby-device discovery is a core feature."),
        "android.permission.BLUETOOTH_CONNECT" to PermissionInfo("Nearby devices", "Bluetooth connection permission is declared.", "Nearby Bluetooth device connection information", "A declaration does not prove continuous scanning."),
        "android.permission.NEARBY_WIFI_DEVICES" to PermissionInfo("Nearby devices", "Nearby Wi-Fi device permission is declared.", "Nearby Wi-Fi device interaction information", "Check whether local-device discovery is expected.")
    )

    fun run(apk: File, networkReport: String = ""): String = runCatching {
        require(apk.isFile && apk.length() > 0L) { "Choose a valid APK first." }
        ZipFile(apk).use { zip ->
            val entries = zip.entries().asSequence().toList()
            require(entries.size <= MAX_ENTRIES) { "APK has too many entries for this bounded scan." }
        }
        val manifest = BinaryManifestDecoder.decodeApk(apk)
        val declarations = parseManifestDeclarations(manifest)
        val permissions = declarations.filter { it.first == "uses-permission" || it.first == "uses-permission-sdk-23" }
            .mapNotNull { (_, attrs) -> attrs["name"]?.let { it to permissionInfo[it] } }
        val appAttrs = declarations.firstOrNull { it.first == "application" }?.second.orEmpty()
        val componentTags = setOf("activity", "activity-alias", "service", "receiver", "provider")
        val components = declarations.filter { it.first in componentTags }
        val background = components.filter { it.first == "service" || it.first == "receiver" }
        val sdkLines = networkReport.lineSequence().filter { line ->
            val normalized = line.trim()
            normalized in listOf("Firebase / Google services", "Facebook SDK", "AppsFlyer", "Adjust",
                "Amplitude", "Mixpanel", "Branch", "OneSignal", "Braze", "Sentry", "Bugsnag", "Datadog")
        }.toList()

        buildString {
            appendLine("XhubREA — PRIVACY INTELLIGENCE")
            appendLine("Mode: static, read-only manifest and bounded metadata correlation")
            appendLine()
            appendLine("1) DECLARED DATA-ACCESS CAPABILITIES")
            appendLine("Sensitive permission declarations with known categories: ${permissions.count { it.second != null }}")
            if (permissions.isEmpty()) appendLine("No recognized sensitive permission declarations were found in the decoded manifest.")
            permissions.forEach { (name, info) ->
                appendLine()
                appendLine("• ${info?.category ?: "Other permission"} — $name")
                appendLine(info?.explanation ?: "Permission is declared; its purpose is not classified by this module.")
                if (info != null) {
                    appendLine("Possible information/capability: ${info.possibleData}.")
                    appendLine("User note: ${info.userNote}")
                }
                appendLine("Evidence: AndroidManifest.xml permission declaration.")
                appendLine("Confidence: CONFIRMED declaration; actual grant and use are UNKNOWN.")
            }
            appendLine()
            appendLine("2) BACKGROUND & COMPONENT CLUES")
            appendLine("Activities / aliases: ${components.count { it.first == "activity" || it.first == "activity-alias" }}")
            appendLine("Services: ${components.count { it.first == "service" }}")
            appendLine("Receivers: ${components.count { it.first == "receiver" }}")
            appendLine("Providers: ${components.count { it.first == "provider" }}")
            appendLine("Application debuggable declaration: ${appAttrs["debuggable"] ?: "not resolved"}")
            appendLine("Component declarations indicate possible entry points or background capabilities, not proof that they are active.")
            appendLine()
            appendLine("3) THIRD-PARTY SDK INDICATORS")
            if (sdkLines.isEmpty()) appendLine("No listed SDK fingerprints were correlated from the Network Intelligence report.")
            else sdkLines.forEach { appendLine("• $it — static package indicator only; initialization and data transfer are UNKNOWN.") }
            appendLine()
            appendLine("4) PRIVACY INTERPRETATION")
            appendLine("• A permission declaration is a request/capability signal, not proof that Android granted it.")
            appendLine("• A granted permission is not proof that data was accessed.")
            appendLine("• An SDK fingerprint is not proof of tracking or transmission.")
            appendLine("• Actual data collection, retention, sharing and backend processing cannot be established from these static clues alone.")
            appendLine()
            appendLine("5) WHAT A USER CAN CHECK")
            appendLine("• Review the app's Android permission page and revoke permissions that are not needed.")
            appendLine("• Prefer one-time, selected-photo, or while-in-use access where Android offers it.")
            appendLine("• Review notification channels and disable categories that are not useful.")
            appendLine("• Read the app's privacy policy and account/data deletion options.")
            appendLine()
            appendLine("6) LIMITATIONS")
            appendLine("This module does not inspect private user data, execute the APK, monitor sensors, observe live traffic, or audit the service backend.")
            appendLine("Some permissions and behaviour may be added dynamically, hidden by obfuscation, or constrained by Android version and target SDK.")
        }
    }.getOrElse { error ->
        "Privacy Intelligence could not complete: ${error.message ?: error.javaClass.simpleName}"
    }

    private fun parseManifestDeclarations(report: String): List<Pair<String, Map<String, String>>> {
        val lines = report.lineSequence().toList()
        val result = mutableListOf<Pair<String, Map<String, String>>>()
        var index = 0
        while (index < lines.size) {
            val tag = Regex("""^<([A-Za-z0-9_.-]+)>$""").matchEntire(lines[index].trim())?.groupValues?.get(1)
            if (tag == null) {
                index++
                continue
            }
            val attrs = linkedMapOf<String, String>()
            index++
            while (index < lines.size) {
                val line = lines[index].trim()
                if (line.isBlank() || line.startsWith("<")) break
                val split = line.split(" = ", limit = 2)
                if (split.size == 2 && !split[0].startsWith("Meaning")) {
                    attrs[split[0].substringAfterLast(':')] = split[1]
                }
                index++
            }
            result += tag to attrs
        }
        return result
    }
}
