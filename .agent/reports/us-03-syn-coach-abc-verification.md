# US-03 Syn coach A/B/C verification — 2026-09-29

## Scope and evidence identity

User authorized A/B/C implementation, basic sufficient testing and a manual user guide; voice deferred. Plan: `plans/US-03-syn-coach-abc.md`. Base `1c53253ee1cc1373a78557c3a790298e00613ea0` plus existing uncommitted P1/P2 and this increment. No commit/push or production deployment. Runtime project is `careersimfresh`, gateway 8080; existing PostgreSQL volume preserved.

## Automated gates

| Command/check | Observed result |
|---|---|
| `backend/.\mvnw.cmd -q spotless:apply verify` | Passed, 139 discovered, 135 executed, 4 opt-in live skips; 0 failures/errors. Includes actual PostgreSQL Testcontainers integration. |
| Final targeted `SynCoachPolicyTest,GeminiSynGuidanceProviderTest` after negated-claim and composed-context refinements | Passed. |
| `frontend/npm run check` | Passed lint, 25 tests, production build. |
| `docker compose config --quiet` | Passed. |
| `git diff --check` | Passed; existing CRLF normalization warnings only. |
| `.env` ignore check | Ignored; no credentials in changes or diagnostic output. |

Regression coverage: exact Vietnamese multi-goal prompt; resources in external follow-up with inherited backend path; frontend/backend ordinal order; resource ordinal skill handles; skill/path SQL fallback and URL dedup; bilingual draft content; no consent; one web call per turn across fallback; grounding sources restricted to HTTPS and supported snippets; unreviewed source UI/sandbox; vector + lexical merge; one Gemini retry for 429/503 with budget/Retry-After limits; broad competence rejection and explicit negation permitted; final synthesis receives mandatory backend tool results even when native model planning omitted them. Existing lifecycle, ownership, idempotency, score and schema tests retained.

Initial full-run failures were outdated ordering/tool-execution expectations plus a missing Mockito import, corrected without disabling tests. Initial Docker build failed on truncated Maven Central artifact downloads, then source build succeeded on retry. This transport error was distinct from application test failures.

## Live gateway checks

`GET http://localhost:8080/api/health`: UP after rebuilding backend/frontend and restarting gateway. Synthetic demo student used; no assessments submitted, no plans saved. Initial API results:

| Scenario | Observed result |
|---|---|
| Quick learning resource | STANDARD_POLICY, 3 catalog resources, 3 simulations; no external call. |
| Vietnamese backend resource + simulation + two-week plan | 2 backend resources, 1 simulation, unsaved 3-step draft preserved; generation rejected as MALFORMED_OUTPUT, Standard fallback. |
| Explicit Spring Boot web source request | AI_READY for explanation, 2 local resources; web UNAVAILABLE, 0 web sources. |
| Compare frontend then backend | AI_READY; comparison order frontend, backend. |
| Follow-up “Cho tôi tài nguyên học của hướng đầu tiên” | AI_READY; MDN accessibility and W3C WAI resources, correctly frontend. |

Turn IDs: quick `f521040a-07c0-46e3-ae70-08bc4b351217`, initial mixed `f0f996f8-414d-41ef-a3a1-0c6bea421ef8`, web `0518946e-fb22-4477-bee9-7742559c1470`, comparison `e72a2490-a554-4a5a-8e4d-4d73782b644d`, follow-up `5a036afb-c095-451d-bef0-2f4c39b74d96`. Turn payload retention is bounded, so these IDs are diagnostic references, not permanent fixtures.

A separate sanitized direct request using the configured model and `google_search` returned HTTP **429 / RESOURCE_EXHAUSTED**, with Google stating quota was exceeded. Therefore live web success is **not demonstrated** on the current key; UI/adapter success paths are verified with mocked grounding metadata. No quota/billing/account settings were changed. Provider capability is documented for Gemini 3.1 Flash-Lite in [Google Search grounding](https://ai.google.dev/gemini-api/docs/generate-content/google-search); actual account limits must be inspected in [Google AI Studio](https://aistudio.google.com/usage?timeRange=last-28-days&tab=rate-limit). Generation success does not prove search quota is available.

The first mixed turn made one unsuccessful query-embedding call and one completed generation request, then failed output validation. This report does not infer a model intelligence defect or a specific malformed field from that category alone. Standard recovery retained all requested local capabilities.

Final source-built backend image: `sha256:da3832e9d983907d203fc3c7c966c19f4587fbaa11b979a47edd45b9530b5d16`. Frontend image: `sha256:1b037fa1c07bdca4b2ead0094e3dd77d6ed71140d36efc0402b4f6126c2e0381`. After final backend recreation, health was UP and the same mixed Vietnamese prompt returned **AI / AI_READY**, two resources (Spring REST and PostgreSQL), one backend simulation and an unsaved two-week draft. Turn: `5efa701b-27f5-4245-bbf7-ba45d34b1f32`. Its explanation named the resources and described week-one reading, week-two practice and reflection. This is one successful final smoke, not proof that the earlier intermittent validation fallback is impossible. Runtime published knowledge document count remains zero.

## Acceptance still open

- B content: all 20 corpus documents and held-out labels remain AI-assisted drafts. Publication requires factual/provenance/license/section review; `docs/data/knowledge/README.md` explicitly requires a human reviewer. `scripts/Publish-SynKnowledge.ps1` and `corpus-review.vi.md` prepare the concrete review/publish step. No fake reviewer, seeded draft publication or claimed live corpus recall score.
- C live availability: current search quota rejection prevents live grounded source acceptance; mocked provider tests do not substitute for this.
- Semantic quality and bilingual usefulness need user review. Planner uses controlled keywords/capabilities, resource memory uses bounded skill handles, and plan is a grounded three-step template. This is not a general reasoning loop, full conversation memory, arbitrary web research or exhaustive semantic verification.
- Search queries are limited to controlled IT paths/skills; constraints like arbitrary price, certificate or schedule are not guaranteed. Grounding links/snippets are not independently fetched/verified. Search costs may extend beyond token estimates.
- UI verified by component tests and build; no fresh browser interaction claimed for this increment. User scenarios: `docs/testing/syn-coach-manual.vi.md`.

Technical implementation is reviewable; A/B/C must not be reported as fully accepted while corpus review/publication, live search quota and user quality review remain open.
