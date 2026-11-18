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
}
