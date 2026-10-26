# AI Usage Log — Career Simulation System

## UC-04 final verification and evidence closure — 2026-10-25

PO authorized read-only diff/runtime/database checks, focused isolated tests, evidence reconciliation and staging/PR preparation. Codex preserved application code, all applied SQL, two worktrees and the current application services; performed no Git write. PO reported 14 PASS / 1 PARTIAL (cursor browser navigation) / 1 BLOCKED (real Google) / 0 FAIL; dropdown TC-04-04A passed all eight checks. These are PO-reported results, not a Codex-observed full UAT session.

Codex executed frontend 19 PASS with scoped lint and Core-enabled production build PASS; backend 14 PASS including synthetic Testcontainers cursor traversal, DTO non-disclosure, fresh V1–V27 and V22→V25 upgrade. Current local SQL CRC32 values match V23–V27 history read-only. Reconciled D03/D09, tracked survey absence as UC-05 review D10, and repaired links after evidence moved to docs/evidence/US-04. No extra UI pagination test was applied after the tool rejected its patch; existing backend cursor tests were used. Held copied taglines/media mappings and third-party-content screenshots from public/shared publication pending rights review; flagged V27 published-content hash integrity and shared App.jsx/main integration. Exact conditional staging groups and draft PR text are in the existing verification report. Full suites/live Google/populated V26–V27 correction coverage remain outside current PASS claims. Human integration/content review and final Git approval remain pending.

## UC-04 UAT correction — 2026-10-25

PO approved career-track dropdown after observing exact-match text input failures. Codex used UI UX Pro Max guidance and existing public catalogue contracts; changed existing catalogue page, EN/VI strings, spacing, focused tests and existing plan/evidence. Rejected hardcoded track names and deriving options only from filtered/page-one results. No runtime AI/private data, backend/schema changes, Docker refresh or Git publication. Focused browse Vitest 19/19 PASS outside sandbox after cache EPERM; scoped ESLint/build PASS. Existing tests corrected filesystem CSS loading and the false conflation of self-review with a review/rating system. Human browser acceptance and frontend container refresh remain pending PO action/approval.

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

| ID    | Responsible contributor / tool                                                       | Key prompt or instruction _(summary; verify actual transcript)_                                                                                                                                                                                                                                                                                                                                                                                                                                          | Main AI-assisted output                                                                                                                                                   | Human verification / changes and evidence                                                                                                                                                                                                                                                                                                                                                                                                                                       | Status                                                                |
| ----- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| AI-01 | **Owner: confirm**; ChatGPT (requirements/design assistant) and AI-assisted planning | Align frozen Requirements Baseline v1/DR1–DR2 to Target Design v1.2, preserve accepted rules, identify safe P1–P6 implementation sequence.                                                                                                                                                                                                                                                                                                                                                               | Core/Agent/ERD design notes, architecture contracts and implementation-plan support.                                                                                      | Team must confirm diagram/cardinality and requirements review. See `docs/requirements/CareerSim_Requirements_Baseline_v1_Chat2_Handoff.md`, `docs/architecture/data-model/`, architecture documentation.                                                                                                                                                                                                                                                                        | Human reviewer and precise prompt: **TO CONFIRM**                     |
| AI-02 | **Owner: confirm**; Sol (AI coding assistant)                                        | Implement P1 authentication/security and P2 versioned Core workflows without altering older migrations; test against isolated PostgreSQL.                                                                                                                                                                                                                                                                                                                                                                | Identity/security, assessments, Enterprise scenarios, Student simulations, tests.                                                                                         | PR [#2](https://github.com/han-labs/career-simulation-platform/pull/2); source under `backend/src/main/java/edu/hcmute/careersim/identity/`, `assessment/`, `simulation/`; corresponding backend/frontend tests. Live Google OAuth **not verified** by these tests.                                                                                                                                                                                                             | Automated evidence available; human reviewer: **TO CONFIRM**          |
| AI-03 | **Owner: confirm**; Sol (AI coding assistant)                                        | Implement P3 Knowledge Base Studio and P4 Agent Builder, immutable versioned configurations, preview/publish/assignment, scoped KB and execution tracking; retain V1–V20.                                                                                                                                                                                                                                                                                                                                | KB ingestion/retrieval, Agent administration, native-provider adapters, V21, React Admin screens and tests.                                                               | PR [#3](https://github.com/han-labs/career-simulation-platform/pull/3); `docs/api/agent-platform-p3-p4.md`; `backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentPlatformIntegrationTest.java`. See **FIX-01** and **FIX-02** below.                                                                                                                                                                                                                                 | Automated evidence available; human reviewer: **TO CONFIRM**          |
| AI-04 | **Owner: confirm**; Sol (AI coding assistant)                                        | Implement P5 Student Agent with authorized tools/SSE/history and P6 InsightReport, confirmed plan and Admin usage, preserving earlier Core/KB behavior; add only additive migration.                                                                                                                                                                                                                                                                                                                     | Student Agent, read-only tools, session/history/streaming, structured outputs, V22, usage dashboard and tests.                                                            | PR [#4](https://github.com/han-labs/career-simulation-platform/pull/4); `docs/api/student-agent-p5-p6.md`; `StudentAgentIntegrationTest.java`, `AgentEvaluationTest.java`, `StudentAgentUpgradeIntegrationTest.java`. 20 deterministic evaluation cases are **not** 20 live-LLM cases; AC-A09/A17 remain pending live/human review.                                                                                                                                             | Automated evidence available; human reviewer: **TO CONFIRM**          |
| AI-05 | **Owner: confirm**; ChatGPT used for report/specification drafting (Chat 3)          | Reconcile 13 selected report stories / 16 use cases with frozen 15-story baseline, design, code evidence and real acceptance gaps.                                                                                                                                                                                                                                                                                                                                                                       | Academic report draft, requirement/use-case wording, traceability support.                                                                                                | Human must review approved business decisions, story/UC IDs, citations and unverifiable claims before submission. Do not claim the document is validated only because AI drafted it.                                                                                                                                                                                                                                                                                            | Exact prompts, editor, and reviewed report version: **TO CONFIRM**    |
| AI-06 | **Owner: confirm**; ChatGPT image generation / ChatGPT HTML generation (Chat 2)      | Create an architecture figure and interactive explanation of frontend, Spring Boot backend, PostgreSQL/pgvector, provider and one AI request.                                                                                                                                                                                                                                                                                                                                                            | Architecture illustration and interactive learning HTML.                                                                                                                  | Use only if included in final deliverables. Human must correct conceptual inaccuracies: pgvector is **inside** PostgreSQL; actual adapters are Gemini/OpenAI; model tool requests are executed/authorized by the backend; SSE events can stream before final completion.                                                                                                                                                                                                        | Inclusion and final reviewed asset: **TO CONFIRM**                    |
| AI-07 | **Owner: confirm**; ChatGPT (GPT-6-Astra), Codex (GPT-5.6-Sol)                       | Audit and implement US-04/UC-04 using the existing Core V2 architecture: public database-backed catalogue/detail, server-side search/filter/sort/cursor pagination, nullable `industry_code=IT`, Forage-inspired CareerSim UI, safe login return, no locked answers, and minimal targeted tests. Later prompts required four compact cards per row, two-line summaries, icon/value metadata, continuous Overview/Video/Tasks with anchor navigation, public task descriptions and practiced-skill chips. | V23 industry contract and tests; V24-V27 local public media/content mappings; catalogue/detail/login-return UI; EN/VI/CSP changes; targeted tests and evidence documents. | PO approved each checkpoint and local study data. Manager reviewed diffs, rejected using `careerTrack` as industry, prevented frontend fixtures and open redirects, kept recommendations/reviews/Feature 2-4 out of scope, and distinguished skipped/blocked tests from PASS. `VersionedCatalogControllerTest` passed 3/3, scoped ESLint and diff check passed, Docker frontend build passed. Final browser UAT, isolated PostgreSQL rerun and final commit SHA remain pending. | Implemented on `feature/browse-simulations`; human acceptance pending |

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

### Contributor B — [Full name / confirmed Git alias]

| Date   | Tool                   | Main code/files or documentation | Real key prompt or faithful short summary | Human checks / edits / tests            | PR/commit |
| ------ | ---------------------- | -------------------------------- | ----------------------------------------- | --------------------------------------- | --------- |
| [date] | [tool or `No AI used`] | [path / module]                  | [prompt, or N/A]                          | [what YOU actually verified or changed] | [link]    |

**Contributor approval:** [name] — [date] — [confirmed / corrections requested]

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

| Case | Observed failure and correction | Verification boundary |
|---|---|---|
| UC07-FIX-01 | A generated immutability test expected raw `IllegalStateException`; the real Spring Repository boundary translates it to `InvalidDataAccessApiUsageException`. The test failed against PostgreSQL. Corrected the expected boundary type and retained the immutable-snapshot error/message assertion; did not weaken the guard. | `AgentPlatformIntegrationTest.attemptSnapshotsAreImmutableFencedAndMergeLateUsageIntoTheirOwnGeneration`; C2/C4 rerun passed. |
| UC07-FIX-02 | Generated UI added `Knowledge base unavailable.` without its VI dictionary entry. Full frontend check failed: 1 failure / 105 passes. Added the missing translation; did not exclude the string or disable translation coverage. | `translate.test.js` and subsequent full `npm run check` passed with 106 tests. |
| UC07-FIX-03 | A generated KB_TEST aggregate test used `Instant.EPOCH`, violating the existing maximum 366-day report range. The real service rejected the request. Corrected the fixture window to one hour, preserving the production range/privacy contract. | `kbTestUsageCountsExecutionsNotIngestionOrReloadAndPreservesUnknownMetrics`; final outcome recorded in completion report. |

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
