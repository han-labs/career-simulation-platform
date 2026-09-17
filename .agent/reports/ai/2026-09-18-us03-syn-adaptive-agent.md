# AI Assistance Log

Date: 2026-09-18
Human owner/reviewer: Huỳnh Gia Hân
User story/foundation item: US-03 Syn adaptive bounded-agent enhancement

## Source material read

- Project authority: `.agent/AGENTS.md`, `.agent/PROJECT_CONTEXT.md`, `.agent/TASKS.md`, `.agent/DESIGN.md`, and relevant architecture, frontend, backend, database, security, testing, reporting, and AI rules.
- Existing implementation: Syn policy/orchestrator/toolbox/providers/DTOs/tests, guidance frontend API/widget/styles/tests, V9 session schema, and US-03 plan/report.
- Human decisions: support newcomers without fabricated evidence; English/Vietnamese replies; adaptive but bounded detail; useful structured memory; no forced scroll-to-bottom; implement all three approved stages.

## Assistance used

- Tool/model if disclosure is required: OpenAI Codex coding agent; Gemini remains the application's configured runtime language provider.
- Work influenced: request/response language contract, Vietnamese intent routing, newcomer flow, adaptive token budget, structured memory state, provider prompt constraints, deterministic fallback, conversation anchoring/progressive display, tests, and synchronized documentation.
- No API keys, private student data, raw prompts, or unrestricted model traces were placed in project files.

## Human-control and quality review

- The human chose the scope and approved all three stages before code work.
- Objective scoring and evidence ownership remained outside the model boundary.
- Deterministic unit/integration tests, schema validation, fallback, code formatting, production build, Compose checks, gateway smokes, and direct browser inspection were used to validate generated changes.
- Raw provider streaming, unrestricted tools, raw-chat memory, autonomous plan execution, and a new agent framework were rejected because they reduce explainability or add risk without improving the course MVP enough.

## Verification actually run

| Command/check | Result |
|---|---|
| `backend\\mvnw.cmd verify` | Passed: 67 tests, 0 failures/errors, 2 live-provider tests skipped; Spotless, JaCoCo, and PostgreSQL integration passed |
| `frontend npm run check` | Passed: ESLint, 11 Vitest tests, Vite production build |
| `docker compose config --quiet` | Passed |
| Rebuilt local stack and gateway API smoke | Passed |
| Sanitized live Gemini Vietnamese follow-up | Passed with `AI` provenance and same owned session |
| Browser scroll/language/cards/provenance inspection | Passed; no forced bottom jump and explicit `Jump to latest` worked |

## Remaining limitations

- Runtime identity/login is pending; localhost demo identity is not deployable authentication.
- Progressive reveal starts after validated completion, so it is not true low-latency streaming.
- Memory is bounded exploration-session context, not a general personal memory system.
- Reviewed resources/catalog records are finite and do not constitute unrestricted current web search.
