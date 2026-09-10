# US-02 Simulation Frontend Phase 1 Plan

Status: Approved for Phase 2
Owner: Mai Tran Thuy Trang
Date: 2026-09-11

## Scope

- Story: US-02, Backend Developer simulation attempt core slice.
- Actor: Student.
- Acceptance: complete three multiple-choice representative tasks, submit once, and view deterministic total and task outcomes.
- Included: local mock data, in-memory attempt state, client-only mock evaluator, workspace, result screen, and one direct route.
- Excluded: catalog, detail, navigation, autosave, backend/API, authentication, RIASEC, AI guidance, answer-key display before evaluation, and enterprise features.

## Phase 1 flow

1. Student opens `/simulations/backend-developer/attempt`.
2. Student selects one answer for each of the three tasks and submits.
3. The mock evaluator compares selected options with non-rendered answer keys.
4. The result view displays objective outcomes and explanations only after evaluation.

## Decision

The mock evaluator is intentionally frontend-only for this phase at the project lead's direction. It is not an API contract or production scoring authority; integration must move score authority to the backend.

## Phase 2 scope

- Add local catalog, detail, and task navigation routes.
- Keep the mock catalog independent from backend API calls.
- Defer autosave, standalone instructions/resources, and submission confirmation to later phases.
