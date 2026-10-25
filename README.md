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

**Allocation updated 2026-10-25:** **Person 1:** UC-01/02/03 → US-01/02/03. **Person 2:** UC-04/05/06 → US-04/05/06/07. **Person 3 (lead):** UC-07/08/09 → US-08/09/10. This branch implements UC-07 only. Names/Git aliases/reviewer rota need separate sign-off. UC-10–16 are supplementary, UC-14 is deferred.

## Development entry point

For current PowerShell/Docker startup, use [the local Compose runbook](docs/deployment/local-compose.md).
Initialize a private stable JWT key before `docker compose config --quiet`, build
and startup; Google credentials do not replace the required signing key.

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

## Run the local Admin Studio

The standard Compose file is the local Docker development environment. It selects
Spring profile `local` and binds application ports to 127.0.0.1. No Account page,
password/token, manual Admin ID or authentication overlay is required.

From the repository root:

```powershell
docker compose -p careersimfresh build backend frontend
docker compose -p careersimfresh stop
docker compose -p careersimfresh up -d --no-build --pull never
```

Open **http://localhost:8080/admin** (redirects to Knowledge Bases). The only Admin
product areas are Knowledge Bases, AI Agents and AI Usage. Removed Account and
Assessment Administration URLs display not-found; Student/backend domains remain.

LocalDemoAuthenticationFilter selects the oldest existing ACTIVE Administrator
from this local database and revalidates on each request. It never creates users,
changes roles or issues credentials. If none exists, access fails closed; account
provisioning belongs to the identity owner. Existing JWT actors take precedence.
CSRF, scope checks, services, persistence and publication guards remain active.

The local principal is absent without profile `local` and cannot activate when
`prod` or `production` is active, even together with `local`. Deployed environments
must explicitly select a production profile and configure secure cookies/normal
JWT/OAuth integration; this Compose file is for local development only.

Keep existing secrets in ignored `.env`. For real KB embeddings the existing Gemini
adapter requires server-side GEMINI_API_KEY; the local profile enables embedding.
A missing/invalid key produces an honest processing failure. Model answer generation
is separate. Never put provider keys in VITE variables or print them in logs.

The two named volumes retain PostgreSQL and uploaded private source files across
stop/start/rebuild. Do not delete volumes or repair Flyway history for this workflow.
Runtime readiness: `Invoke-RestMethod http://localhost:8080/api/health`; then open
the KB list through the gateway. Frontend/backend source edits require rebuilding
the corresponding image; there are no source hot-reload mounts in Compose.

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
