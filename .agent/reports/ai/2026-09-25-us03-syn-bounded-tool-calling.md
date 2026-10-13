# AI Assistance Log

Date: 2026-09-25  
Human owner/reviewer: Huỳnh Gia Hân  
User story/foundation item: US-03 Syn bounded tool-calling agent

## Source material read

- Requirements: project-lead Phase 3 prompt and US-03 bounded-agent rules.
- Design/sequence/domain artifacts: `.agent` project context, task board, design, architecture/GRASP/facade/security/testing/report rules, Phase 1–2 plan/report, and the Phase 3 feature plan.
- Existing code/tests: provider port, Gemini/OpenAI REST adapters, orchestrator, static policy, failure-isolated toolbox, DTOs, service tests, provider tests, configuration, architecture/API/deployment documentation.
- External technical reference: official Google Gemini function-calling and Generate Content REST documentation for `functionDeclarations`, `functionCall`, and `functionResponse` shapes.

## Assistance used

- Tool/model if course disclosure requires it: OpenAI Codex coding agent.
- Prompt/task summary without secrets or private data: implement a two-pass Gemini-native function-calling workflow with an enum allowlist, execute server-owned tools, synthesize validated JSON, and fail closed to the existing static Java plan.
- Files/decisions influenced: provider-neutral contract, Gemini REST bodies/parsers, two-pass orchestration, enum descriptions, failure classifications, model fallback configuration, unit tests, architecture/API documentation, plan, and verification report.
- Regression correction: preserve and replay Gemini's opaque pass-1 model parts so Gemini 3.x `thoughtSignature` metadata survives the bounded tool-call continuation. The opaque value remains request-scoped and is neither logged nor persisted.

## Human review

- Behavior manually inspected: provider request/response bodies via mock server assertions, orchestrator plan selection/fallback behavior, unchanged response DTOs, and sanitized logs.
- Security/privacy/AI-boundary review: no tool arguments, unknown/duplicate/count validation, two-pass limit, no SQL/URL access, consent and ownership retained, bounded memory/evidence, no raw provider errors.
- Architectural responsibility review: provider JSON remains in the infrastructure adapter; orchestration/limit/fallback stays in the application controller; static plan stays in the domain policy; tool execution remains in the toolbox.
- Suggestions changed or rejected and why: an unrestricted `while` ReAct loop and provider-generated tool parameters were rejected because they weaken the approved bounded boundary; OpenAI native parity was deferred because it was optional and would enlarge the change surface.

## Verification actually run

| Command/check | Result | Evidence location |
|---|---|---|
| `backend\.\mvnw.cmd -DskipTests compile` | Passed | Maven output in task history |
| `backend\.\mvnw.cmd -DskipTests test-compile` | Passed | Maven output in task history |
| Focused provider/policy/service suite | 42 tests passed at that checkpoint | Surefire output in task history |
| Focused Gemini/service regression suite | 27 tests passed, including thought-signature replay | Surefire output in task history |
| Opt-in live Gemini suite | 2 tests passed, including a real bounded planning/synthesis exchange | Surefire output in task history |
| `backend\.\mvnw.cmd verify` | Build success; 85 tests, 0 failures/errors, 3 opt-in provider tests skipped; PostgreSQL Testcontainers, Spotless, and JaCoCo passed | Maven/Surefire/JaCoCo output in task history |
| `frontend npm run check` | ESLint, 15 tests, and production build passed | npm output in task history |
| `docker compose config --quiet` | Passed | Compose output in task history |

## Remaining limitations

- The rebuilt local Compose backend currently resolves to `AI_PROVIDER=disabled` because no project-root `.env` is present. An end-to-end gateway AI smoke therefore awaits user-supplied runtime configuration; no secret or env file was edited.
- Live-provider tests remain opt-in during normal `verify`.
- No commit or PR was requested or performed.

No credentials, private student inputs, unrestricted prompts, or raw model responses are included.
