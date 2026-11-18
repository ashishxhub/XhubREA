# XhubREA Backend Integration Contract

Status: **v1 contract documented; deployment and cross-platform live integration are not yet verified.**

This document is the shared integration reference for the React web dashboard, native Android app, and Python FastAPI service. The API accepts operator-supplied evidence and prompts; it is not an APK upload or remote device-control API. Use it only for software and data the operator is authorized to inspect.

## Service boundary

- Service: Python FastAPI module `xhubrea_engine.api:app` (see `python/xhubrea_engine/api.py`).
- Production transport: HTTPS only. Plain HTTP is permitted only for local development on `localhost` or `127.0.0.1`.
- Health route: `GET /health`, public and intended for reachability checks. It does not prove that authentication or the AI provider is configured.
- Protected routes: all `/v1/*` routes require `Authorization: Bearer <access-token>`.
- The server reads `XHUBREA_API_TOKEN`. This is a server-side secret and must never be embedded in Android resources, an APK, web source, browser storage, or a public repository.
- Browser CORS is disabled unless `XHUBREA_CORS_ORIGINS` contains an explicit comma-separated origin allowlist. Credentials are not enabled; allowed methods are GET/POST and allowed headers are Authorization/Content-Type.
- The current process-local rate limit (10 requests per rolling minute) applies to `/v1/agent/chat` and `/v1/research/analyze`. `/v1/research/structure` currently has no rate-limit call; enforce gateway limits before public deployment.
- The current token is a single shared bearer secret, not end-user identity or production-grade user authentication. Do not distribute it to public clients.

## Endpoints

### `GET /health`

No authentication.

Success (200):
```json
{ "status": "ok", "service": "xhubrea-research-api" }
```

A successful health response confirms only that the API process answered.

### `POST /v1/agent/chat`

Authentication required. Request:
```json
{ "prompt": "Explain the supplied observation..." }
```
- `prompt`: trimmed non-empty string, maximum 8,000 characters.
- Success (200): `{ "answer": "..." }`.
- Provider calls are opt-in and depend on server-side Antigravity configuration and credentials.

### `POST /v1/research/analyze`

Authentication required. Request:
```json
{
  "project_name": "Sample application",
  "objective": "Summarize these authorized observations",
  "findings": [
    {
      "evidence_id": "E-001",
      "source": "AndroidManifest.xml",
      "observation": "The package requests network access",
      "confidence": "medium"
    }
  ]
}
```
- `project_name`: non-empty, max 120 characters.
- `objective`: non-empty, max 500 characters.
- `findings`: 1–12 items.
- Each finding has non-empty `evidence_id` (max 80), `source` (max 500), `observation` (max 500), and `confidence` (max 40).
- Success (200): `{ "report": "..." }`.
- Confidence is an operator-provided label, not a calculated probability.

### `POST /v1/research/structure`

Authentication required. Request:
```json
{
  "findings": [
    {
      "evidence_id": "E-001",
      "source": "AndroidManifest.xml",
      "observation": "The package requests network access",
      "confidence": "medium"
    }
  ]
}
```
- Same 1–12 finding limit and field constraints as above.
- Confidence must be one of `high`, `medium`, `low`, or `unverified`.
- Evidence IDs must be unique in the request.
- Success (200) returns `schema_version`, `evidence_count`, normalized `evidence`, `source_correlations`, an empty `hypotheses` list, and `limitations`.
- Source grouping means only that observations share a source; it does not prove causation or independently verify application behavior.

## Error contract

FastAPI validation errors may return 422 for malformed request bodies. Application routes may return:
- 400: invalid research input or prompt.
- 401: missing/invalid bearer token.
- 429: rate limit reached (currently chat and analyze only).
- 502: AI provider request failed.
- 503: server authentication or AI provider is not configured/disabled.

Error responses use FastAPI's `detail` field where applicable. Clients should display safe messages and must not expose stack traces, provider internals, or secrets.

## Platform responsibilities

### Web (React)

- Use `web-dashboard/src/api.js` as the shared web HTTP client.
- Keep the user-entered token in transient page memory only; do not persist it in localStorage, sessionStorage, URLs, analytics, or logs.
- The health check is unauthenticated; a healthy response must not be described as authenticated or AI-ready.
- Remote base URLs must use HTTPS. Local HTTP is limited to localhost/127.0.0.1.
- Never imply a local-preview message was sent to the AI service.

### Android

- Android currently provides local report-based Research Assistant behavior; it is not connected to these remote endpoints.
- Do not add `XHUBREA_API_TOKEN` or another privileged shared secret to the APK.
- Before remote Android integration, introduce a real user sign-in/session design or a backend-issued short-lived, scoped user token, with server-side authorization and revocation. The current shared token is not a safe public mobile-client credential.
- Add network permission and a typed, timeout-aware client only as part of that approved authentication implementation. Keep the existing offline/local analysis functional.

### Backend/deployment owner

- Store `XHUBREA_API_TOKEN` and Antigravity credentials in the deployment secret manager/environment, never in source control.
- Set the exact web origin(s) in `XHUBREA_CORS_ORIGINS`.
- Terminate TLS at a trusted HTTPS ingress; add gateway-level rate limiting, request logging with credential redaction, monitoring, and abuse controls.
- Replace the shared-token model with user-scoped auth before exposing the service to public users.
- Do not accept arbitrary APKs or trigger analysis of third-party apps without the operator's authorization.

## Current integration readiness

| Area | State |
|---|---|
| FastAPI route implementation | Implemented |
| Web health/chat client | Implemented; live deployment not verified |
| Web evidence-structure client | Implemented; not yet connected in the UI |
| Android remote API client | Not implemented; intentionally held until safe user auth exists |
| Public production authentication | Not ready; current shared bearer token is an interim owner-controlled mechanism |
| Deployed HTTPS/CORS/provider end-to-end test | Not verified |

This contract describes the current code and explicitly records the gaps; it is not a claim that a production backend is deployed.
