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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
        // Ask Android for the fastest refresh mode available on this display.
        // The OS may still choose a lower rate to save battery or for thermal reasons.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val supportedModes = windowManager.defaultDisplay.supportedModes
            val fastestRefreshRate = supportedModes.maxOfOrNull { it.refreshRate } ?: 60f
            window.attributes = window.attributes.apply {
                preferredRefreshRate = fastestRefreshRate
            }
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF8AB4FF),
                    onPrimary = Color(0xFF082044),
                    secondary = Color(0xFF63D7D0),
                    background = Color(0xFF080F1E),
                    surface = Color(0xFF111D31),
                    surfaceVariant = Color(0xFF1C2B43),
                    onSurface = Color(0xFFEAF1FF),
                    onSurfaceVariant = Color(0xFFB1C0D8),
                    onBackground = Color(0xFFEAF1FF),
                    secondaryContainer = Color(0xFF123A43),
                    onSecondaryContainer = Color(0xFFD7FFFA),
                    outline = Color(0xFF344761)
                ),
                shapes = Shapes(
                    extraSmall = RoundedCornerShape(8.dp),
                    small = RoundedCornerShape(12.dp),
                    medium = RoundedCornerShape(18.dp),
                    large = RoundedCornerShape(24.dp),
                    extraLarge = RoundedCornerShape(28.dp)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    XhubReaDashboard()
                }
            }
        }
    }
}

@Composable
private fun XhubReaDashboard() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
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
    var nativeLibraryReport by remember { mutableStateOf("Choose an APK, then analyze native library ABIs and file sizes.") }
    var signingCertificateReport by remember { mutableStateOf("Choose an APK, then inspect its signing certificate metadata.") }
    var fullResearchReport by remember { mutableStateOf("Choose an APK, then run the full research scan.") }
    var fullAnalysisSummary by remember { mutableStateOf("Run Complete Analysis to see each module result here.") }
    var isCompleteAnalysisRunning by remember { mutableStateOf(false) }

    var showSettings by remember { mutableStateOf(false) }
    var simpleExplain by remember { mutableStateOf(preferences.getBoolean("simple_explain", true)) }
    var showHints by remember { mutableStateOf(preferences.getBoolean("show_hints", true)) }
    var smoothMotion by remember { mutableStateOf(preferences.getBoolean("smooth_motion", true)) }
    var requestHighRefresh by remember { mutableStateOf(preferences.getBoolean("high_refresh", true)) }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            status = "Reading APK metadata…"
            scope.launch {
                apkReport = withContext(Dispatchers.IO) {
                    runCatching {
                        val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                        copyUriToFileBounded(context, uri, apkFile)

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
                        val sha256 = sha256File(apkFile)

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
                    output.write(buildCombinedResearchReport(apkReport, comparisonReport, dexReport, resourceReport, resourceTypeReport, architectureReport, manifestReport, exposureReport, engineReport, nativeLibraryReport, signingCertificateReport, fullResearchReport).toByteArray(Charsets.UTF_8))
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
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF17365F)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "XhubREA",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Understand apps. Build original ideas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFDCE7FF)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF24517A)
                    ) {
                        Text(
                            text = "✦  STATIC APK INTELLIGENCE",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WORKSPACE STATUS", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(status, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = { showSettings = false }, modifier = Modifier.weight(1f)) {
                    Text("⌂ Home")
                }
                Button(onClick = { showSettings = true }, modifier = Modifier.weight(1f)) {
                    Text("⚙ Settings")
                }
            }
        }
        if (showSettings) {
            item {
                SectionHeading("Settings", "App ko apne hisaab se set karein.")
                PreferenceRow("Simple explanation", "Technical baat ko chhote, aasaan steps mein samjhao.", simpleExplain) {
                    simpleExplain = it
                    preferences.edit().putBoolean("simple_explain", it).apply()
                }
                PreferenceRow("Hints & clues", "Har result ke saath next clue aur kya check karein dikhaye.", showHints) {
                    showHints = it
                    preferences.edit().putBoolean("show_hints", it).apply()
                }
                PreferenceRow("Smooth motion", "Cards aur sections ke expand/collapse par smooth animation.", smoothMotion) {
                    smoothMotion = it
                    preferences.edit().putBoolean("smooth_motion", it).apply()
                }
                PreferenceRow("High refresh preference", "Device support kare to highest refresh rate request kare. Battery/heat ke kaaran Android ise badal sakta hai.", requestHighRefresh) {
                    requestHighRefresh = it
                    preferences.edit().putBoolean("high_refresh", it).apply()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val rate = if (it) activity?.windowManager?.defaultDisplay?.supportedModes?.maxOfOrNull { mode -> mode.refreshRate } ?: 60f else 60f
                        activity?.window?.let { appWindow ->
                            appWindow.attributes = appWindow.attributes.apply { preferredRefreshRate = rate }
                        }
                    }
                }
                InfoCard(
                    "Refresh-rate note",
                    "120 Hz ka matlab screen ek second mein 120 baar tak update ho sakti hai. XhubREA highest supported mode request karta hai, lekin final choice phone, battery saver, heat aur Android ke control mein hoti hai. Isliye 120 Hz har phone par guarantee nahi hai."
                )
            }
        } else {
            item {
                SectionHeading("Understand it like a beginner", "XhubREA report ko simple story ki tarah padhein.")
                Card(
                    modifier = Modifier.fillMaxWidth().then(
                        if (smoothMotion) Modifier.animateContentSize(animationSpec = tween(280)) else Modifier
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🧩 App ko samajhne ka simple map", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (simpleExplain)
                                "APK select karo → XhubREA uske package ka safe, read-only data padhta hai → files aur components ko group karta hai → clues nikalta hai → report banata hai. App ko install ya run nahi karta."
                            else
                                "Select APK → parse package metadata and bounded ZIP/DEX metadata → classify components/resources → generate heuristic observations and a local report.",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showHints) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Text("💡 Clue: DEX = app ka compiled code container. res/ = screen ke text, icons aur layouts ke resources. lib/ = native machine-code libraries. Manifest = app ki basic declaration sheet.",
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(6.dp))
                            Text("🔎 Kaise samjhein: pehle 'kya mila' dekho, phir 'iska matlab' padho, phir 'kyun important hai' aur 'kya confirm nahi hua' check karo.",
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        item {
            SectionHeading(title = "Project workspace", subtitle = "Keep notes and observations for your authorized research.")
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
            SectionHeading(title = "APK research tools", subtitle = "Choose an APK once, then run the analysis modules below.")
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
                        status = "Choose and inspect an APK before running the complete analysis."
                    } else {
                        isCompleteAnalysisRunning = true
                        status = "Running complete analysis suite…"
                        scope.launch {
                            val results = withContext(Dispatchers.IO) {
                                listOf(
                                    runCatching { inspectDexHeaders(apkFile) }
                                        .getOrElse { "DEX analysis failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { inventoryApkResources(apkFile) }
                                        .getOrElse { "Resource inventory failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { analyzeApkFileTypes(apkFile) }
                                        .getOrElse { "Resource type analysis failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { inspectApkArchitecture(apkFile) }
                                        .getOrElse { "Architecture analysis failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { summarizeManifest(apkReport) }
                                        .getOrElse { "Manifest summary failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { reviewExportedComponents(apkReport) }
                                        .getOrElse { "Component review failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { detectAppGameTechnology(apkFile) }
                                        .getOrElse { "Technology detection failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { analyzeNativeLibraries(apkFile) }
                                        .getOrElse { "Native library analysis failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { analyzeApkSigningCertificates(context, apkFile) }
                                        .getOrElse { "Certificate inspection failed: ${it.message ?: "Unknown error"}" },
                                    runCatching { ResearchLab.run(apkFile) }
                                        .getOrElse { "Research Lab failed: ${it.message ?: "Unknown error"}" }
                                )
                            }
                            dexReport = results[0]
                            resourceReport = results[1]
                            resourceTypeReport = results[2]
                            architectureReport = results[3]
                            manifestReport = results[4]
                            exposureReport = results[5]
                            engineReport = results[6]
                            nativeLibraryReport = results[7]
                            signingCertificateReport = results[8]
                            fullResearchReport = results[9]
                            val moduleNames = listOf(
                                "DEX headers", "Resource inventory", "Resource types", "APK architecture",
                                "Manifest summary", "Exported components", "App/game technology",
                                "Native libraries", "Signing certificate metadata", "Research Lab"
                            )
                            val failurePrefixes = listOf(
                                "DEX analysis failed", "Resource inventory failed", "Resource type analysis failed",
                                "Architecture analysis failed", "Manifest summary failed", "Component review failed",
                                "Technology detection failed", "Native library analysis failed",
                                "Certificate inspection failed", "Research Lab failed"
                            )
                            fullAnalysisSummary = buildString {
                                appendLine("Complete analysis module results")
                                results.forEachIndexed { index, result ->
                                    val failed = result.startsWith(failurePrefixes[index], ignoreCase = true)
                                    appendLine("${if (failed) "FAILED" else "COMPLETED"} — ${moduleNames[index]}")
                                }
                                appendLine()
                                appendLine("A completed module may still report limited or heuristic findings. Read each detailed report before drawing conclusions.")
                            }
                            status = "Complete analysis finished. Review each module and its limitations below."
                            isCompleteAnalysisRunning = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleteAnalysisRunning
            ) {
                Text(if (isCompleteAnalysisRunning) "Analysis Running…" else "Run Complete Analysis — All Modules")
            }
            Text(
                "Runs the available read-only analysis modules in sequence after an APK is selected. The button is disabled while the analysis is running.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoCard(title = "Complete Analysis — Module Status", body = fullAnalysisSummary)
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
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        nativeLibraryReport = "Inspect a readable APK first."
                    } else {
                        nativeLibraryReport = "Analyzing native library metadata…"
                        scope.launch {
                            nativeLibraryReport = withContext(Dispatchers.IO) {
                                runCatching { analyzeNativeLibraries(apkFile) }
                                    .getOrElse { error -> "Native library analysis failed: ${error.message ?: "Unknown error"}" }
                            }
                            status = "Native library ABI analysis finished. No library was loaded or executed."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analyze native libraries")
            }
            InfoCard(title = "Native Library & ABI Analyzer (read-only)", body = nativeLibraryReport)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        signingCertificateReport = "Inspect a readable APK first."
                    } else {
                        signingCertificateReport = "Reading APK signer certificate metadata…"
                        scope.launch {
                            signingCertificateReport = withContext(Dispatchers.IO) {
                                runCatching { analyzeApkSigningCertificates(context, apkFile) }
                                    .getOrElse { error -> "Signing certificate inspection failed: ${error.message ?: "Unknown error"}" }
                            }
                            status = "APK signing certificate metadata inspection finished. This is not a full signature validation."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Inspect APK signing certificates")
            }
            InfoCard(title = "APK Signing Certificate Inspector (metadata)", body = signingCertificateReport)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        fullResearchReport = "Inspect a readable APK first."
                    } else {
                        fullResearchReport = "Running bounded read-only research scan…"
                        scope.launch {
                            fullResearchReport = withContext(Dispatchers.IO) {
                                ResearchLab.run(apkFile)
                            }
                            status = "Full research scan finished. Review the limitations in the report."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Full Research Lab")
            }
            InfoCard(title = "Full Research Lab — code, UI, resources, game, API hints & test checks", body = fullResearchReport)
                        InfoCard(title = "APK report", body = apkReport)
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeading(title = "APK information dashboard", subtitle = "A quick summary of the selected package.")
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
                            putExtra(Intent.EXTRA_TEXT, buildCombinedResearchReport(apkReport, comparisonReport, dexReport, resourceReport, resourceTypeReport, architectureReport, manifestReport, exposureReport, engineReport, nativeLibraryReport, signingCertificateReport, fullResearchReport))
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


private fun analyzeApkSigningCertificates(
    context: android.content.Context,
    apkFile: File
): String {
    val packageManager = context.packageManager
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }
    val packageInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
        ?: error("Android could not read package information from this APK.")

    val currentSigners: List<android.content.pm.Signature>
    val certificateHistory: List<android.content.pm.Signature>
    var multipleSigners = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = packageInfo.signingInfo
        if (signingInfo == null) {
            currentSigners = emptyList()
            certificateHistory = emptyList()
        } else {
            multipleSigners = signingInfo.hasMultipleSigners()
            currentSigners = signingInfo.apkContentsSigners?.toList().orEmpty()
            certificateHistory = if (multipleSigners) {
                currentSigners
            } else {
                signingInfo.signingCertificateHistory?.toList().orEmpty()
            }
        }
    } else {
        @Suppress("DEPRECATION")
        val legacySigners = packageInfo.signatures?.toList().orEmpty()
        currentSigners = legacySigners
        certificateHistory = legacySigners
    }

    fun describeCertificate(signature: android.content.pm.Signature, index: Int): String {
        val encoded = signature.toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(encoded)
            .joinToString("") { byte -> "%02X".format(byte) }
        val certificate = runCatching {
            java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(encoded.inputStream()) as java.security.cert.X509Certificate
        }.getOrNull()

        return buildString {
            appendLine("Certificate #$index")
            appendLine("Certificate SHA-256: $digest")
            appendLine("Certificate bytes: ${encoded.size}")
            if (certificate == null) {
                append("X.509 details: unavailable; certificate bytes and fingerprint are shown.")
            } else {
                appendLine("Subject: ${certificate.subjectX500Principal.name}")
                appendLine("Issuer: ${certificate.issuerX500Principal.name}")
                appendLine("Serial number: ${certificate.serialNumber.toString(16)}")
                appendLine("Valid from: ${certificate.notBefore}")
                append("Valid until: ${certificate.notAfter}")
            }
        }
    }

    return buildString {
        appendLine("APK Signing Certificate Metadata — read-only")
        appendLine("Package: ${packageInfo.packageName}")
        appendLine("Current signer certificates: ${currentSigners.size}")
        appendLine("Multiple current signers: $multipleSigners")
        appendLine("Certificates in signing history: ${certificateHistory.size}")
        appendLine()
        appendLine("Current signer details")
        if (currentSigners.isEmpty()) {
            appendLine("No signer certificate metadata was returned by Android.")
        } else {
            currentSigners.forEachIndexed { index, signature ->
                appendLine(describeCertificate(signature, index + 1))
                appendLine()
            }
        }
        if (certificateHistory.isNotEmpty() && certificateHistory != currentSigners) {
            appendLine("Signing certificate history (may include previous signing certificates)")
            certificateHistory.forEachIndexed { index, signature ->
                appendLine(describeCertificate(signature, index + 1))
                appendLine()
            }
        }
        appendLine("Important limitations")
        appendLine("These are signer certificate details reported by Android's package parser. A certificate fingerprint identifies certificate bytes; it does not by itself prove the publisher is trustworthy.")
        append("This screen does not independently verify every APK signature scheme, compare against an official publisher fingerprint, or guarantee the APK is safe.")
    }
}

private fun analyzeNativeLibraries(apkFile: File): String {
    return ZipFile(apkFile).use { zip ->
        val libraries = zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith("lib/") && it.name.endsWith(".so", true) }
            .toList()

        val abiDescriptions = mapOf(
            "arm64-v8a" to "64-bit ARM",
            "armeabi-v7a" to "32-bit ARM",
            "armeabi" to "Legacy ARM",
            "x86" to "32-bit Intel",
            "x86_64" to "64-bit Intel",
            "mips" to "Legacy MIPS",
            "mips64" to "64-bit MIPS"
        )
        val grouped = libraries.groupBy { entry ->
            entry.name.removePrefix("lib/").substringBefore('/')
        }.toSortedMap()
        val totalBytes = libraries.sumOf { it.size.coerceAtLeast(0L) }

        buildString {
            appendLine("Native Library & ABI Analysis — ZIP metadata only")
            appendLine("Native .so files found: ${libraries.size}")
            appendLine("Known uncompressed size total: ${totalBytes} bytes")
            appendLine("ABI groups found: ${grouped.size}")
            appendLine()
            appendLine("ABI summary (folder | architecture | library count | size)")
            if (grouped.isEmpty()) {
                appendLine("No native .so libraries found under lib/. This does not prove the app has no native code; libraries may be packaged differently or loaded through other mechanisms.")
            } else {
                grouped.forEach { (abi, files) ->
                    val size = files.sumOf { it.size.coerceAtLeast(0L) }
                    appendLine("$abi | ${abiDescriptions[abi] ?: "Unknown / unrecognized ABI"} | ${files.size} | ${size} bytes")
                }
            }
            appendLine()
            appendLine("Library file list (up to 100)")
            libraries.sortedBy { it.name }.take(100).forEach { entry ->
                appendLine("${entry.name} | ${entry.size} bytes")
            }
            if (libraries.size > 100) appendLine("Showing 100 of ${libraries.size} native libraries.")
            appendLine()
            append("This analyzer reads ZIP entry names and sizes only. It does not load, execute, disassemble, or modify native libraries. ABI presence suggests packaged architecture support but does not guarantee a device can run the app.")
        }
    }
}

private fun inspectApkArchitecture(apkFile: File): String {
    return ZipFile(apkFile).use { zip ->
        val entries = zip.entries().asSequence()
            .filter { entry -> !entry.isDirectory }
            .toList()
        val paths = entries.map { entry -> entry.name }

        val topLevel = entries
            .groupBy { entry ->
                entry.name.substringBefore('/', missingDelimiterValue = "[root files]")
            }
            .mapValues { groupedEntries -> groupedEntries.value.size }
            .toSortedMap()

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

private fun inspectDexHeaders(apkFile: File): String = AdvancedDexAnalyzer.inspect(apkFile)

private const val MAX_IMPORTED_APK_BYTES = 512L * 1024L * 1024L

private fun copyUriToFileBounded(
    context: android.content.Context,
    uri: android.net.Uri,
    destination: File
) {
    destination.delete()
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Could not open the selected file.")
        input.use { source ->
            destination.outputStream().buffered().use { output ->
                val buffer = ByteArray(32 * 1024)
                var totalBytes = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    totalBytes += count
                    require(totalBytes <= MAX_IMPORTED_APK_BYTES) {
                        "APK is larger than the 512 MB safety limit."
                    }
                    output.write(buffer, 0, count)
                }
            }
        }
        require(destination.length() > 0L) { "The selected file is empty." }
    } catch (error: Throwable) {
        destination.delete()
        throw error
    }
}

private fun sha256File(file: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

private fun inspectApkForComparison(
    context: android.content.Context,
    uri: android.net.Uri
): String {
    val apkFile = File.createTempFile("xhubrea_compare_", ".apk", context.cacheDir)
    try {
        copyUriToFileBounded(context, uri, apkFile)

        val flags = PackageManager.GET_PERMISSIONS
        val info = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: error("This file is not a readable Android APK.")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        val sha256 = sha256File(apkFile)
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
private fun PreferenceRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(3.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildCombinedResearchReport(vararg sections: String): String = buildString {
    appendLine("XhubREA — Combined Research Report")
    appendLine("Generated locally from read-only APK inspection results.")
    sections.forEachIndexed { index, section ->
        appendLine()
        appendLine("========== SECTION ${index + 1} ==========")
        appendLine(section.ifBlank { "Not run yet." })
    }
}