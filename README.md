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
