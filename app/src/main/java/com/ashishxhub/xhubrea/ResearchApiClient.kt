package com.ashishxhub.xhubrea

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small HTTPS-only client for the shared XhubREA research backend.
 * The bearer token is supplied at call time and is never persisted by this class.
 */
class ResearchApiClient(
    private val baseUrl: String,
    private val bearerToken: String,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 20_000
) {
    suspend fun checkHealth(): JSONObject = request("/health", null)

    suspend fun sendMessage(prompt: String): String {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotEmpty()) { "Research prompt cannot be empty." }
        require(cleanPrompt.length <= 8_000) { "Research prompt is too long (maximum 8,000 characters)." }

        val response = request("/v1/agent/chat", JSONObject().put("prompt", cleanPrompt))
        return response.optString("answer").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Backend returned an invalid response.")
    }

    private suspend fun request(path: String, body: JSONObject?): JSONObject = withContext(Dispatchers.IO) {
        val normalizedBase = validateBaseUrl(baseUrl)
        require(bearerToken.isNotBlank()) { "Backend bearer token is required." }

        val connection = (URL(normalizedBase + path).openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $bearerToken")
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
            val localHost = parsed.host.equals("localhost", true) ||
                parsed.host == "127.0.0.1" || parsed.host == "10.0.2.2"
            require(parsed.protocol == "https" || (localHost && parsed.protocol == "http")) {
                "Use HTTPS for remote backend connections."
            }
            require(parsed.userInfo == null && parsed.query == null && parsed.ref == null) {
                "Backend URL must not contain credentials, query parameters, or fragments."
            }
            return trimmed
        }
    }
}
