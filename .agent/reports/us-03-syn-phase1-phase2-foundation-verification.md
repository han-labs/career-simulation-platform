# US-03 Syn Phase 1 and Phase 2 Foundation Verification

Date: 2026-09-25  
Owner/reviewer: Huỳnh Gia Hân  
Branch/commit: current working tree; no commit requested  
Verdict: READY WITH CAVEAT

## 1 Scope and source

This increment implements the approved US-03 Syn robustness and reviewed-data integration plan. It adds an atomic message-use-case boundary, typed intents and tool identifiers, isolated tool degradation, metadata-driven career-path and skill-gap evidence, and richer simulation cards. It excludes autonomous loops, model-selected tools, web retrieval, scoring changes, new personal-data collection, and database migrations.

## 2 Implemented flows

- Main: `GuidanceServiceImpl.sendMessage` executes within one transaction; the deterministic policy selects a typed intent and static typed tool plan; each approved tool runs sequentially; session memory and the audit write commit together.
- Alternate: quick actions and provider-ineligible requests continue through Standard guidance; additive simulation metadata preserves existing response consumers.
- Exception: each toolbox operation catches its own runtime failure, logs only the typed identifier and exception class, records a degraded activity/result, and permits remaining tools and fallback to complete.
- Fallback: existing provider timeout, rejection, configuration, consent, safety, and schema-validation failures still return deterministic Standard guidance.

## 3 Design-to-code mapping

| Design responsibility | Implementation |
|---|---|
| Typed bounded vocabulary | `SynIntent`, `SynToolIdentifier` |
| Deterministic static plan | `SynAgentPolicy` |
| Sequential isolated tool execution | `SynAgentToolbox` |
| Atomic use-case boundary | `GuidanceServiceImpl.sendMessage` |
| Reviewed metadata access | `SynAgentDao`, `JdbcSynAgentDao`, `JdbcSimulationEvidenceAccess` |
| Memory/provider conversion | `SynAgentOrchestrator`, Gemini/OpenAI guidance adapters |

The existing bounded architecture remains intact: one policy pass, a fixed allowlist plan, sequential reads, at most one provider call, and deterministic fallback.

## 4 API and database impact

- Endpoint paths and service facade signatures are unchanged.
- `SimulationCard` adds nullable/additive `summary`, `difficulty`, and `estimatedMinutes`; compatibility constructors retain existing tests and consumers.
- Result evidence now carries exact `careerTrack`, replacing title-substring path inference.
- Existing `career_path_profiles.riasec_codes`, `career_path_profiles.skill_codes`, `career_path_profiles.summary`, `simulation_task_skills`, and rich `career_simulations` columns are queried; no migration was added.
- Skill observations remain constrained to evaluated attempts owned through the already-scoped evidence flow.

## 5 Security, privacy, and AI behavior

- Authentication, student ownership checks, AI consent, response schema validation, provider timeout, and visible provenance are unchanged.
- The model receives only bounded server-owned observations. Raw answers, answer keys, provider errors, credentials, and unrestricted traces are not exposed.
- Failure logs contain the tool identifier and exception type, not student messages or evidence payloads.
- Tools remain server-selected and read-only; neither the provider nor client can introduce arbitrary tool calls.

## 6 Verification evidence

| Check | Command/scenario | Observed result | Evidence |
|---|---|---|---|
| Backend focused | `mvnw.cmd -Dtest=SynAgentPolicyTest,GuidanceServiceTest,StandardGuidancePolicyTest test` | 45 tests passed; 0 failures/errors | Surefire output |
| Backend full | `mvnw.cmd clean verify` | Build success; 76 tests discovered, 0 failures/errors, 6 skipped; Spotless and fresh JaCoCo report passed | Maven/Surefire/JaCoCo output |
| Frontend | `npm run check` | ESLint passed; 4 files / 15 tests passed; Vite build passed | npm/Vitest/Vite output |
| Compose syntax | `docker compose config --quiet` | Exit code 0 | Compose output |
| Diff hygiene | `git diff --check` | Exit code 0 | Git output |
| Failure isolation | DAO exception during path comparison | Standard response returned, comparison empty, degradation visible, later result tool and session update completed | `GuidanceServiceTest` |
| Typed routing | action/stored parsing and exact enum plans | Passed | `SynAgentPolicyTest` |

## 7 Architecture and report reconciliation

No deployment topology or cross-layer ownership changed, so the existing architecture diagram remains accurate. The implementation follows the existing controller-to-service-to-orchestrator/policy/toolbox-to-DAO/provider boundaries. The API documentation now records additive simulation metadata.

## 8 Deviations and known gaps

- Four PostgreSQL Testcontainers tests were skipped because Docker was unavailable in this execution environment. Their source assertions cover the new path, skill, and simulation metadata mappings, but the new PostgreSQL SQL was not executed against a live container in this run.
- Two external live-provider tests remain intentionally opt-in and were skipped.
- The working tree contained unrelated pre-existing changes; this increment used focused edits and did not reset them.
- Next action: run `backend\mvnw.cmd clean verify` on a host with Docker available before release to obtain live PostgreSQL evidence.

## 9 Changed artifacts

- Application: typed Syn domain enums; policy, toolbox, orchestrator, provider, service transaction boundary, DAO/evidence mappings, and additive response DTO metadata.
- Tests: typed-policy parsing/plan assertions, transaction-boundary assertion, tool failure-isolation behavior, and rich metadata repository/service assertions.
- Migrations: none.
- Documentation/evidence: feature plan, API contract note, this verification report, and the AI assistance log.
