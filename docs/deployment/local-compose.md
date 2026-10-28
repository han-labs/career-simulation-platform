# Local Docker Compose startup

Use a current Docker Desktop with Compose v2.17 or newer. Run these commands from
the root of the checkout whose source you intend to build.
For the integrated application, use the existing repository checkout and project
`careersimfresh`, retaining its PostgreSQL and private-asset volumes. Older isolated
US-01/US-03 checkout instructions are historical, not a request to create a new project.

Normal `local` runtime uses real authentication. `LOCAL_DEMO_MODE` defaults to false;
localhost alone never grants Administrator access. Registration/login use the real
account role; logout revokes the backend credential. `auth-test` remains an automated
test profile, not a required everyday mode. Existing local HTTP cookie settings are
profile-scoped; production keeps secure cookies.

Developer auto-Admin requires explicit `LOCAL_DEMO_MODE=true` plus local profile,
and never runs with prod/production. It reuses an active Admin, does not provision
accounts and cannot replace an authenticated Student. Keep it false for real UAT.

## First setup and normal startup (PowerShell)

```powershell
# Copy only when absent. Never overwrite an existing .env containing credentials.
if (-not (Test-Path -LiteralPath .env)) { Copy-Item .env.example .env }
.\scripts\Initialize-LocalEnvironment.ps1
# Edit ignored .env privately if enabling Google or selecting feature flags.
docker compose config --quiet
docker compose build
docker compose -p careersimfresh up -d
docker compose -p careersimfresh ps
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/v2/auth/providers
```

`docker compose up` also works in the foreground. Open Docker Desktop first. The
initializer copies only a missing .env, fills only a missing/blank JWT key using
32 random bytes and preserves all existing settings. It refuses to replace an
invalid nonblank key. Keep the stable key across restarts. No provider credentials
are generated. Protect .env using local OS access controls; never commit it.

Compose rejects an empty `JWT_SIGNING_KEY` before creating containers. Spring
independently rejects malformed base64, decoded keys shorter than 32 bytes and
invalid JWT lifetimes (60–7200 seconds; default 7200 / two hours). Existing
credentials keep their original expiry; sign in again to receive the configured
lifetime. An explicit `JWT_LIFETIME_SECONDS` override still takes precedence over
the default. `JWT_ALLOW_EPHEMERAL_KEY=false` remains the Compose policy. Production keys belong in approved secret storage.

PostgreSQL must become healthy before backend startup. Backend readiness checks
`/api/health`; frontend and gateway wait for it. Allow about a minute for a cold
Spring startup. Inspect `docker compose ps` and `docker compose logs --tail=200
backend` if startup fails. Do not delete volumes, repair Flyway history or change
database passwords to conceal an error. Existing database credentials must match
the credentials used to initialize its persistent volume.

## Google and feature settings

Google is optional in the safe template. To enable it, privately configure all of:

```dotenv
GOOGLE_OAUTH_ENABLED=true
GOOGLE_CLIENT_ID=<your Web application Client ID>
GOOGLE_CLIENT_SECRET=<your Web application Client secret>
GOOGLE_REDIRECT_URI=http://localhost:8080/api/login/oauth2/code/google
```

Register that exact callback in Google Cloud; use one canonical host throughout
the handshake. Enable only `openid`, `email`, `profile`. JWT signing is required
independently of Google. Never place credentials in .env.example or a VITE_ value.
See [Google setup and testing](../testing/google-identity.md). A true provider flag
and a Google authorization redirect prove configuration, not a completed live login.

For versioned Career Assessments, set **both** `CORE_V2_ENABLED=true` and
`VITE_CORE_V2_ENABLED=true`; keep `LEGACY_WRITE_ENABLED=false`. Agent Platform has
its separate matching runtime/build flags. Changing a VITE_ flag requires a new
frontend build. Keep LOCAL_DEMO_MODE=false. Secure cookies and CSRF remain enabled;
production requires HTTPS. Any HTTP-only cookie exception must be an explicitly
isolated local rehearsal, never a production default.

## Common configuration mistakes

- `Set JWT_SIGNING_KEY ...`: run the initializer or supply the valid private key.
- Google bean failure: check enabled flag, both credentials and exact callback.
- Missing `docker-compose.us03-local.yml`: an old ignored .env may set COMPOSE_FILE
  to a removed override. Use this checkout's base docker-compose.yml; remove only
  that stale COMPOSE_FILE setting after checking which checkout you intend to run.
- Environment edits require `docker compose up -d` to recreate the backend;
  `docker restart` preserves the old environment. Dependency `restart: true`
  refreshes the frontend/gateway after an explicit Compose dependency update,
  including Nginx upstream addresses. Use Compose for these updates, rather than
  replacing individual containers manually. See [Docker startup order](https://docs.docker.com/compose/how-tos/startup-order/).
- Full `docker compose config` renders secrets. Prefer `config --quiet`; never
  paste resolved config, .env, auth cookies or tokens into logs/chat/Git.

## Isolated clean-install verification

Use a separate Compose project, .env, loopback ports and fresh volumes. Supply its
own generated JWT key and only synthetic users/content. Run config/build/up/ps and
auth/assessment smoke there; never point a fixture/bootstrap at valuable data.
Stop the disposable project without `-v` so evidence remains recoverable. No
volume reset is part of normal startup.
