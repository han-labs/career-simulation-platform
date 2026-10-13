# US-03 Syn Bounded Tool-Calling Verification

Date: 2026-09-25  
Owner/reviewer: Huỳnh Gia Hân  
Branch/commit: current working tree; no commit requested  
Verdict: READY; runtime environment still requires explicit AI configuration

## 1 Scope and source

This increment implements the approved Phase 3 request to make Gemini-selected native tool calls the primary eligible AI path while retaining the complete static `SynAgentPolicy` plan as the deterministic safety net. It preserves one HTTP request/response lifecycle, existing response artifacts, objective evidence, session ownership, and the confirmed-plan write boundary.

Excluded behavior remains unchanged: arbitrary SQL/URLs, parameterized model tools, write tools, score changes, web/RAG retrieval, background execution, multi-agent coordination, and open-ended ReAct loops.

## 2 Implemented flows

- Main: for consented safe `FREE_TEXT` with an available Gemini adapter, provider pass 1 receives bounded session context and all typed tool declarations; it returns one to four native function calls. Java validates and executes the typed plan through `SynAgentToolbox`. Provider pass 2 receives native function responses and must return the existing validated `AiReply` JSON.
- Alternate: non-AI, no-consent, quick-action, safety-boundary, and confirm-plan requests execute the unchanged static plan. Providers without tool-calling support retain the previous deterministic-tools plus one-pass synthesis compatibility flow.
- Tool degradation: one degraded tool is recorded while successful selected tools may continue; all selected tools degrading skips synthesis and activates the deterministic safety plan.
- Fallback: timeout/transport, provider rejection, malformed output, unavailable tool, non-empty arguments, duplicate/excess calls, and a pass-2 tool request all return Standard guidance with `REPLACED_BY_FALLBACK` audit status.

## 3 Sequence-to-code and design-to-code mapping

| Sequence step | Responsibility | Code |
|---|---|---|
| Build allowlist | Typed domain expert | `SynToolIdentifier.description()` |
| Pass 1 tool planning | Provider port and Gemini adapter | `SynGuidanceProvider.requestTools`, `GeminiSynGuidanceProvider.planningRequestBody/parseToolPlan` |
| Validate bound | Application controller plus adapter defense | `SynAgentOrchestrator.runBoundedToolCalling`, Gemini call parser |
| Execute actions | Existing isolated application collaborator | `SynAgentToolbox.execute` |
| Pass 2 synthesis | Gemini adapter | `GeminiSynGuidanceProvider.synthesisRequestBody/synthesize` |
| Fail closed | Application controller/domain policy | `SynAgentOrchestrator` catch path plus unchanged `SynAgentPolicy` decision |
| Persist memory/audit | Existing transactional use case | `GuidanceServiceImpl.sendMessage`, session/audit DAOs |

The provider-neutral port carries only enums, bounded observations, dashboard DTO values, and final text/suggestions. Gemini JSON remains inside the infrastructure adapter.

## 4 API and database impact

- Public endpoints, requests, responses, `SimulationCard`, `PlanDraft`, and controller/facade signatures are unchanged.
- No database entity, query, table, or Flyway migration was introduced.
- The runtime configuration default is now `${GEMINI_MODEL:gemini-2.0-flash-lite}` consistently in Spring, Compose, `.env.example`, setup/deployment documentation, and the opt-in live test.
- OpenAI remains on the compatible one-pass adapter path; native OpenAI tool calling was explicitly optional and is not part of this increment.

## 5 Security, privacy, and AI-boundary decisions

- Tools are parameterless native declarations. Authentication, ownership, paths, attempt IDs, and database scope are derived only from server state.
- The adapter rejects unknown names, arguments, duplicates, empty plans, and more than four tools. The orchestrator validates plan size/uniqueness again.
- `MAX_PROVIDER_PASSES` is exactly 2. Pass 2 sends `mode: NONE`; any returned `functionCall` is classified as `ITERATION_LIMIT` and falls back without a third call.
- Only bounded session codes, the user message, typed descriptions, selected evidence, and bounded observations are sent. Credentials, IDs, SQL, answer keys, evaluation rules, and raw provider failures remain excluded.
- Tools remain read-only. A failed synthesis cannot create a partial write; the surrounding service transaction still owns session and audit persistence.

## 6 Verification evidence

| Check | Command/scenario | Observed result | Evidence |
|---|---|---|---|
| Backend full | `backend\.\mvnw.cmd verify` | Build success; 85 tests, 0 failures/errors, 3 opt-in provider tests skipped; PostgreSQL Testcontainers, Spotless, and JaCoCo passed | Maven/Surefire/JaCoCo output in task history |
| Gemini native contract | Mock REST planning and synthesis | Function declarations, `ANY`, function responses, `NONE`, and final JSON schema asserted | `GeminiSynGuidanceProviderTest` |
| Gemini 3 continuation regression | Mock pass 1 returns per-call `thoughtSignature` values | Pass 2 replays both signatures unchanged; 27 focused tests passed | `GeminiSynGuidanceProviderTest`, focused Maven output |
| Live bounded exchange | Opt-in `GeminiSynGuidanceLiveTest` using the configured key/model | Real planning plus synthesis completed; 2 tests passed | Maven/Surefire output in task history |
| Unavailable tool | Gemini returns `QUERY_DATABASE` | Classified `UNAVAILABLE_TOOL` | `GeminiSynGuidanceProviderTest` |
| Pass limit | Synthesis returns another function call | Classified `ITERATION_LIMIT`; orchestrator returns Standard guidance | Provider and service tests |
| Selected-tool execution | LLM selects only `FIND_RESOURCES` | Resource DAO invoked; static `SUGGEST_SIMULATIONS` not invoked; exactly planning plus synthesis calls | `GuidanceServiceTest` |
| Total degradation | Selected comparison tool throws | Synthesis skipped; static safety plan used; degradation remains visible | `GuidanceServiceTest` |
| Frontend regression | `frontend npm run check` | ESLint passed; 4 files / 15 tests passed; production build passed | npm/Vitest/Vite output in task history |
| Compose syntax | `docker compose config --quiet` | Exit code 0 | Compose output in task history |

## 7 Manual/integration evidence

The live test reproduced the provider's real two-pass contract and passed after the continuation fix. Before the fix, the same Gemini 3.x synthesis request returned HTTP 400 because the reconstructed model turn omitted `thoughtSignature`; the adapter classified that response as `PROVIDER_REJECTED`, so the orchestrator correctly but repeatedly activated Standard guidance.

The rebuilt local Compose backend was also inspected. It currently starts with `AI_PROVIDER=disabled`, `GEMINI_MODEL=gemini-2.0-flash-lite`, and no project-root `.env` file, so that particular runtime cannot perform an AI smoke test and will intentionally use Standard guidance. No secret or `.env` file was changed during this correction.

## 8 Deviations and unresolved gaps

- The user requested Gemini tool calling and made OpenAI support optional; OpenAI was intentionally kept on the existing compatible path.
- The running Compose stack needs a root `.env` (or explicit shell/`--env-file` values) with `AI_PROVIDER=gemini`, a key, and the desired model before an end-to-end gateway AI smoke can be meaningful.
- Existing presentation artifacts in the dirty working tree still discuss an earlier model-selection comparison and were not rewritten as part of this backend correction.

## 9 Files and artifacts changed

- Application: `SynToolIdentifier`, `SynAgentPolicy`, `SynGuidanceProvider`, `SynAgentOrchestrator`, and `GeminiSynGuidanceProvider`.
- Configuration: `application.yml`, `.env.example`, `docker-compose.yml`, README and deployment instructions.
- Tests: Gemini adapter, policy/tool descriptions, bounded two-pass orchestration, failure transitions, and live-test default.
- Documentation/evidence: architecture/API context, project checkpoint/task board, feature plan, this report, and AI-assistance log.
