# XhubREA Master Integration Blueprint v1.0

**Date:** 2026-11-18  
**Status:** Proposed target architecture; not a production-readiness claim.  
**Product:** Android + Web + shared backend for authorized, evidence-first software/APK research.

## 1. Product objective and boundaries

XhubREA helps users inspect, understand, compare and document software they own, open-source projects, or files they are explicitly authorized to analyze. The platform uses bounded, read-only analysis and AI summaries grounded in supplied evidence.

It must not bypass DRM, authentication, licensing, payment checks, anti-cheat or other access controls; extract secrets/private data; execute unknown APKs; or reproduce proprietary source code. Uploaded files are untrusted and must never be executed by the normal analysis pipeline.

## 2. Existing baseline

Based on the current repository tracker and source documentation:

| Area | Recorded state | Remaining |
|---|---|---|
| Android | Kotlin + Jetpack Compose APK research MVP, analysis modules and V2 navigation | Verify latest branch build and real-device behavior |
| Web | React + Vite dashboard and API client | Several workspace pages remain placeholders; E2E pending |
| Backend | Python + FastAPI routes and analysis modules | Production deployment and user-scoped API contract pending |
| Antigravity | Optional Python adapter; SDK import smoke test passed in CI | Live provider authentication pending; not an Android runtime dependency |
| Authentication | OIDC architecture and provider assessment docs | No provider project/client configured; no real end-user identity |
| Database | Production persistence not connected | User-owned projects/reports and ownership enforcement pending |
| Storage | No production object storage connected | Private upload and retention pipeline pending |
| Security | Shared bearer-token regression tests and documentation | Shared token is private-development-only, not user identity |
| CI | Android, Python and Web workflows exist | Verify workflows for the exact latest commit |
| Deployment | No production backend deployment recorded | HTTPS, secrets, gateway, monitoring, backups and restore remain |

The public health endpoint indicates process reachability only. It does not prove database, authentication, AI-provider or production readiness.

## 3. Target architecture

    Android app                 Web app
         |                         |
         +------ HTTPS/API --------+
                    |
            Edge / WAF / Gateway
                    |
           OIDC + PKCE identity
                    |
             FastAPI application
                    |
       Verified principal + ownership
                    |
       +------------+-------------+
       |            |             |
    Projects     Research     Agent Manager
    & Reports      API        (policy-bound)
       |            |             |
       +-------- Domain services--+
                    |
             Job queue / workers
                    |
       Read-only, isolated analysis
                    |
    PostgreSQL | Private object storage | Redis
                    |
       Logs / metrics / traces / alerts

Start with a modular monolith: one well-structured FastAPI application and separately scaled workers. Do not begin with microservices; split services only when scale or isolation requirements justify it.

## 4. Integration register

These are proposed choices, not live connections, purchases or final provider decisions. Recheck current pricing, quotas, region, terms, data handling and account eligibility before adoption.

| Capability | Preferred starting point | Alternatives | Priority / status |
|---|---|---|---|
| Android | Kotlin + Jetpack Compose | — | P0, existing |
| Web | React + Vite | Next.js if SSR becomes necessary | P0, existing |
| API | Python + FastAPI | Django REST | P0, existing |
| Identity | Supabase Auth prototype candidate | Firebase Auth, Clerk, Auth0, Keycloak | P0, not configured |
| Primary database | PostgreSQL | Managed PostgreSQL | P0, not connected |
| ORM/migrations | SQLAlchemy 2 + Alembic | SQLModel | P0, proposed |
| Authorization | Server-derived principal + owner checks + PostgreSQL RLS | Equivalent policy layer | P0, not implemented |
| File storage | Private S3-compatible bucket or Supabase Storage | Cloud object storage | P0, not connected |
| Cache/rate limits | Redis | Managed Redis-compatible service | P1 |
| Background jobs | Celery + Redis | RQ, Dramatiq | P1 |
| AI | Provider adapter; Gemini/Antigravity optional | Other providers behind same interface | P1, partial |
| Agent orchestration | XhubREA task state machine and policy layer | Framework only if needed | P1, proposed |
| Search | PostgreSQL full-text search initially | OpenSearch/vector DB later | P2 |
| Monitoring | OpenTelemetry + Sentry | Hosting-native monitoring | P1 |
| CI/CD | GitHub Actions | — | P0, existing |
| Containers | Docker | — | P1 |
| Edge protection | Hosting WAF/API gateway | Cloud-native equivalent | P1 |
| Email | Auth provider email initially | Transactional email provider | P2 |
| Payments | Defer | Compliant payment provider later | P3 |
| Product analytics | Privacy-conscious events | Self-hosted analytics | P3 |

P0 = required before multi-user production. P1 = needed before reliable public beta or when that workload is enabled. P2 = later enhancement. P3 = defer until validated product need.

## 5. API and identity contract

Use versioned routes under /v1 and generate OpenAPI from FastAPI so Android and Web share the same contract.

Identity requirements:
- OIDC Authorization Code + PKCE for both Web and Android public clients.
- Backend verifies JWT signature, trusted issuer, audience, expiry, allowed algorithm and required subject using trusted JWKS.
- Internal identity maps immutable issuer + subject. Never trust client-supplied user_id.
- Web should use Secure, HttpOnly, SameSite cookie sessions/BFF where feasible.
- Android uses system browser + PKCE and secure platform token storage.
- Short-lived access tokens and safe refresh/revocation behavior.
- Provider credentials remain server-side.

Proposed routes:

| Route | Purpose | Access |
|---|---|---|
| GET /health | Minimal liveness | Public |
| GET /ready | Database/queue readiness | Internal/operations |
| GET /v1/me | Verified current profile | Authenticated |
| GET/POST /v1/projects | List/create own projects | Owner-scoped |
| GET/PATCH/DELETE /v1/projects/{id} | Manage one project | Owner-scoped |
| POST /v1/files/presign | Request constrained private upload | Auth + quota |
| POST /v1/analyses | Start analysis job | Owner-scoped |
| GET /v1/analyses/{id} | Job status/result | Owner-scoped |
| POST /v1/research/analyze | Evidence-grounded research | Auth + quota |
| POST /v1/research/structure | Structure supplied findings | Auth + quota |
| POST /v1/agent/tasks | Create approved agent task | Auth + capability policy |
| GET /v1/agent/tasks/{id} | Read task state/output | Owner-scoped |
| GET /v1/reports/{id} | Retrieve private report | Owner-scoped |

Existing shared-token protected endpoints are suitable only for private development. Replace shared bearer auth with verified user sessions before public multi-user use. Never embed a server secret in Android, browser storage, frontend bundles, Figma files or Git.

## 6. Data model

PostgreSQL is the proposed primary database. Each user-owned row must have a non-null owner_user_id or inherit ownership through a parent resource whose ownership is verified in the same transaction.

| Table | Purpose |
|---|---|
| users | Internal UUID and identity issuer/subject mapping |
| user_profiles | Minimal editable profile data |
| projects | Owner, name, description, visibility, timestamps |
| files | Owner, project, private object key, size, hash, type, scan state, retention |
| analysis_jobs | Owner, file/project, job type, state, progress, limits |
| analysis_findings | Job, finding type, evidence locator, confidence, severity |
| research_sessions | Owner, project, title and timestamps |
| research_messages | Session, role, bounded content and model metadata |
| agent_tasks | Owner, task type, status, policy snapshot, cancellation/audit link |
| reports | Owner, project/job, format, private object key |
| api_usage | Owner, endpoint/model and quota usage |
| audit_events | Actor, action, resource, outcome and timestamp |
| consents | Purpose, policy version and timestamp |

Use foreign keys, migrations, indexes, parameterized queries and least-privilege DB roles. Apply PostgreSQL RLS as defense in depth plus explicit server-side authorization. Frontend filtering is never a privacy boundary. Keep file bytes in private object storage, not database rows. Define retention, deletion and restore policies before real user uploads. Do not log secrets or raw auth tokens.

## 7. Secure file and APK analysis pipeline

1. Authenticated user requests upload authorization for a project they own.
2. Backend checks quotas, size and allowed types; issues a short-lived upload URL to a private quarantine bucket.
3. Worker validates object key, actual file signature, archive structure and size/decompression limits.
4. Calculate SHA-256; reject malformed, encrypted or over-limit files according to policy.
5. Run bounded static parsing in a disposable, resource-limited, network-restricted worker.
6. Extract supported metadata: manifest, permissions, components, DEX header/table metadata, resources, native libraries, signer metadata and dependencies.
7. Store structured findings with evidence locators, parser version and confidence. Unsupported states must remain unknown, not guessed.
8. AI may summarize supplied findings but must not invent evidence or present heuristics as proof.
9. Generate private reports; authorize every download against the owner.
10. Apply retention/deletion rules and audit events.

Do not install or execute submitted APKs in this pipeline. Do not contact analyzed apps or their services. Any future dynamic-analysis lab requires a separate threat model and isolated design.

## 8. AI and Agent Manager

Use a common model adapter interface for generation, summarization and evidence structuring. Each provider adapter declares capabilities, timeout, retries, limits, data handling and cost metadata. Provider keys stay server-side. Static analysis must work without AI.

Proposed specialist agents:
1. Orchestrator — decomposes requests into approved tasks and tracks state.
2. Research — summarizes supplied evidence and references source locations.
3. APK Analysis — invokes approved static parsers; never executes files.
4. Code/Architecture — explains supplied source trees and dependency metadata.
5. Security Review — defensive configuration and dependency findings.
6. Testing — runs project-owned tests in controlled CI/sandbox.
7. Report — generates JSON/HTML/PDF from validated findings.
8. Documentation — drafts documentation from verified project state.

Agent governance:
- Deny-by-default tool permissions and explicit per-task allowlists.
- No arbitrary shell, arbitrary network, credential access or cross-user access.
- Read-only repository mode by default; code writes require explicit scoped approval and review.
- Human approval for deployments, purchases, destructive changes, publication and other external side effects.
- Per-task time, token, file-size and cost budgets; cancellation and idempotency.
- Audit input references, tool calls, outputs, model version and policy decisions with secret redaction.
- Treat files, repository text and model output as untrusted; defend against prompt injection.
- Require evidence references and confidence labels; mark unsupported claims unknown.

## 9. Security, privacy and release gates

Mandatory:
- OIDC + PKCE and backend JWT/JWKS verification.
- Owner authorization on every read, write, download and background job.
- Two-account negative tests: User A cannot list, read, update, delete or download User B resources.
- PostgreSQL RLS and object-storage policy tests.
- HTTPS, secure headers, restrictive CORS, CSRF controls for cookie sessions.
- Distributed gateway rate limits, request/body limits, upload quotas and abuse controls.
- Secrets manager, rotation/revocation and no secrets in source/client apps.
- Dependency/container scanning and release SBOM.
- Audit logs, retention controls, backup/restore drills and incident response.
- Privacy notice, terms, deletion/retention policy and third-party processing disclosure.
- Accessibility, performance, crash and supported-device testing.

The public multi-user release is BLOCKED until identity, ownership isolation, cross-account tests, private storage access, gateway controls, HTTPS, backups, monitoring and privacy/security review pass. The existing in-process rate limiter is not enough for multiple production workers.

## 10. Android/Web parity

| Capability | Android | Web | Shared backend |
|---|---|---|---|
| Sign-in | System browser + PKCE | OIDC secure session | Verify identity |
| Projects | Own projects | Own projects | Owner-scoped API |
| File intake | System document picker | Browser upload | Quarantine/validation |
| Static analysis | Existing local modules | Preview/status | Isolated worker |
| Research | Local report Q&A initially | Existing API client/preview | Evidence-grounded API |
| Agent tasks | Status and approved actions | Workspace controls | Orchestrator |
| Reports | View/export own | View/export own | Private report storage |
| Settings | Local/account | Account/workspace | Profile/preferences API |

Never ship the shared server token in either client. Android remote API integration starts only after user-scoped authentication and session handling exist.

## 11. Testing strategy

- Unit: parsers, validation, authorization policy, quotas and model adapters.
- Integration: FastAPI with disposable PostgreSQL, test object storage and queue.
- Security: invalid/expired/wrong-issuer/wrong-audience JWT, key rotation, IDOR, cross-account, upload abuse, CORS/CSRF and concurrent rate limits.
- Contract: OpenAPI and Android/Web compatibility.
- E2E: sign-in, project creation, upload, analysis, evidence review, report download and deletion.
- Android: supported API levels, rotation, process death, offline, accessibility and real-device tests.
- Web: browser compatibility, keyboard/screen-reader, responsive layouts and session expiry.
- Operations: load, queue saturation, worker crash recovery, backup restore and alert tests.

A green CI run verifies only its exact commit and jobs; it is not device QA, a penetration test or production certification.

## 12. Phased implementation roadmap

| Phase | Deliverables | Exit criteria |
|---|---|---|
| 0 Baseline | Reconcile branch, tracker, source and workflows | Exact current state recorded |
| 1 API foundation | Typed schemas, errors, OpenAPI, config validation | Unit + contract tests pass |
| 2 Identity | Approved provider, PKCE clients, JWT/JWKS verification | Real provider and negative tests pass |
| 3 Ownership | PostgreSQL schema, migrations, owner policies, RLS | Two-account negative tests pass |
| 4 Storage/jobs | Private upload, quarantine, queue, bounded worker | Abuse and worker-limit tests pass |
| 5 Analysis | Normalized findings and evidence references | Authorized fixture tests pass |
| 6 AI | Provider adapter, budgets, evidence-grounded output | Provider + offline fallback tests pass |
| 7 Agents | Orchestrator, tool allowlists, audit, cancellation | Permission/budget tests pass |
| 8 Client integration | Android/Web sign-in, projects, jobs, reports | E2E parity passes |
| 9 Operations | Deploy, secrets, gateway, monitoring, backups | Restore drill passes |
| 10 Release | Privacy/security, accessibility and device/browser QA | All release gates signed off |

## 13. Cost and provider policy

Prefer local/free development tiers for prototypes but never assume free tiers suit production. Recheck current price, quotas, pause/inactivity behavior, region, data retention, export and eligibility before choosing a service. Do not add payments, vector databases, multiple AI providers or microservices without a validated need. Set global and per-user quotas before enabling paid AI or large file processing. An eligible account holder/legally authorized adult must review provider terms and handle account or billing steps where required; do not bypass age checks. Never paste credentials, payment data or private keys into chat or GitHub.

## 14. Open decisions

1. Identity: Supabase Auth remains a technical prototype candidate only; no final selection or live project exists.
2. Hosting/region: decide based on data residency, cost and operations.
3. Object storage: choose with hosting and define private bucket policy.
4. AI provider: retain adapter design; review data handling and cost before sending files.
5. Queue/Redis: select deployment topology before multi-worker processing.
6. Analytics/payments: defer until there is a clear product need and privacy review.
7. Retention: define exact defaults and deletion behavior before accepting real user uploads.

## 15. Definition of production-ready

XhubREA may be called production-ready only when all P0 requirements are implemented and verified; no critical/high unresolved security findings remain; User A/User B isolation passes across APIs, DB, jobs, reports and storage; Android/Web use verified sessions with no shared client secret; upload workers enforce isolation/resource limits; AI is evidence-grounded and budgeted; CI, device/browser E2E, monitoring, alerts, backups and restore drills pass; privacy/terms/retention/third-party processing are reviewed; and release signing, rollback and incident response are tested.

**Current verdict: NOT PRODUCTION READY.** This document defines the target architecture and implementation sequence. It does not claim proposed services are connected or release gates have passed.
