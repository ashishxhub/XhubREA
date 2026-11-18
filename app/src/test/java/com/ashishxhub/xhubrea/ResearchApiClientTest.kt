package com.ashishxhub.xhubrea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ResearchApiClientTest {
    @Test
    fun acceptsHttpsBackendAndNormalizesTrailingSlash() {
        assertEquals(
            "https://api.example.com",
            ResearchApiClient.validateBaseUrl("  https://api.example.com/  ")
        )
    }

    @Test
    fun rejectsRemoteHttpBackend() {
        assertThrows(IllegalArgumentException::class.java) {
            ResearchApiClient.validateBaseUrl("http://api.example.com")
        }
    }

    @Test
    fun rejectsLocalHttpBackendBecauseAppDisallowsCleartextTraffic() {
        assertThrows(IllegalArgumentException::class.java) {
            ResearchApiClient.validateBaseUrl("http://10.0.2.2:8000")
        }
    }

    @Test
    fun rejectsCredentialsInBackendUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            ResearchApiClient.validateBaseUrl("https://user:pass@api.example.com")
        }
    }

    @Test
    fun rejectsBlankBackendUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            ResearchApiClient.validateBaseUrl("   ")
        }
    }

    @Test
    fun buildsComparisonPayloadWithBothSnapshots() {
        val payload = ResearchApiClient(
            "https://api.example.com", "token"
        ).buildComparisonPayload(
            AppSnapshotInput(appLabel = "App A", permissions = listOf("camera")),
            AppSnapshotInput(appLabel = "App B", architecture = listOf("MVVM"))
        )

        assertEquals("App A", payload.getJSONObject("app_a").getString("app_label"))
        assertEquals("camera", payload.getJSONObject("app_a").getJSONArray("permissions").getString(0))
        assertEquals("App B", payload.getJSONObject("app_b").getString("app_label"))
        assertEquals("MVVM", payload.getJSONObject("app_b").getJSONArray("architecture").getString(0))
    }

    @Test
    fun rejectsMoreThanTwoHundredLabels() {
        assertThrows(IllegalArgumentException::class.java) {
            ResearchApiClient("https://api.example.com", "token").buildComparisonPayload(
                AppSnapshotInput(permissions = List(201) { "permission-$it" }),
                AppSnapshotInput()
            )
        }
    }

    @Test
    fun rejectsOverlongOrBlankLabels() {
        val client = ResearchApiClient("https://api.example.com", "token")
        assertThrows(IllegalArgumentException::class.java) {
            client.buildComparisonPayload(AppSnapshotInput(appLabel = " "), AppSnapshotInput())
        }
        assertThrows(IllegalArgumentException::class.java) {
            client.buildComparisonPayload(
                AppSnapshotInput(components = listOf("x".repeat(301))),
                AppSnapshotInput()
            )
        }
    }

}
