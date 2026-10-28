**Course:** New Technologies in Software Engineering  
**Artifact required by:** Final Project Assignment, Section 5.3  
**Log compiled:** 2026-10-21  
**Status:** **DRAFT — human verification and contributor sign-off required before submission**

> **Transparency note.** This record is being consolidated after several development milestones using existing Git history, AI-assisted development reports, and test evidence. It is **not** evidence that a repository-level `AI_USAGE.md` existed at M2. Do not backdate commits or claim earlier submission. Summarized prompts below are **reconstructions of task instructions**, not verified verbatim chat transcripts. The people who used the tools must validate or replace them with accurate excerpts.

## 1. Recording rules

- The team maintains **one shared root-level `AI_USAGE.md`**. Every contributor adds **their own truthful entries** for AI-assisted code, tests, diagrams, UI, and documentation they actually used.
- Each entry records: **responsible person, tool, approximate date, task/key prompt, AI-produced area, what a human checked/changed, and verifiable evidence** (files, tests, PRs, commits).
- Never list an AI tool or human reviewer merely because a generated report suggested one. **Do not invent prompts, approvals, manual testing, live-model results, or teammate contributions.**
- Keep credentials, student data, JWTs, private chat transcripts, personal local paths, and unreviewed raw logs **out of this file**.
- `PASS` from automated tests establishes only the stated automated checks, not human sign-off or live AI quality.

## 2. Main AI-assisted work — team review required

2026-10-24 Docker startup repair (Codex; responsible human/reviewer to confirm):
diagnosed the observed empty-JWT-key constructor failure separately from Google,
preserved private configuration/data, added Compose readiness and safe local setup
with startup regression tests. Evidence and remaining live-Google limits are in
[the startup report](.agent/reports/compose-startup-2026-10-24.md). No automatic
commit, push, merge or human-acceptance claim.

| ID    | Responsible contributor / tool                                                       | Key prompt or instruction _(summary; verify actual transcript)_                                                                                                                      | Main AI-assisted output                                                                                        | Human verification / changes and evidence                                                                                                                                                                                                                                                                                           | Status                                                             |
| ----- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| AI-01 | **Owner: confirm**; ChatGPT (requirements/design assistant) and AI-assisted planning | Align frozen Requirements Baseline v1/DR1–DR2 to Target Design v1.2, preserve accepted rules, identify safe P1–P6 implementation sequence.                                           | Core/Agent/ERD design notes, architecture contracts and implementation-plan support.                           | Team must confirm diagram/cardinality and requirements review. See `docs/requirements/CareerSim_Requirements_Baseline_v1_Chat2_Handoff.md`, `docs/architecture/data-model/`, architecture documentation.                                                                                                                            | Human reviewer and precise prompt: **TO CONFIRM**                  |
| AI-02 | **Owner: confirm**; Sol (AI coding assistant)                                        | Implement P1 authentication/security and P2 versioned Core workflows without altering older migrations; test against isolated PostgreSQL.                                            | Identity/security, assessments, Enterprise scenarios, Student simulations, tests.                              | PR [#2](https://github.com/han-labs/career-simulation-platform/pull/2); source under `backend/src/main/java/edu/hcmute/careersim/identity/`, `assessment/`, `simulation/`; corresponding backend/frontend tests. Live Google OAuth **not verified** by these tests.                                                                 | Automated evidence available; human reviewer: **TO CONFIRM**       |
| AI-03 | **Owner: confirm**; Sol (AI coding assistant)                                        | Implement P3 Knowledge Base Studio and P4 Agent Builder, immutable versioned configurations, preview/publish/assignment, scoped KB and execution tracking; retain V1–V20.            | KB ingestion/retrieval, Agent administration, native-provider adapters, V21, React Admin screens and tests.    | PR [#3](https://github.com/han-labs/career-simulation-platform/pull/3); `docs/api/agent-platform-p3-p4.md`; `backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentPlatformIntegrationTest.java`. See **FIX-01** and **FIX-02** below.                                                                                     | Automated evidence available; human reviewer: **TO CONFIRM**       |
| AI-04 | **Owner: confirm**; Sol (AI coding assistant)                                        | Implement P5 Student Agent with authorized tools/SSE/history and P6 InsightReport, confirmed plan and Admin usage, preserving earlier Core/KB behavior; add only additive migration. | Student Agent, read-only tools, session/history/streaming, structured outputs, V22, usage dashboard and tests. | PR [#4](https://github.com/han-labs/career-simulation-platform/pull/4); `docs/api/student-agent-p5-p6.md`; `StudentAgentIntegrationTest.java`, `AgentEvaluationTest.java`, `StudentAgentUpgradeIntegrationTest.java`. 20 deterministic evaluation cases are **not** 20 live-LLM cases; AC-A09/A17 remain pending live/human review. | Automated evidence available; human reviewer: **TO CONFIRM**       |
| AI-05 | **Owner: confirm**; ChatGPT used for report/specification drafting (Chat 3)          | Reconcile 13 selected report stories / 16 use cases with frozen 15-story baseline, design, code evidence and real acceptance gaps.                                                   | Academic report draft, requirement/use-case wording, traceability support.                                     | Human must review approved business decisions, story/UC IDs, citations and unverifiable claims before submission. Do not claim the document is validated only because AI drafted it.                                                                                                                                                | Exact prompts, editor, and reviewed report version: **TO CONFIRM** |
| AI-06 | **Owner: confirm**; ChatGPT image generation / ChatGPT HTML generation (Chat 2)      | Create an architecture figure and interactive explanation of frontend, Spring Boot backend, PostgreSQL/pgvector, provider and one AI request.                                        | Architecture illustration and interactive learning HTML.                                                       | Use only if included in final deliverables. Human must correct conceptual inaccuracies: pgvector is **inside** PostgreSQL; actual adapters are Gemini/OpenAI; model tool requests are executed/authorized by the backend; SSE events can stream before final completion.                                                            | Inclusion and final reviewed asset: **TO CONFIRM**                 |

**How to finalize the prompt column:** Replace each summarized instruction with a short **faithful extract of the real prompt** or a precisely labeled paraphrase verified against the relevant chat; do not manufacture verbatim quotations.

## 3. Two actual issues detected and corrected during AI-assisted development

These are **documented defects from the AI-assisted P3/P4 implementation**. Before final submission, the responsible contributor should confirm that the defective change was AI-generated, then sign off on the reproduction and correction evidence. The underlying raw logs remain local/private unless safely redacted.

### FIX-01 — Null-handling defect in Agent configuration validation

- **Area:** `backend/src/main/java/edu/hcmute/careersim/guidance/agent/domain/AgentConfigurationPolicy.java`
- **Problem:** An earlier validation path relied on `List.of(...).contains(null)`, which threw an unexpected `NullPointerException` for a missing value instead of returning a proper validation response.
- **How detected:** Early P3/P4 slice tests. Private evidence: `.agent/reports/p3-p4-first-slice-tests-initial.log` (reported lines 99–104).
- **Fix:** Replace unsafe null-membership logic with null-safe field extraction and explicit allowlist validation. Keep invalid configuration requests rejected without a server error.
- **How checked:** `AgentPlatformIntegrationTest` exact-save/configuration validation tests and later P3/P4 regression gates reported passing. Reviewer should link the exact corresponding test/commit before sign-off.
- **Human attribution:** **TO CONFIRM** (person who reviewed and accepted the correction).

### FIX-02 — Unexpected HTTP 301 in Knowledge Base create/upload routing

- **Area:** `frontend/nginx.conf`, `deployment/nginx/default.conf`
- **Problem:** A trailing-slash Nginx `location` rule redirected the Knowledge Base API path with HTTP **301** instead of preserving the expected API response, breaking the gateway scenario.
- **How detected:** Actual local gateway HTTP test of the P3/P4 upload/ingest/publish flow. Private evidence: `.agent/reports/p3-p4-gateway-initial-failure.log` (reported lines 128–129).
- **Fix:** Adjust exact-prefix routing for the relevant API path to prevent unwanted slash canonicalization. This is separate from any later uncommitted SSE proxy-timeout changes.
- **How checked:** `AgentPlatformIntegrationTest` gateway tests; source in merged `main` for P3/P4. The latest public/deployed proxy configuration must still be checked separately.
- **Human attribution:** **TO CONFIRM**.

## 4. Individual contributor records (each person completes their own section)

> **Do not assign Git aliases or modules to people by guessing.** Each person confirms the work and AI assistance they personally performed. If a person did not use AI for some work, write that accurately; never invent usage to fill a quota.

### Contributor A — [Full name / confirmed Git alias]

| Date   | Tool                   | Main code/files or documentation | Real key prompt or faithful short summary | Human checks / edits / tests            | PR/commit |
| ------ | ---------------------- | -------------------------------- | ----------------------------------------- | --------------------------------------- | --------- |
| [date] | [tool or `No AI used`] | [path / module]                  | [prompt, or N/A]                          | [what YOU actually verified or changed] | [link]    |

**Contributor approval:** [name] — [date] — [confirmed / corrections requested]

### Contributor B — [Person 2: Mai Tran Thuy Trang / confirmed Git alias]

> The rows below record the repository evidence for UC-04 and UC-05/US-06. The
> contributor must confirm that these summaries match the AI assistance they
> personally requested and the checks they personally performed; no name or Git
> alias is inferred here.

| Date       | Tool                                 | Main code/files or documentation                                                                                                                                                                  | Real key prompt or faithful short summary                                                                                                                                                                                                        | Human checks / edits / tests                                                                                                                                                                                                                 | PR/commit                                                                           |
| ---------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| 2026-10-25 | ChatGPT (GPT-6)/ Codex (GPT-5.6-Sol) | UC-04 catalogue API/UI, public detail, filters, login return, tests and evidence under `catalog/`, `simulation/`, `frontend/src/features/simulations/`, and `docs/evidence/US-04/`                | “Audit and complete US-04/UC-04 on Core V2: public database-backed catalogue/detail, server-side search/filter/sort/cursor pagination, fixed IT classification, safe sign-in return, and no locked answers.”                                     | PO-reported UAT: 14 PASS, 1 PARTIAL and 1 BLOCKED; focused browse tests 19 PASS and backend tests 14 PASS. Contributor must confirm their own review and browser actions.                                                                    | [PR #9](https://github.com/han-labs/career-simulation-platform/pull/9), `9834d8f`   |
| 2026-10-25 | Codex (GPT-5.6-Sol)                  | UC-05/US-05/US-06 enrollment, draft/final submission, model-answer release, self-review progression, completion/results UI and tests under `simulation/` and `frontend/src/features/simulations/` | “Complete the UC-05 simulation lifecycle and results flow while preserving ordered tasks, draft recovery, idempotent finalization, locked model answers, explicit self-review, zero-score completion, practiced skills, and owner-only results.” | Automated integration/frontend evidence covers progression, retry stability, zero-score quizzes, owner isolation and result projection. Full browser UAT and contributor sign-off remain pending; do not treat this row as human acceptance. | [PR #10](https://github.com/han-labs/career-simulation-platform/pull/10), `4211315` |
| 2026-10-27 | Codex (GPT-5.6-Sol)                  | `V32__seed_nine_versioned_scenarios.sql`, migration preflight and migration tests shared by UC-04/UC-05                                                                                           | “Use exactly nine approved sanitized UAT simulations as shared data; exclude credentials/private runtime data, fail closed on partial state, test a clean migration, and verify the IT catalogue API.”                                           | Migration-focused tests passed 3/3; isolated V1–V32 migration and `GET /api/v2/scenarios?industry=IT` returned nine records; CI later passed frontend/backend/compose. Contributor/project lead must confirm data approval and final review. | [PR #15](https://github.com/han-labs/career-simulation-platform/pull/15), `bc3d464` |

**Contributor approval:** [full name / confirmed Git alias] — [date] — **pending confirmation**

### Contributor C — [Full name / confirmed Git alias]

| Date   | Tool                   | Main code/files or documentation | Real key prompt or faithful short summary | Human checks / edits / tests            | PR/commit |
| ------ | ---------------------- | -------------------------------- | ----------------------------------------- | --------------------------------------- | --------- |
| [date] | [tool or `No AI used`] | [path / module]                  | [prompt, or N/A]                          | [what YOU actually verified or changed] | [link]    |

**Contributor approval:** [name] — [date] — [confirmed / corrections requested]

## 5. Evidence and acceptance boundaries

- Merged P1/P2, P3/P4, P5/P6: GitHub PRs [#2](https://github.com/han-labs/career-simulation-platform/pull/2), [#3](https://github.com/han-labs/career-simulation-platform/pull/3), [#4](https://github.com/han-labs/career-simulation-platform/pull/4).
- Automated P5/P6 verification as **reported** in the existing evidence: backend 305 PASS / 5 SKIP, frontend 97/97 PASS, isolated Flyway through V22, and browser smoke using mock provider. This does **not** establish live-provider/human acceptance.
- Source-controlled evaluation dataset: `backend/src/test/resources/guidance/agent-evaluation-cases.json`; human review and real-model runs remain separate.
- An existing related note is `docs/ai/AI_USAGE_P5_P6.md`. This top-level file is the **assignment-wide index**, not a replacement for genuine contributor records.
- Do **not** commit raw `.agent/reports/*.log`, `.env` secrets, private screenshots, runtime tokens, local filesystem paths, or raw student transcripts to provide proof. Cite sanitized, shareable paths and tests.

## 6. Final submission checklist

- [ ] Every person confirms their own AI tools, prompt entries, changes and Git identity.
- [ ] All six AI-assistance rows edited to reflect tools/people/files actually used; remove rows that did not occur.
- [ ] Two defect examples confirmed as AI-generated changes, with test/fix evidence and human reviewer.
- [ ] No credential, private data, personal path, raw execution log or invented claim in `AI_USAGE.md`.
- [ ] Final `AI_USAGE.md` merged to `main` by PR before final submission.
- [ ] Do not imply the M2 log existed in Git before it was actually added.

## 7. UC-07 completion — 2026-10-23, human review pending

- **Scope/authorization:** Person 3, implementation assigned to Codex Dev2 (Sol).
  Team Lead approved `.agent/plans/UC-07-manage-knowledge-base.md`, C1–C5 and
  Reindex A. Human acceptance/reviewer sign-off has **not** occurred in this run.
- **Tool:** Codex coding assistant, repository shell/patch tools, deterministic
  Vitest/JUnit/Testcontainers. No live model, paid API, Owner database operation,
  Compose startup, Git publication or delegation.
- **Actual prompt excerpt:** “hoàn thiện TOÀN BỘ UC-07, không dừng ở checkpoint C1”;
  preserve the Owner's 20 staged changes and plan, use existing JSONB, retain
  profiles/contracts, run isolated regression tests, report real browser limits.
- **AI-produced areas:** KB dialogs/revision-safe drafts; attempt/version extraction
  snapshots and diagnostics; version/attempt inspector; explicit publication and
  unchanged-index behavior; independent Test KB modes and request snapshots;
  source revalidation, focused tests, Vietnamese labels and scoped API/traceability
  documentation. File list and actual commands: `.agent/reports/UC-07-completion.vi.md`.
- **Checks performed by the agent:** source/guard self-review, mocked-provider
  tests against disposable PostgreSQL/pgvector, frontend tests/lint/build, Compose
  validation and preservation comparison. These are not human review or AI quality
  evaluation. Chrome was unavailable to the browser tool; the existing gateway
  refused connection. No browser PASS or human PASS is claimed.

### Actual mistakes detected and corrected in this task

These are **AI-generated test/presentation defects**, not fabricated production
incidents or claims that the human reviewer has accepted the fixes.

| Case        | Observed failure and correction                                                                                                                                                                                                                                                                                                | Verification boundary                                                                                                         |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- |
| UC07-FIX-01 | A generated immutability test expected raw `IllegalStateException`; the real Spring Repository boundary translates it to `InvalidDataAccessApiUsageException`. The test failed against PostgreSQL. Corrected the expected boundary type and retained the immutable-snapshot error/message assertion; did not weaken the guard. | `AgentPlatformIntegrationTest.attemptSnapshotsAreImmutableFencedAndMergeLateUsageIntoTheirOwnGeneration`; C2/C4 rerun passed. |
| UC07-FIX-02 | Generated UI added `Knowledge base unavailable.` without its VI dictionary entry. Full frontend check failed: 1 failure / 105 passes. Added the missing translation; did not exclude the string or disable translation coverage.                                                                                               | `translate.test.js` and subsequent full `npm run check` passed with 106 tests.                                                |
| UC07-FIX-03 | A generated KB_TEST aggregate test used `Instant.EPOCH`, violating the existing maximum 366-day report range. The real service rejected the request. Corrected the fixture window to one hour, preserving the production range/privacy contract.                                                                               | `kbTestUsageCountsExecutionsNotIngestionOrReloadAndPreservesUnknownMetrics`; final outcome recorded in completion report.     |

**Human action still required:** review the implementation and these actual
corrections, perform the Chrome checklist after the Owner builds the stack, and
sign off with their real name/date. Earlier draft contributor records and the
project's live-model evaluation gates remain pending.

## UC-09 AI Usage dashboard — 2026-10-26, human review pending

- Assignment: Codex Sol, existing stacked feat/uc09-ai-usage-dashboard, selected
  UC-09/US-10 only. Prompt: inspect persisted usage first; implement Admin request,
  cost, canonical latency and failed/refused metrics, server filters/trend/bounded
  execution list; preserve KB/Agent Studio, privacy and existing accounting.
- Tools: Codex coding/PowerShell/Docker, JUnit/Testcontainers/PostgreSQL and
  Vitest/Testing Library. Existing application data was read for UAT, never sent
  to a provider. No generated production usage, migration, .env/security-policy
  change, stage/commit/push/merge. Browser tool reported disabled in Work settings;
  no browser UAT PASS or screenshot is claimed.
- Reused AgentUsageController/Query/Dao and agent_execution, existing pricing
  snapshots and Admin Studio/shared UI. Safe DTO explicitly excludes Student
  identifiers, prompts, transcripts, provider payloads, tool arguments and source
  passages. Existing aggregate API stays compatible; dashboard uses view=dashboard.
- Actual verification: targeted dashboard PostgreSQL8 PASS; final backend verify
  418 discovered /412 PASS /6 opt-in live SKIP /0 failures/errors; frontend330 PASS
  with lint/build. Real gateway API19 checks PASS; SQL agrees with18 real existing
  executions and nullable costs. All73 table fingerprints and assets unchanged.
- Actual generated-code defects caught and corrected (not invented examples):
  UC09-FIX-01: JDBC getObject(Long.class) rejected jsonb_array_length's PostgreSQL
  int4 (7 errors in the initial isolated run). Explicit bigint cast fixed the
  projection; repeated targeted/full tests passed. UC09-FIX-02: new VI dictionary
  initially overrode7 existing shared-label translations (observed by comparing
  actual dictionaries). Existing dictionary precedence was restored, retaining
  other owners' labels; final330-test frontend check passed.
- Human responsibilities: inspect safe DTO/SQL/semantics and code; execute actual
  desktop/mobile browser UAT and sign off. API UAT uses the unchanged existing
  local-profile Admin principal, not evidence of a fresh real-browser JWT login.
  Real cost/partial-cost data is absent; synthetic isolated tests cover those
  paths. No live AI quality evaluation is implied.
- Detailed scope, files, actual failures/corrections and review checklist:
  `.agent/reports/ai/2026-10-26-uc09-ai-usage-dashboard.md`.

## Admin Studio final repair — 2026-10-26, human review pending

- Authorization/prompt: implement Astra's approved final repair on the stacked
  UC-09 branch; closely adapt supported reference interactions, preserve existing
  business/security contracts, add only explicit stored-file Reprocess, and leave
  all changes unstaged for Team Lead UAT. Optional UC-06/Syn shell excluded.
- Tool: Codex Sol with shell/patch tools, Vitest/Testing Library, JUnit/Mockito/
  Testcontainers, Docker and the in-app browser. Read the actual reference React
  files/tests; reused CareerSim API/storage/configuration semantics and shared UI.
  Applied build-rag-agent correctness guidance to source/publication/generation
  safety. No dependencies, migrations, provider/pricing/auth changes or live AI calls.
- Changes: shared scoped Admin visual rules; KB/Agent card grids; direct document
  detail/actions and publication summary; revision-fenced Reprocess; coherent
  partial Save; Brain/Chat/Integrations/Publish tabs; saved-config Preview with
  bounded UI-only run continuity, keyboard/Expand and revalidated source chips;
  compact Usage layout and explicit nullable/partial/zero cost presentation.
- Actual checks: final full backend verify 422 discovered, 416 PASS, 6 opt-in live
  SKIP, zero failures/errors. Four new Reprocess integration cases use disposable
  PostgreSQL and mocked embeddings. Full frontend check includes shared/i18n and
  Student/KB/Builder/Usage regressions. Exact final count is in the repair report.
- Actual generated-code mistakes detected and corrected:
  1. Generated Reprocess audit metadata used `previousRevision`, outside the real
     AuditWriter allowlist. Targeted PostgreSQL tests failed with Unsafe audit
     field. Changed to existing allowed `revision`; authorization/If-Match,
     unchanged/changed/failure/cancel tests and full verify passed.
  2. Generated compact header selectors lost to the shared `.cs-ui .cs-surface`
     specificity. Real browser measured a 288.49px header with 24px padding.
     Scoped Admin selectors restored 16px padding and a 171.49px header, retaining
     responsive panes; this was observed, not a fabricated screenshot outcome.
  3. Six new static UI labels initially lacked VI entries. Full translation
     coverage failed; added real translations without excluding labels/tests.
- Runtime: standard Compose build, only changed backend/frontend recreated in
  careersimfresh. Existing gateway/PostgreSQL containers and volume bindings kept;
  startup validated 28 migrations, no migration necessary. Read-only API/browser
  checks used existing authorized local Admin context; no new login/password,
  fake usage, provider call, document/configuration/assignment mutation or DB reset.
- Human action: inspect code/contracts and perform state-changing UAT, live provider
  checks only when separately approved, then sign off. Browser layout/read checks
  are not human acceptance or live-model quality evidence. No stage/commit/push/merge.


### 2026-10-26 — Revised Agent Studio (Codex Sol, Team Lead approved Astra plan)

- Prompt/task: implement the approved revised final plan on the existing dirty UC-09 branch;
  preserve prior repair, add real experience config/variables/Admin chats, KB counts and
  narrow native Agent deadline repair; no Git mutations, secrets inspection or .env edits.
- Tool assistance: source/reference analysis, Java/SQL/React implementation, synthetic
  Testcontainers and HTTP-wire tests, focused frontend tests. Live/provider and runtime
  evidence is recorded separately in the revised repair report; no human UAT is inferred.
- Actual AI-generated mistakes detected and corrected: (1) nullable JSON experienceConfig
  arriving through HTTP was written as JSON null and violated the new object constraint;
  DAO normalizes both omitted/null to SQL NULL, preserving legacy hashes. (2) Preview Send
  initially held the mutation lock while waiting for the first transcript GET, disabling Stop;
  acceptance now releases Send while a fenced GET refresh proceeds. The exact-run Stop
  regression test includes a deliberately stalled GET. (3) a datalist was initially a second
  FormField child, breaking its single-control contract; moved outside the field and tested.
- Verification/human boundary: automated tests are evidence of those paths, not human review.
  Team Lead visual/product UAT and approval remain pending. No fabricated incorrect-code
  examples, provider successes or human validation are claimed.

- Final observed evidence for this revised pass: backend verify453 total/447PASS/6SKIP,
  frontend check363PASS, standard Compose build/startPASS, V29/V30 deployed after protected
  backup/checksum preflight. Exactly ONE live Preview FAILED PROVIDER_UNAVAILABLE at2129ms,
  model gemini/gemini-3.1-flash-lite, zero tools/citations, UNKNOWN cost; no repeat provider
  call. API lifecycle/usage checksPASS. Browser disabled in Work settings: actual geometry,
  responsive visual and Chrome authNOT RUN. Detailed evidence/limitations:
  `.agent/reports/ai/2026-10-26-revised-agent-studio.md`.

### 2026-10-26 — UC-08 role publication and provider diagnostics correction (Codex Sol)

- Human prompt/scope: focus UC-08 and blocking auth UX, preserve all dirty prior repair;
  diagnose provider safely, allow exactly one controlled Preview after deterministic gates;
  replace fixed-name variables with bounded key/value entries; replace per-Student
  assignment UX/access with whole-role publication; distinguish local demo from real logout.
- AI assistance: Java/SQL/React changes and synthetic Testcontainers/HTTP-wire tests.
  No pricing lookup, credential inspection/replacement, .env change, Syn restoration,
  Git staging/commit/push/merge or UC-07/UC-09 redesign.
- Actual incorrect AI-generated code caught and repaired: V31 initially referenced an
  out-of-scope SQL alias in the session guard; integration tests caught it and the guard
  now checks NEW.owner_user_id. Role activation initially wrote an unsupported `roles`
  audit metadata field; the existing audit whitelist rejected the transaction. Changed
  to the approved safe status/count fields; rollback/concurrency tests verify the result.
  Publication readiness also initially missed a newly completed Preview; its read identity
  now includes exact execution ID/status, with the save/preview/publish UI regression intact.
- Verification: backend full verify 473 total, 467 passed, 6 existing opt-in skips, no failures.
  Initial frontend parallel runs hit long DOM-workflow timeouts; assertions and runtime
  limits were retained. Final frontend/runtime/live results are in the dated role-publication
  evidence report. Browser is disabled in Work settings; Chrome visual/auth/geometry UAT
  and human approval are not claimed. Team Lead review remains pending.

- Final observed evidence for this pass: frontend check362PASS (37 files; one worker,
  unchanged assertions/timeouts); backend verify473 total/467PASS/6SKIP. Standard
  Compose build/startPASS at23:06, V31 applied normally; old sealed proofs/assets preserved.
  Exactly ONE live Admin Preview FAILED at10356ms with safe HTTP503/
  PROVIDER_HTTP_UNAVAILABLE, outbound=true, configured=true, zero tools/citations,
  UNKNOWN cost; no second provider call. No credential replacement is justified by
  that503. On resumption at23:24 the Docker Desktop Linux engine was unavailable and
  gateway unreachable; this later observation does not erase the earlier startup evidence.
  Browser was disabled in Work settings. Details and exact Git state:
  `.agent/reports/ai/2026-10-26-uc08-role-publication.md`.
## 2026-10-26 — HCM-UTE rebuild from latest main

Codex (GPT-6-based agent) performed the current corrective request on a fresh main 608e812 worktree/feature branch; prior redesign887e0d1 remained untouched. It audited real flows before frontend changes, reused existing callbacks/hooks/routes/cards/state views, applied supplied visual assets and VI/EN copy, and executed baseline/final gates plus actual isolated API/browser journeys. [AI record](.agent/reports/ai/2026-10-26-hcmute-main-rebuild.md), [audit](.agent/reports/hcmute-main-functional-audit.md), [plan](.agent/plans/foundation-hcmute-frontend-flow-alignment.md) and [verification](.agent/reports/hcmute-main-rebuild-verification.md) distinguish mocks/live evidence, original-main defects and human review pending. No backend or feature-owned business implementation, auth/score authority or provider flow was replaced. No human acceptance, Google/live-model UAT or push/main merge is claimed.
### 2026-10-27 — Final integrated system recovery (Codex Sol)

- Prompt/scope: recover integrated main-derived product on `fix/final-integrated-system`
  at `31aece45ffa2d5279a9e81ec769f9e72a9813281`; audit merged PR sources/runtime/data;
  preserve Admin features; no AI/provider work, Git mutation, .env/profile change or seeding.
- Findings verified before edits: PR14 landing/assets remained; PR13 integration merge
  `86be6c2` duplicated i18n imports and omitted a closing test delimiter. Initial `npm run
  check` failed parsing both files. These were actual merge defects; their authoring origin
  is not asserted. Running frontend lacked campus hero/home styles and images.
- Minimal fixes: deduplicate imports while retaining all dictionaries; close the existing
  AppShell test; restore pending simulation only through the active /me effect after login;
  Dashboard alias results use owned enrollment data without fetching public slug `enrolled`.
  No scoring, completion, authorization, provider or schema behavior was rewritten.
- Evidence: focused suites73PASS then75PASS; full frontend377PASS/38files, lint/buildPASS;
  backend verify473total/467PASS/6existingSKIP. Tests retain account-generation remount,
  anti-open-redirect, no automatic enrollment and denied alias-result checks.
- Runtime: standard careersimfresh build and controlled backend/frontend/gateway recreation;
  backend image unchanged, frontend replaced. Gateway bundles match the current enabled local
  build byte-for-byte; HCM-UTE images match source. Database V1–V31 checksums match, no pending
  migration; data counts and private asset hashes unchanged. Protected backup made first.
- Owner boundary: runtime remains local; real-auth demo requires approval for auth-test and
  localhost HTTP cookie configuration. Core scenario/version/enrollment counts are zero;
  no seed/data insertion or Enterprise permission change performed. Quiz has4published
  versions/75questions, so no assessment data addition is needed. Browser disabled in Work
  settings: actual Chrome rendering/login/Student-runtime UAT NOT RUN. Human review pending.
- Detailed current evidence and exact Git status are in
  `.agent/reports/final-integrated-system-recovery-2026-10-27.md`.

### 2026-10-27 — Normal local authentication and integrated V32 verification (Codex Sol)

- Human instruction: normal local must remain anonymous until real registration/login;
  direct login and logout return Home; preserve safe pending simulation return, HCM-UTE
  landing and Admin features. Verify existing quizzes and the approved V32 catalogue;
  no new dataset, provider calls, .env edit or Git publication.
- AI assistance: opt-in default-false LOCAL_DEMO_MODE, explicit Admin API/route guards,
  account navigation/copy/typography, role-aware header/logo, tests and deployment docs.
  Existing JWT/CSRF/PasswordEncoder, assessment scoring and simulation rules were reused.
- Incorrect AI-generated work actually detected and corrected: the new isolated Student
  login fixture omitted its required student_profiles row and correctly received403;
  the fixture was fixed without weakening account validation. A broad test replacement
  changed the unrelated landing Journey CTA expectation; restored that assertion while
  retaining the new Student-only header Dashboard expectation. Runtime harness reused
  locked-task DTOs and sent no deliverable for task2 (422); fixed the harness to reload
  after self-review, matching the existing UI. No application validation was bypassed.
- Verification: focused frontend93PASS and backend14PASS; final npm run check385PASS
  (39files), lint/buildPASS; mvnw.cmd verify477total/471PASS/6existing opt-inSKIP,
  no failures. Standard careersimfresh build/startPASS, V1–V32 checksums match sources,
  all9catalogue/detail APIsPASS, existing React catalogue renders9cards EN/VI from live
  DTOs. Actual registered Student completed RIASEC save/resume/submit/result and a
  two-task Cyber Security Operations draft/file/FINAL/self-review/results flow, followed
  by logout/denial/relogin. Three synthetic UAT accounts and their audit/data remain.
- Human verification: Owner explicitly confirmed Admin login→Home→Admin Studio→logout
  →Home→protected Admin denial→relogin PASS. This is Owner-reported, not a Sol-observed
  Chrome session. Browser control is disabled in Work settings; Sol browser/visual UAT
  NOT RUN. V32 contains0model_answer/0task_resource: release authorization passed but
  example-answer content is unavailable. No guessed content or migration edit applied.
- Protected backup preceded deployment; original5private asset files, PostgreSQL
  container/volumes and13pre-existing untracked Owner files were preserved. Detailed
  evidence: [normal local/V32 report](.agent/reports/final-local-auth-uat-2026-10-27.md).
  Human final product review remains Team Lead's responsibility; no AI quality claim.

### 2026-10-27 — Real Agent model reliability recovery (Codex Sol)

- Human prompt: recover the existing Agent end to end with a real LLM, authorized
  RAG/citations, Student tools, schema-valid artifacts, confirmation/idempotency,
  bounded model failover and existing usage audit. Preserve accepted integration,
  sealed configurations, DB/volumes and Git state; no .env/key change or new runtime.
- AI assistance: trace singleton model allowlist; add optional ordered fallback keys
  to existing experience_config JSON/hash; repair modern Gemini request parameters;
  implement per-execution retry/deadline budget, safe attempt audit, schema repair,
  tool-observation reuse, compact composer-only Quick Actions and focused tests.
  Existing RAG, tools, publication, role discovery, consent and plan confirmation are
  reused. [Decision/evidence boundaries](docs/architecture/agent-model-reliability.md)
  and [real UAT procedure](docs/testing/ai-agent-real-runtime-uat.md).
- Measured server-side probe, one text inference per model with existing credential:
  3.8 metadata200 but22,009ms transport timeout; 3.6 inference200/2,327ms;
  3.5-lite inference200/5,334ms; legacy3.1-lite503/1,480ms. No Student context or
  credential/body/header was logged. New UAT order3.6→3.5-lite is based on these
  actual project observations; metadata is not inference, availability is not quota,
  and no pricing/free-tier assumption is added.
- Incorrect AI-generated work actually detected/corrected: the new usage test tried
  to modify a terminal execution and hit TERMINAL_EXECUTION_IMMUTABLE; the fixture
  now follows RUNNING→SUCCEEDED without disabling the trigger. Adding MODEL_ATTEMPT
  exposed a real Student consent-redaction bug (old code assumed every row had a
  tool name); fixed safe metadata redaction and retained CANCELLED/null-output/
  no-replay assertions. The native-tool test now verifies three actual calls and
  two model-attempt records separately. Quick Action constants initially violated
  Fast Refresh lint and were moved to a plain module. Fallback validation initially
  focused Chat instead of Brain; the focused UI test caught/fixed that routing.
- The24-case Owner pack is reconciled with the existing deterministic Agent
  controller/output-validator framework. Same fixtures: pre-repair single-model
  strategy22/24; repaired bounded strategy24/24. Generated report explicitly says
  DETERMINISTIC_CONTROLLER_SCHEMA_FIXTURES_NOT_LIVE_MODEL_QUALITY. These figures
  do not represent24 live semantic passes or human acceptance.
- Verification: final npm run check391PASS/40files plus lint/build; final backend
  verify532discovered/526PASS/6existing opt-inSKIP,0fail/error. Standard Compose
  build/startPASS; gateway serves byte-identical enabled source bundle, backend JAR
  has the new adapter/failover. V1–V32, PostgreSQL container,7private asset hashes
  and6old sealed configuration hashes preserved. Protected backup made first.
- Runtime/remaining evidence: [task report](.agent/reports/ai/2026-10-27-real-agent-reliability.md).
  Computer Use stopped because it could not confidently determine the current Windows
  browser URL; no UI bypass attempted. Owner Admin operations and real cited KB /
  published Student checks remain pending. No live semantic/human PASS is invented.

#### Owner AI UAT follow-up — 2026-10-27

- Owner supplied actual Preview output; DB audit confirms3.5-lite real answers for
  evidence groups (4,425ms/1citation) and FOUR-item checklist (5,705ms/1citation).
  Its comparison generated output but failed local validation. Rejected raw output
  was not retained, so the historical exact invalid field is not asserted.
- Verified saved settings differed from the UAT recipe: v1 had2rounds/30s/KB_ONLY;
  v3 using3.8 had no fallback. The Data/QA question used simulation catalogue only;
  no second retrieval opportunity remained. Uncited greetings fail strict KB_ONLY.
- Actual AI-generated transport defect detected: header timeout used the attempt
  budget, but stream body checked only the outer execution deadline. New stalled-body
  deterministic test failed before the fix and passed afterward. Absolute body
  deadline plus immediate timeout failover now preserves remaining execution time.
- New versioned prompt v2 clarifies tool/source choice, balanced comparisons, exact
  citation and suggestion limits, trust boundaries and actor context. V1 remains
  byte-exact/readable; new Saves use v2 without rewriting old sealed configurations.
  New Agent defaults4rounds/4calls/60s/GENERAL_ALLOWED; KB Test remains strict.
- A single extra real native-adapter diagnostic, using the exact5 approved synthetic
  passages from the failed comparison and candidate v2, returned a schema-validated
  answer with3literal citations in2,647ms. It is explicitly not an Admin execution
  or publication proof. No retry loop, raw body, key or private Student context logged.
- Model recommendation updated from minimal-probe3.6→3.5 to actual Agent-evidence
  3.5→3.6; timed-out3.8 excluded from the UAT chain. Safe validation diagnostics and
  distinct UI error prevent incorrectly presenting malformed output as a network error.
  Remaining live checks stay pending until their actual observations are recorded.
- Final quality rerun: backend535discovered/529PASS/6existingSKIP; frontend392PASS,
  lint/buildPASS. Second standard build/start and byte-identical served bundlePASS;
  fresh protected backup first.8assets/9pre-switch sealed configs,V1–V32 and PostgreSQL
  container preserved. Owner also supplied a real requested3.6→effective3.5-lite
  cited checklist success (25,764ms); this proves a real failover, not stability for
  every request. Revised-version P1/P2 and Student acceptance remain pending.
- Owner then verified actual P1/P2 on prompt-v2 version5:4,275ms/1citation and
  4,157ms/5citations respectively. Comparison improved from prior v1 local rejection
  to accepted real output. Audit still showed2rounds/30s/KB_ONLY, so those successes
  are not falsely attributed to4rounds orGENERAL_ALLOWED. Identity later hit the old
  round limit; no correct saved conversational version existed yet.
- To avoid repeated manual draft/pinned-chat confusion, prepared an Owner-operated
  localhost API runner preserving instructions/variables/KB while creating the approved
  immutable configuration, pinning a new chat, bounded6-case UAT/source revalidation,
  conditional exact-version publication/role activation. It uses normal Admin login
  and CSRF, hidden local password input, RAM-only auth, safe ID/status evidence and
  stops on failure/no automatic question retry. No credential is read by Sol.
  The first command incorrectly assumed pwsh was in Owner PATH; Owner reported the
  error. Runner was made compatible with Windows PowerShell5.1/7, UTF8-BOM for
  Vietnamese text; its offline self-test passed on actual Windows PowerShell5.1.
  No provider call occurred from that command-not-found error. Runner live result
  remains pending and must not be represented as PASS from its offline self-test.

### 2026-10-27 — real Student follow-through and Owner correction

Codex Sol continued after Owner's six-case real Admin runner PASS and exact version6
publication to STUDENT. Three actual Student turns used assessment.lookup,
simulation.explore and simulation.myEvidence against own/current application data
and returned validated cited answers. A composition turn failed local validation;
live calls stopped before the next deterministic repair. Existing output/artifact
schemas now constrain modern Gemini generation while host validation remains strict.

Owner then explicitly requested no separate Student consent step and alternating
chat bubbles consistent with the system. Only STUDENT_CHAT admission changes; no
consent rows are auto-created, and authorization/privacy revision fences remain.
User-provided historical refusals are retained. No unrelated auth/data changes.

Actual AI-generated errors caught: the new default streamStructured method was not
delegated by Mockito, failing integration fixtures; explicit real-default delegation
fixed the fixture without weakening assertions. A consent UI draft failed effect
lint, then was removed entirely after Owner changed the product policy. Full Admin
UI tests hit the existing 5s timeout under concurrent test load; isolated unchanged
tests passed, so final full checks run without that contention. No timeout assertion
was relaxed. Final API/test evidence is reported separately from human visual UAT.

The first real combined Student test after JSON-schema wiring still failed
CITATION_QUOTE_MISMATCH: all provider decisions and read tools succeeded. This
showed the initial AI-generated repair was incomplete: JSON conformance does not
prove a quote is literal, and the final round left no repair opportunity. Calls
stopped. The correction constrains final citation pairs to short verbatim windows
of already authorized evidence and reserves one repair within the same round cap.
It never rewrites a bad model quote or weakens the local validator. Deterministic
tests cover Unicode/exact pairing, fabricated quote rejection and no tool replay.

The hard-quote anyOf implementation then caused HTTP400 for the actual authorized
fixture schema despite a small synthetic control succeeding. Live execution stopped.
One direct schema diagnostic reproduced400; one changed flat-schema diagnostic with
the same fixture returned200. This caught another actual AI-generated integration
mistake: assuming schema keyword support proves every concrete schema is accepted.
Long quote enums were removed from the wire schema; short current reference bounds
remain, with literal quote examples passed only as untrusted current-run data.
The unchanged host validator still rejects paraphrases and wrong reference/quote
pairs. No raw provider error/body, key or private Owner data was printed.
### 2026-10-28 — Student Agent popup UX

- AI tools: Codex Astra planning direction supplied by Team Lead; Codex Sol implementation/test assistance.
- Task: Reused the Student Agent runtime in one responsive global popup with hidden history, per-message evidence/lazy Run Details, dedicated report/plan actions, and a formatted UTF-8 HTML InsightReport viewer/download.
- Review/evidence: automated Student role gating, account/session isolation, SSE close/reopen, dedicated action/plan confirmation, keyboard/IME, EN/VI and HTML escaping checks; focused 49/49, affected shared/Admin 22/22, full frontend 408/408 plus lint/build PASS (2 workers after parallel-load timeouts). No backend/provider changes or live AI calls. Team Lead visual/real Student UAT pending; local browser automation is disabled.
- Team Lead screenshot review rejected the first popup proportions. Sol corrected shared-CSS collisions, compact icon controls/history/composer, explicit scroll-position locking throughout streaming/close/reopen, conditional history pagination, and on-demand authorized saved-report open/download (no AI regeneration). Focused/shared regression 74/74 and full frontend 416/416 plus lint/build PASS; computed-style tests cover shared stylesheet loading last. Browser visual UAT remains pending because local browser control is disabled.

### 2026-10-28 — Student natural actions / Quick Action payload repair

- Codex Sol used the build-rag-agent debugging workflow after Team Lead real UAT. Confirmed the action handler reused unrelated composer text; CHAT schema and V22 persistence guards also excluded report/plan artifacts. This was an application integration defect, not evidence that the provider lacked the tools.
- Dedicated actions now send matching commands and preserve retry payloads; natural Student CHAT uses a closed model-selected requestedAction and the existing evidence/artifact validators, report storage and explicit plan confirmation. Added versioned student-actions-v1 host instructions and additive V33 guard alternatives; sealed Agent prompts/configurations, four read-tool grants, provider adapter and historical migrations remain unchanged. No keyword router or extra classification model call.
- Verification: focused frontend 54 PASS before the final additional VI plan case; final full frontend 425 PASS plus lint/build. Full backend verify: 547 discovered, 541 PASS, 6 existing opt-in SKIP (fresh reports from this run only), no failure/error. V32-to-V33 tests preserve all existing rows/checksums; canonical DB backup verified and post-upgrade counts/V1–V32 checksums unchanged. Initial full backend run exposed the V32-only import preflight and expected migration count; updated these specific compatibility checks without relaxing guards.
- Real localhost health/build/migration verified. No real provider call or new Student impersonation/account was made by Sol; real Gemini semantic/report UAT remains pending while local browser control is disabled. Team Lead review remains pending.


### 2026-10-28 — Student chat continuity and public discovery

- Codex Sol implementation/test assistance following Team Lead UAT. Accepted sends now clear only submitted text; account/session-scoped unsent drafts and accessible conversation selection survive navigation. New sessions persist the pinned configured greeting once, without AI execution; long answers use justified text. Browser JWT default/maximum is bounded at two hours, with existing revocation/authority checks retained.
- Sanitized real execution metadata confirmed four granted tools, four rounds/four calls, but zero tool calls before INSUFFICIENT_EVIDENCE. Added versioned student-actions-v2 public-catalogue/first-plan guidance and one budget-contained reconsideration before unattempted lookup; no keyword router, forced domain answer or weaker evidence/argument validation. Public simulation metadata is allowlisted; catalogue discovery needs no enrollment/results, while private evidence remains owner-scoped. Empty optional simulation filters and query bounds now match the existing catalogue. No new migration this round.
- Automated evidence: focused frontend 58 PASS; full frontend 432 PASS plus lint/build. Full backend verify: 559 discovered, 553 PASS, 6 existing opt-in SKIP, no failure/error (fresh reports only); final affected backend verify 43 PASS / 1 existing SKIP. Added checks for greeting idempotency/no provider call, draft races/resume, public discovery without personal results, public-evidence plan validation, bounded reconsideration/no replay, and two-hour credential expiry/revocation.
- Rebuilt only canonical careersimfresh backend/frontend; gateway health/latest Agent bundle verified, runtime lifetime 7200 seconds. Aggregate existing DB rows and Flyway checksum unchanged; .env, credentials and volumes untouched. Team Lead reported natural report generation works; new live Gemini simulation/plan and visual UAT remain NOT VERIFIED because browser tooling still reports Local browser use disabled despite the user enabling it. No live provider call or Student impersonation was performed by Sol.


### 2026-10-28 — Submitted-turn anchoring / assessment discovery / Admin Preview alignment

- Codex Sol implementation/test assistance: shared transcript-only anchoring positions the submitted User turn once and holds reading position during generation; compact Admin Preview now uses hidden History, per-answer collapsed sources/details, icon controls and scoped unsent drafts. Existing saved-config Preview conversations, confirmation/revision, idempotency, cancellation and source inspection remain separate from Student runtime.
- assessment.lookup reuses an internal published-only assessment projection with SQL question counts and public explanations/labelled duration estimates aligned with existing Assessment UI. Counts are not hardcoded; estimates are omitted for changed inventories and unknown types have no invented details. Added student-actions-v3 host policy; no new REST endpoint, schema/migration, scoring, provider, auth or publication changes.
- Evidence: focused frontend 69 PASS; full frontend 436 PASS plus lint/build; final full backend verify 562 discovered / 556 PASS / 6 existing opt-in SKIP, zero failure/error. The first full backend run had one existing history-lineage test timeout at 10 seconds; it passed isolated recheck and the complete rerun without weakening assertions/time limits. Updated the old Resume-selector integration assertion to the actual pinned-version UI contract.
- Real DB published inventories verified read-only: 42/12/12/9. Canonical backend/frontend rebuilt, gateway health/latest bundle checked, existing DB counts/Flyway checksum unchanged, current local-demo-off/auth settings preserved. Browser tooling still reports disabled and unauthenticated Admin API returns401; no auth workaround, new Student session, live provider execution or live-model PASS claim. Team Lead visual/EN-VI model UAT remains pending.

### 2026-10-28 — Student self-evidence / duplicate-citation repair

- Codex Sol implementation/test assistance following Team Lead real Student UAT. Sanitized execution records confirmed the Vietnamese self-result request already invoked authorized assessment.lookup and simulation.myEvidence; failure was duplicate-citation validation followed by provider 5xx during bounded repair, not missing tool access. Historical exact HTTP status was not recorded.
- Existing read tools now expose owner-scoped exact assessment/enrollment activity totals separately from bounded details; assessment lookup supports public/personal scopes and applies type filtering before result limits. Student host policy v4 guides natural EN/VI own-result requests and minimal relevant tool use. Citation alternatives are grouped per source; targeted repair retains strict duplicate/quote/score validation and never replays tools. Added safe numeric HTTP status/class audit only. Admin Preview stays public-only; no new REST endpoint, migration, provider/model/credential or auth change this round.
- Evidence: focused backend 70 PASS / 1 existing opt-in SKIP; full backend verify 572 discovered / 566 PASS / 6 existing opt-in SKIP, zero failure/error. Full frontend 436 PASS plus lint/build; diff and conflict checks passed. PostgreSQL fixtures cover owner isolation, exact totals beyond bounded windows, started/submitted/completed distinctions and failure recovery. Tests caught and corrected aggregate-snapshot replay handling before completion.
- Rebuilt only careersimfresh backend; gateway/page health HTTP200, existing canonical DB aggregate counts/Flyway checksum and public auth settings preserved. No live provider call or new Student session by Sol. Browser tool still reports local use disabled; new real-model EN/VI self-result acceptance remains pending Team Lead UAT. No staging/commit/push.

### 2026-10-28 — PR #18 CI source completeness

- Codex Sol traced CI import failures to `git add -u` excluding 13 new product files while committing their consumers. Reviewed the exact missing frontend components/helpers/tests and backend catalogue class, versioned Student policies, V33 and upgrade regression; no application logic or CI gate was changed.
- Verified a separate ignored snapshot of HEAD 8dd578d plus exactly those 13 files, without untracked .agent artifacts or local environment files: frontend check with VITE_CORE_V2_ENABLED=true passed 436 tests/lint/build; backend verify passed 566 tests with 6 existing opt-in skips (572 discovered), zero failure/error. Relative-import/case checks and added-file whitespace checks passed.
- Supplied an explicit follow-up commit allowlist including this log entry. No stage/commit/push, provider call, application rebuild or canonical database operation was performed by Sol. Remote CI after the follow-up push remains pending.
