# Report-selected US-01 / UC-01 registration

Uses the existing [P1/P2 identity architecture](../architecture/p1-p2-security-core.md). No schema, cookie transport, role policy, consent policy or assessment scoring changes.

```mermaid
sequenceDiagram
  actor Student
  participant UI as AccountPage / AuthenticationForm
  participant API as IdentityController
  participant Identity as IdentityService
  participant DB as IdentityDao / PostgreSQL
  Student->>UI: Register, enter identity details
  UI->>API: GET /api/v2/auth/csrf
  API-->>UI: CSRF token and handshake cookie
  UI->>API: POST /api/v2/auth/register + CSRF
  API->>Identity: validated Registration
  Identity->>DB: normalized unique account + password hash
  Identity->>DB: Student profile + audit (same transaction)
  API-->>UI: account DTO (no JWT issued by registration)
  UI-->>Student: Account created; sign in
  Student->>UI: Sign in
  UI->>API: POST /api/v2/auth/login + CSRF
  API-->>UI: HttpOnly JWT cookie; rotate handshake / CSRF
  UI-->>Student: Career Assessments link
  Student->>UI: Open /assessment
  Note over UI,DB: Existing UC-03 selects published version, validates, scores and stores owned result
```

| Request | Success | Failure |
|---|---|---|
| GET `/api/v2/auth/providers` | `data.google` reports configured provider | UI retains email/password path |
| POST `/api/v2/auth/register` | existing 200 account DTO; no authentication cookie, membership escalation or consent | 403 missing CSRF; 422 invalid fields; 409 duplicate normalized email |
| POST `/api/v2/auth/login` | existing 200 account DTO and JWT cookie | 401 invalid credentials/inactive account |
| GET `/api/v2/me` | current authorized account | 401 absent/invalid/revoked JWT |
| POST `/api/v2/auth/logout` | revoke current credential and clear cookies | existing CSRF requirements |

Registration requires email (max 255), password (12–128), displayName (max 150), role STUDENT or existing ENTERPRISE. Enterprise additionally requires enterpriseName (max 240); Student omits it. No new profile fields are collected. Server uniqueness is case-insensitive after trimming. Concurrent duplicate insertion is handled by the database transaction.

Duplicate feedback: “An account with this email already exists. Please sign in.” The UI associates it with email, preserves entries for correction, and permits switching to sign-in. Server field errors use the existing `fieldErrors[{field,message}]` envelope. Connection/5xx registration failures show a temporary-unavailability message and never claim completion. Retry with an existing email is resolved by uniqueness and sign-in guidance.

Cancel before submission clears the in-memory form and returns home without a request. Pending submission disables repeat submit, mode switching and cancellation. Password visibility is explicit; no credentials are stored in browser storage. Session lookup has loading and retry states; authenticated Students get a link to Career Assessments. Existing consent UI remains independent.

Google continues through `/api/oauth2/authorization/google` and `/api/login/oauth2/code/google`, with backend-owned OIDC state/nonce/PKCE, verified email and issuer/subject binding. Collision requires explicit linking from a signed-in account. Disabled Google is explained. Synthetic signed OIDC fixture evidence must not be described as live Google acceptance.

Tests: `RegistrationServiceTest`, `RegistrationAssessmentIntegrationTest`, `AuthenticationContractTest`, `AuthenticationForm.test.jsx`, `AccountPage.test.jsx`; execution status belongs in the dated completion report.
