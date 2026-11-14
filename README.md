# XhubREA

**Universal AI Reverse Engineering Assistant** — a learning project for understanding apps you own, open-source apps, or apps you have explicit permission to inspect, then building independent alternatives.

## Current MVP

- Native Android app built with Kotlin and Jetpack Compose.
- Starter dashboard with an app/game project input.
- Initial sections for app overview, feature mapping and original implementation planning.
- GitHub Actions workflow that builds a debug APK and uploads it as an artifact.

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
