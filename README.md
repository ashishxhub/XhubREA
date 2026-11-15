# XhubREA

**Universal AI Reverse Engineering Assistant** — a learning-focused Android research assistant for understanding apps and games you own, open-source projects, or files you have explicit permission to inspect, then planning original implementations.

## Current implementation

- Native Android app built with Kotlin and Jetpack Compose.
- Read-only APK selection and package metadata inspection.
- Permission and component inventory, including exported-component metadata.
- DEX header/table metadata inspection (not instruction-level decompilation).
- APK resource/file inventory, framework/game-engine hints and native-library inventory.
- Signer certificate metadata (not a complete signature-verification service).
- Basic two-APK metadata comparison and Research Lab report.
- Bounded Kotlin binary AndroidManifest decoder.
- Python APK archive-structure engine, with SHA-256, ZIP-entry inventory, DEX/native/resource counts and path-safety indicators.
- Chaquopy integration and a dashboard action to invoke the Python engine.
- GitHub Actions for Android lint/tests/build and Python unit tests.

The latest recorded main-branch Android workflow for commit `5c3efb3783a9ffb4f3ad466c8dc923d6c606d406` completed successfully, including lint, unit tests, debug APK packaging and unsigned release bundle packaging. This confirms CI build success, not real-device runtime verification of the Python bridge.

## Feature boundaries

XhubREA is an evolving static research MVP, not a complete commercial reverse-engineering suite. It does not execute or install analyzed APKs. It does not currently provide complete instruction-level DEX decompilation, full resource-table decoding, runtime tracing, packet capture, API request replay, or universal interpretation of proprietary app/game logic. Some formats, obfuscated files, split packages and dynamic code may not be fully understood. Findings are evidence-based indicators, not proof.

## Build the APK on GitHub

1. Open the repository's **Actions** tab.
2. Select **Android Debug APK**.
3. Open the latest successful run.
4. Download the **XhubREA-debug-apk** artifact.
5. Extract it and install `app-debug.apk` on a compatible Android device.

## Android compatibility and release status

- Minimum supported Android version: Android 7.0 (API 24), required by the current Chaquopy configuration.
- Compile/target SDK: Android 16 (API 36).
- APK selection uses the system document picker; broad storage access is not requested.
- The current manifest does not declare network permission and disables cleartext traffic.
- CI produces a debug APK and an **unsigned** release AAB. The unsigned AAB is not ready for Play Console submission. A production release still requires secure owner-managed signing, device testing, privacy disclosures and accurate store declarations.

## Build locally

Requirements: JDK 17, Android SDK Platform 36, Gradle 8.11.1, Android Gradle Plugin 8.9.1.

```bash
gradle --no-daemon lintDebug testDebugUnitTest assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/ashishxhub/xhubrea/
├── python/
│   ├── xhubrea_engine/
│   └── tests/
├── .github/workflows/
├── DEVELOPMENT_TRACKER.md
├── PROJECT_PLAN.md
└── README.md
```

## Safety and originality

Use XhubREA only for authorized analysis. Do not bypass DRM, authentication, licensing, payment checks, anti-cheat or other access controls; extract credentials or private data; or execute unknown APKs. Do not copy proprietary code, branding or assets. Analyze locally where possible and obtain explicit consent before any external processing.

## Development progress

See [DEVELOPMENT_TRACKER.md](DEVELOPMENT_TRACKER.md) for implementation status, verification evidence, blockers and the exact next task.
