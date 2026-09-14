# Deployment guide

## Local Docker Compose

```powershell
Copy-Item .env.example .env
# Edit .env. Leave AI_PROVIDER=disabled for deterministic-only Syn, or configure
# AI_PROVIDER=gemini, GEMINI_API_KEY, and GEMINI_MODEL for Google AI Studio;
# or AI_PROVIDER=openai with the corresponding OpenAI values.
# For localhost browser integration before login exists, set LOCAL_DEMO_MODE=true.
# Never enable that flag in a shared or deployed environment.
docker compose up -d --build
docker compose ps
```

Verify:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/v1/simulations
```

The selected provider key is injected into the backend container only. Never use a
`VITE_` variable for it and never call the provider directly from React.

Optional provider smoke test (synthetic data only):

```powershell
cd backend
$env:RUN_LIVE_GEMINI_TEST="true"
$env:GEMINI_API_KEY="replace-locally"
$env:GEMINI_MODEL="gemini-3.5-flash-lite"
.\mvnw.cmd "-Dtest=GeminiSynGuidanceLiveTest" test
Remove-Item Env:RUN_LIVE_GEMINI_TEST,Env:GEMINI_API_KEY,Env:GEMINI_MODEL
```

The normal backend test suite mocks the provider and never spends tokens.

## Checked-in security baseline

- Spring keeps `/v1/guidance/**` authenticated, denies unspecified routes,
  uses an origin allowlist, and emits restrictive response headers.
- The temporary local demo identity is disabled by default and, when explicitly
  enabled, is limited to loopback-host guidance requests for the seeded synthetic
  student. Production still requires the planned identity implementation.
- The gateway rate-limits Syn separately, limits request bodies, bounds proxy
  timeouts, hides its version, and emits CSP, framing, MIME-sniffing,
  referrer, and browser-permission headers.
- Syn sends minimized evidence, caps output, requests strict JSON, does not
  store provider responses, and falls back without returning raw provider
  failures.

These controls reduce common risk; they are not a guarantee against every
attack. The current project still needs a real identity implementation before
live student use. US-01/US-02 development endpoints still use temporary
identity behavior, and the Docker file exposes backend/frontend ports for
local debugging. In production, expose only the TLS gateway.

## Ubuntu VM target

1. Install Docker Engine and the Compose plugin from Docker's official repository.
2. Clone the repository at the exact release commit.
3. Create `.env` from `.env.example` and replace all development credentials.
4. Configure DNS and TLS termination. The checked-in Nginx config is an HTTP
   development baseline; enable HSTS only after HTTPS is working on every
   intended hostname.
5. Run `docker compose pull` and `docker compose up -d --build`.
6. Capture `docker compose ps`, health output, migration history, and the exact commit for report evidence.
7. Configure automated PostgreSQL backups and test restoration before calling the deployment production-ready.

Never commit the VM `.env`, TLS private keys, provider credentials, database backups, or student exports.
