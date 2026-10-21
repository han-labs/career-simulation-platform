<!-- documentation-sync-2026-10-22 -->
# CareerSim

Career Simulation System for student IT-career exploration. React/Vite JavaScript/JSX, Java 17/Spring Boot modular monolith, PostgreSQL/pgvector and Flyway; optional backend-owned AI never changes objective results or blocks Core.

## Current documentation and scope

- [Documentation index](docs/README.md), [requirements/source order](docs/requirements/README.md).
- [10 selected report US](docs/requirements/user-stories/REPORT_SELECTED_10_US.md), [16 UC / nine selected](docs/requirements/use-cases/README.md), [official assignments](docs/TEAM_ASSIGNMENTS.md).
- [Frozen Baseline v1 + DR1–DR2](docs/requirements/CareerSim_Requirements_Baseline_v1_Chat2_Handoff.md): unchanged business invariants, separate 15-US IDs.
- [As-built architecture](docs/architecture/README.md), [API](docs/api/README.md), [database](docs/database/README.md), [deployment](docs/deployment/README.md).
- [Report/code discrepancies and synchronization evidence](docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md).
- [AI_USAGE](AI_USAGE.md): shared draft/contributor review; [P5/P6 AI evidence limits](docs/ai/AI_USAGE_P5_P6.md).

Checked `origin/main@5486f9f` contains P1–P6 and V1–V22. Registration/JWT/Google integration, versioned Core, KB Studio, Builder, assigned Student Agent, reports/plans and safe usage are existing code. Their automated/mock evidence does not establish real Google, live-model quality or deployed UAT. No reimplementation is requested.

**Person 1:** UC-01/02/03 → US-01/02/03. **Person 2:** UC-04/05/09 → US-04/05/06/10. **Person 3 (lead):** UC-06/07/08 → US-07/08/09. Names/Git aliases/reviewer rota need separate sign-off. UC-10–16 are supplementary, UC-14 is deferred.

## Development entry point

Read [AGENTS](AGENTS.md) before changes, then use [.agent/TASKS](.agent/TASKS.md) for the current queue. Existing plans and legacy US identifiers are historical unless a current task explicitly references them.

For code verification use the gates in [.agent/rules/testing-and-quality-gates.md](.agent/rules/testing-and-quality-gates.md). Documentation-only synchronization checks links/IDs/source consistency and preservation; no full suite, DB startup or paid provider is required.

Current auth needs a backend signing-key configuration (or explicitly isolated development ephemeral key), Secure-cookie/HTTPS policy and separately configured Google credentials for live OAuth. Core/Agent runtime and Vite build flags are opt-in. Consult current deployment/auth contracts; do not use the old “no secret required/login pending” demo instructions below as the present deployment procedure.

Never stage `.env`, raw agent logs, JSON evidence dumps, private screenshots or transcripts. The current ignore whitelist does not fully protect `.agent/**`; use explicit paths and inspect diffs. No commit/push/deployment is part of this documentation update.

## Historical README

The previous README is preserved below as a legacy foundation/Syn setup record, including its original commands and old ownership. It is not the current runbook, business baseline or selected catalogue.

<details>
<summary>Historical content preserved unchanged</summary>

<!-- preserved-content-start -->
# CareerSim

CareerSim is the public shorthand for Career Simulation Platform for Students, an OOSE project that helps students explore IT careers through a RIASEC assessment, short task-based simulations, deterministic evaluation, and evidence-grounded AI guidance. The AI layer explains results; it never decides objective scores and is never required for the core workflow.

This repository is an implementation-ready foundation derived from the approved proposal and the strongest architecture, traceability, testing, and AI-development conventions in PeerGrade Hub.

## Current foundation

- React + Vite frontend with a responsive landing page, feature routes, an API client, and a tested catalog adapter.
- Java 17 + Spring Boot modular monolith with a complete catalog vertical slice.
- PostgreSQL schema and Flyway migrations for users, RIASEC assessments, simulations, attempts, deterministic evaluation, and guidance reports.
- Seeded, original IT simulation metadata for local development.
- Docker Compose stack with PostgreSQL, backend, frontend, and Nginx gateway.
- CI, formatting, architecture tests, frontend lint/tests, and documentation quality gates.
- A tracked `.agent` knowledge base containing project context, responsibilities, design rules, report/evidence rules, templates, and source references.

## Architecture

```text
Browser -> Nginx gateway -> React
                      \-> Spring Boot modular monolith -> PostgreSQL
                                                   \-> external LLM adapter (optional)
                                                        -> deterministic fallback (required)
```

The backend dependency direction is:

```text
Controller -> Service interface (subsystem facade) -> ServiceImpl -> DAO/domain -> PostgreSQL
```

See `docs/architecture/README.md` for the system diagram and module boundaries.

## Prerequisites

- Docker Desktop with Docker Compose, or
- Java 17 and Node.js 22 for local application development.

## Run the complete stack

No secret or local `.env` file is required for the demo foundation.

```powershell
docker compose up --build
```

### Enable Syn's optional Gemini or OpenAI provider

Standard guidance works without an API key. To enable AI for eligible free-text
questions, create an ignored local environment file and edit it without sharing the
key:

```powershell
Copy-Item .env.example .env
```

For a Gemini key created in Google AI Studio, set these values in `.env`:

```dotenv
AI_PROVIDER=gemini
GEMINI_API_KEY=replace-with-your-own-ai-studio-key
GEMINI_MODEL=gemini-3.1-flash-lite
GEMINI_BASE_URL=https://generativelanguage.googleapis.com/v1beta
AI_GEMINI_TOOL_PLANNING_ENABLED=false
AI_TIMEOUT_SECONDS=8
AI_MAX_OUTPUT_TOKENS=300
```

Until the full login UI is implemented, a local browser can exercise the protected
guidance flow with the seeded synthetic student. This mode is opt-in and accepts
guidance requests only when the request hostname is a loopback host:

```dotenv
LOCAL_DEMO_MODE=true
LOCAL_DEMO_EMAIL=student@demo.com
```

Flyway V8 prepares this synthetic profile for an immediate end-to-end demo. It adds
a completed 42-question RIASEC baseline, one evaluated Backend Developer result,
AI consent for this demo identity, and three original multiple-choice tasks for each
published simulation. New assessment and simulation attempts are still persisted and
become the latest evidence shown on Dashboard and used by Syn.

Apply the migration and rebuild the application:

```powershell
docker compose up -d --build
Invoke-RestMethod http://localhost:8080/api/health
```

Keep `LOCAL_DEMO_MODE=false` in every shared or deployed environment. The mode is
only a temporary localhost integration aid and does not replace production login.

Alternatively, configure OpenAI:

```dotenv
AI_PROVIDER=openai
OPENAI_API_KEY=replace-with-your-own-project-key
AI_MODEL=gpt-5.6-luna
AI_TIMEOUT_SECONDS=8
AI_MAX_OUTPUT_TOKENS=300
```

Then rebuild only the affected services:

```powershell
docker compose up -d --build backend frontend gateway
```

The key is read only by the backend container. Never rename it with a `VITE_`
prefix, paste it into React, commit `.env`, print it in screenshots, or put it in a
Git command. Quick actions remain deterministic and do not call the provider. An
AI call is attempted only for a safe free-text message from a consenting student;
all other cases return Standard guidance.

To test both adapters without sending real data or depending on unfinished runtime
authentication, run the mocked tests:

```powershell
cd backend
.\mvnw.cmd "-Dtest=GeminiSynGuidanceProviderTest,OpenAiSynGuidanceProviderTest" test
```

An optional live smoke spends a small amount of API usage and sends synthetic
evidence only:

```powershell
$env:RUN_LIVE_GEMINI_TEST="true"
$env:GEMINI_API_KEY="replace-with-your-own-ai-studio-key"
$env:GEMINI_MODEL="gemini-3.1-flash-lite"
.\mvnw.cmd "-Dtest=GeminiSynGuidanceLiveTest" test
Remove-Item Env:RUN_LIVE_GEMINI_TEST,Env:GEMINI_API_KEY,Env:GEMINI_MODEL
```

That command checks the default one-generation-call path. To evaluate the optional
native planning path as well, set `RUN_LIVE_GEMINI_TOOL_TEST=true`; treat it as a
model-compatibility test, not a prerequisite for the reliable default.

Runtime browser calls normally require the planned identity/login foundation and
recorded `consented_to_ai_at`. During local development only, the explicit demo mode
above supplies the V8 synthetic principal while preserving the service's account,
status, role, and consent checks. Quick actions always use Standard guidance; enter a
safe free-text question such as `What should I explore next based on my results?` to
exercise the configured Gemini adapter.

The reliable default is server-planned generation: `SynAgentPolicy` selects the
approved read tools, the backend executes them, and Gemini receives one bounded
generation request with those results. Native Gemini tool planning is retained as an
experimental two-request path; enable it only after the selected model passes the
live tool-calling test by setting `AI_GEMINI_TOOL_PLANNING_ENABLED=true`.

Open:

- Application through Nginx: `http://localhost:8080`
- Frontend container directly: `http://localhost:3000`
- Backend health endpoint: `http://localhost:8081/api/health`
- PostgreSQL: `localhost:5433`

Stop the stack without deleting database data:

```powershell
docker compose down
```

Delete the local demo database volume only when intentionally resetting data:

```powershell
docker compose down -v
```

## Local development

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Run the backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Run the frontend in a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8081`.

## Verification

Backend:

```powershell
cd backend
.\mvnw.cmd verify
```

Frontend:

```powershell
cd frontend
npm run check
```

Container configuration:

```powershell
docker compose config --quiet
```

## Team scope at the current checkpoint

The following assignments apply to frontend implementation only:

| Member | Frontend responsibility |
|---|---|
| Võ Nguyễn Ngọc Bích | US-01 RIASEC assessment and the landing page |
| Mai Trần Thùy Trang | US-02 career simulation workflow |
| Huỳnh Gia Hân | Repository/frontend setup and US-03 feedback/guidance |

Backend ownership has not been assigned in the supplied material and must not be inferred by an AI agent.

## Working rules

Read `AGENTS.md` and `.agent/README.md` before implementation. Every feature must preserve this traceability chain:

```text
Proposal/SRS -> User story -> Acceptance criteria -> Sequence/domain design -> Code -> Tests -> Evidence -> Report update
```

Do not commit `.env`, API keys, model credentials, private student data, or copied third-party simulation content. Public external catalog metadata may be collected for the non-commercial course project only under `docs/data/simulation-content-policy.md`; CareerSim simulation tasks remain original or separately licensed.


### Syn core and vector RAG implementation

Current component map, flows, fallback and Vietnamese viva guide: [Syn implementation](docs/architecture/syn-core-rag-implementation.vi.md). See [verification and open acceptance gates](.agent/reports/us-03-syn-core-vector-rag-verification.md) and [reviewed corpus ingestion/evaluation](docs/data/knowledge/README.md). The draft corpus is not automatically published. Voice/realtime remain future phases.

<!-- preserved-content-end -->

</details>
