# XhubREA — Project Plan

## Vision
A learning-focused, evidence-based Android assistant for understanding apps and games the user owns, open-source projects, or files they have explicit permission to inspect, then planning original implementations.

## Current implementation
- Jetpack Compose Android dashboard and local project workspace.
- Read-only APK selection, package metadata, permissions and component inventory.
- Exported-component review.
- DEX header/table metadata inspection (not a decompiler).
- APK resource/file inventory, framework/game-engine hints and native-library inventory.
- Signer certificate metadata (not full signature verification).
- Basic APK comparison and Research Lab summary.
- Bounded binary AndroidManifest decoder.
- Python APK archive engine with ZIP validation, SHA-256, bounded entry counts, file inventories and path-safety warnings.
- Chaquopy Python 3.13 integration and dashboard action.
- Android CI passed for main commit `5c3efb3783a9ffb4f3ad466c8dc923d6c606d406`; Python CI passed for the previously tested Python engine revision. Python bridge still needs on-device runtime verification.

## Development phases
1. Repository inspection and build stabilization.
2. Python archive engine hardening and regression tests.
3. Kotlin-Python runtime verification and error handling.
4. Binary manifest parser fixture tests and decoder improvements.
5. Resource table/value decoding and safe previews.
6. Bounded hex, binary and string explorers.
7. DEX metadata/disassembly using a maintained parser, without executing inspected code.
8. Normalized findings model, evidence provenance and confidence labels.
9. Dependency explorer, architecture mapper and knowledge graph.
10. Game research inventory and engine evidence.
11. Compare Intelligence and structured report formats.
12. REA Detective and optional AI explanations with consent and secure key management.
13. Room persistence, history, projects and plugin contracts.
14. UI/UX, accessibility, performance, cancellation and offline behavior.
15. Full regression, emulator/device validation and release preparation.

## Quality gates
- Implement one coherent module at a time.
- Run focused tests and regression tests.
- Run Android lint, unit tests and build after meaningful Android changes.
- Run Python compile checks and unit tests after Python changes.
- Inspect actual failure logs and fix root causes before proceeding with dependent work.
- Track CI build verification separately from on-device runtime verification.
- Do not claim unsupported formats or untested behavior.

## Safety and originality
Inspect only owned, open-source or explicitly authorized files. Do not bypass DRM, authentication, licenses, payment checks or anti-cheat; extract secrets; or execute unknown APKs. Keep analysis read-only and produce original implementations, branding and assets.
