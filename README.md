# XhubREA

**Universal AI Reverse Engineering Assistant** — a learning project for understanding apps you own, open-source apps, or apps you have explicit permission to inspect, then building independent alternatives.

## Current MVP

- Native Android app built with Kotlin and Jetpack Compose.
- Starter dashboard with an app/game project input.
- Initial sections for app overview, feature mapping and original implementation planning.
- GitHub Actions workflow that builds a debug APK and uploads it as an artifact.

## Implemented research workspace

The current Android MVP includes:
- Local project notes saved on the device.
- Read-only APK selection and package metadata (name, package, version, SDK, flags and declared permissions).
- Component inventory for activities, services, receivers and providers.
- Exported-component review based on manifest metadata.
- DEX header/table structure inspection (counts and bounds; not a decompiler).
- APK resource/file inventory, file-type summary and package-layout overview.
- Heuristic framework/game-engine fingerprints and native-library ABI inventory.
- APK signer certificate metadata (not a complete signature-verification service).
- Basic two-APK metadata comparison.
- Full Research Lab report: DEX presence, UI/resource filename inventory, engine hints, native library inventory, hostname-only network hints, archive size checks and original-project planning notes.
- Combined report sharing and TXT export.

All inspection is static and read-only. The app does not install or execute the selected APK.

## Important feature boundaries

This is an early research MVP, not a complete commercial reverse-engineering suite. It does not currently provide instruction-level DEX decompilation, decoded binary Android XML, live runtime tracing, packet capture, API request replay, automatic screen capture, AI-cloud analysis, or a verified generated app build. Some APKs use obfuscation, split packages, dynamic code or formats that this MVP cannot interpret. Reports must be treated as clues, not proof. Test the app on-device with APKs you own or are authorized to inspect.

## Build the APK on GitHub

1. Open the **Actions** tab in this repository.
2. Select **Android Debug APK**.
3. Open the latest successful run.
4. Under **Artifacts**, download **XhubREA-debug-apk**.
5. Extract the ZIP and install `app-debug.apk` on your Android device. You may need to allow installation from your file manager.

Every push to `main` also starts the build workflow automatically.

## Build locally

Requirements: JDK 17 and Gradle 8.9.

```bash
gradle --no-daemon assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/ashishxhub/xhubrea/MainActivity.kt
├── .github/workflows/android-build.yml
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Safety and originality

Use XhubREA only for authorized analysis. Do not use it to bypass DRM, payment or license checks, authentication, anti-cheat, or other access controls; extract private data or secrets; or run unknown APKs. Do not copy proprietary code, branding, or assets. Build original implementations and use assets you have rights to use.

## Roadmap

1. Android project foundation and CI build.
2. Structured app/project notes.
3. Authorized APK metadata and manifest viewer.
4. Manual screen and feature documentation.
5. Original app specification and implementation workspace.
6. Optional AI-assisted explanations, with privacy and safety controls.
