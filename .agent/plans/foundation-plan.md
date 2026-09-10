# Project Foundation Plan

Status: Complete — foundation verified locally; feature user stories remain planned work  
Date: 2026-09-10

## Goal

Create a runnable and implementation-ready repository that preserves the supplied proposal while reusing the strongest traceability, layered architecture, GRASP, facade, testing, reporting, and AI-quality rules from PeerGrade Hub.

## Foundation deliverables

- Java 17 Spring Boot modular monolith.
- React + Vite feature-based frontend and landing starter.
- PostgreSQL + Flyway schema for assessment, simulations, evaluation, and guidance.
- Public simulation catalog vertical slice.
- Docker Compose with PostgreSQL, backend, frontend, and Nginx.
- CI, formatter, architecture test, unit/controller tests, frontend lint/test/build.
- `.agent` knowledge base with source proposal and architecture references.
- Original seed simulation metadata; no copied third-party task content.

## Boundary decisions

- The foundation demonstrates architecture through the catalog but does not claim US-01, US-02, or US-03 complete.
- Frontend ownership follows the user's supplied assignment.
- Backend ownership remains unassigned.
- AI provider is disabled by default and no credential is required to run the foundation.
- Deterministic fallback is a mandatory design constraint before any provider is enabled.
- Security currently permits only health and public catalog endpoints and denies all other undeclared endpoints.

## Verification plan

1. Run `backend/mvnw.cmd verify`.
2. Run `npm run check` in `frontend`.
3. Run `docker compose config --quiet`.
4. Build and start the Docker stack.
5. Verify gateway UI, `/api/health`, and `/api/v1/simulations`.
6. Record exact observed results in `reports/foundation-verification.md`.

All six foundation verification steps passed on 2026-09-10. This status covers the scaffold and catalog demonstration slice only; it does not mark US-01, US-02, or US-03 complete.
