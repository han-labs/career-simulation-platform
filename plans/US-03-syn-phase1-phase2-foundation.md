# US-03 Syn Phase 1 and Phase 2 Foundation Plan

Status: Implemented  
Owner: Huỳnh Gia Hân / guidance subsystem  
Date: 2026-09-25

## 1 Scope

- User story or foundation item: US-03 bounded Syn agent robustness and richer reviewed-data integration.
- Actor: Authenticated active student.
- Source proposal/SRS section: `docs/requirements/user-stories.md` US-03 and the project-lead request dated 2026-09-25.
- Acceptance criteria: atomic message persistence; typed intents and tool identifiers; isolated tool failures; metadata-driven path/skill analysis; richer simulation cards; existing flows and bounded single-pass behavior preserved.
- Explicit exclusions: multi-agent orchestration, autonomous loops, arbitrary provider-selected tools, web browsing, vector retrieval, score changes, new personal-data collection, and schema rewrites.
- Related owner boundaries: guidance backend and compatible guidance DTO/frontend normalization only; assessment and simulation scoring remain unchanged.

## 2 Flow inventory

### Main success flow

1. The service assembles owned evidence and starts one transaction for the Syn message use case.
2. The policy resolves a typed intent and a bounded typed tool plan.
3. The toolbox executes each approved tool independently and continues when one tool degrades.
4. Path comparison and skill-gap observations use reviewed path/skill metadata.
5. Published simulation suggestions include summary, difficulty, and estimated duration.
6. The optional provider or deterministic fallback returns the existing structured response contract.
7. Session memory and the guidance audit commit together.

### Alternate flows

1. Quick actions continue to use Standard guidance without a provider call.
2. Empty path/skill metadata returns exploratory empty artifacts instead of inventing matches.
3. Existing response consumers remain compatible through additive DTO fields.

### Exception and fallback flows

1. A single DAO/tool failure is logged by tool identifier, recorded as degraded activity, and does not abort the remaining plan.
2. Provider timeout/rejection/malformed output continues to return Standard guidance.
3. Any uncaught failure outside the isolated tool/provider boundaries rolls back session and audit writes.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Typed intent vocabulary | Domain value type | `SynIntent` | Removes string drift across policy, orchestrator, memory, and toolbox. |
| Typed tool vocabulary | Domain value type | `SynToolIdentifier` | Makes the allowlist compile-time visible. |
| Static routing | Domain policy | `SynAgentPolicy` | Preserves deterministic bounded planning. |
| Failure-isolated tool execution | Application collaborator | `SynAgentToolbox` | Owns tool execution and typed degradation results. |
| Reviewed metadata queries | DAO | `SynAgentDao` / `JdbcSynAgentDao` | Keeps SQL and persistence mapping out of policy/toolbox decisions. |
| Transaction boundary | Service implementation | `GuidanceServiceImpl` | Owns atomic session/audit orchestration. |

## 4 Change surface

- Frontend routes/pages/components/hooks/API adapters: additive simulation-card normalization/rendering only if required by tests.
- Backend controller/request/response DTOs: additive `SimulationCard` metadata; no route change.
- Service facade methods: no signature change.
- Service/domain/evaluator/provider implementation: typed intent/tools and failure-isolated toolbox.
- DAO queries/entities: path profiles include RIASEC/skill metadata; simulation options include summary/difficulty/duration; task-skill evidence query.
- Flyway migrations: none; reuse V9 schema.
- Configuration/environment: none.
- Documentation/diagram/report: this plan and final command evidence; no topology change.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| POST | `/api/v1/guidance/syn/messages` | Existing request contract | Existing response plus additive simulation metadata | Existing validation/auth/server errors |

## 6 Security privacy and AI

- Authentication/role: existing authenticated active `STUDENT` checks remain.
- Ownership/visibility: evidence and session queries remain student-scoped; catalog metadata remains published/reviewed only.
- Sensitive data: no raw answers, evaluation rules, prompts, or credentials added.
- AI context fields: only typed server-owned observations; bounded counts remain.
- Output schema/validation: provider `text`/`suggestions` contract remains unchanged.
- Timeout/fallback: provider fallback retained; individual tool failures now degrade locally.
- Logging/redaction: log tool identifier and exception class/message only; never raw message or evidence payload.

## 7 Tests and evidence

- Domain/policy tests: typed intent and exact typed tool-plan coverage.
- Service tests: existing AI/fallback/ownership flows plus tool failure isolation.
- Controller/API tests: additive response compatibility.
- Repository/migration tests: V1-V9 queries for path skills and rich simulation metadata.
- Frontend tests: run existing guidance normalization tests; add assertions only if contract normalization changes.
- End-to-end/manual evidence: bounded response through existing gateway if the stack is available.
- Commands to run: `backend/.\\mvnw.cmd verify`, `frontend/npm run check`, `docker compose config --quiet`.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Existing dirty worktree overlaps Syn files. | Accidental loss of team changes. | Apply focused patches only and preserve all unrelated diffs. | Approved by proceeding on current working tree. |
| A tool failure may leave partial artifacts. | Misleading activity/result combinations. | Isolate each tool and return an explicit degraded activity entry with empty artifacts for that tool. | Approved. |
| Metadata matching may broaden evidence. | Ungrounded recommendations. | Use reviewed V9 path/skill metadata and evaluated owned outcomes only. | Approved. |
| DTO enrichment could break frontend assumptions. | Rendering/test regression. | Make fields additive and update normalization defensively. | Approved. |

## 9 Implementation evidence

- Backend `mvnw.cmd clean verify`: passed with 76 tests discovered, 0 failures, 0 errors, and 6 environment/opt-in skips. Four PostgreSQL Testcontainers tests were skipped because no Docker runtime was available; two live-provider tests remain opt-in.
- Focused typed-policy and failure-isolation suite: passed 45 tests with 0 failures/errors.
- Frontend `npm run check`: passed ESLint, 15 Vitest tests, and the Vite production build.
- `docker compose config --quiet`: passed.
- `git diff --check`: passed.
- The implementation remains one deterministic typed plan, sequential approved-tool execution, and at most one provider call; it introduces no autonomous loop or model-selected tool execution.
