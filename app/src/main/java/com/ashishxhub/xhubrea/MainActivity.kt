package com.ashishxhub.xhubrea

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    XhubReaDashboard()
                }
            }
        }
    }
}

@Composable
private fun XhubReaDashboard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember {
        context.getSharedPreferences("xhubrea_project", android.content.Context.MODE_PRIVATE)
    }

    var target by remember { mutableStateOf(preferences.getString("project_name", "") ?: "") }
    var notes by remember { mutableStateOf(preferences.getString("project_notes", "") ?: "") }
    var status by remember {
        mutableStateOf("Create a project note for an app you own, an open-source app, or an app you have permission to study.")
    }
    var apkReport by remember { mutableStateOf("No APK inspected yet.") }
    var comparisonReport by remember { mutableStateOf("Select a second APK to compare.") }
    var secondApkReport by remember { mutableStateOf("") }
    var dexReport by remember { mutableStateOf("Choose an APK, then analyze its DEX headers.") }
    var resourceReport by remember { mutableStateOf("Choose an APK, then scan its resource file inventory.") }
    var resourceTypeReport by remember { mutableStateOf("Choose an APK, then analyze file types and sizes.") }
    var architectureReport by remember { mutableStateOf("Choose an APK, then inspect its high-level package layout.") }
    var manifestReport by remember { mutableStateOf("Choose an APK, then generate a manifest summary.") }
    var exposureReport by remember { mutableStateOf("Choose an APK, then review exported components.") }
    var engineReport by remember { mutableStateOf("Choose an APK, then detect likely app/game technology.") }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            status = "Reading APK metadata…"
            scope.launch {
                apkReport = withContext(Dispatchers.IO) {
                    runCatching {
                        val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            apkFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        } ?: error("Could not open the selected file.")

                        val inspectFlags = PackageManager.GET_PERMISSIONS or
                            PackageManager.GET_ACTIVITIES or
                            PackageManager.GET_SERVICES or
                            PackageManager.GET_RECEIVERS or
                            PackageManager.GET_PROVIDERS

                        val packageInfo = context.packageManager.getPackageArchiveInfo(
                            apkFile.absolutePath,
                            inspectFlags
                        ) ?: error("This file is not a readable Android APK.")

                        packageInfo.applicationInfo?.let { appInfo ->
                            appInfo.sourceDir = apkFile.absolutePath
                            appInfo.publicSourceDir = apkFile.absolutePath
                        }
                        val appLabel = packageInfo.applicationInfo?.loadLabel(context.packageManager)
                            ?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown"
                        val apkSize = android.text.format.Formatter.formatShortFileSize(
                            context,
                            apkFile.length()
                        )
                        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
                            .digest(apkFile.readBytes())
                            .joinToString("") { byte -> "%02x".format(byte) }

                        val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode.toLong()
                        }
                        val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            packageInfo.applicationInfo?.minSdkVersion?.toString() ?: "Unknown"
                        } else {
                            "Not available on this Android version"
                        }
                        val targetSdk = packageInfo.applicationInfo?.targetSdkVersion?.toString()
                            ?: "Unknown"
                        val appFlags = packageInfo.applicationInfo?.flags ?: 0
                        val appEnabled = packageInfo.applicationInfo?.enabled?.toString() ?: "Unknown"
                        val debuggable = (appFlags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                        val allowBackup = (appFlags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
                        val testOnly = (appFlags and android.content.pm.ApplicationInfo.FLAG_TEST_ONLY) != 0

                        val permissionList = packageInfo.requestedPermissions?.toList().orEmpty()
                        val permissionAnalysis = permissionList.map { permissionName ->
                            val category = runCatching {
                                val info = context.packageManager.getPermissionInfo(permissionName, 0)
                                when (info.protectionLevel and android.content.pm.PermissionInfo.PROTECTION_MASK_BASE) {
                                    android.content.pm.PermissionInfo.PROTECTION_DANGEROUS -> "Dangerous (runtime-sensitive)"
                                    android.content.pm.PermissionInfo.PROTECTION_SIGNATURE -> "Signature (restricted)"
                                    android.content.pm.PermissionInfo.PROTECTION_NORMAL -> "Normal"
                                    else -> "Other / special"
                                }
                            }.getOrDefault("Unknown / not available")
                            "$permissionName — $category"
                        }
                        val activities = packageInfo.activities?.map {
                            "${it.name} | exported=${it.exported} | enabled=${it.enabled}"
                        }.orEmpty()
                        val services = packageInfo.services?.map {
                            "${it.name} | exported=${it.exported} | enabled=${it.enabled}"
                        }.orEmpty()
                        val receivers = packageInfo.receivers?.map {
                            "${it.name} | exported=${it.exported} | enabled=${it.enabled}"
                        }.orEmpty()
                        val providers = packageInfo.providers?.map {
                            "${it.name} | exported=${it.exported} | enabled=${it.enabled}"
                        }.orEmpty()

                        buildString {
                            appendLine("APK metadata (read-only)")
                            appendLine("App name: $appLabel")
                            appendLine("APK file size: $apkSize")
                            appendLine("SHA-256: $sha256")
                            appendLine("Package: ${packageInfo.packageName}")
                            appendLine("Version name: ${packageInfo.versionName ?: "Unknown"}")
                            appendLine("Version code: $version")
                            appendLine("Minimum SDK: $minSdk")
                            appendLine("Target SDK: $targetSdk")
                            appendLine("Application enabled: $appEnabled")
                            appendLine("Debuggable: $debuggable")
                            appendLine("Allow backup flag: $allowBackup")
                            appendLine("Test-only flag: $testOnly")
                            appendLine()
                            appendLine("Declared permissions (${permissionList.size})")
                            appendLine(permissionList.take(30).joinToString("\n").ifBlank { "None declared" })
                            appendLine()
                            appendLine("Permission analysis (${permissionAnalysis.size})")
                            appendLine(permissionAnalysis.take(30).joinToString("\n").ifBlank { "No permissions to analyze" })
                            appendLine()
                            appendLine("Activities (${activities.size})")
                            appendLine(activities.take(20).joinToString("\n").ifBlank { "None found" })
                            appendLine()
                            appendLine("Services (${services.size})")
                            appendLine(services.take(20).joinToString("\n").ifBlank { "None found" })
                            appendLine()
                            appendLine("Receivers (${receivers.size})")
                            appendLine(receivers.take(20).joinToString("\n").ifBlank { "None found" })
                            appendLine()
                            appendLine("Providers (${providers.size})")
                            appendLine(providers.take(20).joinToString("\n").ifBlank { "None found" })
                            if (permissionList.size > 30) {
                                appendLine()
                                appendLine("Only the first 30 permissions are displayed.")
                            }
                        }
                    }.getOrElse { error ->
                        "Could not inspect this file: ${error.message ?: "Unknown error"}"
                    }
                }
                status = "APK metadata inspection finished."
            }
        }
    }

    val secondApkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            status = "Reading second APK for comparison…"
            scope.launch {
                secondApkReport = withContext(Dispatchers.IO) {
                    runCatching {
                        inspectApkForComparison(context, uri)
                    }.getOrElse { error ->
                        "Could not inspect second APK: ${error.message ?: "Unknown error"}"
                    }
                }
                comparisonReport = if (secondApkReport.startsWith("Could not inspect")) {
                    secondApkReport
                } else if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect")) {
                    "First inspect the primary APK, then compare it with this APK."
                } else {
                    compareApkReports(apkReport, secondApkReport)
                }
                status = "APK comparison finished."
            }
        }
    }

    val reportSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(apkReport.toByteArray(Charsets.UTF_8))
                } ?: error("Could not create the report file.")
            }.onSuccess {
                status = "Report saved successfully."
            }.onFailure { error ->
                status = "Could not save report: ${error.message ?: "Unknown error"}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "XhubREA",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Understand apps. Build original ideas.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        item {
            Text("Project workspace", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("App or game name") },
                placeholder = { Text("Example: My own Android app") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Project notes") },
                placeholder = { Text("Write visible features, screens and behavior...") },
                minLines = 4
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (target.isBlank()) {
                        status = "Enter an app or game name before saving."
                    } else {
                        preferences.edit()
                            .putString("project_name", target.trim())
                            .putString("project_notes", notes)
                            .apply()
                        status = "Saved on this device: ${target.trim()}"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save project notes")
            }
        }
        item {
            Text("Authorized APK metadata viewer", style = MaterialTheme.typography.titleLarge)
            Text(
                "Choose an APK file to read basic package metadata. The APK is not installed or executed.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    apkPicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose APK file")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        dexReport = "Inspect a readable APK first."
                    } else {
                        dexReport = "Reading DEX headers…"
                        scope.launch {
                            dexReport = withContext(Dispatchers.IO) {
                                runCatching { inspectDexHeaders(apkFile) }
                                    .getOrElse { error -> "Could not read DEX information: ${error.message ?: "Unknown error"}" }
                            }
                            status = "DEX header analysis finished."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analyze DEX file information")
            }
            InfoCard(title = "DEX File Information Explorer", body = dexReport)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        resourceReport = "Inspect a readable APK first."
                    } else {
                        resourceReport = "Scanning APK file inventory…"
                        scope.launch {
                            resourceReport = withContext(Dispatchers.IO) {
                                runCatching { inventoryApkResources(apkFile) }
                                    .getOrElse { error -> "Could not scan APK inventory: ${error.message ?: "Unknown error"}" }
                            }
                            status = "APK resource inventory finished. No files were extracted or executed."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan APK resource inventory")
            }
            InfoCard(title = "Resource & File Inventory (read-only)", body = resourceReport)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        resourceTypeReport = "Inspect a readable APK first."
                    } else {
                        resourceTypeReport = "Analyzing file types and sizes…"
                        scope.launch {
                            resourceTypeReport = withContext(Dispatchers.IO) {
                                runCatching { analyzeApkFileTypes(apkFile) }
                                    .getOrElse { error -> "Could not analyze file types: ${error.message ?: "Unknown error"}" }
                            }
                            status = "Resource type analysis finished. File contents were not opened."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analyze resource types")
            }
            InfoCard(title = "Resource Type Analyzer (read-only)", body = resourceTypeReport)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        architectureReport = "Inspect a readable APK first."
                    } else {
                        architectureReport = "Inspecting APK package layout…"
                        scope.launch {
                            architectureReport = withContext(Dispatchers.IO) {
                                runCatching { inspectApkArchitecture(apkFile) }
                                    .getOrElse { error -> "Could not inspect package layout: ${error.message ?: "Unknown error"}" }
                            }
                            status = "APK architecture overview finished. No files were extracted or executed."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Explore APK architecture")
            }
            InfoCard(title = "APK Architecture Explorer (read-only)", body = architectureReport)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    manifestReport = if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect")) {
                        "Inspect a readable APK first."
                    } else {
                        summarizeManifest(apkReport)
                    }
                    status = "Manifest summary generated from PackageManager metadata."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate manifest summary")
            }
            InfoCard(title = "Manifest Summary Analyzer (read-only)", body = manifestReport)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    exposureReport = reviewExportedComponents(apkReport)
                    status = "Exported component review generated from APK metadata."
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Review exported components")
            }
            InfoCard(title = "Component Exposure Review (read-only)", body = exposureReport)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        engineReport = "Inspect a readable APK first."
                    } else {
                        engineReport = "Checking package fingerprints…"
                        scope.launch {
                            engineReport = withContext(Dispatchers.IO) {
                                runCatching { detectAppGameTechnology(apkFile) }
                                    .getOrElse { error -> "Technology detection failed: ${error.message ?: "Unknown error"}" }
                            }
                            status = "Technology fingerprint scan finished. Results are heuristic."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Detect app / game technology")
            }
            InfoCard(title = "Game Engine & Framework Detector (heuristic)", body = engineReport)
            Spacer(modifier = Modifier.height(8.dp))
            InfoCard(title = "APK report", body = apkReport)
            Spacer(modifier = Modifier.height(8.dp))
            Text("APK App Information Dashboard", style = MaterialTheme.typography.titleLarge)
            InfoCard(title = "App identity", body = reportSection(apkReport, "App name:"))
            InfoCard(title = "Package and version", body = reportSection(apkReport, "Package:"))
            InfoCard(title = "File size", body = reportSection(apkReport, "APK file size:"))
            InfoCard(title = "APK SHA-256 fingerprint", body = reportSection(apkReport, "SHA-256:"))
            InfoCard(title = "Android compatibility", body = reportSection(apkReport, "Minimum SDK:"))
            InfoCard(title = "Application manifest flags", body = reportSection(apkReport, "Application enabled:"))
            Button(
                onClick = {
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect")) {
                        status = "Inspect a readable APK before exporting its report."
                    } else {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "XhubREA APK metadata report")
                            putExtra(Intent.EXTRA_TEXT, apkReport)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share APK report"))
                        status = "APK report is ready to share."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export / Share APK report")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect")) {
                        status = "Inspect a readable APK before saving its report."
                    } else {
                        reportSaver.launch("XhubREA_APK_Report.txt")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save report as TXT file")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("APK Comparison Engine", style = MaterialTheme.typography.titleLarge)
            Text(
                "Inspect one APK above, then choose a second APK to compare basic metadata. Both files are read-only and are not installed or executed.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    secondApkPicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose second APK")
            }
            InfoCard(title = "Comparison results", body = comparisonReport)
            Spacer(modifier = Modifier.height(12.dp))
            Text("App Components Explorer", style = MaterialTheme.typography.titleLarge)
            Text(
                "Component lists are read from the selected APK manifest. They are metadata only and are not launched.",
                style = MaterialTheme.typography.bodySmall
            )
            InfoCard(title = "Activities", body = reportSection(apkReport, "Activities"))
            InfoCard(title = "Services", body = reportSection(apkReport, "Services"))
            InfoCard(title = "Broadcast receivers", body = reportSection(apkReport, "Receivers"))
            InfoCard(title = "Content providers", body = reportSection(apkReport, "Providers"))
            InfoCard(title = "Declared permissions", body = reportSection(apkReport, "Declared permissions"))
            InfoCard(title = "Permission risk overview", body = reportSection(apkReport, "Permission analysis"))
            Text(
                "Permission categories describe Android's declared protection level. They do not prove how an app uses a permission or whether its behavior is safe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            InfoCard(
                title = "📦 App overview",
                body = "Record the app name, version, developer, supported Android version and source."
            )
        }
        item {
            InfoCard(
                title = "🧭 Feature mapping",
                body = "Document screens, buttons, navigation and user-visible behavior."
            )
        }
        item {
            InfoCard(
                title = "🛠️ Build an alternative",
                body = "Plan and implement an original app with your own code, design and assets."
            )
        }
        item {
            InfoCard(title = "Current status", body = status)
        }
        item {
            Text(
                text = "Notes are stored locally on this device. Use only apps you own, open-source projects, or apps you have permission to inspect. This viewer reads basic APK metadata only; it does not bypass protections or execute the selected APK.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


private fun detectAppGameTechnology(apkFile: File): String {
    return ZipFile(apkFile).use { zip ->
        val paths = zip.entries().asSequence()
            .filter { !it.isDirectory }
            .map { it.name.lowercase() }
            .toList()
        val fingerprints = listOf(
            "Unity" to listOf("lib/arm64-v8a/libunity.so", "lib/armeabi-v7a/libunity.so", "assets/bin/data/", "assets/bin/data/managed/"),
            "Unreal Engine" to listOf("lib/arm64-v8a/libue4.so", "lib/armeabi-v7a/libue4.so", "lib/arm64-v8a/libunreal.so", "assets/ue4game/"),
            "Godot" to listOf("lib/arm64-v8a/libgodot_android.so", "lib/armeabi-v7a/libgodot_android.so", "assets/.godot/"),
            "Flutter" to listOf("lib/arm64-v8a/libflutter.so", "lib/armeabi-v7a/libflutter.so", "assets/flutter_assets/"),
            "React Native" to listOf("lib/arm64-v8a/libreactnative.so", "assets/index.android.bundle", "lib/armeabi-v7a/libreactnative.so"),
            "Xamarin / .NET for Android" to listOf("assemblies/monodroid.dll", "lib/arm64-v8a/libmonodroid.so", "assemblies/")
        )
        val matches = fingerprints.mapNotNull { (technology, markers) ->
            val found = markers.filter { marker -> paths.any { path ->
                if (marker.endsWith("/")) path.startsWith(marker) else path == marker || path.endsWith("/" + marker.substringAfterLast('/'))
            } }
            if (found.isNotEmpty()) technology to found else null
        }
        val nativeLibraries = paths.filter { it.startsWith("lib/") && it.endsWith(".so") }
        val frameworkEvidence = paths.filter { path ->
            path.startsWith("assets/flutter_assets/") ||
                path.startsWith("assets/bin/data/") ||
                path.startsWith("assets/ue4game/") ||
                path.endsWith("index.android.bundle")
        }.take(25)

        buildString {
            appendLine("App / Game Technology Fingerprint — read-only heuristic")
            appendLine("Package entries scanned: ${paths.size}")
            appendLine()
            appendLine("Likely technology matches")
            if (matches.isEmpty()) {
                appendLine("No known engine/framework fingerprint matched.")
                appendLine("Possible explanations: native Android app, renamed/packed files, uncommon engine, or insufficient evidence.")
            } else {
                matches.forEach { (technology, evidence) ->
                    appendLine("$technology — possible match")
                    evidence.forEach { appendLine("  Evidence: $it") }
                }
            }
            appendLine()
            appendLine("Native libraries (.so): ${nativeLibraries.size}")
            nativeLibraries.take(40).forEach { appendLine(it) }
            if (nativeLibraries.size > 40) appendLine("Showing 40 of ${nativeLibraries.size} native libraries.")
            appendLine()
            appendLine("Additional framework-related paths")
            frameworkEvidence.forEach { appendLine(it) }
            if (frameworkEvidence.isEmpty()) appendLine("No additional known framework paths found.")
            appendLine()
            append("Important: these are filename/path fingerprints, not proof of the engine or framework. Obfuscation, custom packaging, split APKs, and engine versions can change the layout. File contents were not extracted or executed.")
        }
    }
}


private fun inspectApkArchitecture(apkFile: File): String {
    return ZipFile(apkFile).use { zip ->
        val entries = zip.entries().asSequence()
            .filter { !it.isDirectory }
            .toList()
        val paths = entries.map { it.name }

        val topLevel = entries.groupBy { entry ->
            entry.name.substringBefore('/', missingDelimiterValue = "[root files]")
        }.mapValues { groupedEntries -> groupedEntries.size }.toSortedMap()

        val importantFiles = listOf(
            "AndroidManifest.xml",
            "resources.arsc",
            "classes.dex"
        )

        val dexFiles = paths.filter { Regex("""classes(\d*)\.dex""").matches(it) }.sorted()
        val hasSigningMetadata = paths.any { path ->
            path.startsWith("META-INF/") &&
                (path.endsWith(".RSA", true) || path.endsWith(".DSA", true) ||
                    path.endsWith(".EC", true) || path.endsWith(".SF", true))
        }

        buildString {
            appendLine("APK Architecture Overview — ZIP metadata only")
            appendLine("Total entries: ${entries.size}")
            appendLine()
            appendLine("Important package entries")
            importantFiles.forEach { name ->
                val present = if (name == "classes.dex") dexFiles.isNotEmpty() else name in paths
                appendLine("$name: ${if (present) "Present" else "Not found"}")
            }
            appendLine("DEX files found: ${dexFiles.size}")
            appendLine("Signing-related metadata files: ${if (hasSigningMetadata) "Present" else "Not found"}")
            appendLine("Note: presence of signing metadata does not verify APK signature validity.")
            appendLine()
            appendLine("Top-level package layout (folder | file count)")
            topLevel.forEach { (folder, count) -> appendLine("$folder | $count") }
            appendLine()
            appendLine("Common APK areas")
            appendLine("res/: Android resources")
            appendLine("assets/: bundled app assets")
            appendLine("lib/: native libraries")
            appendLine("META-INF/: package metadata and possible signing-related files")
            appendLine()
            append("This is a structural overview only; file contents were not extracted or executed.")
        }
    }
}

private fun analyzeApkFileTypes(apkFile: File): String {
    return ZipFile(apkFile).use { zip ->
        val entries = zip.entries().asSequence()
            .filter { !it.isDirectory }
            .toList()

        fun extensionOf(path: String): String {
            val name = path.substringAfterLast('/')
            val dot = name.lastIndexOf('.')
            return if (dot > 0 && dot < name.lastIndex) {
                name.substring(dot + 1).lowercase()
            } else {
                "[no extension]"
            }
        }

        val grouped = entries.groupBy { extensionOf(it.name) }
            .mapValues { (_, files) ->
                files.size to files.sumOf { it.size.coerceAtLeast(0L) }
            }
            .toList()
            .sortedWith(compareByDescending<Pair<String, Pair<Int, Long>>> { it.second.first }.thenBy { it.first })

        val largest = entries.sortedByDescending { it.size }.take(10)
        val totalBytes = entries.sumOf { it.size.coerceAtLeast(0L) }

        buildString {
            appendLine("Resource Type Analysis — metadata only")
            appendLine("Files analyzed: ${entries.size}")
            appendLine("Known uncompressed-size total: $totalBytes bytes")
            appendLine()
            appendLine("File types (extension | count | total size)")
            grouped.take(40).forEach { (extension, stats) ->
                appendLine("$extension | ${stats.first} | ${stats.second} bytes")
            }
            if (grouped.size > 40) {
                appendLine("Showing 40 of ${grouped.size} file types.")
            }
            appendLine()
            appendLine("Largest files (up to 10)")
            largest.forEach { entry ->
                appendLine("${entry.name} | ${entry.size} bytes")
            }
            appendLine()
            append("Only ZIP directory metadata was read; file contents were not opened or extracted.")
        }
    }
}

private fun inventoryApkResources(apkFile: File): String {
    return ZipFile(apkFile).use { zip ->
        val entries = zip.entries().asSequence()
            .filter { !it.isDirectory }
            .toList()

        fun countMatching(prefix: String): Int =
            entries.count { it.name.startsWith(prefix) }

        val categories = listOf(
            "Android resources (res/)" to countMatching("res/"),
            "App assets (assets/)" to countMatching("assets/"),
            "Native libraries (lib/)" to countMatching("lib/"),
            "META-INF files" to countMatching("META-INF/"),
            "DEX bytecode files" to entries.count { Regex("""classes(\d*)\.dex""").matches(it.name) }
        )

        buildString {
            appendLine("APK file inventory — read-only")
            appendLine("Total files: ${entries.size}")
            appendLine()
            appendLine("Category counts")
            categories.forEach { (label, count) -> appendLine("$label: $count") }
            appendLine()
            appendLine("File paths (first 80)")
            entries.take(80).forEach { entry ->
                appendLine("${entry.name} — ${entry.size} bytes")
            }
            if (entries.size > 80) {
                appendLine()
                appendLine("Showing 80 of ${entries.size} files.")
            }
            appendLine()
            append("No files were extracted, modified, or executed.")
        }
    }
}

private fun inspectDexHeaders(apkFile: File): String {
    return ZipFile(apkFile).use { zip ->
        val entries = zip.entries().asSequence()
            .filter { !it.isDirectory && Regex("""classes(\d*)\.dex""").matches(it.name) }
            .sortedBy { it.name }
            .toList()

        if (entries.isEmpty()) {
            return@use "No DEX files were found in this APK."
        }

        entries.joinToString("\n\n") { entry ->
            val header = ByteArray(112)
            zip.getInputStream(entry).use { input ->
                java.io.DataInputStream(input).readFully(header)
            }
            val magic = header.copyOfRange(0, 8).toString(Charsets.US_ASCII)
                .replace("\u0000", "")
            if (!magic.startsWith("dex\n")) {
                return@joinToString "${entry.name}\nInvalid or unrecognized DEX header."
            }

            val data = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            fun uint(offset: Int): Long = data.getInt(offset).toLong() and 0xffffffffL

            buildString {
                appendLine("File: ${entry.name}")
                appendLine("DEX magic: $magic")
                appendLine("ZIP entry size: ${entry.size} bytes")
                appendLine("Header file size: ${uint(32)} bytes")
                appendLine("Header size: ${uint(36)} bytes")
                appendLine("Endian tag: 0x${uint(40).toString(16)}")
                appendLine("String IDs: ${uint(56)}")
                appendLine("Type IDs: ${uint(64)}")
                appendLine("Method IDs: ${uint(88)}")
                appendLine("Class definitions: ${uint(96)}")
                append("Header inspection: read-only")
            }
        }
    }
}

private fun inspectApkForComparison(
    context: android.content.Context,
    uri: android.net.Uri
): String {
    val apkFile = File.createTempFile("xhubrea_compare_", ".apk", context.cacheDir)
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            apkFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open the selected file.")

        val flags = PackageManager.GET_PERMISSIONS
        val info = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: error("This file is not a readable Android APK.")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        apkFile.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val sha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        val permissions = info.requestedPermissions?.toSet().orEmpty()
        val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            info.applicationInfo?.minSdkVersion?.toString() ?: "Unknown"
        } else {
            "Unavailable"
        }
        val targetSdk = info.applicationInfo?.targetSdkVersion?.toString() ?: "Unknown"

        return buildString {
            appendLine("App name: ${info.applicationInfo?.loadLabel(context.packageManager) ?: "Unknown"}")
            appendLine("Package: ${info.packageName}")
            appendLine("Version name: ${info.versionName ?: "Unknown"}")
            appendLine("Version code: $versionCode")
            appendLine("Minimum SDK: $minSdk")
            appendLine("Target SDK: $targetSdk")
            appendLine("APK file size: ${apkFile.length()}")
            appendLine("SHA-256: $sha256")
            appendLine("Permissions: ${permissions.size}")
            appendLine(permissions.sorted().joinToString(","))
        }
    } finally {
        apkFile.delete()
    }
}

private fun compareApkReports(first: String, second: String): String {
    val keys = listOf(
        "App name:",
        "Package:",
        "Version name:",
        "Version code:",
        "Minimum SDK:",
        "Target SDK:",
        "APK file size:",
        "SHA-256:",
        "Permissions:"
    )
    val firstLines = first.lines()
    val secondLines = second.lines()
    return buildString {
        appendLine("Comparison (Primary APK vs Second APK)")
        keys.forEach { key ->
            val a = firstLines.firstOrNull { it.startsWith(key) }?.substringAfter(key)?.trim() ?: "Not available"
            val b = secondLines.firstOrNull { it.startsWith(key) }?.substringAfter(key)?.trim() ?: "Not available"
            appendLine()
            appendLine(key.removeSuffix(":"))
            appendLine("Primary: $a")
            appendLine("Second: $b")
            appendLine(if (a == b) "Result: SAME" else "Result: DIFFERENT")
        }
        val firstPermissions = firstLines.dropWhile { !it.startsWith("Declared permissions (") }
            .drop(1).takeWhile { it.isNotBlank() }.filter { it != "None declared" }.toSet()
        val secondPermissionsLine = secondLines.firstOrNull { it.startsWith("Permissions:") }
        val secondPermissions = secondPermissionsLine?.substringAfter(":")?.split(",")
            ?.filter { it.isNotBlank() }?.toSet().orEmpty()
        val added = secondPermissions - firstPermissions
        val removed = firstPermissions - secondPermissions
        appendLine()
        appendLine("Permission differences")
        appendLine("Added in second APK: ${added.joinToString().ifBlank { "None" }}")
        appendLine("Not present in second APK: ${removed.joinToString().ifBlank { "None" }}")
    }
}

private fun summarizeManifest(report: String): String {
    if (report == "No APK inspected yet." || report.startsWith("Could not inspect")) {
        return "Choose a readable APK first."
    }

    fun metadata(label: String): String =
        report.lineSequence().firstOrNull { it.startsWith(label) }
            ?.substringAfter(label)?.trim() ?: "Not available"

    fun section(name: String): List<String> {
        val lines = report.lines()
        val index = lines.indexOfFirst { it.startsWith("$name (") }
        if (index < 0) return emptyList()
        return lines.drop(index + 1).takeWhile { it.isNotBlank() }
            .filterNot { it == "None found" }
    }

    val activities = section("Activities")
    val services = section("Services")
    val receivers = section("Receivers")
    val providers = section("Providers")
    val allComponents = activities + services + receivers + providers
    val exportedCount = allComponents.count { it.contains("exported=true", ignoreCase = true) }
    val enabledCount = allComponents.count { it.contains("enabled=true", ignoreCase = true) }
    val permissionHeader = report.lineSequence()
        .firstOrNull { it.startsWith("Declared permissions (") } ?: "Declared permissions (0)"
    val permissionCount = Regex("""\\((\\d+)\\)""").find(permissionHeader)
        ?.groupValues?.getOrNull(1) ?: "0"
    val permissionAnalysis = report.lineSequence()
        .dropWhile { !it.startsWith("Permission analysis (") }
        .drop(1)
        .takeWhile { it.isNotBlank() }
        .toList()
    val categories = listOf("Normal", "Dangerous (runtime-sensitive)", "Signature (restricted)", "Other / special", "Unknown / not available")

    return buildString {
        appendLine("Manifest Summary — PackageManager metadata")
        appendLine("App: ${metadata("App name:")}")
        appendLine("Package: ${metadata("Package:")}")
        appendLine("Version: ${metadata("Version name:")} (${metadata("Version code:")})")
        appendLine("Minimum SDK: ${metadata("Minimum SDK:")}")
        appendLine("Target SDK: ${metadata("Target SDK:")}")
        appendLine()
        appendLine("Application flags")
        appendLine("Enabled: ${metadata("Application enabled:")}")
        appendLine("Debuggable: ${metadata("Debuggable:")}")
        appendLine("Allow backup: ${metadata("Allow backup flag:")}")
        appendLine("Test-only: ${metadata("Test-only flag:")}")
        appendLine()
        appendLine("Component summary")
        appendLine("Activities: ${activities.size}")
        appendLine("Services: ${services.size}")
        appendLine("Receivers: ${receivers.size}")
        appendLine("Providers: ${providers.size}")
        appendLine("Total listed components: ${allComponents.size}")
        appendLine("Exported components: $exportedCount")
        appendLine("Enabled components: $enabledCount")
        appendLine()
        appendLine("Declared permissions: $permissionCount")
        categories.forEach { category ->
            val count = permissionAnalysis.count { it.contains("— $category") }
            appendLine("$category: $count")
        }
        appendLine()
        append("This is a summary of metadata exposed by Android PackageManager, not a raw binary XML decode. Exported status alone does not establish a vulnerability.")
    }
}

private fun reviewExportedComponents(report: String): String {
    if (report == "No APK inspected yet." || report.startsWith("Could not inspect")) {
        return "Inspect a readable APK first."
    }

    fun readSection(name: String): List<String> {
        val lines = report.lines()
        val index = lines.indexOfFirst { it.startsWith("$name (") }
        if (index < 0) return emptyList()
        return lines.drop(index + 1).takeWhile { it.isNotBlank() }
            .filterNot { it == "None found" }
    }

    val groups = listOf(
        "Activities" to readSection("Activities"),
        "Services" to readSection("Services"),
        "Receivers" to readSection("Receivers"),
        "Providers" to readSection("Providers")
    )
    val exported = groups.flatMap { (kind, components) ->
        components.filter { it.contains("exported=true", ignoreCase = true) }
            .map { "$kind: $it" }
    }
    val notExported = groups.sumOf { (_, components) ->
        components.count { it.contains("exported=false", ignoreCase = true) }
    }

    return buildString {
        appendLine("Exported component review — read-only metadata")
        appendLine("Exported components found in displayed metadata: ${exported.size}")
        appendLine("Components marked not exported: $notExported")
        appendLine()
        appendLine("Exported component entries")
        if (exported.isEmpty()) {
            appendLine("No exported components were listed in the available metadata.")
        } else {
            exported.take(40).forEach { appendLine(it) }
            if (exported.size > 40) appendLine("Showing 40 of ${exported.size} entries.")
        }
        appendLine()
        append("Exported means Android may allow other apps or system components to address a component, subject to its configuration and permissions. This status alone is not a security flaw; review intended design and access controls. Only the first 20 components per category are included in the base report.")
    }
}

private fun reportSection(report: String, heading: String): String {
    if (report == "No APK inspected yet." || report.startsWith("Could not inspect")) {
        return "Choose a readable APK to view this section."
    }

    val lines = report.lines()

    // Metadata cards use labels such as "App name:" followed by a value
    // on the same line. Return that value instead of looking for a section.
    val metadataLine = lines.firstOrNull { it.startsWith(heading) }
    if (metadataLine != null && heading.endsWith(":")) {
        return metadataLine.substringAfter(heading).trim().ifBlank { "Not available." }
    }

    // List sections use headings such as "Activities (3)" and have
    // their entries on the following lines.
    val headerIndex = lines.indexOfFirst { it.startsWith("$heading (") }
    if (headerIndex < 0) return "Section not available."

    val content = lines.drop(headerIndex + 1).takeWhile { it.isNotBlank() }
    return content.joinToString("\n").ifBlank { "No entries found." }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
