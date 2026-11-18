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


## Provider assessment — 2026-11-18

### Technical recommendation (not yet an account/provider selection)

**Recommended candidate for the first implementation: Supabase Auth**, subject to the project owner's approval and review of account eligibility/terms.

Why it is the strongest initial fit for the current XhubREA repository:
- The project already has a Python FastAPI backend and React + Vite web dashboard; Supabase issues signed JWT access tokens that the API can verify using the project's JWKS endpoint.
- Official Supabase documentation describes JWT claims including `iss`, `exp` and `sub`, asymmetric signing keys/JWKS, and a Python `get_claims` verification path.
- Supabase publishes a Kotlin client reference including verified JWT claims, which is relevant to the native Android client.
- It combines Auth with Postgres and Row Level Security, which may simplify the later owner-scoped persistence phase. RLS still must be designed and tested; enabling Supabase Auth alone does not isolate XhubREA's existing API resources.
- The published Free plan currently lists $0/month and 50,000 monthly active users, but includes resource limits and pauses free projects after one week of inactivity. Treat plan details as changeable and re-check before use.

### Shortlist comparison

| Candidate | Fit for XhubREA now | Main trade-off |
|---|---|---|
| Supabase Auth | **Best initial technical fit**: JWT/JWKS, Postgres/RLS, React and Kotlin/Python references | Adds a hosted database/auth dependency; RLS and backend policy still require careful implementation |
| Firebase Authentication | Strong Android/Google ecosystem option; can also support Web | XhubREA would still need a separate ownership/persistence design for the FastAPI API |
| Clerk | Attractive hosted sign-in and user-management experience | Another separate identity service; verify native Android integration and cost/limits for the exact plan |
| Auth0 | Established standalone identity platform and enterprise connections | Likely more capability than this early project currently needs; verify plan limits and implementation complexity |
| Keycloak | Standards-based self-hosted option | XhubREA would own hosting, patching, backups, monitoring and incident response |

### Safe implementation decision

- Technical recommendation: **Supabase Auth is the preferred candidate to prototype**, not a final provider selection.
- No Supabase project, account, payment method, credentials, API keys or database has been created or configured.
- The current shared `XHUBREA_API_TOKEN` remains in place for private development only; do not distribute it to end users.
- Before any account creation or acceptance of cloud-service terms, have a parent/guardian or other legally authorized adult review the provider's current terms and eligibility requirements and handle any required account/contract steps. Do not enter false age details or bypass an eligibility check.
- Do not add billing or paid-plan requirements. Start with a no-cost prototype only if the account holder is eligible and the current plan terms permit the intended use.
- After an eligible account holder approves the provider and supplies project configuration through a secure channel, implement backend JWT verification first. Never commit service-role keys or signing secrets; a publishable key is not a substitute for server-side authorization.

### Official references checked

- Supabase Auth overview: https://supabase.com/docs/guides/auth
- Supabase JWT and JWKS verification: https://supabase.com/docs/guides/auth/jwts
- Supabase Python verified claims: https://supabase.com/docs/reference/python/auth-getclaims
- Supabase Kotlin verified claims: https://supabase.com/docs/reference/kotlin/auth-getclaims
- Supabase React + Vite quickstart: https://supabase.com/docs/guides/auth/quickstarts/react
- Supabase pricing: https://supabase.com/pricing
- Supabase Terms of Service: https://supabase.com/terms
