package com.ashishxhub.xhubrea

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
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
import androidx.compose.material3.LinearProgressIndicator
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let Android manage the display refresh rate by default.
        // Users can explicitly enable a higher refresh preference in Settings.
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
    var dexSearch by remember { mutableStateOf("") }
    var resourceReport by remember { mutableStateOf("Choose an APK, then scan its resource file inventory.") }
    var resourceTypeReport by remember { mutableStateOf("Choose an APK, then analyze file types and sizes.") }
    var resourcePreviews by remember { mutableStateOf<List<ResourcePreview>>(emptyList()) }
    var resourceSearch by remember { mutableStateOf("") }
    var fileIntelligenceSearch by remember { mutableStateOf("") }
    var resourceExplorerStatus by remember { mutableStateOf("Load an APK resource preview to explore its images and file categories.") }
    var architectureReport by remember { mutableStateOf("Choose an APK, then inspect its high-level package layout.") }
    var manifestReport by remember { mutableStateOf("Choose an APK, then generate a manifest summary.") }
    var binaryManifestReport by remember { mutableStateOf("Choose an APK, then decode its binary AndroidManifest.xml.") }
    var pythonEngineReport by remember { mutableStateOf("Choose an APK, then run the embedded Python structure analyzer.") }
    var resourceIntelligenceReport by remember { mutableStateOf("Run Complete Analysis to generate the Python resource intelligence report.") }
    var networkIntelligenceReport by remember { mutableStateOf("Run Complete Analysis to generate the network intelligence report.") }
    var privacyIntelligenceReport by remember { mutableStateOf("Run Complete Analysis to generate the privacy intelligence report.") }
    var humanBehaviourReport by remember { mutableStateOf("Run Complete Analysis to generate the human behaviour intelligence report.") }
    var isPythonAnalysisRunning by remember { mutableStateOf(false) }
    var exposureReport by remember { mutableStateOf("Choose an APK, then review exported components.") }
    var componentSearch by remember { mutableStateOf("") }
    var permissionSearch by remember { mutableStateOf("") }
    var expandedComponent by remember { mutableStateOf("") }
    var engineReport by remember { mutableStateOf("Choose an APK, then detect likely app/game technology.") }
    var nativeLibraryReport by remember { mutableStateOf("Choose an APK, then analyze native library ABIs and file sizes.") }
    var signingCertificateReport by remember { mutableStateOf("Choose an APK, then inspect its signing certificate metadata.") }
    var fullResearchReport by remember { mutableStateOf("Choose an APK, then run the full research scan.") }
    var fullAnalysisSummary by remember { mutableStateOf("Run Complete Analysis to see each module result here.") }
    var isCompleteAnalysisRunning by remember { mutableStateOf(false) }
    var completeAnalysisProgress by remember { mutableStateOf(0) }
    var completeAnalysisCurrentModule by remember { mutableStateOf("") }
    var completeAnalysisJob by remember { mutableStateOf<Job?>(null) }

    var showSettings by remember { mutableStateOf(false) }
    var simpleExplain by remember { mutableStateOf(preferences.getBoolean("simple_explain", true)) }
    var showHints by remember { mutableStateOf(preferences.getBoolean("show_hints", true)) }
    var smoothMotion by remember { mutableStateOf(preferences.getBoolean("smooth_motion", false)) }
    var requestHighRefresh by remember { mutableStateOf(preferences.getBoolean("high_refresh", false)) }

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
                            appendLine("Uses cleartext traffic: Unknown (not exposed by current metadata reader)")
                            appendLine("Network security config resource: Unknown (not decoded by current analyzer)")
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
                    if (!apkFile.exists()) {
                        status = "Pehle APK select karo."
                    } else {
                        status = "Binary manifest decode ho raha hai…"
                        scope.launch {
                            binaryManifestReport = withContext(Dispatchers.IO) {
                                runCatching { BinaryManifestDecoder.decodeApk(apkFile) }
                                    .getOrElse { "Decode nahi ho saka: ${it.message ?: "Unknown error"}" }
                            }
                            status = "Binary manifest decoding finished."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Decode Binary AndroidManifest") }
            Spacer(Modifier.height(8.dp))
            InfoCard("Binary Manifest Decoder", binaryManifestReport)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (!apkFile.exists()) {
                        status = "Pehle APK select karo."
                    } else {
                        isPythonAnalysisRunning = true
                        status = "Python engine APK structure inspect kar raha hai…"
                        scope.launch {
                            pythonEngineReport = withContext(Dispatchers.IO) {
                                runCatching {
                                    if (!Python.isStarted()) {
                                        Python.start(AndroidPlatform(context.applicationContext))
                                    }
                                    Python.getInstance()
                                        .getModule("xhubrea_engine")
                                        .callAttr("analyze_apk_json", apkFile.absolutePath)
                                        .toString()
                                }.getOrElse {
                                    "Python analysis error: ${it.message ?: it.javaClass.simpleName}"
                                }
                            }
                            isPythonAnalysisRunning = false
                            status = if (pythonEngineReport.startsWith("Python analysis error:")) {
                                "Python engine analysis failed. Error details report mein hain."
                            } else {
                                "Python APK structure analysis complete."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPythonAnalysisRunning
            ) {
                Text(if (isPythonAnalysisRunning) "Python Analysis Running…" else "Run Python APK Structure Analysis")
            }
            Spacer(Modifier.height(8.dp))
            InfoCard("Python Engine — Read-only APK Report", pythonEngineReport)
            Spacer(Modifier.height(8.dp))
            InfoCard("Python Resource Intelligence", resourceIntelligenceReport)
            Spacer(Modifier.height(8.dp))
            InfoCard("Network Intelligence — Static Evidence", networkIntelligenceReport)
            Spacer(Modifier.height(8.dp))
            InfoCard("Privacy Intelligence — Permission & Data Clues", privacyIntelligenceReport)
            Spacer(Modifier.height(8.dp))
            InfoCard("Human Behaviour Intelligence — Static UI Clues", humanBehaviourReport)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect") || !apkFile.exists()) {
                        status = "Choose and inspect an APK before running the complete analysis."
                    } else if (!isCompleteAnalysisRunning) {
                        isCompleteAnalysisRunning = true
                        completeAnalysisProgress = 0
                        completeAnalysisCurrentModule = "Preparing analysis…"
                        status = "Running complete analysis suite…"
                        completeAnalysisJob = scope.launch {
                            val moduleNames = listOf(
                                "DEX headers", "Resource inventory", "Resource types", "APK architecture",
                                "Manifest summary", "Exported components", "App/game technology",
                                "Native libraries", "Signing certificate metadata", "Research Lab", "Python resource intelligence", "Network intelligence", "Privacy intelligence", "Human behaviour intelligence"
                            )
                            val modules = listOf<Pair<String, () -> String>>(
                                moduleNames[0] to { inspectDexHeaders(apkFile) },
                                moduleNames[1] to { inventoryApkResources(apkFile) },
                                moduleNames[2] to { analyzeApkFileTypes(apkFile) },
                                moduleNames[3] to { inspectApkArchitecture(apkFile) },
                                moduleNames[4] to { summarizeManifest(apkReport) },
                                moduleNames[5] to { reviewExportedComponents(apkReport) },
                                moduleNames[6] to { detectAppGameTechnology(apkFile) },
                                moduleNames[7] to { analyzeNativeLibraries(apkFile) },
                                moduleNames[8] to { analyzeApkSigningCertificates(context, apkFile) },
                                moduleNames[9] to { ResearchLab.run(apkFile) },
                                moduleNames[10] to {
                                    if (!Python.isStarted()) {
                                        Python.start(AndroidPlatform(context.applicationContext))
                                    }
                                    val raw = Python.getInstance()
                                        .getModule("xhubrea_engine")
                                        .callAttr("analyze_apk_json", apkFile.absolutePath)
                                        .toString()
                                    formatResourceIntelligenceReport(raw)
                                },
                                moduleNames[11] to { NetworkIntelligenceAnalyzer.run(apkFile) },
                                moduleNames[12] to { PrivacyIntelligenceAnalyzer.run(apkFile, networkIntelligenceReport) },
                                moduleNames[13] to { HumanBehaviourIntelligenceAnalyzer.run(apkFile, networkIntelligenceReport) }
                            )
                            val results = mutableListOf<String>()
                            try {
                                modules.forEachIndexed { index, module ->
                                    completeAnalysisCurrentModule = module.first
                                    status = "Analyzing ${module.first} (${index + 1}/${modules.size})…"
                                    val result = try {
                                        withContext(Dispatchers.IO) { module.second.invoke() }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        "${module.first} failed: ${error.message ?: error.javaClass.simpleName}"
                                    }
                                    results.add(result)
                                    when (index) {
                                        0 -> dexReport = result
                                        1 -> resourceReport = result
                                        2 -> resourceTypeReport = result
                                        3 -> architectureReport = result
                                        4 -> manifestReport = result
                                        5 -> exposureReport = result
                                        6 -> engineReport = result
                                        7 -> nativeLibraryReport = result
                                        8 -> signingCertificateReport = result
                                        9 -> fullResearchReport = result
                                        10 -> resourceIntelligenceReport = result
                                        11 -> networkIntelligenceReport = result
                                        12 -> privacyIntelligenceReport = result
                                        13 -> humanBehaviourReport = result
                                    }
                                    completeAnalysisProgress = index + 1
                                    fullAnalysisSummary = buildString {
                                        appendLine("Complete analysis progress: ${index + 1}/${modules.size}")
                                        results.forEachIndexed { resultIndex, moduleResult ->
                                            val failed = moduleResult.startsWith("${moduleNames[resultIndex]} failed:")
                                            appendLine("${if (failed) "FAILED" else "COMPLETED"} — ${moduleNames[resultIndex]}")
                                        }
                                        appendLine()
                                        appendLine("A completed module may still report limited or heuristic findings. Read each detailed report before drawing conclusions.")
                                    }
                                }
                                status = "Complete analysis finished. Review each module and its limitations below."
                            } catch (cancelled: CancellationException) {
                                status = "Analysis cancelled. Completed module results have been kept."
                                throw cancelled
                            } finally {
                                isCompleteAnalysisRunning = false
                                completeAnalysisCurrentModule = ""
                                completeAnalysisJob = null
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCompleteAnalysisRunning
            ) {
                Text(if (isCompleteAnalysisRunning) "Analysis Running…" else "Run Complete Analysis — All Modules")
            }
            if (isCompleteAnalysisRunning) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Module ${completeAnalysisProgress}/14 • ${completeAnalysisCurrentModule}",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(
                    onClick = {
                        status = "Stopping analysis after the current operation responds…"
                        completeAnalysisJob?.cancel()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Analysis")
                }
            }
            Text(
                "Runs the available read-only modules one at a time. Progress updates after each module; cancellation takes effect when the current operation responds.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoCard(title = "Complete Analysis — Module Status", body = fullAnalysisSummary)
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeading("Visual Intelligence", "Numbers ko graph aur app ke flow ke roop mein samjho.")
            val visualData = listOf(
                "Permissions" to extractSectionCount(apkReport, "Declared permissions"),
                "Activities" to extractSectionCount(apkReport, "Activities"),
                "Services" to extractSectionCount(apkReport, "Services"),
                "Receivers" to extractSectionCount(apkReport, "Receivers"),
                "Providers" to extractSectionCount(apkReport, "Providers")
            )
            val largestValue = visualData.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
            Card(
                modifier = Modifier.fillMaxWidth().then(
                    if (smoothMotion) Modifier.animateContentSize(animationSpec = tween(300)) else Modifier
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 App structure at a glance", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect")) {
                        Text("Graph dekhne ke liye pehle APK choose karo. Abhi koi data available nahi hai.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        visualData.forEach { (label, value) ->
                            MetricBar(label = label, value = value, maximum = largestValue)
                            Spacer(Modifier.height(9.dp))
                        }
                        Text("Ye bars sirf count dikhate hain. Zyada count ka matlab automatically danger ya better app nahi hota.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text("🧭 App ko samajhne ka flow", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "① APK select → ② Manifest aur package metadata read → ③ DEX / resources / libraries ki inventory → ④ Components aur technology ke clues → ⑤ Local report",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (showHints) {
                        Spacer(Modifier.height(8.dp))
                        Text("💡 Clue: Activity aksar ek screen ya user-facing entry point hoti hai. Service background kaam kar sakti hai. Receiver system/app events sun sakta hai. Provider structured data share karne ke liye hota hai.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeading("Architecture map", "Components ko simple diagram mein dekho — ye types ka map hai, proven connections nahi.")
            Card(
                modifier = Modifier.fillMaxWidth().then(
                    if (smoothMotion) Modifier.animateContentSize(animationSpec = tween(300)) else Modifier
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ArchitectureNode("📱 Android App Package", "APK container")
                    Text("↓", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    ArchitectureNode("📄 Manifest", "App declarations & component list")
                    Text("↓", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        ArchitectureNode("🖥 Activities", "Screens / entry points", Modifier.weight(1f))
                        ArchitectureNode("⚙ Services", "Background work", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        ArchitectureNode("📡 Receivers", "Event listeners", Modifier.weight(1f))
                        ArchitectureNode("🗂 Providers", "Structured data access", Modifier.weight(1f))
                    }
                    if (showHints) {
                        Spacer(Modifier.height(10.dp))
                        Text("🔍 Important: Is diagram se component ke types pata chalte hain. Kaun sa screen kis screen ko kholta hai, ya kis component ko call karta hai, woh static manifest count se confirm nahi hota.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeading("Evidence & confidence", "Jo confirm hai aur jo abhi unknown hai, unhe alag rakho.")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EvidenceLine("🟢 Direct metadata", "Package name, version aur listed component counts", "High — Android PackageManager se read kiya", apkReport != "No APK inspected yet." && !apkReport.startsWith("Could not inspect"))
                    EvidenceLine("🟡 Pattern-based clue", "Game engine, framework aur file markers", "Medium / heuristic — marker milna hint hai, final proof nahi", engineReport != "Choose an APK, then detect likely app/game technology.")
                    EvidenceLine("⚪ Runtime behaviour", "Actual screen navigation, taps, network response aur live behaviour", "Unknown — app ko install ya run nahi kiya jata", false)
                    if (showHints) {
                        Spacer(Modifier.height(8.dp))
                        Text("🧠 Confidence ka matlab: High = seedha metadata mila. Medium = file-name/pattern se andaza. Unknown = is tool ne abhi test hi nahi kiya. Unknown ka matlab safe ya unsafe dono nahi hota.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeading("DEX Intelligence Explorer", "DEX files ke structure aur counts ko aasaan tareeke se samjho.")
            OutlinedTextField(
                value = dexSearch,
                onValueChange = { dexSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search DEX file or table name") },
                singleLine = true
            )
            val dexEntries = remember(dexReport) { parseDexSummaries(dexReport) }
            val shownDexEntries = dexEntries.filter { entry ->
                "${entry.fileName} ${entry.version} ${entry.tables.joinToString(" ")} ${entry.validation}"
                    .contains(dexSearch, ignoreCase = true)
            }
            if (dexEntries.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArchitectureNode("DEX files", dexEntries.size.toString(), Modifier.weight(1f))
                    ArchitectureNode("Classes", dexEntries.sumOf { it.classCount }.toString(), Modifier.weight(1f))
                    ArchitectureNode("Methods", dexEntries.sumOf { it.methodCount }.toString(), Modifier.weight(1f))
                }
                shownDexEntries.forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(entry.fileName, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("DEX version: ${entry.version} • ${entry.byteCount} bytes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Classes: ${entry.classCount} • Methods: ${entry.methodCount}",
                                color = MaterialTheme.colorScheme.onSurface)
                            entry.tables.forEach { table ->
                                Text(table, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Header: ${entry.validation}", fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary)
                            Text("Simple meaning: classes = code structure ke class records; methods = method reference records. Counts code quality, app behaviour ya vulnerability prove nahi karte.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (shownDexEntries.isEmpty()) {
                    Text("Search se koi DEX file match nahi hui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                InfoCard("DEX learning clue", "classes.dex primary bytecode file hoti hai. classes2.dex, classes3.dex jaise files multidex apps mein extra bytecode rakh sakti hain. Ye explorer header tables aur basic bounds hi inspect karta hai; methods disassemble ya class names extract nahi karta.")
            } else if (dexReport.startsWith("No DEX files")) {
                InfoCard("No DEX files", "Is APK mein standard classes*.dex entry nahi mili.")
            }
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
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeading("Visual Resource Explorer", "APK ke image assets aur file categories ko browse karo.")
            OutlinedTextField(
                value = resourceSearch,
                onValueChange = { resourceSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search resource path") },
                singleLine = true
            )
            Button(
                onClick = {
                    val apkFile = File(context.cacheDir, "xhubrea_inspection.apk")
                    if (!apkFile.exists() || apkReport.startsWith("Could not inspect") || apkReport == "No APK inspected yet.") {
                        resourceExplorerStatus = "Pehle ek readable APK select karo."
                        resourcePreviews = emptyList()
                    } else {
                        resourceExplorerStatus = "Resource previews load ho rahe hain…"
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { loadResourcePreviews(apkFile) }
                            }
                            result.onSuccess { previews ->
                                resourcePreviews = previews
                                resourceExplorerStatus = if (previews.isEmpty()) {
                                    "Supported image preview nahi mila. File inventory neeche explain ki gayi hai."
                                } else {
                                    "${previews.size} safe image preview(s) loaded. Maximum 12 thumbnails."
                                }
                            }.onFailure { error ->
                                resourceExplorerStatus = "Preview load nahi hua: ${error.message ?: "Unknown error"}"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🖼 Load visual resource previews") }
            InfoCard("Explorer status", resourceExplorerStatus)
            val visiblePreviews = resourcePreviews.filter {
                it.path.contains(resourceSearch, ignoreCase = true) ||
                    it.category.contains(resourceSearch, ignoreCase = true)
            }
            if (visiblePreviews.isNotEmpty()) {
                visiblePreviews.forEach { preview ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            preview.bitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Preview of ${preview.path}",
                                    modifier = Modifier.height(76.dp).weight(0.32f)
                                )
                            }
                            Column(modifier = Modifier.weight(0.68f).padding(start = 10.dp)) {
                                Text(preview.path.substringAfterLast('/'), fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(preview.category, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                                Text("${preview.width} × ${preview.height} • ${preview.sizeBytes} bytes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else if (resourcePreviews.isNotEmpty()) {
                Text("Search se match karta koi resource nahi mila.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showHints) {
                InfoCard("Resource guide", "res/drawable = app graphics; res/mipmap = launcher icons; assets/ = app ke bundled files. Sirf supported raster images ka thumbnail banta hai. XML, vector aur binary resources ko file inventory mein dekho. Preview karna app ka runtime behaviour prove nahi karta.")
            }
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
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeading("APK File Intelligence", "APK ke andar kis type ki files hain aur kaunsi sabse badi hain?")
            OutlinedTextField(
                value = fileIntelligenceSearch,
                onValueChange = { fileIntelligenceSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search extension or file path") },
                singleLine = true
            )
            val fileTypeStats = remember(resourceTypeReport) { parseApkFileTypeStats(resourceTypeReport) }
            val largestApkFiles = remember(resourceTypeReport) { parseApkLargestFiles(resourceTypeReport) }
            if (fileTypeStats.isNotEmpty()) {
                val totalFiles = fileTypeStats.sumOf { it.count }
                val totalKnownBytes = fileTypeStats.sumOf { it.totalBytes }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArchitectureNode("File types", fileTypeStats.size.toString(), Modifier.weight(1f))
                    ArchitectureNode("Files counted", totalFiles.toString(), Modifier.weight(1f))
                    ArchitectureNode("Known size", formatBytes(totalKnownBytes), Modifier.weight(1f))
                }
                fileTypeStats.filter { it.extension.contains(fileIntelligenceSearch, true) }
                    .forEach { stat ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stat.extension, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("${stat.count} files • ${formatBytes(stat.totalBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            LinearProgressIndicator(
                                progress = { if (totalFiles == 0) 0f else stat.count.toFloat() / totalFiles },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                Text("Largest files (top ${largestApkFiles.size})", style = MaterialTheme.typography.titleMedium)
                largestApkFiles.filter { it.path.contains(fileIntelligenceSearch, true) }
                    .forEachIndexed { index, entry ->
                        InfoCard(
                            title = "${index + 1}. ${entry.path.substringAfterLast('/')}",
                            body = "${entry.path}\nSize: ${formatBytes(entry.sizeBytes)}\nPossible role: ${explainApkFilePath(entry.path)}"
                        )
                    }
                if (fileTypeStats.none { it.extension.contains(fileIntelligenceSearch, true) } &&
                    largestApkFiles.none { it.path.contains(fileIntelligenceSearch, true) }) {
                    Text("Search se koi file type ya large file match nahi hui.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                InfoCard("File Intelligence status", "Pehle APK inspect karo aur 'Analyze resource types' run karo. File-type report available hone par yahan breakdown dikhega.")
            }
            InfoCard("File Intelligence guide", "DEX = app ka compiled code; res/ = interface aur graphics resources; assets/ = bundled app files; lib/ = native machine-code libraries. File size aur extension clues dete hain, lekin exact purpose ya runtime usage prove nahi karte. ZIP metadata ka analysis hota hai; files execute nahi hoti.")
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
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeading("APK Security Intelligence", "Security-related manifest clues ko simple language mein samjho.")
            val securityFindings = remember(apkReport, signingCertificateReport, exposureReport) {
                buildApkSecurityFindings(apkReport, signingCertificateReport, exposureReport)
            }
            securityFindings.forEach { finding ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(finding.title, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(finding.status, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(finding.explanation, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Evidence: ${finding.evidence}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            InfoCard("Security review limitations", "Ye static metadata-based preliminary review hai, complete security audit nahi. Debuggable/backup/exported/cleartext flags context ke saath interpret karne chahiye. Network security XML ko abhi decode/verify nahi kiya jata. Signing certificate metadata identity clues deta hai, trusted hone ka proof nahi. APK install ya execute nahi hota.")
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeading("Interactive Component Explorer", "Har component ko search karke uski details samjho.")
            OutlinedTextField(
                value = componentSearch,
                onValueChange = { componentSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search name, type or status") },
                singleLine = true
            )
            val componentItems = remember(apkReport) { parseComponentEntries(apkReport) }
            val filteredComponents = componentItems.filter { item ->
                "${item.type} ${item.name} ${item.exported} ${item.enabled}"
                    .contains(componentSearch, ignoreCase = true)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArchitectureNode("Total", componentItems.size.toString(), Modifier.weight(1f))
                ArchitectureNode("Exported", componentItems.count { it.exported }.toString(), Modifier.weight(1f))
                ArchitectureNode("Not exported", componentItems.count { !it.exported }.toString(), Modifier.weight(1f))
            }
            if (filteredComponents.isEmpty()) {
                InfoCard("No components", if (componentItems.isEmpty()) "Pehle APK inspect karo. Available PackageManager metadata mein component entries nahi mili." else "Is search se koi component match nahi hua.")
            } else {
                filteredComponents.forEach { item ->
                    val expanded = expandedComponent == item.name
                    Card(
                        modifier = Modifier.fillMaxWidth().animateContentSize().clickable {
                            expandedComponent = if (expanded) "" else item.name
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name.substringAfterLast('.'), fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(item.type, color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                                Text(if (item.exported) "EXPORTED" else "NOT EXPORTED",
                                    color = if (item.exported) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            if (expanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Full name: ${item.name}", color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall)
                                Text("Enabled: ${if (item.enabled) "Yes" else "No"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(when (item.type) {
                                    "Activity" -> "Screen ya user interface ka ek hissa. Example: app ka Home screen."
                                    "Service" -> "Background kaam karne wala component. Example: music playback service."
                                    "Receiver" -> "System ya app event receive karta hai. Example: battery ya boot event."
                                    else -> "Structured data provide karne wala component. Example: contacts-style data provider."
                                }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                                Text(if (item.exported)
                                    "Other apps ya Android system ise address kar sakte hain, lekin permissions aur configuration access ko limit kar sakte hain. Exported hona apne aap vulnerability nahi hai."
                                else "Is metadata ke mutabik component doosri apps ke liye exported nahi hai.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Tap for details", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (componentItems.isNotEmpty()) {
                InfoCard("Coverage note", "Base APK metadata report har type ke pehle 20 components tak limited hai. Isliye yahan listed count complete APK count nahi bhi ho sakta. Exported status security flaw ka proof nahi hota.")
            }

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
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeading("Permission Intelligence", "App ne kaunsi Android permissions declare ki hain?")
            OutlinedTextField(
                value = permissionSearch,
                onValueChange = { permissionSearch = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search permission name or category") },
                singleLine = true
            )
            val permissionEntries = remember(apkReport) { parsePermissionEntries(apkReport) }
            val filteredPermissions = permissionEntries.filter {
                "${it.name} ${it.category}".contains(permissionSearch, ignoreCase = true)
            }
            val permissionCategories = listOf(
                "Normal", "Dangerous (runtime-sensitive)", "Signature (restricted)",
                "Other / special", "Unknown / not available"
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ArchitectureNode("Listed", permissionEntries.size.toString(), Modifier.weight(1f))
                ArchitectureNode("Dangerous", permissionEntries.count { it.category.contains("Dangerous") }.toString(), Modifier.weight(1f))
                ArchitectureNode("Signature", permissionEntries.count { it.category.contains("Signature") }.toString(), Modifier.weight(1f))
            }
            permissionCategories.forEach { category ->
                val count = permissionEntries.count { it.category == category }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(count.toString(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { if (permissionEntries.isEmpty()) 0f else count.toFloat() / permissionEntries.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
            }
            if (filteredPermissions.isEmpty()) {
                InfoCard("Permission list", if (permissionEntries.isEmpty()) "Pehle APK inspect karo. Ya report mein permission details available nahi hain." else "Search se koi permission match nahi hui.")
            } else {
                filteredPermissions.forEach { permission ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(permission.name, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(permission.category, color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall)
                            Text(permissionExplanation(permission.name, permission.category),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            InfoCard("How to read this", "Dangerous ka matlab Android permission group runtime approval maang sakta hai; ye app malicious hone ka proof nahi. Signature permissions aam taur par same certificate se signed apps tak restricted hoti hain. Normal permissions lower-risk protection level hoti hain, lekin context phir bhi important hai. Ye report declared permissions dikhati hai, actual usage nahi.")
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

private data class ApkSecurityFinding(
    val title: String,
    val status: String,
    val explanation: String,
    val evidence: String
)

private fun buildApkSecurityFindings(
    apkReport: String,
    certificateReport: String,
    componentReport: String
): List<ApkSecurityFinding> {
    if (apkReport == "No APK inspected yet." || apkReport.startsWith("Could not inspect")) {
        return listOf(ApkSecurityFinding(
            "APK not inspected", "WAITING",
            "Pehle APK select karke inspect karo.", "No APK metadata available"
        ))
    }
    fun value(label: String): String =
        apkReport.lineSequence().firstOrNull { it.startsWith("$label:") }
            ?.substringAfter(":")?.trim() ?: "Unknown"
    fun flag(label: String, yes: String, no: String, unknown: String): ApkSecurityFinding {
        val v = value(label)
        val positive = v.equals("true", true)
        val negative = v.equals("false", true)
        return ApkSecurityFinding(label, when { positive -> yes; negative -> no; else -> "UNKNOWN" },
            when {
                positive -> when (label) {
                    "Debuggable" -> "Debuggable enabled hai. Release app ke liye ise review karna chahiye; development builds mein expected ho sakta hai."
                    "Allow backup flag" -> "Backup flag enabled hai. Backup behavior Android version, policy aur device configuration par depend karta hai."
                    "Uses cleartext traffic" -> "Cleartext traffic allowed flag set hai. Plain HTTP use ka risk context aur network security config par depend karta hai."
                    else -> "Flag true report hua hai; app context mein review karein."
                }
                negative -> "Flag false report hua hai; ye ek positive configuration clue hai, full security guarantee nahi."
                else -> "Is property ki value available nahi, isliye conclusion nahi nikala ja sakta."
            },
            "PackageManager ApplicationInfo metadata: $label=$v")
    }
    val findings = mutableListOf<ApkSecurityFinding>()
    findings += flag("Debuggable", "REVIEW", "NOT FLAGGED", "UNKNOWN")
    findings += flag("Allow backup flag", "REVIEW", "NOT FLAGGED", "UNKNOWN")
    findings += flag("Uses cleartext traffic", "REVIEW", "NOT FLAGGED", "UNKNOWN")
    val networkRes = value("Network security config resource")
    findings += ApkSecurityFinding(
        "Network security configuration",
        if (networkRes == "Unknown") "UNKNOWN" else if (networkRes == "0") "NOT REPORTED" else "RESOURCE DECLARED",
        if (networkRes == "0" || networkRes == "Unknown") "PackageManager ne network security config resource id report nahi kiya. Iska matlab ye nahi ki network secure ya insecure hai."
        else "Manifest metadata ek network security config resource ka reference deti hai; uski XML policy abhi decode nahi hui.",
        "networkSecurityConfigRes=$networkRes"
    )
    val exported = Regex("""exported=true""", RegexOption.IGNORE_CASE).findAll(componentReport).count()
    findings += ApkSecurityFinding(
        "Exported components",
        if (componentReport.isBlank() || componentReport.contains("not available", true)) "UNKNOWN"
        else "$exported listed as exported",
        "Exported component doosri apps se accessible ho sakta hai, lekin actual exposure component permissions, intent filters aur platform rules par depend karta hai.",
        if (componentReport.isBlank()) "Component review not generated" else "Exported entries counted from current component review"
    )
    val certReady = certificateReport.isNotBlank() &&
        !certificateReport.startsWith("Choose an APK") &&
        !certificateReport.startsWith("Could not")
    findings += ApkSecurityFinding(
        "Signing certificate",
        if (certReady) "METADATA AVAILABLE" else "NOT ANALYZED",
        if (certReady) "Certificate report mein signer metadata dekho. Certificate hona trustworthiness ka proof nahi; known publisher se fingerprint compare karna zaroori hai."
        else "Signing certificate analyzer run karke signer metadata inspect karo.",
        if (certReady) certificateReport.lineSequence().take(6).joinToString(" | ") else "No certificate report available"
    )
    return findings
}

private data class ApkFileTypeStat(val extension: String, val count: Int, val totalBytes: Long)
private data class ApkLargestFile(val path: String, val sizeBytes: Long)

private fun parseApkFileTypeStats(report: String): List<ApkFileTypeStat> {
    val start = report.indexOf("File types (extension | count | total size)")
    if (start < 0) return emptyList()
    return report.substring(start).lineSequence().drop(1).takeWhile { it.isNotBlank() }
        .filter { it.contains("|") }
        .mapNotNull { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.size < 3) null else runCatching {
                ApkFileTypeStat(parts[0], parts[1].toInt(), parts[2].substringBefore(" ").toLong())
            }.getOrNull()
        }.toList()
}

private fun parseApkLargestFiles(report: String): List<ApkLargestFile> {
    val start = report.indexOf("Largest files (up to 10)")
    if (start < 0) return emptyList()
    return report.substring(start).lineSequence().drop(1).takeWhile { it.isNotBlank() }
        .mapNotNull { line ->
            val sizeText = line.substringAfterLast("|", "").trim().substringBefore(" ")
            val path = line.substringBeforeLast("|", "").trim()
            runCatching { ApkLargestFile(path, sizeText.toLong()) }.getOrNull()
        }.toList()
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    bytes >= 1024L -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "$bytes B"
}

private fun explainApkFilePath(path: String): String = when {
    path.startsWith("classes") && path.endsWith(".dex", true) -> "Compiled Android/Dalvik bytecode."
    path.startsWith("res/") -> "Android app resource such as UI graphics or XML."
    path.startsWith("assets/") -> "Bundled asset; exact use depends on app code."
    path.startsWith("lib/") -> "Native library for a CPU architecture."
    path.endsWith(".so", true) -> "Native shared library."
    path.endsWith(".arsc", true) -> "Compiled Android resource table."
    path.endsWith(".xml", true) -> "XML resource or configuration; may be binary-compiled."
    else -> "File category alone cannot confirm exact purpose."
}

private data class ResourcePreview(
    val path: String,
    val category: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap?
)

private fun loadResourcePreviews(apkFile: File): List<ResourcePreview> {
    val maxImageBytes = 2L * 1024L * 1024L
    val imageExtensions = setOf(".png", ".jpg", ".jpeg", ".webp", ".bmp")
    return ZipFile(apkFile).use { zip ->
        zip.entries().asSequence()
            .filter { entry ->
                !entry.isDirectory &&
                    entry.size in 1..maxImageBytes &&
                    imageExtensions.any { entry.name.lowercase().endsWith(it) } &&
                    (entry.name.startsWith("res/drawable") ||
                        entry.name.startsWith("res/mipmap") ||
                        entry.name.startsWith("assets/"))
            }
            .take(12)
            .mapNotNull { entry ->
                val bytes = zip.getInputStream(entry).use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > maxImageBytes) break
                        output.write(buffer, 0, count)
                    }
                    if (total > maxImageBytes) null else output.toByteArray()
                } ?: return@mapNotNull null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@mapNotNull null
                var sample = 1
                while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) {
                    sample *= 2
                }
                val bitmap = BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )
                val category = when {
                    entry.name.startsWith("res/mipmap") -> "Launcher icon"
                    entry.name.startsWith("res/drawable") -> "Drawable image"
                    else -> "Bundled asset"
                }
                ResourcePreview(
                    path = entry.name,
                    category = category,
                    sizeBytes = entry.size,
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    bitmap = bitmap
                )
            }.toList()
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

private fun inspectDexHeaders(apkFile: File): String = DexIntelligenceEngine.inspect(apkFile)

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

        val flags = PackageManager.GET_PERMISSIONS or
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
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
            appendLine("Declared permissions (${permissions.size})")
            appendLine(permissions.sorted().joinToString("\n").ifBlank { "None declared" })
            appendLine()
            appendLine("Activities (${info.activities?.size ?: 0})")
            appendLine(info.activities.orEmpty().mapNotNull { it.name }.sorted().joinToString("\n").ifBlank { "None found" })
            appendLine()
            appendLine("Services (${info.services?.size ?: 0})")
            appendLine(info.services.orEmpty().mapNotNull { it.name }.sorted().joinToString("\n").ifBlank { "None found" })
            appendLine()
            appendLine("Receivers (${info.receivers?.size ?: 0})")
            appendLine(info.receivers.orEmpty().mapNotNull { it.name }.sorted().joinToString("\n").ifBlank { "None found" })
            appendLine()
            appendLine("Providers (${info.providers?.size ?: 0})")
            appendLine(info.providers.orEmpty().mapNotNull { it.name }.sorted().joinToString("\n").ifBlank { "None found" })
        }
    } finally {
        apkFile.delete()
    }
}

private fun compareApkReports(first: String, second: String): String {
    val keys = listOf(
        "App name:", "Package:", "Version name:", "Version code:",
        "Minimum SDK:", "Target SDK:", "APK file size:", "SHA-256:"
    )
    val firstLines = first.lines()
    val secondLines = second.lines()

    fun section(reportLines: List<String>, title: String): Set<String> {
        val index = reportLines.indexOfFirst { it.startsWith("$title (") }
        if (index < 0) return emptySet()
        return reportLines.drop(index + 1).takeWhile { it.isNotBlank() }
            .filter { it != "None found" && it != "None declared" }
            .map { it.trim() }.toSet()
    }

    fun value(lines: List<String>, key: String): String =
        lines.firstOrNull { it.startsWith(key) }?.substringAfter(key)?.trim() ?: "Not available"

    val firstPackage = value(firstLines, "Package:")
    val secondPackage = value(secondLines, "Package:")
    val samePackage = firstPackage == secondPackage && firstPackage != "Not available"
    return buildString {
        appendLine("APK Comparison Intelligence — Primary vs Second")
        appendLine("Package identity: " + if (samePackage) "SAME PACKAGE" else "DIFFERENT / UNCONFIRMED PACKAGE")
        if (!samePackage) appendLine("Note: These may be two different apps, not two versions of the same app.")
        keys.forEach { key ->
            val a = value(firstLines, key)
            val b = value(secondLines, key)
            appendLine()
            appendLine(key.removeSuffix(":"))
            appendLine("Primary: $a")
            appendLine("Second: $b")
            appendLine(if (a == b) "Result: SAME" else "Result: DIFFERENT")
        }

        val firstPermissions = section(firstLines, "Declared permissions")
        val secondPermissions = section(secondLines, "Declared permissions")
        appendLine()
        appendLine("PERMISSION CHANGES (Primary → Second)")
        appendLine("Added in second: ${(secondPermissions - firstPermissions).sorted().joinToString().ifBlank { "None" }}")
        appendLine("Removed in second: ${(firstPermissions - secondPermissions).sorted().joinToString().ifBlank { "None" }}")
        appendLine("Primary listed: ${firstPermissions.size}; Second listed: ${secondPermissions.size}")

        listOf("Activities", "Services", "Receivers", "Providers").forEach { title ->
            val before = section(firstLines, title)
            val after = section(secondLines, title)
            appendLine()
            appendLine("$title changes")
            appendLine("Primary listed: ${before.size}; Second listed: ${after.size}")
            appendLine("Added in second: ${(after - before).sorted().joinToString().ifBlank { "None" }}")
            appendLine("Removed in second: ${(before - after).sorted().joinToString().ifBlank { "None" }}")
        }
        appendLine()
        appendLine("Interpretation guide")
        appendLine("A difference means metadata changed; it does not by itself indicate a security problem or prove a feature is used.")
        appendLine("Coverage note: the primary APK report may list only a limited number of components/permissions. Differences are limited to the entries available in each report.")
        appendLine("Both APKs were inspected as static metadata only; neither was installed or executed.")
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

private data class PermissionEntry(val name: String, val category: String)

private fun parsePermissionEntries(report: String): List<PermissionEntry> {
    if (report == "No APK inspected yet." || report.startsWith("Could not inspect")) return emptyList()
    val lines = report.lines()
    val start = lines.indexOfFirst { it.startsWith("Permission analysis (") }
    if (start < 0) return emptyList()
    return lines.drop(start + 1).takeWhile { it.isNotBlank() }
        .filter { it.contains(" — ") }
        .map { line ->
            val parts = line.split(" — ", limit = 2)
            PermissionEntry(parts[0].trim(), parts.getOrElse(1) { "Unknown / not available" }.trim())
        }
}

private fun permissionExplanation(name: String, category: String): String {
    val purpose = when {
        name.contains("CAMERA", true) -> "Camera access se app photo ya video capture kar sakti hai, agar runtime/device conditions allow karein."
        name.contains("RECORD_AUDIO", true) -> "Microphone access audio record karne ya voice features ke liye use ho sakta hai."
        name.contains("LOCATION", true) -> "Location permission approximate ya precise location features ke liye ho sakti hai."
        name.contains("CONTACTS", true) -> "Contacts permission address-book information read ya manage karne ke liye ho sakti hai."
        name.contains("READ_SMS", true) || name.contains("RECEIVE_SMS", true) -> "SMS permission messages read ya receive karne se related hai."
        name.contains("READ_MEDIA", true) || name.contains("READ_EXTERNAL_STORAGE", true) -> "Media/storage permission photos, videos ya shared files access karne se related hai."
        name.contains("BLUETOOTH", true) -> "Bluetooth permission nearby devices se connection/discovery features ke liye ho sakti hai."
        name.contains("INTERNET", true) -> "Internet permission network connections allow karti hai; isse ye prove nahi hota ki data kis server ko bheja jata hai."
        name.contains("NOTIFICATIONS", true) || name.contains("POST_NOTIFICATIONS", true) -> "Notification permission app notifications dikhane se related hai."
        else -> "Permission ka exact use app ke code aur runtime behaviour par depend karta hai. Sirf naam se actual use confirm nahi hota."
    }
    return "$purpose Protection level: $category."
}

private data class DexSummary(
    val fileName: String,
    val version: String,
    val byteCount: Long,
    val classCount: Int,
    val methodCount: Int,
    val tables: List<String>,
    val validation: String
)

private fun parseDexSummaries(report: String): List<DexSummary> {
    val headers = Regex("""(?m)^===== (.+?) =====\\s*$""").findAll(report).toList()
    return headers.mapIndexed { index, match ->
        val bodyStart = match.range.last + 1
        val bodyEnd = headers.getOrNull(index + 1)?.range?.first ?: report.length
        val body = report.substring(bodyStart, bodyEnd)
        val fileName = match.groupValues[1]
        val version = Regex("""DEX version: (.+)""").find(body)?.groupValues?.get(1) ?: "Unknown"
        val byteCount = Regex("""Actual ZIP entry bytes: (\\d+)""").find(body)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val classCount = Regex("""Class definitions: (\\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val methodCount = Regex("""Method IDs: (\\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val tableNames = listOf("String IDs", "Type IDs", "Prototype IDs", "Field IDs", "Method IDs", "Class definitions")
        val tables = tableNames.mapNotNull { name ->
            Regex("""(?m)^$name:.*$""").find(body)?.value
        }
        val validation = Regex("""Header validation: (.+)""").find(body)?.groupValues?.get(1)
            ?: if (body.contains("Skipped:")) "Skipped (size limit)" else "Unavailable / invalid"
        DexSummary(fileName, version, byteCount, classCount, methodCount, tables, validation)
    }
}

private data class ComponentEntry(
    val type: String,
    val name: String,
    val exported: Boolean,
    val enabled: Boolean
)

private fun parseComponentEntries(report: String): List<ComponentEntry> {
    if (report == "No APK inspected yet." || report.startsWith("Could not inspect")) return emptyList()
    val sections = listOf(
        "Activities" to "Activity",
        "Services" to "Service",
        "Receivers" to "Receiver",
        "Providers" to "Provider"
    )
    return sections.flatMap { (heading, type) ->
        val lines = report.lines()
        val start = lines.indexOfFirst { it.startsWith("$heading (") }
        if (start < 0) emptyList() else {
            lines.drop(start + 1).takeWhile { it.isNotBlank() }
                .filter { it != "None found" }
                .mapNotNull { line ->
                    val name = line.substringBefore(" | ").trim()
                    if (name.isBlank()) null else ComponentEntry(
                        type = type,
                        name = name,
                        exported = Regex("""exported=true""", RegexOption.IGNORE_CASE).containsMatchIn(line),
                        enabled = Regex("""enabled=true""", RegexOption.IGNORE_CASE).containsMatchIn(line)
                    )
                }
        }
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

private fun extractSectionCount(report: String, heading: String): Int {
    val match = Regex("""${Regex.escape(heading)}\s*\((\d+)\)""")
        .find(report)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
}

@Composable
private fun ArchitectureNode(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EvidenceLine(
    title: String,
    detail: String,
    confidence: String,
    available: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(confidence, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        Text(if (available) "Available" else "Not tested",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun MetricBar(label: String, value: Int, maximum: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(value.toString(), style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value.toFloat() / maximum.coerceAtLeast(1).toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
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

private fun formatResourceIntelligenceReport(rawJson: String): String {
    return runCatching {
        val root = JSONObject(rawJson)
        val intelligence = root.optJSONObject("resource_intelligence")
            ?: error("resource_intelligence field missing")
        val categories = intelligence.optJSONObject("category_counts") ?: JSONObject()
        val extensions = intelligence.optJSONObject("extension_counts") ?: JSONObject()
        val largest = intelligence.optJSONArray("largest_files")
        buildString {
            appendLine("PYTHON RESOURCE INTELLIGENCE — READ ONLY")
            appendLine("Total resource and asset files: ${intelligence.optInt("total_files")}")
            appendLine()
            appendLine("CATEGORY BREAKDOWN")
            val categoryKeys = categories.keys().asSequence().toList().sorted()
            if (categoryKeys.isEmpty()) appendLine("No resource categories found.")
            categoryKeys.forEach { key -> appendLine("$key: ${categories.optInt(key)} files") }
            appendLine()
            appendLine("FILE EXTENSIONS")
            val extensionKeys = extensions.keys().asSequence().toList()
                .sortedByDescending { extensions.optInt(it) }
            if (extensionKeys.isEmpty()) appendLine("No extensions found.")
            extensionKeys.forEach { key -> appendLine("$key: ${extensions.optInt(key)}") }
            appendLine()
            appendLine("LARGEST RESOURCE FILES (maximum 30)")
            if (largest == null || largest.length() == 0) {
                appendLine("No resource files found.")
            } else {
                for (index in 0 until largest.length()) {
                    val item = largest.optJSONObject(index) ?: continue
                    val bytes = item.optLong("size_bytes")
                    appendLine("${index + 1}. ${item.optString("path")} — $bytes bytes — ${item.optString("category")}")
                }
            }
            appendLine()
            appendLine("LIMITATIONS")
            appendLine("• Classification uses archive paths and file extensions only.")
            appendLine("• Resource contents are not extracted, decoded, or executed.")
            appendLine("• Compiled resource IDs and resources.arsc values are not decoded here.")
        }
    }.getOrElse { error ->
        "Python resource intelligence could not be formatted: ${error.message ?: "Invalid report"}\n\nRaw report:\n${rawJson.take(6000)}"
    }
}
