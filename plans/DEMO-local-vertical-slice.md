# Local Demo Vertical Slice Plan

Status: Implemented and verified  
Owner: Shared integration work  
Date: 2026-09-15

## 1 Scope

- Foundation item: provide a synthetic local dataset that supports the full assessment -> simulation -> Dashboard -> Syn demonstration before runtime login is implemented.
- Actor: student using the application on `localhost`.
- Source: explicit project-lead request on 2026-09-15, plus BR-01 to BR-03, BR-09 to BR-12, and BR-17 in `.agent/PROJECT_CONTEXT.md`.
- Acceptance criteria: all 42 assessment questions load; every published simulation can be opened and completed; the seeded demo student has completed assessment and simulation evidence; Dashboard renders live evidence immediately; safe free-text Syn messages can use the configured Gemini adapter; Standard guidance remains available when AI is disabled or fails; local access does not require a login screen.
- Explicit exclusions: production authentication, registration, authorization redesign, real student data, AI scoring, answer-key exposure, automatic plan saving, and production deployment defaults.
- Related owner boundaries: no redesign of the US-01, US-02, or US-03 frontend; this is additive seed/integration work plus the minimum evidence-mapping correction required by the existing US-02 result contract.

## 2 Flow inventory

### Main success flow

1. Flyway creates the synthetic demo student, RIASEC questions, five simulations, tasks, and a completed evidence baseline.
2. Local demo authentication supplies only `student@demo.com` to guidance requests received through a loopback hostname.
3. The student can complete a new assessment and any published simulation without a login screen.
4. Server-side deterministic evaluation persists the new result.
5. Dashboard aggregates the latest assessment and recent simulation outcomes.
6. A safe free-text Syn message uses minimized evidence through Gemini when configured; otherwise Standard guidance is returned.

### Alternate flows

1. The fresh database already contains live Dashboard evidence before the student performs a new action.
2. Completing a new assessment or simulation supersedes or adds to the seeded evidence through existing latest/recent queries.
3. Quick actions remain deterministic and do not spend provider tokens.

### Exception and fallback flows

1. With `AI_PROVIDER=disabled`, a missing key, quota failure, timeout, unsafe input, or malformed provider output, Syn returns Standard guidance.
2. With `LOCAL_DEMO_MODE=false`, protected guidance endpoints remain unauthorized until real authentication exists.
3. A non-loopback hostname never receives the demo principal.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/file | Reason |
|---|---|---|---|
| Add reproducible synthetic demo content | Flyway migration | `V8__seed_complete_local_demo.sql` | Append-only, repeatable across developer machines, and kept out of production logic. |
| Translate persisted US-02 task outcomes into US-03 evidence | Evidence access adapter | `JdbcSimulationEvidenceAccess` | The integration boundary owns representation mapping; scoring remains unchanged. |
| Verify migrated demo state | PostgreSQL integration test | `DemoSeedIntegrationTest` | Proves migration counts, synthetic consent, evidence, and public task safety against PostgreSQL. |
| Explain safe local startup | Documentation | `README.md`, `docs/database/README.md` | Keeps demo enablement explicit and prevents it from being mistaken for production authentication. |

## 4 Change surface

- Frontend routes/pages/components/hooks/API adapters: no behavioral change expected.
- Backend controller/request/response DTOs: no contract change.
- Service facade methods: no change.
- Service/domain/evaluator/provider implementation: no scoring or provider behavior change.
- DAO queries/entities: adjust only persisted outcome-to-guidance evidence mapping.
- Flyway migrations: add V8; do not edit V1-V7.
- Configuration/environment: retain opt-in `LOCAL_DEMO_MODE`; Gemini key remains backend-only.
- Documentation/diagram/report: update local demo data and migration inventory.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| GET | `/api/v1/assessments/questions` | none | 42 active questions | 5xx |
| POST | `/api/v1/assessments/attempts` | optional temporary `X-Student-Id` | in-progress attempt | validation/5xx |
| GET | `/api/v1/simulations/{slug}/tasks` | none | three public tasks without answer keys | 404/5xx |
| POST | `/api/v1/simulations/{slug}/attempts` | answer map | deterministic evaluated result | 400/404/5xx |
| GET | `/api/v1/guidance/dashboard` | local demo principal | live evidence summary | 401/403/404/5xx |
| POST | `/api/v1/guidance/syn/messages` | message/action/context | AI or Standard guidance | 400/401/403/404/429/5xx |

## 6 Security privacy and AI

- Authentication/role: login is bypassed only for opt-in loopback guidance requests; service checks still require an active STUDENT account.
- Ownership/visibility: US-01/US-02 temporarily use demo student ID 1; production auth remains explicitly out of scope.
- Sensitive data: all seeded records are synthetic; public simulation payloads exclude `correctOption`, explanations, and evaluation rules.
- AI context fields: latest RIASEC signals, at most three skill summaries, three recent results with bounded outcomes, and an optional plan.
- Output schema/validation: unchanged structured provider validation.
- Timeout/fallback: unchanged eight-second default timeout and deterministic Standard fallback.
- Logging/redaction: no API key, raw prompt, assessment answer payload, or private model response is added to seed or logs.

## 7 Tests and evidence

- Domain/policy tests: existing deterministic assessment, simulation, and Standard guidance tests.
- Service tests: existing guidance AI eligibility/fallback tests.
- Controller/API tests: existing endpoint and local-demo authentication tests.
- Repository/migration tests: add PostgreSQL migration/seed assertions and outcome mapping assertions.
- Frontend tests: run the existing frontend quality gate because contracts stay stable.
- End-to-end/manual evidence: fresh Compose volume; inspect migration/counts; call health, questions, each simulation detail, dashboard, Standard quick action, and safe free-text Syn.
- Commands: `backend\\mvnw.cmd verify`, `frontend\\npm run check`, `docker compose config --quiet`, then Compose smoke through `http://localhost:8080`.

Observed on 2026-09-16:

- Backend: 50 tests, 0 failures, 0 errors, 2 opt-in live-provider tests skipped.
- Frontend: 3 test files / 9 tests passed; lint and production build passed.
- PostgreSQL 17 Testcontainers: all V1-V8 migrations applied and both integration tests passed.
- Running Compose stack: four services up; PostgreSQL healthy; `/api/health` returned `UP`.
- Gateway smoke: 42 questions, 5 simulations, 3 safe public tasks per simulation, live Dashboard with 3 signals/3 skills/1 result, Standard quick-action reply, and Gemini free-text reply with `AI` provenance.
- Live US-02 -> US-03 bridge: submitting Frontend Accessibility Review returned 100% and Dashboard immediately exposed three `OBSERVED_STRENGTH` outcome summaries.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Demo bypass could be mistaken for production authentication. | Private endpoints could be exposed if enabled remotely. | Keep disabled by default and restricted to loopback hostnames; document the boundary prominently. | Approved by explicit local-test request. |
| Editing an applied migration would break existing volumes. | Flyway checksum failure. | Add V8 only. | Required by project rules. |
| Seeded evidence could look like real student data. | Privacy/reporting confusion. | Use clearly synthetic identity/content and document BR-17 compliance. | Approved. |
| Existing evaluated JSON uses `title`/`isCorrect`, while US-03 reads `label`/`status`. | New simulation completions do not appear as skills. | Map both persisted formats in the evidence adapter without changing objective scores. | Required for the requested vertical slice. |
