# XhubREA Development Tracker

Latest implementation commit: e6c7c4ade6a01e65bf952c21aaa3be2f89257d62. Android CI run 44101181501 is in progress; do not treat the resource-table decoder as verified until it passes.

## Status definitions
- NOT_STARTED: no implementation exists.
- IN_PROGRESS: implementation is underway.
- IMPLEMENTED: code exists but required verification is incomplete.
- TESTING: verification is underway.
- VERIFIED: relevant automated checks passed; device/runtime checks are tracked separately.
- BLOCKED: cannot proceed until a documented dependency or environment issue is resolved.

## Phase 1 — Repository and build foundation
| Requirement | Status | Evidence / remaining work |
|---|---|---|
| Repository inspection | VERIFIED | Main branch tree and build configuration inspected. |
| Android debug build | VERIFIED | Actions run 43685074373 succeeded, including lint, unit tests, assembleDebug and bundleRelease. |
| Python unit-test workflow | VERIFIED | Python workflow run 43684458135 previously succeeded. Rerun after next Python changes. |
| Chaquopy plugin configuration | VERIFIED | Main build passed with Chaquopy 16.1.0 / Python 3.13 packaging. |
| Python bridge dashboard action | IMPLEMENTED | Kotlin bridge code is present and Android CI passes; on-device Python invocation still requires real-device verification. |
| Development tracker | VERIFIED | This file. |

## Phase 2 — Existing analysis modules
| Requirement | Status | Evidence / remaining work |
|---|---|---|
| APK metadata and package inspection | IMPLEMENTED | Existing MainActivity implementation; add focused tests and device verification. |
| Binary AndroidManifest decoder | IMPLEMENTED | Bounded Kotlin parser exists; add fixture-based parser tests and validate varied manifests. |
| DEX header/table metadata | IMPLEMENTED | Metadata-only analyzer exists; not a decompiler. |
| APK resources/assets inventory | IMPLEMENTED | Basic inventory exists; resource table decoding and safe previews remain. |
| Native library inventory | IMPLEMENTED | Existing archive-based inventory. |
| Certificate metadata | IMPLEMENTED | Metadata reporting exists; not full signature verification. |
| Framework/game fingerprinting | IMPLEMENTED | Heuristic indicators only; expand supported fingerprints and evidence. |
| APK comparison | IMPLEMENTED | Basic comparison exists; detailed normalized comparison remains. |
| Python archive structure report | VERIFIED | Python tests passed in workflow 43684458135; Android bridge runtime still needs device test. |

## Optional AI agent integration — Google Antigravity SDK
| Requirement | Status | Evidence / remaining work |
|---|---|---|
| SDK dependency manifest for separate Python backend | IMPLEMENTED | `python/requirements-antigravity.txt`; excluded from Android/Chaquopy dependencies. |
| Read-only optional adapter | IMPLEMENTED | `python/xhubrea_engine/antigravity_adapter.py`; opt-in flag, server-side API-key check, lazy import. |
| Adapter safety guard tests | IMPLEMENTED | `python/tests/test_antigravity_adapter.py`; covers disabled-by-default, missing key and invalid prompts. |
| Linux Python 3.12 SDK install/import smoke test | VERIFIED | GitHub Actions run 44094622362 passed on commit `35c75411f45e274ad92bfb4f3f968491ab5f4a48`. |
| Optional authenticated backend API | VERIFIED | FastAPI endpoints `/health`, `/v1/agent/chat`, `/v1/research/analyze`, `/v1/research/structure`; Python CI passed for core backend tests in run 44095951345. |\n| Live-agent authentication test | NOT_STARTED | Requires owner-provided Gemini API key; no secret is stored in GitHub source. |
| Android/Chaquopy compatibility | BLOCKED | SDK compiled runtime wheel compatibility with Android/Chaquopy is not established; keep SDK outside Android app. |

## Phase 3 — Remaining development roadmap
| Requirement | Status | Dependencies / notes |
|---|---|---|
| Python archive safety regression tests | VERIFIED | Added Windows absolute-path, symlink metadata, encrypted-entry metadata, archive limits, duplicate names, malformed input, and bounded resource inventory checks. Python workflow run 44098844747 passed on commit `0edb87875013cd8f2d3f6f1298feed6167316a8e`. |
| Binary manifest parser fixture tests | VERIFIED | Added synthetic valid AXML string-pool/element fixture plus malformed header, wrong type, document-size, chunk-boundary, and string-offset tests. Android CI run 44099995219 passed lint, `testDebugUnitTest`, `assembleDebug`, and `bundleRelease` on commit `cb8d6834cc2c30ff28563154b99aa24da505c4d8`. |
| Resource table/value decoder | TESTING | Added bounded read-only package/type/configuration metadata parser and synthetic valid/malformed fixtures. Android CI run 44101181501 is in progress; actual APK fixture validation and runtime integration remain. |
| DEX instruction metadata/disassembly | NOT_STARTED | Use maintained parser; metadata/disassembly only for authorized files, no execution. |
| Hex and binary viewer | NOT_STARTED | Bounded paging; never load huge files fully into memory. |
| String explorer | NOT_STARTED | Filtered, bounded extraction; avoid exposing credentials/secrets. |
| Dependency and architecture graph | NOT_STARTED | Build from verified component/file evidence. |
| Game research lab expansion | NOT_STARTED | Heuristic asset inventory and engine evidence. |
| REA Detective evidence correlation | NOT_STARTED | Findings must retain evidence and confidence labels. |
| Evidence-grounded REA research workflow | VERIFIED | `research_workflow.py` builds bounded prompts from operator-supplied evidence; `/v1/research/analyze` endpoint and unit tests passed in run 44095412888. |\n| Structured Intelligence Engine | IMPLEMENTED | `intelligence.py` labels operator observations, confidence tiers, groups same-source evidence without causal claims; `/v1/research/structure` added. Python CI run 44095951345 passed after correction; latest endpoint commit still awaits CI. |\n| Web dashboard API client | IMPLEMENTED | HTTPS-enforcing API client, health check, authenticated chat and evidence-structure calls, Vitest coverage. Latest Web CI pending. |\n| Web dashboard connection settings | IMPLEMENTED | User-entered backend URL and temporary token held in page memory only; token is not persisted. Backend CORS uses explicit origin allowlist. Latest Web CI pending. |\n| AI assistant | IN_PROGRESS | Optional Antigravity adapter and authenticated backend endpoints exist; live provider test and Android integration remain pending. |
| Knowledge graph visualization | NOT_STARTED | Depends on normalized analysis model. |
| Compare Intelligence | NOT_STARTED | Depends on normalized reports. |
| PDF/HTML/JSON/CSV reports | NOT_STARTED | JSON first, then HTML/PDF; CSV only for tabular data. |
| Room persistence and project history | NOT_STARTED | Add schema migrations and tests. |
| Plugin architecture | NOT_STARTED | Define stable module contract before plugin loading. |
| UI/UX redesign and accessibility | IN_PROGRESS | Existing Compose UI; comprehensive redesign and accessibility tests remain. |
| Performance and cancellation | NOT_STARTED | Add bounded IO, cancellation, progress and memory tests. |
| Full regression and device tests | NOT_STARTED | Requires test devices/emulators and representative authorized fixtures. |
| Signed production release | NOT_STARTED | Requires owner-controlled signing configuration; never commit signing secrets. |

## Mandatory execution loop
Inspect -> implement one bounded module -> test -> build -> inspect logs -> fix -> retest -> rebuild -> update this tracker -> continue.

Do not mark a feature VERIFIED based only on the presence of code or a successful compile. Record CI verification separately from real-device verification.

## Current exact resume point
Latest main commit `e6c7c4ade6a01e65bf952c21aaa3be2f89257d62` adds the initial bounded read-only `resources.arsc` metadata decoder and fixture tests. Android CI run 44101181501 is currently in progress. Previous Android CI run 44100550499 passed on commit `41eb0f2bc77c5b2f231069d74d31cad82a03c77d`. Python archive safety regression tests and binary AndroidManifest decoder fixture tests are complete and previously verified. Current task: finish resource-table decoder verification, correct any CI failures, then connect the report to the APK analysis flow.


## Frontend V2 implementation update — 2026-11-17

| Deliverable | Status | Evidence / remaining work |
|---|---|---|
| Editable Figma desktop dashboard | IMPLEMENTED | 1440×980 frame, editable layers. |
| Editable Figma Android home reference | IMPLEMENTED | 390×844 frame, editable layers. |
| Web dashboard V2 visual tokens | IMPLEMENTED | Updated web-dashboard/src/styles.css on feat/xhubrea-v2-frontend-design; existing React navigation, composer, settings and help states retained. |
| Android Compose V2 palette alignment | IMPLEMENTED | Updated Material 3 color scheme in MainActivity.kt; existing APK inspection workflows were preserved. |
| Frontend design handoff | IMPLEMENTED | docs/FRONTEND_DESIGN.md. |
| Pull request | IN_PROGRESS | PR #20 is open; not merged. |
| Web production build and UI tests for this branch | NOT_VERIFIED | No workflow run is currently associated with the latest frontend commit. Run GitHub Actions and fix any failures before merge. |
| Android lint, unit tests and debug APK for this branch | NOT_VERIFIED | Theme-only Kotlin change has not yet been built in CI or on a device. |
| Android screen layout redesign to match the Figma home frame | NOT_STARTED | Current Android dashboard is feature-rich and should be redesigned incrementally without removing existing APK-analysis functions. |
| Backend-connected end-to-end UI verification | NOT_STARTED | Requires deployed API endpoint, owner-managed token and device/browser testing. |

The V2 Android change so far is a Material 3 color-token alignment only. It does not claim that the entire Compose screen has been redesigned to match the Figma mobile frame.


### Android Compose V2 — dashboard foundation (2026-11-17)
- [x] Reworked the Android dashboard hero into the V2 dark workspace visual language.
- [x] Added local-workspace and evidence-first status chips.
- [x] Clarified the read-only APK analysis purpose in the first-screen copy.
- [x] Preserved existing APK picker, analysis modules, reports, settings and project-note flows.
- [ ] Bottom navigation and dedicated screen destinations.
- [ ] Dedicated Research Assistant, APK upload and Reports screens.
- [ ] Android build, lint and device verification (not yet run).
- Note: this is a first-screen UI implementation step, not a complete Android redesign.


### Android V2 workspace navigation (2026-11-17)
- [x] Added section navigation for Overview, APK, Research, Reports and Settings.
- [x] Added dedicated APK entry screen with APK picker and route to the full existing analysis workspace.
- [x] Added Research screen for current research, network, privacy, behaviour and resource reports.
- [x] Added Reports screen with analysis summary and report export action.
- [x] Preserved the existing detailed Overview analysis workspace and Settings preferences.
- [ ] Replace the compact top section tabs with a persistent bottom navigation bar.
- [ ] Add a dedicated conversational Research Assistant (not yet implemented).
- [ ] Verify Android compile, lint, emulator/device navigation and report export.
