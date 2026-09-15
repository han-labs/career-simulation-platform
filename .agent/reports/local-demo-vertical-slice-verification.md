# Local Demo Vertical Slice Verification

Date: 2026-09-16  
Scope source: explicit project-lead request for a no-login localhost demo with enough US-01/US-02 data to ground Dashboard and Syn.  
Plan: `plans/DEMO-local-vertical-slice.md`

## Implemented behavior

- Added append-only Flyway V8 with synthetic data only.
- Kept 42 active RIASEC questions and added a completed six-dimension baseline for `student@demo.com` (`I=32`, `C=29`, `R=26`, `A=24`, `S=23`, `E=21`).
- Added three original, deterministic multiple-choice tasks to each of the four catalog simulations that previously had no tasks. All five published simulations now contain three tasks.
- Added one evaluated Backend Developer baseline with two strengths and one `NEEDS_MORE_EVIDENCE` outcome.
- Recorded AI consent only for the synthetic demo identity so a configured backend Gemini adapter can be exercised on localhost.
- Left the exploration plan empty so drafting, review, and explicit save remain testable.
- Corrected the US-02 -> US-03 evidence bridge: persisted `title/isCorrect` outcomes are now translated to the US-03 `label/status` representation. Objective scores are unchanged.

## Security and AI boundary

- `LOCAL_DEMO_MODE` remains disabled by default and the demo filter accepts only loopback hostnames.
- Guidance service checks for the seeded account, `STUDENT` role, `ACTIVE` status, and consent.
- Assessment and simulation still use their existing temporary student-ID contract; this is not production authentication.
- Public task responses were inspected and contained neither `correctOption` nor `evaluationRule`.
- The Gemini key remained in ignored `.env` and was never printed or sent to the browser.
- Quick actions returned `STANDARD`; safe free text returned `AI`; provider failure still maps to Standard fallback.

## Executed evidence

| Check | Observed result |
|---|---|
| `backend\\mvnw.cmd verify` | BUILD SUCCESS; 50 tests, 0 failures, 0 errors, 2 opt-in live-provider tests skipped |
| `frontend\\npm run check` | ESLint passed; 3 files / 9 tests passed; Vite production build passed |
| `docker compose config --quiet` | Passed |
| `git diff --check` | Passed |
| PostgreSQL Testcontainers integration | PostgreSQL 17.11; V1-V8 applied; 2 integration tests passed |
| `docker compose up -d --build` | Backend/frontend built; Postgres healthy; backend/frontend/gateway running |
| Gateway health | `UP` |
| Demo data smoke | 42 questions; 5 published simulations; 15 tasks; live Dashboard with 3 signals, 3 skills, and 1 initial result |
| Public task leakage check | all five simulations reported `leakedAnswerKey=False` |
| Syn quick action | `STANDARD` provenance |
| Syn safe free text | `AI` provenance; non-empty response; audit model `gemini-3.5-flash-lite` |
| Live simulation submission | Frontend Accessibility Review returned 100%; Dashboard then showed 2 recent results and 3 mapped strengths |

## Known gaps

- Login, registration, real session/JWT identity, and production-grade authorization are still pending.
- US-01 and US-02 endpoints must stop trusting the temporary `X-Student-Id` contract before deployment.
- Seed data is intended for course demonstration and development, not production student records.

## Files changed

- `backend/src/main/resources/db/migration/V8__seed_complete_local_demo.sql`
- `backend/src/main/java/edu/hcmute/careersim/simulation/access/JdbcSimulationEvidenceAccess.java`
- `backend/src/test/java/edu/hcmute/careersim/simulation/access/JdbcSimulationEvidenceAccessTest.java`
- `backend/src/test/java/edu/hcmute/careersim/guidance/dao/GuidancePersistenceIntegrationTest.java`
- `README.md`
- `docs/database/README.md`
- `.agent/PROJECT_CONTEXT.md`
- `.agent/TASKS.md`
- `plans/DEMO-local-vertical-slice.md`

