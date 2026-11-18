# XhubREA Repository Audit — Phase 0

**Audit date:** 2026-11-18  
**Audited ref:** `feat/xhubrea-v2-frontend-design`  
**Audited HEAD:** `c89251c4394b3474736ffebc74bfb6401c4532ba`  
**Scope:** GitHub tree, Android/Gradle configuration, FastAPI backend, Antigravity adapter, Web dashboard, docs, CI workflow results and PR state.  
**Method:** Read-only source/configuration inspection and GitHub Actions metadata review. No production environment, device, database or cloud account was accessed.

## Executive summary

**Verdict: XhubREA is an early working Android/static-analysis MVP with a V2 Web UI preview and an optional Python AI API scaffold. It is not yet a connected multi-user product or production-ready service.**

The repository contains real APK metadata/static archive analysis modules and test suites. It does not yet contain a production identity provider integration, database schema/migrations, user-owned persistence, private object-storage pipeline, distributed job queue, production deployment configuration or multi-user authorization.

### Repository snapshot

- Public GitHub repository: `ashishxhub/XhubREA`.
- Default branch: `main`.
- Active development branch: `feat/xhubrea-v2-frontend-design`.
- Audited branch HEAD: `c89251c4394b3474736ffebc74bfb6401c4532ba`.
- PR #20 is open and unmerged; its target is `main`.
- Audited tree contains 74 entries, including Android app, Python package/tests, Web dashboard, three CI workflows and architecture/security docs.
- No Gradle wrapper files, npm lockfile, backend container/deployment manifest, database migration directory or production infrastructure-as-code were present in the audited tree.

## 1. Implemented and present in repository

### Android
- Kotlin + Jetpack Compose app.
- APK document picker and bounded local copy into app cache.
- Read-only package metadata and SHA-256 reporting.
- Permissions and component/exported-component inventory.
- DEX header/table metadata inspection; not full instruction-level decompilation.
- APK resource/file inventory and bounded resource-table metadata decoder.
- Binary AndroidManifest decoder.
- Native library inventory, framework/game heuristics and signer certificate metadata.
- Two-APK metadata comparison and Research Lab.
- Python archive analysis through Chaquopy 3.13.
- Workspace destinations for Overview, APK, Research, Reports and Settings.
- Research Assistant answers from local report state only; no cloud LLM connection in Android.
- Report export flow.
- Android manifest disables cleartext traffic and does not request broad storage access.

**Android gaps:** No INTERNET permission or remote API client; no OIDC login; no Room persistence; no user account/project sync; no production signing setup. The Android UI and logic are concentrated in a large `MainActivity.kt` (about 2,930 lines), so a future refactor into feature modules/view models/repositories is advisable, but should preserve working analysis functions.

### Python backend and analysis
- FastAPI app with `GET /health`.
- Protected routes: `POST /v1/agent/chat`, `POST /v1/research/analyze`, `POST /v1/research/structure`.
- Pydantic request validation and sanitized provider errors.
- Optional explicit CORS origin allowlist.
- Shared `XHUBREA_API_TOKEN` authentication for protected routes.
- Process-local rolling rate limiter (10 requests/minute) with thread lock.
- Evidence prompt builder and deterministic evidence structuring.
- Optional Antigravity adapter, disabled unless explicitly enabled and server-side `GEMINI_API_KEY` exists.
- Separate optional Antigravity dependency manifest; SDK kept out of Android/Chaquopy runtime.

**Backend gaps:** Shared bearer token is not user identity. No JWT/JWKS verification, OIDC principal, per-user authorization, database, migrations, user-owned project/report endpoints, job queue, private storage, distributed limits, production health/readiness probes or deployed HTTPS service.

### Web
- React + Vite dashboard with sidebar/workspace navigation, composer, settings and local preview.
- Optional backend URL/token connection settings.
- HTTPS enforcement for remote backend URLs; local HTTP allowed only for localhost/127.0.0.1.
- Health check and research chat client.
- Token held in transient component state rather than localStorage/sessionStorage.
- Explicit local-preview message when backend is not configured.

**Web gaps:** Workspace pages are still placeholders; no sign-in/OIDC, user projects, upload/analysis job workflow, persistent history, report library or real account-scoped API. Evidence-structure API client exists but is not wired into the visible UI. Backend live E2E is not verified.

## 2. External service connection status

| Service/system | Actual status in audited source |
|---|---|
| GitHub | Connected; source repository, PR and Actions workflows |
| Figma | Design artifact exists; not a runtime application dependency |
| Google Antigravity SDK | Optional Python adapter/dependency; SDK import smoke test passes in CI; live Gemini agent auth/response not verified |
| Gemini API | No live credential or authenticated request verified |
| Supabase Auth | Architecture/provider assessment only; no project/client configured |
| PostgreSQL | Not connected; no schema/migrations |
| Redis | Not present/configured |
| Object storage/S3 | Not connected |
| Celery/RQ/Dramatiq | Not present/configured |
| Sentry/OpenTelemetry | Not present/configured |
| Hosting/backend deployment | No production deployment verified in this audit |
| Payment/email providers | Not connected; not required for core MVP |

## 3. CI verification at audited HEAD

For exact HEAD `c89251c4394b3474736ffebc74bfb6401c4532ba`:

| Workflow | Run | Result | What it verifies |
|---|---:|---|---|
| Python Analysis Engine Tests | 44236170908 | SUCCESS | Python compilation, unit tests, optional Antigravity SDK install/import smoke test |
| Android Debug APK | 44236170870 | SUCCESS | Android lint, unit tests, debug APK and unsigned release bundle packaging |
| Web Dashboard CI | No run found for this docs-only HEAD | NOT RUN for this SHA | Latest known prior Web CI success was for an earlier commit; do not attribute it to this HEAD |

CI success does not prove Android runtime behavior on a physical device, live Gemini auth, backend deployment, user isolation or production security. The Android AAB produced by CI is unsigned and not Play Store release-ready.

## 4. Security findings and release blockers

| Finding | Severity | Evidence / action |
|---|---|---|
| Shared bearer token used for all API users | Critical for public multi-user release | Replace with verified OIDC/JWT principal; never ship token in clients |
| No owner-scoped user data model | Critical | Add owner IDs, server-side policy checks, RLS and two-account negative tests |
| No real user auth on Android/Web | Critical | Implement PKCE clients after eligible account-holder approves provider setup |
| No private upload/storage pipeline | High | Add quarantine bucket, constrained upload, object ownership and retention |
| Process-local rate limiter only | High before multi-worker/public deployment | Add shared gateway/distributed limits and quotas |
| No deployed HTTPS backend verified | High | Deploy with managed secrets, TLS, restricted CORS, readiness checks |
| No production observability/restore evidence | High | Add logs/metrics/traces/alerts, backups and restore drill |
| No production signing/release process | High for app distribution | Owner-managed signing, secure CI handling, rollback/release checks |
| Untrusted APK parsing | Medium/High operational risk | Preserve static-only parsing; strengthen worker isolation, resource/time limits and fuzz/fixture coverage before accepting remote uploads |
| Web dependencies use `latest`, no lockfile in tree | Medium | Pin reviewed versions and commit reproducible lockfile |
| No Gradle wrapper in tree | Medium | Add wrapper with pinned distribution for reproducible local/CI builds |
| Single large Android activity | Maintainability risk | Refactor incrementally after regression tests; do not rewrite working analysis wholesale |

## 5. Work that should NOT be rebuilt

Preserve and extend these existing assets:
- Android APK picker, local metadata inspection and report flow.
- Kotlin binary manifest decoder and its fixture tests.
- Resource table decoder and its fixture tests.
- Python APK archive engine and safety regression tests.
- Chaquopy bridge, while separately verifying it on a real supported device.
- Evidence-grounded research prompt builder and deterministic evidence structurer.
- Antigravity optional adapter and server-side-only credential boundary.
- Web API client URL validation, timeout handling and local-preview messaging.
- Existing GitHub Actions workflows and frontend design tokens.

Avoid replacing the working Android analysis engine with a new generic upload-only interface. Integrate new modules behind stable contracts.

## 6. Recommended next implementation order

1. **Phase 1 — Reproducible foundation:** Add Gradle wrapper, pin Web dependencies/lockfile, centralize backend settings validation, document supported Python/Node/JDK versions and add config tests.
2. **Phase 2 — Provider setup gate:** An eligible account holder/legally authorized adult reviews current provider terms, eligibility and pricing and approves any account/client setup. Do not bypass age restrictions or share secrets in chat.
3. **Phase 3 — Identity backend:** Implement provider-neutral JWT/JWKS verification with strict issuer/audience/expiry/algorithm checks and local generated-key tests. Live provider validation waits for real trusted issuer configuration.
4. **Phase 4 — Persistence and ownership:** PostgreSQL schema + Alembic migrations, owner-scoped repositories, RLS and two-account integration tests.
5. **Phase 5 — Secure file jobs:** Private quarantine storage, constrained uploads, queue and isolated static analysis workers.
6. **Phase 6 — Android/Web user-scoped integration:** Only after identity and ownership gates pass; keep Android local/offline analysis functional.
7. **Phase 7 — Agent Manager and operations:** Capability allowlists, budgets, cancellation, audit, distributed limits, monitoring, backup/restore and deployment.
8. **Phase 8 — Release:** Device/browser E2E, accessibility, signing, privacy/terms review and security verification.

## 7. Audit conclusion

The codebase has a useful foundation and several verified automated test/build paths. The main architectural gap is not a missing collection of AI APIs: it is the absence of a secure multi-user identity, ownership, persistence and file-processing foundation. Build those controls before adding more third-party integrations or exposing public user accounts.

**Phase 0 status: AUDIT COMPLETE (repository/source/CI metadata scope).**  
**Production status: NOT READY.**  
**Next recommended task: Phase 1 — reproducible build and configuration foundation.**
