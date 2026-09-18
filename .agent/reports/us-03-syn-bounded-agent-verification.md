# US-03 Syn Bounded Agent Verification

Date: 2026-09-17
Owner/reviewer: Huỳnh Gia Hân
Scope: upgrade Syn from one-shot evidence-grounded chat to a bounded career-exploration agent.

## Implemented outcome

- Added deterministic intent routing for RIASEC interpretation, result review, evidence gaps, path comparison, career questions, learning suggestions, and plan drafting.
- Added a fixed tool allowlist for owned evidence, reviewed path profiles, published simulations, reviewed learning resources, and plan drafts.
- Added student-owned session memory with a 30-day expiry. Only a structured summary, last intent, and path codes are stored; raw chat is not stored.
- Added contextual follow-up: a question such as “Which of those should I test first?” reuses the paths from the preceding comparison.
- Kept Gemini as the primary optional language provider. Free text makes at most one provider call; quick actions use deterministic tools and spend no provider tokens.
- Kept Standard fallback feature-complete for tool artifacts when provider configuration, consent, quota, timeout, or output validation fails.
- Added visible activity, evidence references, path comparison cards, reviewed resource cards, memory state, and a clear-memory control to the global three-mode widget.
- Kept objective scores immutable and kept plan persistence behind the existing explicit confirmation flow.

## Traceability

| Requirement | Implementation |
|---|---|
| Bounded memory and contextual follow-up | `syn_sessions`, `SynAgentDao`, `SynAgentOrchestrator`, widget session ID |
| Path comparison and career questions | `SynAgentPolicy`, `SynAgentToolbox`, `career_path_profiles` |
| Skill/evidence gaps | dashboard evidence plus `FIND_SKILL_GAPS` tool plan |
| Reviewed learning suggestions | `learning_resources`, DAO filter `status = 'REVIEWED'`, resource cards |
| Provider efficiency | quick actions skip provider; one call maximum for eligible `FREE_TEXT`; bounded context and output |
| Fallback | tool result exists before provider call; provider errors retain Standard response and artifacts |
| Explainability | response `intent`, `activity`, `evidenceReferences`, and visible provenance |
| Safety/privacy | fixed tool allowlist, ownership predicates, no raw chat, no provider database access, clear-memory endpoint |

## Observed verification

| Check | Observed result |
|---|---|
| `frontend npm run check` | Passed: ESLint, 10 Vitest tests, Vite production build |
| `backend mvnw.cmd verify` | Passed: 63 tests, 0 failures/errors, 2 opt-in live-provider tests skipped; Docker/PostgreSQL integration tests included; Spotless and JaCoCo passed |
| `docker compose config --quiet` | Passed |
| Rebuilt Compose stack | Backend/frontend images built; four services running |
| Flyway startup | 9 migrations validated; V9 applied successfully to PostgreSQL 17 |
| Health/dashboard through gateway | `UP`; dashboard source `LIVE` |
| Standard path comparison | intent `COMPARE_PATHS`, provenance `STANDARD`, 2 path cards, 3 activity items, 2 evidence references |
| Contextual Gemini follow-up | intent `CAREER_QUESTION`, provenance `AI`, same session ID, 2 remembered paths |
| Learning-resource tool | 2 reviewed resource cards returned; first provider Spring |
| Clear memory | DELETE endpoint returned `true` |

## Boundaries retained

- Syn does not make a final career decision, certify competence, predict employment, grade work, or alter assessment/simulation results.
- The provider does not select SQL, browse arbitrary URLs, run code, or persist a plan.
- Long-term inferred user profiles, vector search, autonomous loops, multi-agent work, and local/open-weight inference remain outside this increment.
- Local demo authentication remains loopback-only, opt-in, and unsuitable for deployment.

## Demo script

1. Open `/dashboard`, launch Syn, and observe `Evidence ready`.
2. Click **Compare backend and frontend**. Expand **What Syn checked** and inspect the two path cards and evidence chips. This is Standard and does not spend a Gemini request.
3. Ask: **Which of those should I test first, and why?** The answer should be AI-assisted, retain both compared paths, and avoid choosing a career for the student.
4. Ask: **What evidence am I missing before I decide?** Syn should connect the response to the current simulation outcomes.
5. Click **Suggest a learning resource** or ask: **Suggest a learning resource for SQL.** Verify that reviewed links appear as structured cards.
6. Ask: **Draft a three-step plan to test backend work next week.** Review the draft, then use explicit confirmation only if it should be saved.
7. Click the trash icon in the Syn header. The messages and server-side structured session memory should clear without deleting assessment or simulation evidence.
8. To observe fallback, temporarily set `AI_PROVIDER=disabled`, rebuild/restart backend, and repeat a free-text question. The source badge should read **Standard guidance** while comparison/resource cards remain usable.
