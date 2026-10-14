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
$env:GEMINI_MODEL="gemini-3.1-flash-lite"
.\mvnw.cmd "-Dtest=GeminiSynGuidanceLiveTest" test
Remove-Item Env:RUN_LIVE_GEMINI_TEST,Env:GEMINI_API_KEY,Env:GEMINI_MODEL
```

The normal backend test suite mocks the provider and never spends tokens.

Production-style local runs default to `AI_GEMINI_TOOL_PLANNING_ENABLED=false`.
The backend then chooses allowlisted read tools deterministically and makes one
Gemini generation call. Set the flag to `true` only for a model that has passed the
optional native tool-calling live test; unsupported or malformed function calls
otherwise cause the intended Standard fallback.

## Checked-in security baseline

- Spring keeps `/v1/guidance/**` authenticated, denies unspecified routes,
  uses an origin allowlist, and emits restrictive response headers.
- The temporary local demo identity is disabled by default and, when explicitly
  enabled, is limited to loopback-host guidance requests for the seeded synthetic
  student. Production still requires the planned identity implementation.
- The gateway rate-limits Syn separately, limits request bodies, bounds proxy
  timeouts, hides its version, and emits CSP, framing, MIME-sniffing,
  referrer, and browser-permission headers.
- Syn sends minimized evidence, exposes only the typed read-tool allowlist,
  permits at most two Gemini passes, caps output, requests strict JSON, does
  not store provider responses, and falls back without returning raw provider
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


## Syn vector RAG deployment

The database image is pinned to `pgvector/pgvector:0.8.2-pg17`; V13 needs the `vector` extension. The PostgreSQL major version remains 17. Before changing an existing deployment, take a verified logical backup and restore it into a fresh pgvector instance; do not delete the existing volume. Validate encoding/collation and extensions when moving from the earlier Alpine image. The implementation smoke uses an isolated Compose project and new volume, not the developer's original DB.

`KNOWLEDGE_EMBEDDING_ENABLED=false` is the default. Enable only with a backend Gemini key and an approved corpus. Profile is pinned to `gemini-embedding-001:768:retrieval-v1`; new models/dimensions need reindexing and evaluation. See [corpus publication and evaluation commands](../data/knowledge/README.md). No approved corpus is automatically seeded.

`CORS_ALLOWED_ORIGINS` is passed through Compose. When testing on a custom gateway port, include that exact origin; never replace it with a wildcard to fix a local test. Test project ports should bind `127.0.0.1`. Keep LOCAL_DEMO_MODE=false outside controlled local verification.

Syn uses a 20-second external budget with finalization reserve; gateway timeout is 30 seconds. HTTP bodies are capped at 256 KiB and concurrency at eight provider HTTP calls. Database calls remain bounded by short transaction timeouts where appropriate; these limits are not a production capacity benchmark.

Backup/restore smoke procedure on an isolated database:

```text
pg_dump -U <user> -d <source_test_db> -Fc -f /tmp/syn-test.dump
createdb -U <user> <fresh_restore_test_db>
pg_restore -U <user> -d <fresh_restore_test_db> --exit-on-error /tmp/syn-test.dump
```

Then inspect pg_extension, Flyway history, active knowledge versions and `vector_dims(embedding)`. Preserve the original database until the restored instance is validated. Deployment gate results and limits are recorded in the [P1/P2 report](../../.agent/reports/us-03-syn-core-vector-rag-verification.md).


Syn phase-specific usage and optional pinned price estimates: [cost trace configuration](syn-usage-cost.md).
# Syn A/B/C runtime options

`SYN_WEB_ENABLED=false` remains the safe default; opt in to Google Search grounding using the backend Gemini key and `SYN_WEB_MODEL` (defaults to GEMINI_MODEL). User AI consent is also required. One bounded search request may incur Google Search charges beyond token cost estimates. Availability depends on the configured model/key/quota. See [implementation limits](../architecture/syn-coach-abc.vi.md).

After source changes run `docker compose up -d --build backend frontend`, then restart gateway to refresh upstream DNS. For environment-only changes recreate backend; restarting an existing container does not reload its environment. Keep the existing Compose project name and database volume. The local A/B/C verification enables web and embeddings in the ignored `.env`; no corpus is automatically approved/published.

Publish reviewed content with `scripts/Publish-SynKnowledge.ps1 -Manifest <approved-json>`. The one-shot backend uses the current Compose database, enables embeddings, mounts the manifest read-only, opens no host port and exits after ingestion. It does not roll back earlier documents if a later document fails. See [content review and evaluation](../data/knowledge/README.md).
