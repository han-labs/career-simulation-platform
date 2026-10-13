# AI Assistance Log

Date: 2026-09-25  
Human owner/reviewer: Huỳnh Gia Hân  
User story/foundation item: US-03 Syn Phase 1 and Phase 2 foundation

## Source material read

- Requirements: project-lead request for transactionality, typed bounded routing, failure isolation, and richer reviewed-data integration.
- Design/sequence/domain artifacts: `.agent/AGENTS.md`, project context, tasks, design, relevant rules, existing US-03 plans/reports, and V1-V9 schema.
- Existing code/tests: guidance controller/service/orchestrator/policy/toolbox/providers/DAOs/DTOs, simulation evidence access, unit tests, and PostgreSQL integration tests.

## Assistance used

- Tool/model if course disclosure requires it: OpenAI Codex coding agent.
- Prompt/task summary without secrets or private data: refactor Syn to typed intents/tools, make session/audit updates transactional, isolate approved-tool failures, and ground comparison/gap/simulation artifacts in existing reviewed schema metadata.
- Files/decisions influenced: guidance domain vocabulary, policy plans, toolbox degradation contract, DAO SQL/mappings, additive DTO fields, provider/memory conversions, tests, API note, plan, and verification report.

## Human review

- Behavior manually inspected: code diff and deterministic response construction; automated focused failure-isolation and compatibility tests were executed.
- Security/privacy/AI-boundary review: retained student scoping, consent, bounded context, strict provider output, server-owned artifacts, read-only fixed tools, and sanitized failure logs.
- Architectural responsibility review: retained facade/controller boundaries and single-pass orchestration; persistence SQL stays in DAO adapters and transactions stay in the service.
- Suggestions changed or rejected and why: no autonomous retry/agent loop, provider-selected tools, new migration, or external retrieval was introduced because each exceeds the approved bounded foundation.

## Verification actually run

| Command/check | Result | Evidence location |
|---|---|---|
| `backend\mvnw.cmd -DskipTests compile` | Passed | Maven output in task history |
| `backend\mvnw.cmd -DskipTests test-compile` | Passed | Maven output in task history |
| Focused backend unit suite | 45 passed, 0 failures/errors | Surefire output in task history |
| `backend\mvnw.cmd clean verify` | Build success; 76 discovered, 0 failures/errors, 6 skipped; Spotless and fresh JaCoCo report passed | Surefire/JaCoCo output in task history |
| `frontend npm run check` | ESLint, 15 tests, and production build passed | npm output in task history |
| `docker compose config --quiet` | Passed | Compose output in task history |
| `git diff --check` | Passed | Git output in task history |

## Remaining limitations

- Docker was unavailable, so four PostgreSQL Testcontainers tests were skipped; the new database SQL still needs a Docker-enabled verification run before release.
- Two live-provider tests remain opt-in and were not executed.
- No commit, deployment, or live gateway smoke was requested or performed.

No credentials, private student inputs, unrestricted prompts, or raw model responses are included.
