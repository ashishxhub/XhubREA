# XhubREA Android Research API Client

The Android app can call the shared XhubREA FastAPI backend through
`ResearchApiClient`.

## Requirements

- A deployed XhubREA backend reachable over HTTPS.
- A bearer token configured by the backend owner.
- The Android app's INTERNET permission (already declared in the manifest).

## Usage

Create the client at the point where the user has supplied the backend URL
and a short-lived bearer token:

```kotlin
val api = ResearchApiClient(
    baseUrl = configuredBackendUrl,
    bearerToken = sessionBearerToken
)

// Public health endpoint; no bearer token is sent.
val health = api.checkHealth()

// Authenticated, read-only AI research request.
val answer = api.sendMessage("Summarize the supplied authorized app metadata.")
```

Do not hard-code API tokens in source code, commit them, or store them in
plain-text preferences. Supply the token at call time from an appropriate
session/secure credential flow. Remote connections must use HTTPS.

## Current scope

This client is a networking foundation only. It does not change the Android
screen/UI, persist credentials, or automatically upload APKs. Live AI calls
require a deployed backend with server-side Antigravity/Gemini configuration.
Unknown APKs remain untrusted data and must not be executed.
