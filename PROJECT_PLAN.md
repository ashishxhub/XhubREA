# XhubREA — Project Plan

## Vision
A learning-focused Android research assistant for understanding apps and games the user owns, open-source projects, or apps they have explicit permission to inspect, then planning independent implementations.

## Implemented in the current MVP
- Jetpack Compose dashboard and local project notes.
- APK picker and read-only package metadata inspection.
- Declared permissions and component inventory.
- Exported component metadata review.
- DEX header and identifier-table structure report.
- APK resource/file inventory, file-type summary and package-layout overview.
- Heuristic game/framework detection and native library inventory.
- APK signer certificate metadata report.
- Basic two-APK metadata comparison.
- Full Research Lab summary for code/DEX presence, resource/UI filenames, framework hints, native libraries, hostname-only network hints and archive checks.
- Combined TXT report save/share.
- GitHub Actions debug APK build.

## Planned follow-up work (not yet implemented)
1. Binary AndroidManifest.xml decoder with tested parsing.
2. Resource table/value decoder and safe preview of supported image assets.
3. Real DEX instruction disassembly/decompilation using a maintained parser library, with strict size limits.
4. Structured UI/layout analysis for supported resource formats.
5. More reliable API endpoint discovery with false-positive controls; no secret/token extraction.
6. Project blueprint export and starter-project generation.
7. Automated unit/UI test templates and test execution guidance.
8. Optional AI provider integration only after explicit API-key setup, privacy notice and secure key storage.
9. Device-level usability, performance, accessibility and APK compatibility testing.

## Technology
- Kotlin, Android SDK, Jetpack Compose and Gradle.
- GitHub Actions for repeatable debug builds.
- Optional future AI provider; no cloud AI is currently connected.

## Safety and originality
- Inspect only owned, open-source or explicitly authorized APKs.
- Never bypass DRM, authentication, licenses, payment checks or anti-cheat.
- Do not extract secrets, credentials or private user data.
- Do not execute unknown APKs or native libraries.
- Build alternatives with original code, branding and assets.

## Quality gate
A GitHub Actions build passing proves compilation and APK packaging only. Before calling the app complete, also test APK selection, analysis, error handling, report export, large files, Android-version compatibility and usability on a real device.
