# AI Assistance Log — website EN/VI

Date: 2026-10-13
Human owner/reviewer: project lead requested shared frontend localization; wording review by feature/content owners recommended.
Foundation item: EN/VI presentation across current US-01, US-02 and US-03 routes.

## Source material read
- Current user request; root AGENTS and `.agent/AGENTS.md`, PROJECT_CONTEXT, TASKS, DESIGN.
- Testing, security/privacy/AI, architecture, AI-assisted development rules and feature-plan template.
- Current root/shell, landing, assessment hooks/content, simulation adapters/workflow, dashboard, Syn API/widget and tests.
- Actual localhost public questions/catalog/tasks and synthetic dashboard; original V4/V5/V8 seed text.

## Assistance used
- Codex: shared React context, reviewed source-to-Vietnamese dictionaries, accessible preference selector, presentation-only JSX localization, responsive header and tests.
- Temporary local Babel mechanical rewrite preserved IDs, handlers, routes, styles and workflow state; script stays ignored under `.codex-tmp`.
- Server post-evaluation mapper adds optional explanationVi. No scoring, service transaction, persistence ownership, auth, migration, consent or RAG publication changes.
- Explicit user request authorizes presentation changes across frontend owners. No external translation API or paid model invocation used for this task.

## Human review
- Browser inspected landing, assessment intro, catalog/detail/attempt, dashboard and Syn controls in Vietnamese; switched selected simulation answer to English and back without losing its option ID/value.
- Preference survived reload. 320px landing/dashboard had no horizontal overflow. Temporary viewport override reset.
- Raw messages, user drafts, model source quotes, links and provider HTML remain original; UI preference governs future Syn requests unless explicitly overridden.
- Rejected: DOM mutation translation, remote translation, translating SQL/HTTP tokens, resetting workflows, shipping additional evaluated explanations in frontend bundles.
- Existing local three-task backend mock/evaluator and temporary identity mechanism remain pre-existing limitations.

## Verification actually run
| Check | Result |
|---|---|
| Frontend npm run check | 56 passing; lint and production build passed |
| Backend mvnw.cmd spotless:apply verify | 149 discovered / 145 executed / 4 opt-in skips; zero failures/errors; BUILD SUCCESS |
| Compose configuration and git diff --check | Passed |
| Translation coverage | All 42 prompts, 48 additional options, 24 additional public titles/instructions and 15 server-side evaluated explanations verified |
| Evaluated contract | Added explanationVi only after evaluation; original fields/score unchanged; no explanation in public task DTO; absent optional field in persisted service DTO |
| Syn component tests | EN/VI request preference, explicit override, raw transcript/draft/source preservation; retry retains original request language/idempotency ID after a UI language change |
| Browser | EN/VI current pages, reload persistence, answer preservation, responsive menu and 320px no-overflow |

First intermediate frontend coverage check caught a missing View Simulation translation; fixed. An intermediate backend verification caught formatting of a test added during the run; formatted and reran full gate successfully. Failed /detail diagnostic URL was not an endpoint; actual /tasks route was used for content verification.

Baseline HEAD: a69429088d793a8b9ec080b514339eac93c3d274 with pre-existing dirty Syn/RAG and landing work retained. No commit/push.

## Remaining limitations
- Future unreviewed server content uses original English fallback until reviewed translations are added.
- Existing messages, generated plans, external titles/passages and external websites are not retroactively translated.
- Human content review still recommended; this change is presentation translation, not new RIASEC validation evidence.
- Existing production identity and landing track slug mismatches remain outside localization scope.
- Docker/gateway rebuilt and final browser bundle verified; screenshot and image hashes recorded in `reports/site-localization-verification.md`.
