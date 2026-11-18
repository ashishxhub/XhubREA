# XhubREA Security Test Report

Date: 2026-11-18  
Scope: repository-level review of the current FastAPI research API and regression-test coverage. This is a controlled code review, not an external penetration test.

## Executive result

**Production security verdict: NOT READY.** The current API has useful baseline protections, but it does not yet implement end-user identity or per-user data authorization. The requirement that each user can see only their own details is therefore **not implemented and cannot yet pass an isolation test**.

## Checks added in this pass

- Reject missing, malformed, wrong-scheme, blank and incorrect bearer authorization headers.
- Assert every `/v1/*` route is registered with the server-token dependency.
- Assert `/health` remains public and is not mistaken for an authenticated route.
- Retain existing checks for missing server configuration, valid/invalid token, prompt validation and rolling rate-limit behavior.

The new regression tests passed in GitHub Actions run `44234291071`: 42 Python unit tests passed, and the optional Antigravity SDK import smoke test also succeeded. This verifies the tested code paths only; it is not a penetration test or proof of user-data isolation.

## Findings and risk

| Area | Current observation | Result |
|---|---|---|
| Protected route gate | Versioned API routes use a server-configured bearer token dependency. | Baseline control present; regression tests added |
| End-user identity | One shared `XHUBREA_API_TOKEN` authenticates every caller. No verified user subject is attached to a request. | **Fail / not implemented** |
| Per-user data isolation | Current API accepts prompts and supplied evidence; there is no user-owned profile/project/report persistence or owner filter to validate. | **Not testable yet / release blocker** |
| Token lifecycle | Shared token has no per-user expiry, refresh, revocation or scoped permissions. | **Fail for public user accounts** |
| Rate limiting | In-process rolling limit is 10 requests/minute and is shared only within one process; not distributed across workers. | Partial; gateway limit required |
| CORS | Explicit origin allowlist is supported. CORS is not authentication or authorization. | Partial |
| Health route | `/health` is intentionally public and reveals service reachability only. | Expected behavior |
| Transport/secrets | Deployment must enforce HTTPS and keep server secrets in server-side secret storage. Deployment has not been verified. | Not verified |
| Audit/monitoring | Production audit events, alerting and secret redaction are not demonstrated by this code review. | Not verified |

## Required next controls before multi-user release

1. Add OIDC sign-in with Authorization Code + PKCE for Android and Web.
2. Verify issuer, audience, signature, expiry and token claims on the backend.
3. Derive the user identity only from the verified server-side token; never trust a client-supplied `user_id`.
4. Add ownership checks to every user-owned resource query and mutation. Prefer database row-level security where supported, with backend authorization as a second layer.
5. Add negative cross-account tests: User A must receive no User B profile, project, uploaded file, analysis, report or history, including when guessing/changing resource IDs.
6. Add per-user quotas/rate limits, session revocation, audit logging with credential redaction, gateway limits and production HTTPS verification.
7. Keep admin/support access separate, least-privileged and audited.

## Safe test boundary

Only the project's own code and test fixtures are in scope. No third-party service, real user account, production endpoint, destructive test, exploit execution or load test was targeted.

## Release gate

Do not expose multi-user functionality publicly until OIDC user identity, server-enforced ownership checks, cross-account negative tests, deployment controls and CI verification are complete.
