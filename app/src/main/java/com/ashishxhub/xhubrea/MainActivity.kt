package com.ashishxhub.xhubrea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val preferences = remember {
        context.getSharedPreferences("xhubrea_project", android.content.Context.MODE_PRIVATE)
    }

    var target by remember { mutableStateOf(preferences.getString("project_name", "") ?: "") }
    var notes by remember { mutableStateOf(preferences.getString("project_notes", "") ?: "") }
    var status by remember {
        mutableStateOf("Create a project note for an app you own, an open-source app, or one you have permission to study.")
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
                text = "Notes are stored locally on this device. Use only apps you own, open-source projects, or apps you have permission to inspect. Do not use this project to bypass security, paid access, authentication or anti-cheat systems.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
