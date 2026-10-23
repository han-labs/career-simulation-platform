# Google sign up / sign in — current US-01 and US-02

## Existing implementation and disabled local UI

CareerSim already implements server-side Spring Security authorization code + OIDC
with PKCE, state and nonce. Both email forms use the same
`/api/oauth2/authorization/google` navigation. `/api/v2/auth/providers` exposes a
boolean availability flag, never credentials. The latest local stack initially has
`GOOGLE_OAUTH_ENABLED=false`; the root `.env` also has empty client credentials and
callback. The previous UI rendered a text note in this state, with no Google button.
Now both forms retain a disabled, explained Google option. A provider availability
request failure has a separate retry action. No production mock or token-in-storage
fallback is used. Email/password remains available.

## Google Cloud setup

Use a dedicated development Google Cloud project and Web application OAuth client.
In Google Cloud Console → **Google Auth Platform**:

1. **Branding**: set the app name, user support email and developer contact email.
2. **Audience**: choose External for ordinary Google accounts (or an organization
   policy that actually applies). While in Testing, add the accounts used for UAT
   as test users. Do not invent an `hcmute` email-domain eligibility restriction.
3. **Data Access**: use only `openid`, `email`, `profile`. CareerSim does not need
   Gmail, Drive or offline access for authentication.
4. **Clients → Create client → Web application**. Add the exact authorized redirect
   URI for the chosen browser origin:
   `http://127.0.0.1:18080/api/login/oauth2/code/google`.
   If using `localhost` instead, add
   `http://localhost:18080/api/login/oauth2/code/google` and configure that same URI
   in the backend. Choose one canonical origin per test session; do not start on
   `127.0.0.1` and return to `localhost`, since the handshake cookie is host-scoped.
5. Record the Client ID and Client secret privately. This backend redirect flow
   does not use a Google JavaScript SDK or require JavaScript authorized origins.
   An origin, when used by another client, contains no callback path. Do not put
   the secret into a `VITE_` variable, source, screenshot or chat.

The URI must match the configured scheme, host, port and path exactly, without a
trailing slash. In production use the actual HTTPS host, a separate production
client/consent configuration and `SESSION_COOKIE_SECURE=true`. Local HTTP is only
for loopback development. Provider publication/verification is a separate release
step governed by Google and the application's audience.

Sources: [Google Web Server OAuth](https://developers.google.com/identity/protocols/oauth2/web-server),
[OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect),
[OAuth client configuration](https://support.google.com/cloud/answer/15549257?hl=en).

## Backend environment and Docker

Put real values only in an ignored local environment file, protected by OS access:

```dotenv
GOOGLE_OAUTH_ENABLED=true
GOOGLE_CLIENT_ID=<Web application Client ID>
GOOGLE_CLIENT_SECRET=<Web application Client secret>
GOOGLE_REDIRECT_URI=http://127.0.0.1:18080/api/login/oauth2/code/google
```

The checked-in `docker-compose.yml` already forwards all four variables to Spring.
Use `docker compose --env-file <private-env-file> config --quiet`, then
`docker compose --env-file <private-env-file> up -d --no-deps backend` for an existing
stack. Retain its database, JWT signing key and asset volumes. If using the ordinary
gateway on port 8080, register and configure port 8080 instead of 18080. To run Java
directly, set the same four backend process environment variables before starting
Spring; the configured callback still needs a gateway that serves the frontend and
proxies `/api`. Never enable the flag with empty or placeholder credentials.

For the already-created isolated **careersim-latest-local** stack in
`.codex-tmp/us01-us03`, the ignored `tmp/local-latest-compose.yml` forwards the same
variables. Append them privately to `tmp/local-latest.env`, validate and recreate
only its backend from that worktree:

```powershell
docker compose --env-file tmp/local-latest.env -f tmp/local-latest-compose.yml config --quiet
docker compose --env-file tmp/local-latest.env -f tmp/local-latest-compose.yml up -d --no-deps backend
```

This reuses the existing local database and JWT key. Do not remove volumes. Refresh
`http://127.0.0.1:18080/account`; the provider endpoint should report `google: true`
and both Google options should become links. Do not log or publish `compose config`
without `--quiet`, container environment dumps, OAuth callback codes, JWTs or secrets.

Both Nginx configurations retain `/api/` and forward `Host` including its port and
`X-Forwarded-Proto`. The callback is handled by Spring at
`/login/oauth2/code/google` under context `/api`, never by a React route. There is
no frontend Google client ID or alternate callback implementation.

## Sequence and outcomes

```mermaid
sequenceDiagram
  actor Student
  participant UI as React via Nginx
  participant Backend as Spring /api
  participant Google
  participant DB as PostgreSQL
  Student->>UI: Continue with Google
  UI->>Backend: GET oauth2/authorization/google
  Backend-->>Google: Redirect (state, nonce, PKCE, exact callback)
  Google-->>Backend: Callback with authorization code + state
  Backend->>Google: Exchange code and verify signed OIDC claims
  Backend->>DB: Lookup issuer + subject / provision Student + profile
  DB-->>Backend: Authorized current account
  Backend-->>UI: HttpOnly JWT cookie; /account?oauth=success
  UI->>Backend: GET v2/me
  Backend-->>UI: Current account and role
  UI-->>Student: Authorized workspace
```

Issuer + subject determines identity even if the provider email later changes.
New identities are Students; existing Enterprise/Admin roles are preserved. Matching
a local email never grants access or silently links accounts. Sign in to the existing
account, choose **Link Google to this account**, then complete Google authorization
using the same still-live browser credential. The link POST requires CSRF; its
short-lived intent binds the credential hash, account and credential version.

Callback errors expose fixed codes only: `cancelled`, `provider-unavailable`,
`link-required`, `account-disabled`, `denied`. React translates them to EN/VI.
After success, `/v2/me` must succeed before routing Student to `/assessment`,
Enterprise to `/enterprise`, or Admin to `/admin/assessments` (Core-enabled build).
When Core is off, Enterprise/Admin stay on their account page. Session restoration
failure offers retry/sign-in without claiming authenticated access.

JWT cookies, current-account checks, logout revocation, CSRF and assessment ownership
remain the P1/P2 implementation. OAuth provider tokens are transient. No migrations,
assessment scoring changes, implicit AI consent or new eligibility rule are added.

## Acceptance checklist and evidence boundaries

- Frontend: both modes, visible disabled/configured/loading/start/retry states,
  localized labels, cancellation/provider/link feedback, restoration and role landing.
- Local signed OIDC fixture: real authorization/state/nonce/PKCE, signed ID token,
  verified email/audience/expiry, new/repeat identity, collision, explicit link,
  cancellation, provider failure, cookie/current role/status and legacy quiz result.
- PostgreSQL Core service integration: identity provision → Student/profile → JWT
  and CSRF → published assessment → submit/save → logout → subject login → reload.
  This service-boundary fixture does not itself exercise Google's external service.
- Existing password registration/login and US-03 frontend/backend/gateway regressions.
- Browser: inspect desktop/tablet/320px layout, label association, focus and keyboard.
- **Real Google acceptance remains NOT VERIFIED** until actual credentials are supplied
  privately and an external Google browser flow completes. A local signed-provider
  test, enabled UI mock or 302 redirect is not real Google acceptance.

For live UAT, test a fresh Google Student, repeat sign-in without duplicate records,
provider cancel, local-email collision plus explicit link, existing privileged account
role isolation, logout, and a saved career assessment after sign-in/reload. Record only
sanitized results; retain no provider codes, private email or session tokens.
