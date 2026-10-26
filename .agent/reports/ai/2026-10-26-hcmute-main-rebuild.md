# AI Assistance Log

Date: 2026-10-26
Human owner/reviewer: requester authorizes cross-cutting frontend work; named review/sign-off pending
User story/foundation item: main-derived HCM-UTE frontend flow alignment; current 10 US/16 UC catalogue, existing selected UC-01–09 only

## Source material read

- Current corrective request and supplied approved screenshots/hcmute.zip; old rejected implementation inspected only for differences, never copied as functional source.
- Root/.agent mandatory entry/context/tasks/design/all eight rules, current requirements index/selected US/UC/assignments/Baseline/DR1–DR2/alignment, relevant API/architecture and tests.
- Latest origin/main 608e812 actual entry/router/shared identity/API/state/feature implementation, separately documented before UI edits in the functional audit and template-based plan.

## Assistance used

- Tool: Codex, GPT-6-based coding agent; shell, Git, Docker/Maven/npm, bundled Playwright with installed Chrome. No subagents, Sites hosting, external AI model/provider, image generation or source upload.
- Task: rebuild approved visual layout from latest main while preserving functional callbacks/state/routes/security/business ownership; establish baseline, document flows, implement, test actual workflows, commit on new branch.
- Influenced files/decisions: exact per-component mapping and final file inventory in [verification report](../hcmute-main-rebuild-verification.md). Main auth/features/backend retained, shared SimulationCard compact option and SimulationCatalog lifecycle views reused, approved visual text/assets and original navigation/actions preserved.

## Human review

- Agent source review and automated real-browser operation are recorded; no human manual test or named human acceptance is claimed.
- Security/privacy/AI boundary: backend authority unchanged; ignored generated local credentials/JWT and synthetic disposable data only; no raw logs/JSON/screenshots staged. No live Google/LLM quality claim.
- Architecture: existing modular frontend/shared HTTP → backend facades/services/DAO → DB; no new API/schema/persistence/provider logic. Business-owned assessment/simulation/identity/Agent/Enterprise/Admin sources remain unchanged.
- Rejected prior account menu, custom preview-card/domain logic and removal of legacy assessment state/actions. Retained main direct Home/Guidance/logout and server-authenticated role checks.
- Corrected generated test selector whitespace/ambiguous Enterprise Title and two-pixel native scroll-snap expectations, initial test syntax/unused import, and one missing VI journey label. Kept all existing tests; did not weaken auth, role or scoring contracts.
- New route-level test and real main browser revealed pending simulation redirect race (D13); did not silently change identity behavior to make a new expectation pass. Proposed frontend alias-results workaround was not applied because feature business ownership remains separate (D14). Classification500 (D12) likewise not fixed under presentation authorization.
- Real identity verification initially used local profile; main's intentional loopback Admin fallback was traced and disabled via an isolated verification runtime profile, without changing security code. Automation pacing was increased after429; rate policy stayed unchanged.

## Verification actually run

| Command/check | Observed result | Evidence |
|---|---|---|
| Clean-main npm ci; default/enabled npm run check, two workers | 249 tests/31 files each, lint/build pass | ignored baseline-frontend*.log; audit |
| Clean-main and final .\mvnw.cmd verify | Both 404 discovered,398 passed, six existing skips, zero failures/errors; build/Spotless pass | ignored baseline-backend/final-backend.log |
| Final default/enabled frontend checks | 260 tests/32 files each, lint/build pass | ignored final-frontend*.log |
| Existing guarded opt-in local quiz bootstrap | Pass; actual reviewed versions used live | ignored assessment-fixture.log |
| Unique Compose config/build/up, gateway health | Pass; separate volumes/ports, unchanged backend | ignored stack logs; execution report |
| Actual Chrome Student, Enterprise, Admin and negative role/ownership APIs | Ten checks completed, no page errors; preserved D13/D14 baseline failures separately | ignored final-live-evidence.json; execution report |
| VI/EN desktop/tablet/mobile visual/browser checks | 10 VI/EN width combinations and 3 real role headers; zero page errors/overflow/broken assets | ignored final-visual-evidence.json and screenshots |
| Git fetch/diff/protected-source/preservation checks | Main unchanged at 608e812; old branch untouched/unmerged; exact final commits in report | Git history and execution report |

## Remaining limitations

D12 classification 500, D13 pending return and D14 Dashboard alias results are pre-existing main gaps requiring their business owners. External Google, assigned live AI/consent/SSE/tool/plan workflows and 20-case human/model quality acceptance are not claimed by this UI task. Human review, identity of responsible reviewer and shared release approval remain pending. No push or main merge performed.

Tested application commit: 5f526358763bb93d02a1eeeff0ec1885ea9b8902; exact frontend tree deaaab23da60e5e9eea1fab0b0147253091704fa. Served enabled JS/CSS matched the committed build after commit. The following documentation commit preserves that application tree.
