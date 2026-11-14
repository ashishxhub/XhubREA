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

                        val permissionList = packageInfo.requestedPermissions?.toList().orEmpty()
                        val activities = packageInfo.activities?.map { it.name }.orEmpty()
                        val services = packageInfo.services?.map { it.name }.orEmpty()
                        val receivers = packageInfo.receivers?.map { it.name }.orEmpty()
                        val providers = packageInfo.providers?.map { it.name }.orEmpty()

                        buildString {
                            appendLine("APK metadata (read-only)")
                            appendLine("Package: ${packageInfo.packageName}")
                            appendLine("Version name: ${packageInfo.versionName ?: "Unknown"}")
                            appendLine("Version code: $version")
                            appendLine("Minimum SDK: $minSdk")
                            appendLine("Target SDK: $targetSdk")
                            appendLine()
                            appendLine("Declared permissions (${permissionList.size})")
                            appendLine(permissionList.take(30).joinToString("\n").ifBlank { "None declared" })
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
            InfoCard(title = "APK report", body = apkReport)
            Spacer(modifier = Modifier.height(8.dp))
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


private fun reportSection(report: String, heading: String): String {
    if (report == "No APK inspected yet." || report.startsWith("Could not inspect")) {
        return "Choose a readable APK to view this section."
    }
    val lines = report.lines()
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
