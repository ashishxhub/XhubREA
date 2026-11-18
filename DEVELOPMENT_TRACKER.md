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
| Evidence-grounded REA research workflow | VERIFIED | `research_workflow.py` builds bounded prompts from operator-supplied evidence; `/v1/research/analyze` endpoint and unit tests passed in run 44095412888. |\n| Structured Intelligence Engine | IMPLEMENTED | `intelligence.py` labels operator observations, confidence tiers, groups same-source evidence without causal claims; `/v1/research/structure` added. Python CI run 44095951345 passed after correction; security/rate-limit regression suite subsequently passed in run `44234291071`. |\n| Web dashboard API client | IMPLEMENTED | HTTPS-enforcing API client, health check, authenticated chat and evidence-structure calls, Vitest coverage. Latest Web CI pending. |\n| Web dashboard connection settings | IMPLEMENTED | User-entered backend URL and temporary token held in page memory only; token is not persisted. Backend CORS uses explicit origin allowlist. Latest Web CI pending. |\n| AI assistant | IN_PROGRESS | Optional Antigravity adapter and authenticated backend endpoints exist; live provider test and Android integration remain pending. |
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


### XhubREA 60-30-10 interface color system (2026-11-17)
- [x] Applied the 60% base / 30% surface / 10% accent hierarchy to the editable Figma Web Dashboard and Android Home frames.
- [x] Base: #0C0F15 for the main canvas and large background areas.
- [x] Surfaces: #151A23 and #1A2230 for sidebars, cards, composers and raised panels.
- [x] Accent: #6487FA for primary actions, active navigation and key highlights.
- [x] Standardized supporting text (#EDF1F8 / #929EAF) and borders (#293241); removed competing decorative accent colors from the primary visual hierarchy.
- [x] Added matching 60-30-10 token overrides to the web dashboard stylesheet.
- [ ] Android runtime theme/build verification remains pending; Figma Android frame is a design reference and does not itself change the installed APK.


### Android Research Assistant MVP (2026-11-18)
- [x] Added a dedicated Research Assistant experience inside the Research tab.
- [x] Added a question composer and quick prompts for APK explanation, permissions, unknowns and network findings.
- [x] Answers are generated only from the locally available APK/research reports and label missing evidence.
- [x] Clearly disclosed that this MVP is local report Q&A, not a connected cloud LLM.
- [ ] Add structured evidence references and richer report parsing.
- [ ] Run Android compile, lint and emulator/device checks; not yet verified.


### Android persistent bottom navigation (2026-11-18)
- [x] Added a reusable Material 3-styled bottom navigation component for APK, Research and Reports destinations.
- [x] Added the same persistent bottom navigation to Settings, with working routes back to Overview and other sections.
- [x] Kept the existing analysis screens, local Research Assistant and settings preferences intact.
- [ ] Move Overview's legacy top tabs into the bottom bar so navigation placement is consistent on every destination.
- [ ] Run Kotlin compile, Android lint, unit tests, emulator/device navigation and report-export checks; not yet verified.
- Implementation commit: `cae1062cf2fa45978b6e6ddb98f791209de079b5`.


### Android navigation consistency follow-up (2026-11-18)
- [x] Removed the Overview screen's legacy top tabs.
- [x] Added the persistent bottom bar to Overview, matching APK, Research, Reports and Settings.
- [x] Kept the overview analysis workspace scrollable above the navigation bar.
- [ ] Run Kotlin compile, Android lint, unit tests and emulator/device checks; not yet verified.
- Implementation commit: `3dd7ea20745a6e5f5ac055769cf07da404af61e6`.


### Android + Web CI verification (2026-11-18)
- [x] Android CI passed on commit `3dd7ea20745a6e5f5ac055769cf07da404af61e6` (run 44231486300): Android lint, unit tests, debug APK build, unsigned release bundle build, and artifact uploads all succeeded.
- [x] Web Dashboard CI passed on the same commit (run 44231486293).
- [ ] Physical-device/emulator navigation, screen clipping, accessibility and report-export interaction checks remain outstanding; CI success is not a substitute for runtime UX testing.
- Android workflow: https://github.com/ashishxhub/XhubREA/actions/runs/44231486300
- Web workflow: https://github.com/ashishxhub/XhubREA/actions/runs/44231486293


### Android runtime QA handoff (2026-11-18)
- [x] Confirmed latest Android CI job on commit `1044194f03f7b818b52957ba7bcd06c53095a1e2` completed successfully (run 44231955958); lint, unit tests, debug APK, and unsigned release bundle steps all passed.
- [x] Confirmed debug APK artifact exists: `XhubREA-debug-apk` (artifact ID 14395616884; SHA-256 `b384128986c47714fe20ee7156d0efeaca75dce5295beadcfe6898903cc3c098`).
- [x] Confirmed Web Dashboard CI run 44231956061 passed component tests and production build.
- [ ] Physical-device/emulator runtime QA is BLOCKED pending installation and interaction on an Android device; GitHub Actions runner only built the APK and did not launch the app in an emulator.
- [ ] Manual checks required: cold launch; tap Overview/APK/Research/Reports/Settings in sequence; verify selected-state and screen content; rotate/reopen; verify bottom bar does not cover scroll content; choose a permitted test APK and run analysis; open Research quick prompts; export report; revisit Settings and confirm preferences persist.
- [ ] Record device model, Android version, screenshots/logcat for any issue, then fix and rerun CI. Do not mark runtime QA VERIFIED from build success alone.
- Debug APK workflow run: https://github.com/ashishxhub/XhubREA/actions/runs/44231955958
- Web workflow run: https://github.com/ashishxhub/XhubREA/actions/runs/44231956061


### Web-to-backend integration UX guard (2026-11-18)
- [x] Fixed the Research Composer's unconfigured-backend path: it now returns an explicit assistant message explaining that the question stayed in local preview and was not sent to an AI service.
- [x] Gives clear next steps to configure a deployed HTTPS backend and temporary bearer token in Settings.
- [ ] Live end-to-end request remains unverified; requires a deployed backend, configured CORS origin, server-side API token, enabled Antigravity provider and owner-managed credentials.
- [ ] Android currently remains local report-based research; it is not yet connected to this remote API.
- Implementation commit: `a1cc92e780527bc2918ac491945863b285410f33`.


### Backend Integration Phase 2 — shared contract and client security boundary (2026-11-18)
- [x] Inspected the current FastAPI endpoints, web API client/tests, Android manifest/build setup and existing API tests.
- [x] Added `docs/BACKEND_INTEGRATION.md` documenting the shared route contract, request/response shapes, validation, error codes, CORS, transport and platform responsibilities.
- [x] Recorded that `GET /health` is public and only proves process reachability; it does not prove authenticated or AI-ready status.
- [x] Documented the current shared bearer-token limitation and explicitly prohibited embedding the server token in Android APKs or browser-persisted storage.
- [x] Closed the evidence-structure rate-limit gap; all three protected routes now use the process-local limiter. Gateway limits are still required before public deployment.
- [x] Kept Android remote networking on hold until user-scoped authentication/short-lived scoped sessions are designed; local/offline research remains the current Android behavior.
- [ ] Add gateway-level rate limiting and production user authentication before public exposure.
- [ ] Add Android remote API client only after the authentication contract is implemented.
- [ ] Run targeted Python/Web tests and CI for the new contract commit; no test run has yet been confirmed for this documentation-only update.
- [ ] Deploy HTTPS backend, configure exact CORS origins and server-managed credentials, then run authenticated end-to-end tests.
- Documentation commit: `ebb6b8cf9421976ff3de720dc2fb3627b8cdd050`.


### Backend Integration Phase 2.1 — API rate-limit hardening (2026-11-18)
- [x] Added a thread lock around the in-process rolling-window limiter to avoid concurrent requests racing the shared deque.
- [x] Applied the same limiter to `POST /v1/research/structure`; previously only chat and analyze were rate-limited.
- [x] Added unit coverage for the 10-request cap, exact 60-second expiry boundary, and structure endpoint rate limiting.
- [ ] Python tests/CI have not yet run for these commits; do not mark this work VERIFIED until the workflow passes.
- [ ] The in-process limiter is per worker/process and is not a substitute for shared gateway limits in production.
- [ ] Public production authentication remains blocked on replacing the shared bearer token with user-scoped sessions.
- API implementation commit: `668ade89e5f155b0ab9670f3549030a15cb66e1a`.
- Test commits: `8e692fb8b1fa9fd3d5a7765b2c1f92979447ed49`, `577141972308a0b6d657706521f244fc778fe147` (made the endpoint rate-limit test clock deterministic).


### Security verification — API authentication baseline (2026-11-18)
- [x] Added regression tests for missing/malformed/wrong-scheme/incorrect bearer headers.
- [x] Added a route-registration guard ensuring every `/v1/*` endpoint depends on server-side token authentication.
- [x] Added a guard confirming `/health` remains public and is not an authenticated readiness signal.
- [x] Added `docs/SECURITY_TEST_REPORT.md` with findings, risk ratings, safe test scope and release gates.
- [x] Corrected backend contract documentation: the rolling limiter now covers chat, analyze and evidence-structure endpoints.
- [x] Python security regression suite passed in GitHub Actions run `44234291071`: 42 unit tests passed; optional Antigravity SDK import smoke test also succeeded.
- [ ] **Critical release blocker:** shared bearer token is not end-user identity. User-specific profiles/projects/reports and server-enforced ownership isolation are not implemented.
- [ ] OIDC + PKCE, verified user claims, ownership filters/database row-level controls, and cross-account negative tests remain required before multi-user public release.
- [ ] Gateway-level distributed rate limits, production HTTPS, secret management, audit logging and deployment checks remain outstanding.
- Security regression tests commit: `62161440035bb6ef5fc7f8ca14f7bf66e35ed037`.
- Security report commit: `3eb82bb50522a197e15a80df992fede27f73a871`.
- Backend contract correction commit: `ec6d0b3a34d99e86d78f3f3a79eb707b42028edd`.


### Security regression test result (2026-11-18)
- [x] Python workflow run `44234291071` passed on code commit `4dd5cb2b2efa1ed5c0b24dbe9237774516bce366`.
- [x] 42 Python unit tests passed, including malformed bearer-header rejection, protected-route dependency registration, public health-route boundary and rate-limit checks.
- [x] Optional Antigravity SDK import smoke test passed in the same workflow.
- [x] Corrected FastAPI dependency inspection after the first CI attempt exposed an incorrect test assumption; rerun passed.
- [ ] This is regression-test verification only, not a penetration test.
- [ ] Multi-user isolation remains **NOT IMPLEMENTED**: no verified user identity or per-resource owner checks exist yet. Do not release multi-user accounts until cross-account negative tests pass.
- [ ] Production deployment controls (OIDC/PKCE, HTTPS, distributed gateway limits, secret management, audit logging) remain outstanding.
- Python workflow: https://github.com/ashishxhub/XhubREA/actions/runs/44234291071
- Test-fix commit: `4dd5cb2b2efa1ed5c0b24dbe9237774516bce366`.


### User data isolation — design and test plan (2026-11-18)
- [x] Inspected the current FastAPI routes and confirmed they use one shared bearer token, with no verified end-user identity or owner-scoped persisted resources.
- [x] Added `docs/USER_DATA_ISOLATION.md` defining the trusted identity contract, resource ownership matrix, implementation sequence and two-account negative test suite.
- [x] Explicitly recorded that cross-account isolation is currently not testable because there are no authenticated user principals or persisted user-owned API resources.
- [ ] Select/configure an OIDC provider; then implement backend token verification and server-derived principal.
- [ ] Add owner-scoped persistence and authorization for profiles, projects, files, analysis, research history and reports.
- [ ] Implement and pass User A/User B cross-account integration tests against production-equivalent database and object storage.
- [ ] Multi-user release remains BLOCKED until all ownership checks and negative tests pass.
- Design commit: `3dacd5663dfe23740b192d4827a6628555e9abe8`.


### OIDC authentication architecture — provider decision gate (2026-11-18)
- [x] Added `docs/OIDC_AUTHENTICATION_ARCHITECTURE.md` with provider-neutral OIDC Authorization Code + PKCE architecture for Web and Android.
- [x] Documented backend JWT verification requirements, trusted issuer/audience/JWKS, immutable issuer+subject identity mapping, and rejection of client-supplied user IDs.
- [x] Documented Web BFF/secure-cookie and Android system-browser/PKCE session requirements.
- [x] Recorded provider evaluation criteria and candidates for later review; no provider was selected, configured or registered.
- [x] Explicitly kept this phase documentation-only: no login code, accounts, credentials or public registration were added.
- [ ] Owner must choose an age-eligible provider after checking current terms, cost and feature limits; then configure separate Web and Android public clients.
- [ ] Backend verified-principal dependency, platform sign-in, owner-scoped persistence and two-account negative tests remain NOT IMPLEMENTED.
- [ ] Multi-user/public release remains BLOCKED until the identity and ownership release gates pass.
- Architecture document commit: `4fae7fd3a8f9c6adf81368e0a1940e1145dd0efc`.


### Identity provider assessment — preferred prototype candidate (2026-11-18)
- [x] Reviewed current official Supabase Auth, JWT/JWKS, Python claims, Kotlin claims, React quickstart, pricing and terms references.
- [x] Compared Supabase Auth, Firebase Authentication, Clerk, Auth0 and Keycloak against the current React + Vite, Kotlin Android and FastAPI stack.
- [x] Recorded Supabase Auth as the **preferred technical prototype candidate**, not a final provider selection or live integration.
- [x] Documented current Free plan limits and that pricing/terms must be rechecked before use.
- [x] No cloud project/account, payment method, credentials, API keys or database were created.
- [ ] An eligible account holder / legally authorized adult must review current provider terms and eligibility and explicitly approve any account setup; do not bypass age checks.
- [ ] After approval and secure configuration, implement FastAPI JWT/JWKS verification and negative claim tests before connecting either client.
- [ ] Existing shared bearer token remains private-development-only; no public multi-user release.
- Assessment document commit: `7da259725d84ed9b9f10c653e29a26cb7545591f`.
