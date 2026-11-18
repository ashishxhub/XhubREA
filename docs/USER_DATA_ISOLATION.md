# XhubREA — User Data Isolation Design and Test Plan

Date: 2026-11-18  
Status: DESIGN SPECIFICATION — not an implemented access-control layer.

## Security objective

Every authenticated account may access only resources it owns. This applies consistently to Android, Web, API clients, background jobs, exports and support tooling. Hiding another user's records in the frontend is not an access-control measure.

## Current state (verified by source inspection)

- The FastAPI API currently uses one shared `XHUBREA_API_TOKEN` for all protected `/v1/*` routes.
- Requests do not carry a verified end-user subject.
- Current research endpoints accept evidence in each request and return results; there is no authenticated profile/project/report persistence layer in this API.
- Consequently there is no real two-account dataset or owner-filtered resource endpoint against which cross-account isolation can currently be tested.
- **Do not describe user isolation as implemented or passed.**

## Required identity contract

1. Android and Web authenticate using an OIDC provider with Authorization Code + PKCE.
2. The backend validates signature, issuer, audience, expiry and allowed algorithms against the provider's trusted metadata/JWKS.
3. The backend derives an immutable internal `user_id` from the verified identity subject. A client-supplied `user_id` is never authoritative.
4. Each persisted user-owned row has a non-null `owner_user_id`; ownership is assigned by backend code from the verified principal at creation time.
5. Every read, update, delete, export and relationship traversal is constrained by the verified owner. Unknown and non-owned IDs should return the same non-disclosing 404 response.
6. If the selected database supports row-level security, enable it as defense in depth. Backend authorization remains mandatory.
7. Background jobs and file storage paths carry an immutable owner reference and re-check it before reading or publishing results.
8. Admin/support access uses separate least-privilege roles and auditable, time-bounded authorization. No implicit global access.

## Resource ownership matrix

| Resource | Owner key | Required access rule |
|---|---|---|
| Profile and preferences | `owner_user_id` | User can read/update own record only |
| Projects | `owner_user_id` | All CRUD operations scoped to principal |
| Uploaded APKs/files | `owner_user_id` | Download, preview and delete require ownership |
| Analysis runs | `owner_user_id` | Parent project and run must both belong to caller |
| Research history | `owner_user_id` | List/detail/export scoped to caller |
| Reports and exports | `owner_user_id` | Generated artifact must retain owner; signed links short-lived |
| API usage/quota | verified principal | Counters and rate limits keyed by account, not client input |

## Mandatory negative test suite

Create two independent test principals, User A and User B, and seed one resource of every supported type for each. For every resource endpoint, test:

- A can list/read/update/delete A's own resource as permitted.
- B can list/read/update/delete B's own resource as permitted.
- A cannot read, update, delete, export, download or infer B's resource by changing the resource ID.
- B cannot access A's resource.
- Supplying another account's `user_id` in JSON, query parameters, headers or path does not change the server-derived owner.
- Nested objects cannot be reached through a parent ID belonging to another user.
- Bulk operations and search results never mix owners.
- Storage object keys and background task IDs cannot be used to cross account boundaries.
- A revoked/expired token is denied, and a token for a different issuer/audience is denied.
- Error responses do not reveal whether another user's resource exists.

Run these tests against the real API + database integration environment, not just a mocked helper. Include unit tests for policy helpers and integration tests for every route.

## Implementation sequence

1. Select and configure an OIDC identity provider; do not commit client secrets.
2. Implement a verified request principal dependency and tests for invalid claims.
3. Introduce database schema/migrations with owner columns and indexes; backfill only where ownership can be established safely.
4. Add repository/service methods that require a principal and scope all queries by owner.
5. Add object-storage authorization and short-lived download links.
6. Add the two-account negative integration suite and run it in CI.
7. Add audit events, per-user quotas, session revocation and production deployment controls.
8. Only then enable multi-user registration and remote Android/Web data features.

## Release gate

Multi-user release is **BLOCKED** until the full two-account integration suite passes against the production-equivalent database and storage configuration. A passing shared-token authentication test is not evidence of user isolation.
