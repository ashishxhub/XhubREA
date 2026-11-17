# XhubREA Development Tracker

Last verified against main commit: 0edb87875013cd8f2d3f6f1298feed6167316a8e (Python engine CI passed; Android CI pending)

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
| Resource table/value decoder | NOT_STARTED | Requires bounded parser and format fixtures. |
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
Main commit `cb8d6834cc2c30ff28563154b99aa24da505c4d8` has passing Android lint, unit tests, debug APK build, and unsigned release bundle build (workflow run 44099995219); Python archive-engine CI passed in run 44098844747 on commit `0edb87875013cd8f2d3f6f1298feed6167316a8e`. Completed: Python archive safety regression coverage and binary AndroidManifest decoder fixture tests. Next task: inspect and implement a bounded read-only Android resources.arsc table inventory/value decoder with strict chunk and string-pool limits.
