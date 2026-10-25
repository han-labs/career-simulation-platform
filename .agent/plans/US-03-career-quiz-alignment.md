# Current catalogue US-03 / UC-03 — Career Quiz

Status: Local technical AC1–AC5 verified with four published quizzes, real browser/API and restart persistence; human/public-release sign-off remains separate.
Owner: Person 1. Human reviewer: proposed Person 2, identity/sign-off pending.
Date: 2026-10-23. Baseline HEAD: 62bbe1012e9e5c03dad1719b84cd40fff5fce499; initially clean.

## 1 Scope

Actor: authenticated active Student. Sources read: root/.agent instructions, current
requirements index, selected US-03 (five AC), UC-03, team assignments, alignment
register, frozen Baseline v1 US-04 / DR1–DR2, P1/P2 architecture, current source/tests.
There was no existing alignment plan. Historical US-03 Guidance is excluded.

| AC (in source order) | Existing implementation | Scoped completion |
|---|---|---|
| 1 independent selection | published catalogue + typed routes | loading/retry/empty, optional simulation link |
| 2 instructions/questions | immutable questions + Likert renderer | type-specific instructions, accessible form |
| 3 field guidance | form missing-answer state + server validator | exact UC message, invalid local values, focus/error association |
| 4 valid submit/summary | transactional submit + deterministic evaluator | error recovery, dynamic ranges, persisted result only |
| 5 separate results | owner/version pinned result storage | assessment-owned history/read/resume without guidance dependency |

Exclusions: shared auth/router/navigation, other Person scope, AI, simulation rules,
schema/migrations V1–V22, scoring changes, legacy result rewrites, production config,
content publication, deployment/commit/push. Read Landing/Dashboard/Simulation JSX
and design CSS as references; browser visual verification remains separate.

## 2 Flow inventory

Main: load actor/catalogue/history → select published type → start idempotently →
read instructions/pinned questions → answer → validate → transactional backend score
and save → display stored summary. Alternatives: postpone to selection without
result; retain drafts/results across retakes/types/versions; view history.
Exceptions: highlight missing/invalid answers; unavailable type/version returns to
selection; failed writes preserve work and do not claim completion; conflicts require
explicit server/local resolution; result-read retry; storage failure remains visible.
Quick demo: legacy prefill keeps normal validation; v2 must not map unreviewed legacy
profiles to arbitrary published questions. Explain this on demo deep links.

## 3 Responsibility design

| Responsibility | Expert/component | Reason |
|---|---|---|
| HTTP/actor extraction | VersionedAssessmentController | existing facade boundary |
| lifecycle/availability/owner | VersionedAssessmentServiceImpl | transactions + current actor |
| scoring/schema | existing evaluator registry/RiasecV1Evaluator | frozen deterministic rules |
| owner history query | VersionedAssessmentDao | normalized existing storage |
| request adaptation | assessmentApi | feature → shared HTTP |
| state/recovery | useVersionedAssessment | one attempt/answers source |
| presentation | VersionedAssessmentPage/Form/Result | assessment-only UI/styles |

Sequence: Controller → service interface → implementation → DAO/evaluator → PostgreSQL;
page → hook/components → assessment API → shared HTTP. No topology change.

## 4 Change surface

Assessment backend contracts/controller/service/DAO only; assessment frontend page,
hook, adapter, scoped CSS, Form/QuestionCard/Result;
additive translations; focused domain/service/controller/component tests. No migration,
environment change, dependency, or other feature edit.
Docs: this plan, UC-03 verification links, alignment entry, scoped verification/AI log.

## 5 API contract

Existing v2 start/get/save/submit/result stay compatible; add GET
`/v2/assessment-attempts` returning up to 100 own attempt summaries (type/version/status/
timestamps). Authenticated active Student only, under existing Core flag; no owner input.
409 stale revision/changed final payload, 404 unavailable/foreign attempt, 422 malformed
answers. Old published pinned versions remain valid even after new publication; inactive
type blocks new saves/submits, historical reads/idempotent submitted retry stay available.

## 6 Security privacy and AI

Reuse ActorAccess at controller/service. No new roles/permissions or AI context.
Local owner+attempt+revision drafts; never adopt unowned/foreign/stale revision silently.
No raw answer logging; no fabricated career suitability or composite score.

## 7 Tests and evidence

Baseline commands attempted before edits: backend verify initially blocked by read-only
Maven cache, then ran: 212 PASS, 8 Docker environment ERROR, 20 SKIP; frontend check
blocked by missing eslint/dependencies. npm ci later succeeded without lockfile changes.
Docker API remained unavailable after startup attempts and a bounded probe.
Add deterministic/schema/publication unit tests; service unavailable/history decisions;
controller role/owner contract; extend existing PostgreSQL coverage for own history,
availability and rollback; frontend loading/empty/retry/missing/invalid answers, save/
submit failures, conflict, storage failure, history/retake and dynamic scores.
Run backend `./mvnw.cmd verify`, frontend `npm run check` default + Core, Compose config,
diff checks. Run isolated gateway/browser flows if Docker becomes available; otherwise
explicit BLOCKED/NOT TESTED. Existing integration tests retain simulation/auth/Agent regression.

Execution: focused backend 19 PASS; final full verify 231 PASS / 0 assertion FAIL /
8 Docker ERROR / 20 SKIP, so the full backend gate is not green. Frontend default
115 PASS + lint/build ở cả mặc định và Core. Spotless,
Compose config and diff checks PASS. Browser EN/VI/loading/error observed at desktop
and 320px without horizontal scroll; server-backed success/UAT not verified.

## 8 Risks and decisions

Latest authorized publication and acceptance run supersedes earlier local BLOCKED
notes: [local demo evidence](../reports/us-03-local-acceptance-2026-10-23.vi.md).
Four versions published through services, bootstrap reruns 4/4/75, real browser
completed all four, negative/API/ownership and restart checks passed. Backend 327
PASS/6 SKIP (5 live + opt-in bootstrap), frontend default/Core each 117 PASS.
Local-only Compose override selected through ignored .env; base deployment unchanged.
Setup/UAT: docs/testing/us03-local-demo.vi.md. No US-01/02 implementation or auth bypass.

Local-demo authorization follow-up: user explicitly requests publication of the four
existing project quizzes and a synthetic Student. Implement an opt-in test-source
bootstrap with local-us03 profile + explicit flag + exact localhost JDBC guard;
no production security change or impersonation endpoint. Reuse legacy repositories,
review existing EN/VI content, validate exact category/count coverage, publish through
AssessmentAuthoringService using a synthetic non-login Admin fixture. Provision a
Student through the existing service with a random locally stored password. Refuse
identity/content collisions; reruns reuse exact published content, never mutate it.
Provide a PowerShell setup script and ignored credentials; bootstrap survives rebuild
through existing PG volume. Scope: test bootstrap/guard tests, setup/UAT docs, category
presentation labels, real API/browser evidence. No new Flyway or auth implementation.

Latest evidence: [2026-10-23 stabilization](../reports/us-03-stabilization-2026-10-23.vi.md):
backend 326 PASS/5 live SKIP; frontend default/Core each 116 PASS + lint/build;
Docker/gateway healthy. Earlier execution notes above retain their original context.
No production questionnaire was seeded or published; content/UAT sign-off pending.

Follow-up stabilization (2026-10-23): preserve all existing work and auth code.
Runtime is now healthy (see runtime-diagnosis report). Recheck cached start UUID
corruption with a focused component test before changing the assessment hook;
invalid browser metadata must not trap starting a quiz in repeated 422 responses.
Keep valid pending UUIDs for idempotency. Use isolated existing PG fixtures, not
Google registration/login or publication in the local DB. Re-run mandatory gates;
missing approved published questionnaires remain BLOCKED for local browser success.

No reviewed v2 questionnaire is seeded by migrations. Do not publish four legacy
quizzes automatically. Human content review/publication and UAT remain release gates.
History endpoint is assessment-owned, additive, schema-neutral; owner/lead review pending.
No unresolved scoring/privacy/other-module rule is implemented. Full browser/PG proof
depends on local Docker availability; never substitute mock evidence for runtime acceptance.
