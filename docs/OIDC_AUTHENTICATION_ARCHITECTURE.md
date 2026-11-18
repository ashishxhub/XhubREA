# XhubREA — OIDC Authentication Architecture

Date: 2026-11-18  
Status: **PROVIDER-NEUTRAL ARCHITECTURE — no identity provider selected, configured or integrated.**

## Goal

Replace the interim shared `XHUBREA_API_TOKEN` with real end-user authentication for Web and Android. The API must derive identity from a cryptographically verified access token and must never treat a client-supplied account ID as proof of ownership.

This document is an architecture decision preparation, not a working login implementation. No accounts have been created and no provider credentials are present.

## Recommended protocol

Use OpenID Connect (OIDC) Authorization Code flow with PKCE (S256) for public clients.

- Web and Android are separate public clients with separate client IDs where the provider requires them.
- Use the system browser for sign-in. Android should use the provider-supported AppAuth/custom-tab pattern and registered redirect URI; do not embed a client secret in the APK.
- Web should prefer a backend-for-frontend (BFF) session using Secure, HttpOnly, SameSite cookies. Avoid storing long-lived access or refresh tokens in localStorage.
- The API accepts short-lived access tokens intended for this API, not ID tokens as API bearer credentials.
- The backend validates JWT signature using trusted issuer metadata/JWKS, exact issuer, API audience, expiry/not-before, allowed signing algorithm and required subject. Handle key rotation safely and fail closed.
- Map the verified issuer + subject pair to an immutable internal user identifier. Do not use email as the primary key; email can change and may be unverified.
- Never accept `user_id`, `owner_user_id`, role or tenant identity from request JSON, query parameters or headers as an authorization source.
- Apply the verified principal to every resource query and mutation. OIDC alone does not provide resource ownership isolation.

## Provider decision criteria

Evaluate providers against the same requirements before selecting one:

| Criterion | What must be confirmed |
|---|---|
| OIDC standards | Authorization Code + PKCE S256, discovery metadata, JWKS, audience configuration |
| Android | Supported native-app flow, browser redirect handling, maintained Android SDK or AppAuth compatibility |
| Web | Secure server-side session/BFF pattern, cookie configuration and CSRF guidance |
| Backend | Python/FastAPI JWT verification support, issuer/audience controls and key rotation |
| Account lifecycle | Sign-out, refresh-token rotation, revocation/session termination and account deletion/export |
| Operations | Free-tier limits, pricing at expected usage, availability, logs, support and data-region options |
| Age and terms | Minimum-age requirements and guardian/consent rules applicable to the account owner and intended users |
| Portability | Standards-based subject mapping and a documented migration/export path |

Potential candidates to evaluate (not endorsements or selections): Auth0, Clerk, Firebase Authentication, Supabase Auth and self-hosted Keycloak. Verify current plan limits, pricing, product capabilities and age/terms directly before making a decision. Do not assume a free tier or eligibility.

## Provider-neutral backend contract

After a provider is selected:

1. Configure trusted issuer URL, API audience, accepted algorithms and required claims in deployment configuration; never hardcode secrets.
2. Fetch discovery/JWKS only from the configured issuer. Cache keys with expiry and support key rotation; reject unknown issuers and invalid signatures.
3. Validate token time claims with a small documented clock-skew allowance.
4. Require a non-empty `sub`; derive the internal principal from the verified `iss` + `sub` mapping.
5. Expose a typed request-principal dependency to protected routes. Keep health reachability separate from authenticated readiness.
6. Return a generic 401 for invalid/expired/wrong-audience credentials. Do not leak token contents or detailed verification failures.
7. Add tests for wrong issuer, wrong audience, invalid signature, expired token, missing subject, unsupported algorithm, rotated signing key and revoked/disabled account behavior where provider introspection or revocation supports it.
8. During migration, do not silently treat the old shared token as a user identity. Keep the API private until a controlled cutover is complete.

## Platform session requirements

### Web

- Prefer a BFF that completes OIDC code exchange server-side and issues an opaque application session cookie.
- Set `Secure`, `HttpOnly`, and an appropriate `SameSite` value; add CSRF protection for state-changing cookie-authenticated requests.
- Keep OAuth state, nonce and PKCE verifier transaction-bound and short-lived.
- Do not place provider client secrets or long-lived tokens in browser bundles, localStorage, URLs or logs.

### Android

- Use the system browser / Custom Tabs with Authorization Code + PKCE S256 and a registered app redirect URI.
- Validate state and use the provider-supported redirect/AppAuth integration.
- Store refresh/session material only using an approved secure-storage design backed by Android Keystore; keep access tokens short-lived and memory-scoped where practical.
- Never ship a client secret, the server's shared API token or Antigravity credentials in the APK.
- Preserve local/offline APK analysis when signed out or the service is unavailable.

## Security boundary and rollout

OIDC authentication is only the identity layer. It does not complete the user-data isolation design. Before multi-user launch, also implement owner-scoped persistence, authorization on every read/write/export/download, storage/job ownership, audit events, per-user quotas and two-account negative integration tests as specified in `docs/USER_DATA_ISOLATION.md`.

Rollout sequence:
1. Owner chooses an eligible provider after reviewing terms, age requirements, cost and operational fit.
2. Register separate Web and Android public clients and configure redirect URIs and API audience.
3. Implement and test backend token verification with a verified-principal dependency.
4. Add Web BFF and Android PKCE sign-in/session flows.
5. Add owner-scoped persistence and cross-account integration tests.
6. Run security review and production-equivalent deployment checks.
7. Only then enable public registration and remote user data.

## Decision gate

**No provider selected. No login code changed. No credentials requested or stored.** Provider choice and any account registration require explicit owner approval after checking current provider terms, age eligibility and pricing. Do not bypass provider age restrictions. Until identity verification and ownership controls are implemented, keep multi-user access and public account registration disabled.
