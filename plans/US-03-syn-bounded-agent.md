# US-03 Syn Bounded Agent Plan

Status: Completed and verified
Owner: Huỳnh Gia Hân
Date: 2026-09-18

## 1 Scope

- User story: extend Syn from one-shot evidence-grounded chat into a bounded career-exploration agent that can remember the current conversation, select approved tools, compare exploration paths, identify possible skill gaps, answer career questions, recommend reviewed learning resources, and propose a next exploration action.
- Actor: Student.
- Source: US-03 in `docs/requirements/user-stories.md` plus the project-lead decisions on 2026-09-17.
- Acceptance criteria: contextual follow-up through session memory; evidence-grounded personalized debrief; skill-gap interpretation; path comparison; career-question support; reviewed learning resources; simulation/next-action suggestion; visible tool activity and evidence references; explicit confirmation before plan persistence; Gemini as primary provider; deterministic Standard fallback preserving the same core tools.
- Explicit exclusions: career prediction, recruitment/job matching, autonomous background work, unrestricted web browsing, vector database, proprietary model training, local/open-weight runtime, multi-agent architecture, direct LLM-to-database access, and silently persisted inferred personal facts.
- Related owner boundaries: changes remain inside US-03 frontend/guidance backend plus additive guidance metadata. US-01/US-02 scoring, evaluation rules, and owned screens are unchanged.

## 2 Flow inventory

### Main success flow

1. The frontend creates or reuses a Syn session ID.
2. The backend derives the active student and loads only that student's evidence and session memory.
3. The intent router classifies the question and builds a bounded tool plan.
4. Approved deterministic tools inspect evidence, compare paths, find gaps, and retrieve simulations/resources.
5. Safe free text with consent is sent to Gemini with minimized tool observations and the conversation summary.
6. The response returns a concise answer, provenance, activity summary, evidence references, and optional comparison/resource/plan cards.
7. The session stores a short contextual summary; plan persistence still requires a separate confirmed request.

### Alternate flows

1. Quick actions use the same tool plan but skip Gemini and return Standard guidance.
2. A contextual follow-up such as "what about the second path?" reuses the previous intent and compared paths from the session.
3. The student can clear Syn memory without deleting objective assessment/simulation results.
4. Missing evidence produces an exploration-oriented next step instead of a fabricated conclusion.

### Exception and fallback flows

1. Unsafe/out-of-domain or prompt-injection input is stopped before provider invocation.
2. Missing consent, disabled provider, quota rejection, timeout, malformed output, or provider error returns a deterministic response built from the same tool observations.
3. Unknown or expired session IDs create a new owned session; another student's session is never reused.
4. Resource and simulation suggestions are restricted to reviewed/active and published records.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Intent classification and bounded plan | Domain policy | `SynAgentPolicy` | Keeps agent decisions deterministic, testable, and provider-independent. |
| Execute approved evidence/catalog tools | Application collaborator | `SynAgentToolbox` | Central allowlist; no arbitrary SQL or provider-selected code execution. |
| Orchestrate memory, tools, provider, validation | Service/application | `SynAgentOrchestrator` | Owns the agent state-action-observation sequence while `GuidanceServiceImpl` remains the subsystem facade. |
| Session/resource persistence | DAO | `SynAgentDao`, `JdbcSynAgentDao` | Queries and upserts only; no guidance decisions. |
| Optional natural-language synthesis | Provider adapter | existing `GeminiSynGuidanceProvider` | Gemini receives minimized structured observations, never database access. |
| Render agent artifacts | React view | existing `SynWidget` | Keeps UI changes in the owned US-03 component. |

## 4 Change surface

- Frontend: `SynWidget.jsx`, `guidanceApi.js`, relevant US-03 CSS/tests.
- Backend controller/request/response DTOs: session ID in request; richer response; clear-session endpoint.
- Service facade methods: message orchestration and clear-session operation.
- Service/domain/provider: agent policy, orchestrator/toolbox, provider context/prompt update, deterministic fallback.
- DAO: owned session memory, curated path/resource metadata, tool queries.
- Flyway migrations: V9 adds session memory and curated guidance metadata/mappings.
- Configuration/environment: Gemini remains primary through existing variables; no new secret.
- Documentation/diagram/report: requirements, API/database/architecture, verification report, AI assistance log.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| POST | `/api/v1/guidance/syn/messages` | `{message, actionType, sessionId?, context?}` | agent response with session, intent, provenance, artifacts | 400/401/403/404/429/5xx |
| DELETE | `/api/v1/guidance/syn/sessions/{sessionId}` | none | memory cleared | 401/403/404/5xx |
| POST | `/api/v1/guidance/plans` | confirmed plan | persisted plan | 400/401/403/5xx |

## 6 Security privacy and AI

- Authentication/role: existing authenticated principal and active STUDENT checks remain mandatory; localhost demo principal remains opt-in and loopback-only.
- Ownership/visibility: every session query includes `student_id`; clear-memory is ownership checked.
- Sensitive data: memory stores only bounded summary, last intent/paths, and pending action; no raw conversation, assessment answers, prompts, keys, or chain-of-thought.
- AI context fields: top signals, at most three results/outcomes, bounded tool observations, curated metadata, and short session summary.
- Output schema/validation: existing structured provider schema plus length/count bounds; server-owned artifacts are not accepted from provider output.
- Timeout/fallback: existing timeout; at most one provider call and a bounded deterministic tool plan.
- Logging/redaction: log intent/tool names/status only, not message, prompts, evidence payloads, or raw provider output.

## 7 Tests and evidence

- Domain/policy tests: at least ten intent/safety/follow-up/tool-plan cases.
- Service tests: AI success, Standard quick action, provider failure, memory follow-up, ownership, empty evidence, and unchanged objective scores.
- Controller/API tests: extended response and clear-session contract.
- Repository/migration tests: V1-V9, seed counts, session ownership/upsert/delete, reviewed resource filtering.
- Frontend tests: normalization of agent artifacts, session reuse, fallback, and memory clearing.
- End-to-end/manual evidence: path comparison, skill gaps, career question, learning resources, follow-up, plan confirmation, memory reset, provider fallback.
- Commands: backend verify, frontend check, Compose validation, rebuilt stack smoke through Nginx, sanitized live Gemini case.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| No GPU on the developer laptop. | Local model would add latency/setup risk. | Keep Gemini primary; local/open-weight provider remains future evaluation scope. | Approved. |
| Agent may become an uncontrolled loop. | Cost, latency, and unsafe actions. | Deterministic intent/tool plan, maximum four tools, one provider call, no background execution. | Approved. |
| Memory may retain incorrect/private information. | Privacy and misleading context. | Store only short session state with expiry and user-accessible clear action; long-term inferred memory deferred. | Approved. |
| Model could invent resources or evidence. | Broken links and ungrounded advice. | Resources, comparisons, references, and action cards are server-owned tool results; Gemini only synthesizes language. | Approved. |
| Too many abstractions harm explainability. | Difficult course presentation. | One policy, one orchestrator, one toolbox, one DAO boundary; no agent framework dependency. | Approved. |

## 9 Completion evidence

- Backend: `mvnw.cmd verify` passed with 63 tests, 0 failures/errors, 2 opt-in live-provider tests skipped, Spotless clean, and JaCoCo report generated. Docker-backed PostgreSQL integration tests applied V1-V9 and passed.
- Frontend: `npm run check` passed ESLint, 10 Vitest tests, and the Vite production build.
- Deployment: `docker compose config --quiet` passed; rebuilt four-service stack is running; Flyway upgraded the persistent local database from V8 to V9.
- Gateway smoke: health `UP`, Dashboard `LIVE`, Standard comparison returned two paths plus evidence/activity, Gemini contextual follow-up returned `AI` with the same session and two remembered paths, reviewed resources returned, and session deletion returned `true`.
- Detailed evidence: `.agent/reports/us-03-syn-bounded-agent-verification.md`.

## 10 Follow-up: bilingual onboarding, adaptive memory, and conversation UX

Source: project-lead review on 2026-09-18.

### Approved behavior

1. Keep the product UI English while Syn can answer in `AUTO`, English, or Vietnamese. `AUTO` reuses the bounded session language for ambiguous short messages and otherwise detects Vietnamese conservatively.
2. A student with no evidence receives honest onboarding from reviewed CareerSim path/catalog knowledge. Syn must not invent a personalized strength, gap, or career fit.
3. Responses use bounded adaptive depth: concise for simple evidence questions, more explanatory for onboarding, comparison, and plan questions. Quick actions retain deterministic Standard guidance.
4. Session memory remains a compact structured summary only. It may retain language, exploration stage, current goal, covered topics, considered paths, an open follow-up code, and a proposed next-action code. It must not retain unrestricted raw chat or silently inferred sensitive facts.
5. The frontend anchors the submitted question, progressively reveals validated reply text, never forces the viewport to the bottom while the student is reading, and offers a `Jump to latest` control when content grows below the viewport.
6. Syn may proactively offer one contextual follow-up or reviewed next action, but it cannot execute background work or persist a plan without explicit confirmation.

### Added flow and fallback acceptance criteria

- Vietnamese, common unaccented Vietnamese, and common IT abbreviations route to the same approved tools as their English equivalents.
- Provider prompts answer in the resolved session language; Standard fallback and onboarding remain useful in both languages.
- Empty evidence selects a `GET_STARTED` flow and reviewed paths/simulations, not a fabricated evidence interpretation.
- Memory updates are deterministic, length-bounded, owner-scoped, expire under the existing 30-day policy, and remain clearable.
- Provider output is still fully schema-validated before progressive display. This enhancement intentionally does not stream unvalidated provider tokens.
- Reduced-motion users receive the complete validated message without a typing animation.

### Change surface

- Existing files only where practical: `SynAgentPolicy`, `SynAgentOrchestrator`, `SynAgentToolbox`, provider adapters, request/response DTOs, `SynWidget`, guidance API adapter, US-03 CSS and tests.
- No database migration is required: the existing bounded `syn_sessions.summary` field stores a versioned structured summary within its current 1,200-character limit; existing `last_intent` and `last_paths` columns remain authoritative routing fields.
- The existing REST route remains compatible. `responseLanguage` is an optional request field, and the response reports the resolved language and exploration stage.

### Added verification

- Policy tests: Vietnamese/accentless intent, abbreviations, explicit/automatic language, contextual follow-up, and no-evidence onboarding.
- Service/provider tests: adaptive language/depth prompt context, structured memory evolution, fallback language, schema bounds, and unchanged score/tool constraints.
- Frontend tests: request language propagation and response normalization; manual checks cover anchored viewport, progressive reveal, `Jump to latest`, keyboard behavior, and reduced motion.

### Completion evidence for the follow-up

- Backend: `mvnw.cmd verify` passed with 67 tests, 0 failures/errors, and 2 opt-in live-provider tests skipped; Spotless, JaCoCo, and PostgreSQL integration verification passed.
- Frontend: `npm run check` passed ESLint, 11 Vitest tests, and the Vite production build.
- Deployment: `docker compose config --quiet` passed; backend/frontend images were rebuilt and all four services ran with PostgreSQL healthy.
- Gateway API: Vietnamese shorthand selected `COMPARE_PATHS`, resolved `VI`, returned `PLANNING`, reused the same session for a contextual follow-up, and produced an AI-assisted Vietnamese reply. Explicit `GET_STARTED` and memory deletion also passed.
- Browser: the submitted question remained anchored while the long answer increased the scroll height; `Jump to latest` appeared and reached the bottom only after explicit activation. Language selection, evidence cards, provenance, follow-ups, and three-mode controls remained accessible.
- Detailed evidence: `.agent/reports/us-03-syn-adaptive-agent-enhancement.md`.
