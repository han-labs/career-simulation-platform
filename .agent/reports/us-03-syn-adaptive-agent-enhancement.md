# US-03 Syn Adaptive Agent Enhancement Verification

Date: 2026-09-18
Owner/reviewer: Huỳnh Gia Hân
Scope: bilingual onboarding, adaptive bounded memory/responses, and reader-controlled conversation UX.

## Implemented outcome

- Removed the unconditional scroll-to-bottom behavior. A submitted question is anchored near the top of the conversation, the growing answer leaves the viewport in place, and `Jump to latest` is offered when unread content exists below.
- Added progressive reveal after the complete provider response passes backend schema validation. Reduced-motion users receive the complete response immediately. This is intentionally not raw provider-token streaming.
- Added `AUTO`, `EN`, and `VI` reply modes. The deterministic policy recognizes common Vietnamese, unaccented shorthand, and IT abbreviations, and session memory preserves the resolved language for short follow-ups.
- Added an honest `GET_STARTED` flow for students without evidence. Reviewed path, simulation, and resource records may explain options, but Syn cannot claim a personal strength, gap, or fit without student evidence.
- Added bounded response depth: simple result/RIASEC questions use a short budget; onboarding, comparisons, planning, and explicit requests for more detail receive a larger bounded budget.
- Upgraded memory inside the existing `syn_sessions.summary` column to a compact versioned state: language, stage, goal, covered topics, open follow-up, proposed next action, and completed exploration-step count. Raw chat remains unstored.
- Kept the agent bounded: deterministic routing, approved read tools, at most one provider call, server-owned cards, explicit plan confirmation, visible provenance, and deterministic fallback.

## Three implementation stages

### Stage 1 — usability and language

`SynWidget` owns anchoring, progressive display, reduced-motion behavior, the language selector, and the optional jump control. `guidanceApi` sends the language preference and normalizes the new response metadata. `SynAgentPolicy` resolves language and understands Vietnamese shorthand without asking the LLM to route the request.

### Stage 2 — onboarding and adaptive guidance

`GET_STARTED` is a first-class action/intent. `SynAgentToolbox` supplies reviewed CareerSim paths and simulations, and `StandardGuidancePolicy` provides equivalent English/Vietnamese fallback. Both Gemini and OpenAI adapters receive explicit language, stage, response depth, covered topics, and deterministic observations. Token ceilings remain bounded and vary by task complexity.

### Stage 3 — structured memory and agent continuity

`SynAgentOrchestrator` reads/writes a code-only state rather than concatenated dialogue. It tracks exploration stage, current goal, covered topics, open follow-up, proposed next action, and objective step progress. The state helps Syn avoid repeating the same explanation and makes contextual follow-up useful without creating an opaque long-term personality profile.

## Traceability

| Concern | Main implementation |
|---|---|
| No forced bottom jump | `SynWidget` anchor ref, overflow detection, `Jump to latest` |
| Safe progressive display | `SynWidget.revealReply`; full response is validated before the UI animation |
| Vietnamese/English and shorthand | `SynAgentPolicy`, provider adapters, `StandardGuidancePolicy`, language selector |
| No-evidence newcomer | `GET_STARTED`, `SynAgentOrchestrator`, `SynAgentToolbox` |
| Adaptive length/token use | policy response depth and provider-specific output budget |
| Bounded memory | versioned summary in `SynAgentOrchestrator`; existing owner-scoped DAO/session |
| Safety and fallback | fixed tool plan, provider schema validation, Standard policy, separate confirmed-plan endpoint |

## Observed verification

| Check | Observed result |
|---|---|
| `frontend npm run check` | Passed: ESLint, 11 Vitest tests, Vite production build |
| `backend mvnw.cmd verify` | Passed: 67 tests, 0 failures/errors, 2 opt-in live-provider tests skipped; Spotless, JaCoCo, and PostgreSQL integration included |
| `docker compose config --quiet` | Passed |
| Rebuilt Compose stack | Backend/frontend rebuilt; gateway, frontend, backend, and healthy PostgreSQL running |
| Vietnamese gateway message | `so sanh FE vs BE giup minh` resolved `COMPARE_PATHS`, `VI`, `PLANNING`, and returned two reviewed path cards |
| Contextual follow-up | `cai con lai thi sao?` reused the same session and returned a Vietnamese AI-assisted response |
| Standard newcomer flow | Explicit `GET_STARTED` returned useful Vietnamese Standard onboarding and no fabricated personal claim |
| Browser anchoring | Long reply grew conversation height while `scrollTop` stayed at the submitted-question anchor; no automatic bottom jump |
| Browser jump control | `Jump to latest` appeared for content below the viewport and reached the bottom only after clicking |
| Memory clear | Existing owner-scoped DELETE returned `true` and did not delete objective evidence |

## Retained boundaries and known limits

- Syn still does not decide a career, certify a skill, predict employment, grade work, alter scores, or autonomously execute a plan.
- Cards, references, resources, simulations, and objective progress are server-owned. The LLM only synthesizes natural language from minimized observations.
- Progressive reveal improves reading behavior but does not reduce provider first-token latency because raw tokens are not streamed before validation.
- There is no arbitrary web browsing, vector database, multi-agent loop, background execution, or long-term inferred personal profile in this increment.
- Runtime login/registration remains outside this increment; the opt-in localhost demo principal is for internal testing only.

## Focused demo script

1. Open `/dashboard`, open Syn, choose `VI`, and ask: `mình mới vào chưa bt bắt đầu sao, FE với BE khác nhau ntn?`.
2. Observe that the submitted question stays in view while the response grows. Use `Jump to latest` only when ready.
3. Ask: `cái còn lại thì sao?` and confirm that Syn keeps the language and path context without requiring the full question again.
4. Ask: `nói rõ hơn nhưng ngắn thôi, mình nên thử gì trước?` and inspect the reviewed comparison/simulation cards and visible evidence/provenance.
5. Clear Syn memory. Objective RIASEC and simulation results must remain unchanged.
6. Disable the provider and repeat a question. The answer should remain usable in Vietnamese with `Standard guidance` provenance.
