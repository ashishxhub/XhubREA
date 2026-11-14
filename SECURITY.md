# Security Policy

## Intended use
XhubREA is a local, read-only research tool for APKs that you own, open-source APKs, or APKs you have explicit permission to inspect.

## Current safeguards
- APKs are selected through Android's system document picker; the app does not request broad storage permissions.
- APK import is size-limited to 512 MiB and copied with a bounded streaming operation.
- File hashing is streamed rather than loading the entire APK into memory.
- APK inspection is static and read-only. The app does not install or execute selected APKs.
- The Android manifest does not declare the INTERNET permission.
- Cleartext network traffic is disabled.
- Android backup is disabled for app data.

## Data handling
Project notes and imported APK working copies are stored locally by the app. Do not import APKs or files containing information you are not authorized to handle. Before sharing a report, review it for information you do not intend to disclose.

## Supported platform
The current minimum SDK is Android 6.0 (API 23), with target SDK 36. Compatibility on physical devices still requires testing.

## Reporting a security issue
Do not publish exploit details or private data in a public issue. Contact the repository owner privately through GitHub's available contact options and include steps to reproduce, affected version, and impact. Do not include real user data or confidential APKs.
