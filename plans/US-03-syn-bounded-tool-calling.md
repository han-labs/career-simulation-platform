# US-03 Syn Bounded Tool-Calling Agent Plan

Status: Implemented  
Owner: Huỳnh Gia Hân / guidance subsystem  
Date: 2026-09-25

## 0 Regression correction: Gemini 3 tool continuation

- Observed defect: every Gemini 3.x bounded exchange fell back after pass 1 because the adapter reduced the provider response to `List<SynToolIdentifier>` and reconstructed the model function-call turn without Gemini's required `thoughtSignature`.
- Required correction: retain the validated provider continuation parts as an opaque, request-scoped value in `ToolPlan`, replay them unchanged in pass 2, and never persist or log that value.
- Compatibility: manually constructed/provider-neutral plans without continuation metadata keep the existing reconstruction path; the two-pass limit, enum allowlist, deterministic policy, tool isolation, API contract, and database schema remain unchanged.
- Regression evidence: add an adapter test that includes `thoughtSignature` in pass 1 and proves it is present in the pass-2 request, plus an opt-in live bounded two-pass test.

## 1 Scope

- User story or foundation item: US-03 Phase 3 bounded LLM tool calling.
- Actor: Authenticated active student with recorded AI consent.
- Source proposal/SRS section: Project-lead Phase 3 request dated 2026-09-25 and US-03.
- Acceptance criteria: Gemini receives the approved tool declarations; selects a bounded typed tool plan; backend executes it through the existing failure-isolated toolbox; Gemini receives bounded tool results and returns validated `AiReply`; no more than two provider passes occur; every unusable agent exchange falls back to the existing deterministic plan and Standard guidance.
- Explicit exclusions: unrestricted ReAct loops, model-generated SQL, arbitrary tool parameters, write tools, score changes, provider-side persistence, background agents, multi-agent orchestration, web search, RAG, and removal of `SynAgentPolicy`.
- Related owner boundaries: backend guidance/provider code and configuration only; public API, frontend ownership, objective assessment/simulation logic, and database schema remain unchanged.

## 2 Flow inventory

### Main success flow

1. The service loads the owned dashboard and bounded Syn session in the existing transaction.
2. Java computes the deterministic decision as a safety plan but does not execute it yet when Gemini tool calling is eligible.
3. Provider pass 1 receives the untrusted user message, bounded memory metadata, and the server allowlist with descriptions; Gemini returns one to four native function calls.
4. The adapter validates every function name against `SynToolIdentifier`, rejects arguments and unknown tools, and returns a typed plan.
5. The toolbox executes the typed plan sequentially with Phase 1 failure isolation.
6. Provider pass 2 receives native function responses containing bounded execution status and observations, with further function calling disabled.
7. Gemini returns the existing schema-validated `AiReply`; server-owned cards and memory/audit persistence remain unchanged.

### Alternate flows

1. AI disabled, no consent, quick action, safety boundary, or confirm-plan requests use the deterministic flow without a provider call.
2. A configured provider without tool-calling capability keeps the existing one-pass, deterministic-tool compatibility path.
3. One selected tool may degrade while other tools and synthesis continue; all selected tools degrading triggers deterministic fallback.

### Exception and fallback flows

1. Timeout, transport/provider rejection, malformed planning/final output, unknown tool, non-empty tool arguments, too many tools, or a new tool request on pass 2 triggers the deterministic plan and Standard guidance.
2. The provider is never called a third time; a pass-2 function call is classified as an iteration-limit failure.
3. Fallback records `FALLBACK` / `REPLACED_BY_FALLBACK`, preserves objective evidence, and persists the normal bounded session state.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Tool vocabulary and descriptions | Domain value type | `SynToolIdentifier` | One compile-time allowlist shared by policy and provider declarations. |
| Provider-neutral two-pass contract | Application port | `SynGuidanceProvider` | Prevents Gemini JSON types from leaking into orchestration. |
| Native Gemini request/response mapping | Infrastructure adapter | `GeminiSynGuidanceProvider` | Owns function declarations, calls/responses, and provider schema validation. |
| Two-pass limit and fallback transition | Application controller | `SynAgentOrchestrator` | Owns use-case flow, provider capability selection, and deterministic recovery. |
| Read-tool execution | Application collaborator | `SynAgentToolbox` | Reuses existing typed, sequential, failure-isolated execution. |
| Static safety plan | Domain policy | `SynAgentPolicy` | Remains authoritative when the external agent exchange is unusable. |

## 4 Change surface

- Frontend routes/pages/components/hooks/API adapters: none.
- Backend controller/request/response DTOs: none.
- Service facade methods: none.
- Service/domain/evaluator/provider implementation: provider tool-calling contract, Gemini native adapter, explicit two-pass orchestration, tool descriptions.
- DAO queries/entities: none.
- Flyway migrations: none.
- Configuration/environment: Gemini default model becomes `gemini-2.0-flash-lite` in Spring, Compose, and example environment configuration.
- Documentation/diagram/report: API/architecture behavior note, implementation verification report, and AI-assistance log.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| POST | `/api/v1/guidance/syn/messages` | Unchanged | Unchanged structured Syn response and server-owned artifacts | Unchanged; provider failures are internal Standard fallback |

## 6 Security privacy and AI

- Authentication/role: existing active-student and ownership checks remain before any provider call.
- Ownership/visibility: the LLM receives no database connection, SQL, user identifier, or unrestricted record access.
- Sensitive data: tool calls have no parameters; backend derives scope from authenticated server state.
- AI context fields: user message, bounded versioned session summary, language/stage/depth, allowlisted descriptions, bounded tool observations, and selected evidence only.
- Output schema/validation: tool names, empty arguments, call count, final JSON fields, lengths, and pass-2 no-more-tools rule are enforced.
- Timeout/fallback: existing HTTP timeout plus an application maximum of two provider passes; all listed failures run the deterministic plan.
- Logging/redaction: log only classified failure kind, never raw prompts, function payloads, private evidence, or provider bodies.

## 7 Tests and evidence

- Domain/policy tests: enum descriptions remain non-blank and policy plans remain unchanged.
- Service tests: successful two-pass exchange, selected-tool execution, two-call bound, malformed/unknown/iteration failure transition, all-tools-degraded transition, and legacy fallback.
- Provider tests: Gemini declarations, native function-call parsing, function responses, final JSON schema, unknown tool rejection, and pass-2 tool-call rejection.
- Controller/API tests: existing contract suite unchanged.
- Repository/migration tests: existing suite; no database change.
- Frontend tests: existing regression gate only; no contract change.
- End-to-end/manual evidence: opt-in live provider only when key/runtime are available; otherwise record blocker.
- Commands to run: `backend/.\mvnw.cmd clean verify`, `frontend/npm run check`, `docker compose config --quiet`, `git diff --check`.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Native function calling could create an open-ended loop. | Cost, latency, and uncontrolled behavior. | Exactly two provider passes; pass 2 disables tools and any returned call fails closed. | Approved by request. |
| Model may request arbitrary names/arguments. | Boundary bypass. | Empty-parameter declarations plus strict enum/name/count validation. | Approved. |
| Provider may fail after tools already ran. | Partial read observations exist. | Tools are read-only; discard AI text and rerun the deterministic safety plan. | Approved. |
| OpenAI parity increases scope. | Additional adapter/test surface. | Keep its existing compatibility path; Gemini is the required Phase 3 implementation. | Approved by “optionally OpenAI”. |
| Gemini model default differs from current repository value. | Runtime selection changes. | Apply the explicitly requested `gemini-2.0-flash-lite` fallback consistently. | Approved by request. |

## 9 Implementation evidence

- `backend\.\mvnw.cmd clean verify`: build success; 84 tests discovered, 0 failures, 0 errors, 6 skipped; Spotless and fresh JaCoCo report passed.
- Focused tool-calling suite: 42 tests passed before the final two boundary tests were added; the final full suite contains all new cases.
- `frontend npm run check`: ESLint, 4 files / 15 tests, and the Vite production build passed.
- `docker compose config --quiet`: passed.
- Four PostgreSQL Testcontainers tests were skipped because Docker was unavailable; two live-provider tests remain opt-in.
- No live Gemini request, latency measurement, deployment, or gateway smoke was performed in this increment.
