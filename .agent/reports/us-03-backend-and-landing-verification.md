# US-03 Backend and Landing Simplification Verification

Date: 2026-09-13  
Commit: working tree; not committed or pushed by this task

## 1 Scope and source

- Implemented the backend contract for `docs/requirements/user-stories.md` US-03:
  Dashboard aggregation, structured Syn Standard guidance, inline result evidence,
  guidance provenance, and explicit current-plan save.
- Lightly simplified the authorized landing page using `.agent/DESIGN.md` and
  `.agent/theforage.com.design.md` as structural references.
- Did not implement authentication/JWT, US-01 scoring, US-02 evaluation, a selected
  external LLM provider, chat history, or a separate result report/detail page.

## 2 Implemented flows

- Dashboard returns live partial/empty states when completed evidence does not yet
  exist and combines only the authenticated student's completed/evaluated records.
- Syn supports six action types, bounded free text, owned attempt context, inline
  objective result cards, suggestions, and unsaved plan drafts.
- Standard guidance handles missing/ambiguous evidence, protected-material requests,
  prompt injection phrases, and delegated career-choice requests without changing
  objective scores.
- Confirmed plans are validated and upserted; draft generation alone does not persist.
- Guidance audit is written only when completed evidence exists and excludes raw
  prompts, raw answers, evaluation rules, and provider errors.
- Landing now follows hero -> three-step loop -> starter catalog -> footer. The
  redundant track section, duplicate closing CTA, and non-essential micro-copy were
  removed; primary actions and catalog behavior remain.

## 3 Sequence maps to code

`GuidanceController` adapts HTTP and principal name; `GuidanceService` is the facade;
`GuidanceServiceImpl` enforces account/ownership policy and transactions;
`StandardGuidancePolicy` owns deterministic response decisions; focused identity,
assessment, and simulation access interfaces own safe reads; `GuidanceDao` owns plan
and provenance persistence. See `.agent/plans/US-03-sequence-to-code-trace.md`.

## 4 API and database impact

- Added protected `GET /api/v1/guidance/dashboard`.
- Added protected `POST /api/v1/guidance/syn/messages`.
- Added protected `POST /api/v1/guidance/plans`.
- Added Flyway V3 with `exploration_plans`, `guidance_reports.action_type`, and
  `guidance_reports.generation_ms`.
- Did not create dashboard or chat-message tables.

## 5 Security, privacy, and AI boundary

- Spring Security requires authentication for all guidance endpoints; direct gateway
  request without authentication returned `401`.
- Role, `ACTIVE` status, and attempt ownership are rechecked server-side.
- No student ID is accepted from the browser and no production demo identity is
  hard-coded.
- External AI remains disabled because provider selection and runtime authentication /
  consent enforcement are not complete. API provenance is `STANDARD`; persisted source
  is `FALLBACK`. This is a complete deterministic path, not an AI-success claim.

## 6 Tests and exact commands

| Command/check | Observed result |
|---|---|
| `backend\\mvnw.cmd test` before PostgreSQL integration test | PASS: 30 tests |
| Selected PostgreSQL persistence/service tests | PASS: 8 tests; Flyway V1-V3, plan upsert, and audit insert verified on PostgreSQL 17 |
| `backend: .\\mvnw.cmd spotless:apply verify` | PASS: 33 tests, PostgreSQL integration, architecture, package, Spotless, and JaCoCo report |
| `frontend: npm run check` | PASS: lint, 9 tests, production build |
| `docker compose config --quiet` | PASS |
| `docker compose up -d --build` | PASS: backend/frontend images built; four services running |
| Gateway `/api/health` | PASS: `success=true`, backend status `UP` |
| Gateway `/api/v1/simulations` | PASS: 5 published simulations |
| Gateway `/api/v1/guidance/dashboard` without auth | PASS: expected `401` |
| PostgreSQL Flyway history | PASS: V1, V2, and V3 recorded successful |

## 7 Manual UI evidence

- Browser DOM at 1440 x 900: three main landing sections, no horizontal overflow,
  both hero actions visible within the initial viewport, footer grid rendered in two
  columns.
- Browser DOM at 320 x 844: no horizontal overflow, both hero actions 277 px wide,
  mobile menu visible, footer navigation rendered in two balanced columns.
- Visual mobile inspection confirmed the simulation/catalog transition and complete
  professional footer remain readable.
- Browser console inspection returned no warning or error entries.

## 8 Deviations and next action

- Full authenticated runtime smoke for the three guidance endpoints is blocked by the
  explicitly out-of-scope identity/login backend. Controller and service behavior are
  covered with authenticated test principals; the gateway correctly denies anonymous
  access.
- US-01 and US-02 backend writers must persist assessment scores and structured task
  outcomes before the live Dashboard contains evidence.
- Selecting and enabling a real AI provider still requires consent enforcement,
  explicit output schema validation, timeout/failure classification, and provider
  evaluation. Standard mode remains the safe operational path.
