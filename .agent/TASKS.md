# Career Simulation Platform Task Board

## 0 Ownership boundary

The supplied assignment is explicitly a **frontend assignment**:

| Member | Frontend ownership | Required collaboration boundary |
|---|---|---|
| Võ Nguyễn Ngọc Bích | US-01 RIASEC assessment and landing page | Coordinate API contracts for assessment questions, autosave, submit, score, and directions |
| Mai Trần Thùy Trang | US-02 career simulation | Coordinate catalog/detail, attempt lifecycle, task submission, and deterministic result contracts |
| Huỳnh Gia Hân | Project/frontend setup and US-03 Dashboard/Syn | Own shared setup changes carefully; coordinate evidence, AI/fallback, provenance, and Syn contracts |

Backend, database, AI integration, enterprise-management, and authentication ownership were not assigned in the supplied material. Do not infer an owner.

Each owner should still verify the full vertical slice with the responsible backend contributor before calling a frontend story complete.

## 1 Recommended implementation order

1. **Foundation:** repo conventions, CI, Docker/PostgreSQL, API envelope, catalog slice, security decision.
2. **Identity foundation:** authentication, current-user context, roles, profile, consent.
3. **US-01:** assessment question model, deterministic scoring, API, frontend, tests.
4. **US-02:** simulation details, attempts, three-task backend demo, deterministic evaluator strategies, frontend, tests.
5. **US-03:** controlled evidence context, deterministic feedback first, AI provider adapter, schema validation, fallback, frontend, evaluation cases.
6. **Enterprise management:** owned content lifecycle after student flow contracts are stable.
7. **Integration/deployment:** full Docker slice, Ubuntu VM, evidence, report and diagram reconciliation.

## 2 Current foundation status

| Item | Status | Evidence |
|---|---|---|
| Repository and agent knowledge base | Ready | root and `.agent` documentation |
| React/Vite shell and landing starter | Ready for feature iteration | frontend build/lint/test gates |
| Spring Boot/PostgreSQL foundation | Ready | catalog facade/controller/DAO/entity and Flyway migrations |
| Public simulation catalog API | Ready foundation slice | service/controller/architecture tests |
| Docker Compose and Nginx | Ready for smoke verification | compose config and health/catalog endpoints |
| Authentication/profile | Not implemented | planned foundation dependency |
| US-01 workflow | Preliminary backend/frontend integration | deterministic assessment API and connected frontend from the latest team pull; full acceptance audit remains |
| US-02 workflow | Preliminary backend/frontend integration | catalog/attempt/evaluation API and connected frontend from the latest team pull; full acceptance audit remains |
| US-03 workflow | Backend/frontend and optional AI adapter ready | `/dashboard`, global three-mode Syn, protected guidance API, minimized evidence, consent-gated structured provider output, Standard fallback, plan persistence, provenance and tests; opt-in localhost demo identity verified while runtime login remains pending |
| Enterprise management | Not implemented | boundary reserved |

## 3 Definition of Done for every story

- Approved plan and sequence-to-code trace exist under `plans/`.
- Main, alternate, exception, permission, status, and fallback flows are implemented.
- Business rules live in domain/service code; controllers and React components remain adapters/views.
- API uses documented DTOs and stable error codes/messages.
- Flyway migration is additive and tested when schema changes.
- Service/domain tests cover each decision boundary.
- Controller/API tests verify status and representation independently.
- Frontend covers loading, success, empty, validation, retry, and unavailable states.
- Accessibility includes semantic controls, keyboard use, visible focus, labels, errors, and reduced-motion support.
- `backend verify`, `frontend check`, Compose validation, and relevant smoke checks pass.
- AI feature has normal, ambiguous, malformed, unsafe/out-of-domain, timeout, and provider-failure evaluation cases.
- Sequence/domain/architecture artifacts match the implementation.
- Evidence and course report are updated without fabricated measurements.
- AI assistance and human review are logged.

## 4 US-01 frontend scope

Owner: **Võ Nguyễn Ngọc Bích**

- Landing-page iteration within `frontend/src/features/landing`.
- Assessment intro, questions, progress, validation, resume, submit, and result screens.
- Six-dimension score visualization with text equivalents.
- Clear non-deterministic-language disclaimer.
- API contract and state handling without client-side score authority.

Required tests: question navigation, invalid/missing answer, resume, submit success/failure, six-score display, no-AI/fallback result display, accessible labels and keyboard flow.

## 5 US-02 frontend scope

Owner: **Mai Trần Thùy Trang**

- Catalog/filter integration and simulation detail.
- Start/resume attempt, task navigation, autosave, submission confirmation, locked/completed states.
- Objective total and task-level outcome presentation.
- No rendering of answer keys before evaluated completion.

Required tests: catalog states, unpublished inaccessible, start/resume, task validation, network retry, submit, evaluated result, answer-key non-disclosure.

## 6 US-03 frontend scope and setup responsibility

Owner: **Huỳnh Gia Hân**

- Maintain shared app/router/API/style setup without breaking another feature boundary.
- Dashboard loading, ready, preview/fallback, partial, and retry states.
- Syn conversation, quick actions, inline result cards, plan drafting, limitations, and source/provenance.
- Never hide fallback or imply AI certainty.

Required tests: AI success, fallback, timeout, malformed response mapped to fallback, empty evidence, source label, and objective score stability.

## 7 Foundation and backlog items requiring assignment

- Authentication/profile/consent.
- Assessment backend and scoring.
- Simulation attempt/evaluator backend.
- Guidance context/provider/fallback backend.
- Enterprise content management.
- End-to-end automation and deployment ownership.
- Final report integration and presentation ownership.

## 8 PR checklist

Copy `.github/pull_request_template.md` and fill every section. A PR with missing trace source, tests, evidence, report impact, or known gaps is not ready for review.
