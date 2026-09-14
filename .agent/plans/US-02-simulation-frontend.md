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


- Frontend only, mock data
- No backend, no AI, no RIASEC

## Phase completion
- Phase 1: Core vertical slice ✅
- Phase 2: Catalog + Detail + Navigation ✅
- Phase 3: Confirmation dialog ✅
- Phase 4: Traceability docs ✅

## Files delivered
[list tất cả file]

## Behavior
- Deterministic scoring via `simulationEvaluator.js`
- No answer key before submit
- Confirmation dialog before final submit
- Route dynamic `simulations/:slug/attempt`
- Responsive 320px, WCAG 2.2 AA basics

## Limitations
- No autosave (removed)
- No component tests (RTL not available)
- Mock data only
