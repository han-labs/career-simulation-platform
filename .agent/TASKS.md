<!-- documentation-sync-2026-10-22 -->
# CareerSim current tasks

Updated 2026-10-22. This file owns **current progress**; [TEAM_ASSIGNMENTS](../docs/TEAM_ASSIGNMENTS.md) owns the confirmed person/UC allocation. Current catalogue is [10 selected US / 16 UC](../docs/requirements/README.md), not legacy 3 US or frozen Baseline 15 US.

## Official allocation and current state

| Person | Assigned UC | Selected report US | Current state |
|---|---|---|---|
| Person 1 | UC-01, UC-02, UC-03 | US-01, US-02, US-03 | Existing P1/P2; reconcile API/UI/tests and complete owned UAT; live Google remains pending |
| Person 2 | UC-04, UC-05, UC-09 | US-04, US-05, US-06, US-10 | Existing catalogue/simulation/usage; resolve completion/recommendation wording before affected code |
| Person 3 (project lead) | UC-06, UC-07, UC-08 | US-07, US-08, US-09 | Existing Student Agent/KB/Builder; coordinate integration, source-hash plan semantics and live AI evidence |

Exact people/Git identities and reviewer rotation remain pending group sign-off. Cross-cutting security/schema/proxy work needs a named task owner and project-lead review. Supplementary UC maintenance owners are not inferred; UC-14 is deferred.

## Current work queue

| Work | Owner / dependency | Status / completion condition |
|---|---|---|
| Import CORRECTED catalogue and synchronize navigation | Documentation task authorized by project lead | Documentation updated; static verification in [alignment register](../docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md); no runtime acceptance claimed |
| Decide report/code wording differences | Chat 3/business owner with affected Person and lead | OPEN: UC-04 recommendations, UC-05 final review/completion/new attempt/evaluation, UC-06 editable plan; retain current rules until explicit decision |
| Assigned integration/UAT | Respective Person 1/2/3 | Pending explicit task plan and actual evidence; do not rebuild P1–P6 |
| Live Google and authorized live AI evaluation | Assigned UC owner; lead coordinates credentials/budget | PENDING; this documentation task does not authorize provider/database calls |
| AC-A09 / full AC-A17 | Person 3 coordinates with report/evaluation owners | PENDING live native three-domain-tool evidence and human-reviewed product cases; 20 deterministic fixtures are not equivalent |
| Supplementary specs UC-10–13/15–16 | Owner to be agreed | Supporting documentation only; no automatic new implementation allocation |
| Retention, deployed HTTPS, restart/NFR and course evidence | Lead coordinates a named owner | OPEN; no invented measurements, policy or acceptance |
| Safe Git publication | Repository owner | Review explicit Markdown whitelist, existing dirty changes and contact/raw-log risks; no staging/commit/push performed here |

P1–P6/V22 already exist on checked main. Previous build/test counts are dated evidence in completion reports, not freshly executed results. Immutable V1–V22, private histories and existing working files/deletions must be preserved.

## Gate before any application change

Use a plan in `.agent/plans/` with catalogue-qualified UC/US and AC references, owner/reviewer, current API/tests, main/alternative/exception flows, scope and current discrepancies. Await an explicit decision only for affected business/security/ownership conflicts; do not infer approval from report wording or an old plan. For final AI acceptance use at least 20 cases and separate mock, wire, live and human evidence.

## Historical tasks

The previous task ledger follows unchanged for traceability. Its old frontend-only allocations, “not implemented” rows, package labels and completed milestones are historical and do not override the official current allocation.

<details>
<summary>Historical content preserved unchanged</summary>

<!-- preserved-content-start -->
# Career Simulation Platform Task Board

## 0 Ownership boundary

The supplied assignment is explicitly a **frontend assignment**:

| Member | Frontend ownership | Required collaboration boundary |
|---|---|---|
| Võ Nguyễn Ngọc Bích | US-01 RIASEC assessment and landing page | Coordinate API contracts for assessment questions, autosave, submit, score, and directions |
| Mai Trần Thùy Trang | US-02 career simulation | Coordinate catalog/detail, attempt lifecycle, task submission, and deterministic result contracts |
| Huỳnh Gia Hân | Project/frontend setup and US-03 Dashboard/Syn | Own shared setup changes carefully; coordinate evidence, AI/fallback, provenance, and Syn contracts |

Backend, database, AI integration, enterprise-management, and authentication ownership were not assigned in the supplied material. Do not infer an owner.

Each owner should still verify the full vertical slice with the responsible backend contributor before calling a frontend story complete.

## 1 Recommended implementation order

1. **Foundation:** repo conventions, CI, Docker/PostgreSQL, API envelope, catalog slice, security decision.
2. **Identity foundation:** authentication, current-user context, roles, profile, consent.
3. **US-01:** assessment question model, deterministic scoring, API, frontend, tests.
4. **US-02:** simulation details, attempts, three-task backend demo, deterministic evaluator strategies, frontend, tests.
5. **US-03:** controlled evidence context, deterministic feedback first, AI provider adapter, schema validation, fallback, frontend, evaluation cases.
6. **Enterprise management:** owned content lifecycle after student flow contracts are stable.
7. **Integration/deployment:** full Docker slice, Ubuntu VM, evidence, report and diagram reconciliation.

## 2 Current foundation status

| Item | Status | Evidence |
|---|---|---|
| Repository and agent knowledge base | Ready | root and `.agent` documentation |
| React/Vite shell and landing starter | Ready for feature iteration | frontend build/lint/test gates |
| Spring Boot/PostgreSQL foundation | Ready | catalog facade/controller/DAO/entity and Flyway migrations |
| Public simulation catalog API | Ready foundation slice | service/controller/architecture tests |
| Docker Compose and Nginx | Ready for smoke verification | compose config and health/catalog endpoints |
| Authentication/profile | Not implemented | planned foundation dependency |
| US-01 workflow | Preliminary backend/frontend integration; local demo data ready | deterministic assessment API, connected frontend, 42 questions, and a synthetic completed baseline; full acceptance audit remains |
| US-02 workflow | Preliminary backend/frontend integration; local demo data ready | catalog/attempt/evaluation API, connected frontend, and three original tasks for each of five published simulations; full acceptance audit remains |
| US-03 workflow | Adaptive bounded agent backend/frontend verified | `/dashboard`, global three-mode Syn, structured memory and clear control, typed tool allowlist, server-planned single-generation default plus optional two-pass Gemini tool calling, static-policy fallback, new-student onboarding, English/Vietnamese replies, adaptive response depth, path comparison, evidence gaps, reviewed resources, bilingual Standard fallback, anchored conversation viewport, confirmed plan persistence, provenance, automated backend/frontend gates, V9 migration and prior gateway/browser smoke; runtime login remains pending |
| Enterprise management | Not implemented | boundary reserved |

## 3 Definition of Done for every story

- Approved plan and sequence-to-code trace exist under `plans/`.
- Main, alternate, exception, permission, status, and fallback flows are implemented.
- Business rules live in domain/service code; controllers and React components remain adapters/views.
- API uses documented DTOs and stable error codes/messages.
- Flyway migration is additive and tested when schema changes.
- Service/domain tests cover each decision boundary.
- Controller/API tests verify status and representation independently.
- Frontend covers loading, success, empty, validation, retry, and unavailable states.
- Accessibility includes semantic controls, keyboard use, visible focus, labels, errors, and reduced-motion support.
- `backend verify`, `frontend check`, Compose validation, and relevant smoke checks pass.
- AI feature has normal, ambiguous, malformed, unsafe/out-of-domain, timeout, and provider-failure evaluation cases.
- Sequence/domain/architecture artifacts match the implementation.
- Evidence and course report are updated without fabricated measurements.
- AI assistance and human review are logged.

## 4 US-01 frontend scope

Owner: **Võ Nguyễn Ngọc Bích**

- Landing-page iteration within `frontend/src/features/landing`.
- Assessment intro, questions, progress, validation, resume, submit, and result screens.
- Six-dimension score visualization with text equivalents.
- Clear non-deterministic-language disclaimer.
- API contract and state handling without client-side score authority.

Required tests: question navigation, invalid/missing answer, resume, submit success/failure, six-score display, no-AI/fallback result display, accessible labels and keyboard flow.

## 5 US-02 frontend scope

Owner: **Mai Trần Thùy Trang**

- Catalog/filter integration and simulation detail.
- Start/resume attempt, task navigation, autosave, submission confirmation, locked/completed states.
- Objective total and task-level outcome presentation.
- No rendering of answer keys before evaluated completion.

Required tests: catalog states, unpublished inaccessible, start/resume, task validation, network retry, submit, evaluated result, answer-key non-disclosure.

## 6 US-03 frontend scope and setup responsibility

Owner: **Huỳnh Gia Hân**

- Maintain shared app/router/API/style setup without breaking another feature boundary.
- Dashboard loading, ready, preview/fallback, partial, and retry states.
- Syn conversation, quick actions, inline result cards, plan drafting, limitations, and source/provenance.
- Syn contextual follow-up, path comparison, reviewed learning resources, visible tool activity, evidence references, and clear-memory control.
- Never hide fallback or imply AI certainty.

Required tests: AI success, fallback, timeout, malformed response mapped to fallback, empty evidence, source label, and objective score stability.

## 7 Foundation and backlog items requiring assignment

- Authentication/profile/consent.
- Assessment backend and scoring.
- Simulation attempt/evaluator backend.
- Guidance context/provider/fallback backend.
- Enterprise content management.
- End-to-end automation and deployment ownership.
- Final report integration and presentation ownership.

## 8 PR checklist

Copy `.github/pull_request_template.md` and fill every section. A PR with missing trace source, tests, evidence, report impact, or known gaps is not ready for review.


## 9 Syn core/vector RAG checkpoint — 2026-09-28

- P1/P2 implementation delivered for review; continue from this checkpoint instead of restarting the earlier foundation plan.
- Technical evidence: backend 121 discovered / 117 executed / 4 opt-in skips, frontend 24 passing tests and lint/build, Compose/whitespace, real PostgreSQL tests and dump/restore, live synthetic embedding/vector path, API lifecycle/revocation and browser citations.
- Open acceptance work: human corpus and gold review; held-out lexical/vector/hybrid plus generated-answer evaluation; assigned production identity integration; deployment-specific price snapshots. These are not reported as completed acceptance criteria.
- Voice P3 and realtime P4 remain planned, outside the approved implementation increment.
- Full traceability and limitations: `reports/us-03-syn-core-vector-rag-verification.md`.

## 10 Syn coach A/B/C checkpoint — 2026-09-29

User feedback follow-up: `plans/US-03-syn-conversation-quality.md` supersedes chat presentation: natural explanation first, compact/collapsed evidence, no unsolicited simulation on comparison, DevOps/AI topic routing, missing-RAG general explanation and one bounded output repair. See `reports/us-03-syn-conversation-quality-verification.md` for verification; human corpus publication remains open.

Continuation 2026-10-02: private evidence scope and negated simulation fixes, bounded HTTP timeout correction. Final backend verify 144 discovered / 140 executed / 4 opt-in skips; frontend retained 26 passing tests. Local gateway smoke distinguishes actual AI answers, quota-limited web and no published RAG corpus; see the same report for failed as well as successful live turns.

- Continue from `plans/US-03-syn-coach-abc.md`; preserve previous P1/P2 work. Voice deferred by user.
- Implemented composed EN/VI capabilities, ordered path/resource follow-up, reviewed resource ranking/fallback, evidence-based draft template, bounded Gemini 429/503 retry, heuristic overclaim guard and optional grounded web resource discovery.
- RAG hybrid code reused and tested; one-shot publish workflow prepared. Corpus and evaluation drafts still require real human review before publication/quality claims. This is an open acceptance item, not an implementation failure hidden behind a completed label.
- Verification: `reports/us-03-syn-coach-abc-verification.md`; user test instructions: `../docs/testing/syn-coach-manual.vi.md`.

## 11 Package A implementation checkpoint — 2026-10-14

- Human approved Stages 0–7 and V13–V16; implemented on codex/package-a-core.
- Final backend verify: 186 discovered, 182 passed, 4 opt-in live skips. Frontend
  core-enabled check: 65 passed, lint/build passed. Compose config passed.
- Isolated PG17 fresh/upgrade/history/guards/concurrency/import rollback, gateway
  no-AI core workflow and dump/restore passed. Valuable runtime was not accessed.
- Five Astra specifications, staged user design artifacts and V1–V12 unchanged.
- Evidence, API/module/file inventory, limitations and Package B prerequisites:
  reports/package-a-core-verification.vi.md. No commit/push/deployment.
- Revised SRS is unavailable; human UAT/content review and real rollout remain gated.
## 12 Package A migration numbering after main merge — 2026-10-14

- Human accepted the reported second collision at Syn V14 and the replacement
  numbering: Package A V15–V18, planned Package B V19–V21.
- Preserve main/Syn V1–V14 and all four Package A SQL contents; no shared DB access,
  history repair, reset, commit or push. Verification is recorded separately in
  reports/package-a-migration-numbering-verification.vi.md. Section 11 is historical.
- Merged-source final clean verify: 196 discovered / 192 passed / 4 live skips;
  frontend core-enabled check: 76 passed plus lint/build. Source/JAR versions 1–18
  unique, 18 SQL hashes unchanged; isolated V14 upgrade/history/preservation and
  gateway/restore passed. Compose/diff checks passed.

## 13 Approved Security/Auth P1 + Core Alignment P2 — 2026-10-19

- Continue from `plans/p1-p2-security-core.md`; P3–P6 remain excluded. Here P1/P2
  refer to the 2026-10-19 target v1.2 packages, not the historical Syn phases.
- Google OIDC/JWT cookie/current-actor/CSRF, safe explicit linking and per-token
  logout; real-actor legacy Career Quiz; account-scoped frontend drafts/responses.
- Ordered task/self-review policy and optional quiz results use additive V19/V20;
  existing published versions keep historical policy. Existing normalized tables,
  BIGINT Core identifiers, consents, evidence and Syn infrastructure are preserved.
- Consolidated evidence and acceptance status: `reports/p1-p2-completion.vi.md`.
  Final backend: 213 PASS / 4 opt-in SKIP / 0 FAIL/ERROR; frontend default/Core
  each 86 PASS plus lint/build. Protected inputs 40/40 unchanged; 20 migrations
  unique with V1–V18 preserved and isolated V18→V20 verified.
  Browser automation was disabled in Work settings during final continuation;
  the new confirmation UI has component evidence and HTTP gateway regression,
  but the final full browser rerun/live Google remain explicitly unverified.
- Production cutover, valuable-data migration, real Google/provider verification,
  human content/UAT review and course-report integration are separate handoffs.

## 14 Approved KB Studio and Agent Builder P3/P4 — 2026-10-20

- Scope and sequence: `plans/US-09-10-agent-platform-foundation.md`.
- Implemented new Admin KB/Agent folders and public assigned-Agent Student list;
  minimal coordinated navigation/shared JSON-editor integration preserves other
  member-owned Core/Syn UI. Stable contracts: `../docs/api/agent-platform-p3-p4.md`.
- V21 reuses V13 knowledge storage, adds scoped source lifecycle and immutable
  registry/execution integrity. V1–V20 append-only; no valuable database migrated.
- Consolidated current-run evidence/AC status/limitations and P5/P6 prerequisites:
  `reports/p3-p4-completion.vi.md`. Browser use disabled by Work settings; HTTP
  gateway and component tests must not be described as manual browser acceptance.
- P5/P6 remain outside this approval. Real content review, provider/model access,
  D5 retention, course report and owner coordination are explicit release handoffs.

  Final P3/P4 gate: backend264 total/260 PASS/4 opt-in live SKIP/0 FAIL/ERROR;
  frontend default/enabled each90 PASS +lint/build. Isolated gateways/restore and
  V20-to-V21 upgrade passed;21 unique migrations,62/62 protected inputs preserved.
  Browser UAT and live model verification remain unavailable/not executed.


## P5/P6 implementation checkpoint — 2026-10-20

Approved Student Agent + Reports/Plans/Usage code is integrated on
`feat/student-agent-and-ai-reporting` from main `1b11b593e0bc0cbc613eedfbfb7ba5307f89e12a`.
Existing P1–P4/Core/Syn and V1–V21 remain preserved. Additive V22 adds source lineage,
durable events/reports and exact-source plan-confirmation integrity; no legacy backfill.
Student/Admin/KB hosts share BoundedNativeExecution with native provider adapters;
current owner/assignment/consent/source/epoch checks govern dispatch, commit and replay.
No autonomous business writes or deferred Enterprise/private-upload features.

Final local gates: backend clean verify310 tests (305 pass,0 fail/error,5 skip),
frontend97 pass with lint/build in default and enabled configurations; isolated
fresh/upgrade/checksum/backup/gateway checks pass. Browser13 smoke assertions used
real UI/Nginx/PG and mocked providers.20 actual deterministic schema/evidence cases
are separate from live-model quality. AC-A09 and full AC-A17 remain NOT VERIFIED;
real Google OAuth, approved live-model/human20-case review and release/course delivery
are pending. No commit, push, merge, valuable DB migration or deployment.

See `.agent/reports/p5-p6-completion.vi.md`, verification/preservation manifests,
`docs/api/student-agent-p5-p6.md`, `docs/architecture/student-agent-p5-p6.md` and
`docs/ai/AI_USAGE_P5_P6.md`. Original user content above is preserved byte-for-byte.

<!-- preserved-content-end -->

</details>
