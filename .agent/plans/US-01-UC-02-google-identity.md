# Current report US-01/US-02 — Google registration and sign-in

Status: Implementation authorized by the attached user request; acceptance pending evidence.
Owner: Person 1, UC-01/02; human reviewer and lead security review pending.
Date: 2026-10-24.

## 1 Scope and sources

Current selected US-01/02 and UC-01 Register account / UC-02 Log in, frozen Baseline
identity invariants, existing P1/P2 security and UC-01 implementation. Actors:
Student, existing Enterprise and Administrator. Preserve US-03 and P1–P6/V1–V22.
The user authorizes the shared Google option, states, role landing and configuration
documentation. No domain restriction, implicit email linking, new identity service,
provider token storage, schema migration, main merge or production deployment.

## 2 Flows and acceptance

Both email forms retain their controls and show primary submit, separator, Google
option and (registration only) cancellation. Configured Google starts the existing
backend authorization-code/PKCE flow. Missing configuration, provider lookup failure
and authorization start are explicit accessible EN/VI states. Cancel/provider error,
unverified identity, locked account, email collision and restoration failure show
recoverable feedback. A verified issuer/subject provisions a Student/profile once;
an existing subject restores its original account. Email collisions require existing
authentication plus explicit CSRF-protected linking. JWT cookie plus `/v2/me` restore
precedes role landing. No email/password or assessment behavior is replaced.

## 3 Responsibility and sequence mapping

| Responsibility | Existing expert / planned change |
|---|---|
| Form and Google action | AuthenticationForm; shared Google option styles/official asset |
| Provider availability/session/role landing | AccountPage, existing providers and me APIs |
| OAuth request/state/nonce/PKCE | SecurityConfig and GoogleOAuthFlow; classify safe failure codes |
| Identity uniqueness/provision/link | BrowserAuthentication facade/Impl and BrowserAuthDao, reused |
| Credential and authorization | Existing BrowserCredentialCookies/filter and ActorAccess |

Browser → Nginx `/api` → Spring Google authorization → Google verified OIDC callback
→ existing identity transaction → JWT HttpOnly cookie → account `/v2/me` → role workspace.
DAO owns persistence; services own eligibility/uniqueness. No controller business rules.

## 4 Change surface and API

Identity frontend/tests/i18n/CSS and official logo provenance; Google callback failure
handler/tests; environment example, setup/sequence documentation and sanitized report.
Reuse GET `/api/oauth2/authorization/google`, GET `/api/login/oauth2/code/google`, GET
`/api/v2/auth/providers`, GET `/api/v2/me`, POST `/api/v2/auth/google/link`, existing
JWT/CSRF APIs. Callback error query contains only fixed safe codes. No DDL changes.

## 5 Security, privacy and AI

Keep issuer/audience/signature/nonce/state verification, verified email, stable subject,
current account/role/status, short JWT, cookie flags, CSRF and credential-bound linking.
Secrets backend-only; no tokens in browser storage or logs. AI is uninvolved and disabled
in the isolated local stack. No real Student data in fixtures; no provider mock in runtime.

## 6 Tests and evidence

Frontend configured/unconfigured/lookup error, both form actions, pending, localization,
return feedback, session recovery and role redirects plus password regressions. Backend
signed local OIDC fixture: create/profile/repeat login, uniqueness/collision/link, cancel,
provider failure and invalid claims, cookies/current role/CSRF, new Student assessment
submit/retrieval. `npm run check`, `mvnw verify`, Compose validation and gateway/UI smoke.
Use feature worktree then temporary integration branch containing committed US-03.
Real Google acceptance stays unverified without external credentials and a completed
Google browser flow. Existing unrelated StudentAgentIntegrationTest failure on unchanged
main remains a separate gate issue; do not weaken it. Record actual counts and commit.

## 7 Risks and decisions

Google credentials are absent from the current local runtime (flag false). Document
Google Auth Platform Branding/Audience/Web client and exact canonical callback/origin;
never enable a dead provider. Current HCM-UTE blue/white styling is explicitly retained
per user request, qualifying historical DESIGN palette. Original US-03 branch/checkout
remain intact; only the existing disposable integration worktree receives the auth merge.
