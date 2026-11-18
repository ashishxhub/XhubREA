package com.ashishxhub.xhubrea

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTPS-only client for XhubREA's read-only research backend.
 * The bearer token is supplied at call time and is never persisted by this class.
 */

/** Caller-supplied metadata only; this model never reads or executes an APK. */
data class AppSnapshotInput(
    val appLabel: String = "Application",
    val permissions: List<String> = emptyList(),
    val components: List<String> = emptyList(),
    val sourceStructure: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val architecture: List<String> = emptyList()
)

class ResearchApiClient(
    private val baseUrl: String,
    private val bearerToken: String,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 20_000
) {
    // The backend health endpoint is public; do not require or send a bearer token.
    suspend fun checkHealth(): JSONObject = request("/health", null, authenticated = false)

    suspend fun sendMessage(prompt: String): String {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotEmpty()) { "Research prompt cannot be empty." }
        require(cleanPrompt.length <= 8_000) { "Research prompt is too long (maximum 8,000 characters)." }

        val response = request("/v1/agent/chat", JSONObject().put("prompt", cleanPrompt))
        return response.optString("answer").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Backend returned an invalid response.")
    }

    /**
     * Compares two caller-provided inventories through the authenticated backend.
     * Each category is limited to 200 labels of at most 300 characters.
     */
    suspend fun compareApps(appA: AppSnapshotInput, appB: AppSnapshotInput): JSONObject {
        return request("/v1/apps/compare", buildComparisonPayload(appA, appB))
    }

    internal fun buildComparisonPayload(appA: AppSnapshotInput, appB: AppSnapshotInput): JSONObject {
        fun snapshot(input: AppSnapshotInput): JSONObject {
            require(input.appLabel.isNotBlank() && input.appLabel.length <= 300) {
                "App label must contain 1–300 non-blank characters."
            }
            fun labels(values: List<String>, category: String): org.json.JSONArray {
                require(values.size <= 200) { "$category can contain at most 200 entries." }
                values.forEach {
                    require(it.isNotBlank() && it.length <= 300) {
                        "$category entries must contain 1–300 non-blank characters."
                    }
                }
                return org.json.JSONArray(values)
            }
            return JSONObject()
                .put("app_label", input.appLabel.trim())
                .put("permissions", labels(input.permissions, "Permissions"))
                .put("components", labels(input.components, "Components"))
                .put("source_structure", labels(input.sourceStructure, "Source structure"))
                .put("dependencies", labels(input.dependencies, "Dependencies"))
                .put("architecture", labels(input.architecture, "Architecture"))
        }
        return JSONObject().put("app_a", snapshot(appA)).put("app_b", snapshot(appB))
    }

    private suspend fun request(
        path: String,
        body: JSONObject?,
        authenticated: Boolean = true
    ): JSONObject = withContext(Dispatchers.IO) {
        val normalizedBase = validateBaseUrl(baseUrl)
        if (authenticated) require(bearerToken.isNotBlank()) { "Backend bearer token is required." }

        val connection = (URL(normalizedBase + path).openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            if (authenticated) setRequestProperty("Authorization", "Bearer $bearerToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(text).optString("detail") }.getOrDefault("")
                throw IllegalStateException(detail.ifBlank { "Backend request failed ($code)." })
            }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        internal fun validateBaseUrl(value: String): String {
            val trimmed = value.trim().trimEnd('/')
            require(trimmed.isNotEmpty()) { "Backend URL is required." }
            val parsed = URL(trimmed)
            require(parsed.protocol == "https") { "Use HTTPS for the backend connection." }
            require(parsed.userInfo == null && parsed.query == null && parsed.ref == null) {
                "Backend URL must not contain credentials, query parameters, or fragments."
            }
            return trimmed
        }
    }
}
