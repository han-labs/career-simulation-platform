# Current report and repository alignment

Checked 2026-10-22. This register records documentation provenance, verified source behavior and unresolved wording; it does not approve new business rules or claim new runtime acceptance.

## Source and repository baseline

- Fetched `origin/main`: **5486f9f4a3a56519c396bcebffd49fe0ff4776e2**. P1/P2 merge `98f664c`, P3/P4 `1b11b59`, P5/P6 `2dffd6a`; main's later `5486f9f` adds `AI_USAGE.md` only. Local branch remains `feat/student-agent-and-ai-reporting`, HEAD `6a3e9893e359e438ab2e97d6f7632891b88f5dd8`; its application tree matches checked main. Local `AI_USAGE.md` is identical to main although untracked at this older checkout.
- Current report read: **MTSE-FinalProjectReport (1).docx**, SHA-256 `cd06cbc20bc8bfc43bf6ff5a88a05b0b02092df5ad7bff675df6effb8c6b5468`. Section 1.3 has ten selected stories; section 1.5/catalogue has sixteen UC and nine detailed selected UC. The report's source tables confirm UC-08 Create AI agent and the current order.
- Supplement read: **MTSE-FinalProjectReport_Requirements_Design_Ready_2026-10-21 (1).docx**, SHA-256 `b7d6d5d10a83dfeb5bf30cfb9c081365a83ca0e607f482f224fef6ecae9c0dfc`. It supplies the seven explicitly labelled historical supplementary specs; their old-to-new numbering is retained in [the UC index](use-cases/README.md).
- CORRECTED ZIP SHA-256 `4de9f5b38864f89699d1bfaa89283e7f2c21f1f6f87df4d1a59edacdaf7dac57`: imported twenty reviewed Markdown files under `docs/` (sixteen UC, UC index, ten-US document, team allocation and refresh checklist). Archive paths were checked before writing; no existing destination was overwritten. `README-INTEGRATE.md` was read as source instructions, not installed as an agent entry point or executed as a Git script.
- Report DOCX/ZIP binaries were not copied into the repository. Source paths here are repo-relative; no credentials, contact details or personal local paths are reproduced.
- [Frozen Baseline v1/DR1–DR2](CareerSim_Requirements_Baseline_v1_Chat2_Handoff.md), Target v1.2 sources, all historical reports and V1–V22 SQL remain unchanged. Existing dirty Nginx changes, nine user-deleted historical design files and untracked evidence are preserved.

## Confirmed allocation

| Official person slot | Selected UC | Selected report US |
|---|---|---|
| Person 1 | UC-01, UC-02, UC-03 | US-01, US-02, US-03 |
| Person 2 | UC-04, UC-05, UC-09 | US-04, US-05, US-06, US-10 |
| Person 3 (project lead) | UC-06, UC-07, UC-08 | US-07, US-08, US-09 |

This is confirmed by the user's current instruction, superseding the ZIP's initial “proposed ownership” sentence. Exact names/Git identities and reviewer rotation are still pending. UC-10–16 have no new selected-work allocation; UC-14 is deferred. Do not inherit historical named frontend allocations as today's person mapping.

## Discrepancies requiring explicit reconciliation

Paths in this table are relative to repository root. Existing tests are evidence locations, not checks executed by this synchronization.

| ID / scope | Report or handoff wording | As-built evidence | Current disposition |
|---|---|---|---|
| D01 — UC-05 completion/self-review | Postconditions/main flow require all required tasks **and self-reviews** before overall completion. Current DOCX contains this sentence; imported prose is preserved. | `backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/TaskSubmissionServiceImpl.java:125` invokes completion at FINAL. `CompletionServiceImpl.java:29–33` in that directory checks required FINAL count, not final review. `simulation/domain/TaskProgressionPolicy.java:20` gates the next task by FINAL + reviewed. `backend/src/test/java/edu/hcmute/careersim/system/migration/CorePackageAIntegrationTest.java:55` covers quiz zero and explicit review. Approved P2 contract preserves this interpretation of DR1–DR2. | **OPEN, Person 2 + Chat 3/lead.** Do not add a final-review completion prerequisite or fabricate reviews/completions from report import. Last FINAL may complete before its own review; preserve historical version policy. |
| D02 — UC-05 new attempt/evaluation | Alternative 1a mentions Start a New Attempt; 5a/5b and exceptions mention asynchronous rubric/artifact evaluation. | `simulation/service/impl/EnrollmentServiceImpl.java:32` delegates to enrollment create/pinning; unique owner/version enrollment is retained in V17. `TaskSubmissionServiceImpl.finalizeTask` validates deliverables and optionally calls `QuizTaskEvaluator`, not a practical auto-grading workflow. Core tests cover single final/idempotency and practical completion without grading. | **OPEN wording/scope.** No general same-version retry/new-attempt or asynchronous practical grader is established. Do not invent either, a pass threshold, or grading prerequisites. |
| D03 — UC-04 recommendations | Alternatives 2a/2b highlight recommendations or mark them preliminary. | `CoreCatalogPage` and `VersionedCatalogServiceImpl` implement an ordinary public catalogue. | **RESOLVED for Feature 1 by explicit PO decision; see D09.** Recommendations are excluded, not a failed UC-04 acceptance item. Historical imported source remains preserved. |
| D04 — UC-06 plan editing / selected US-07 AC | Actor may review and **edit** a proposal before confirm. | `backend/src/main/java/edu/hcmute/careersim/guidance/agent/service/impl/StudentAgentServiceImpl.java:548–586` rechecks source execution, canonical hash, authority and active plan revision, then saves the server-held suggestion. `frontend/src/features/agents/StudentAgentPage.jsx:59` displays exact suggestion confirmation without an arbitrary suggestion editor. `StudentAgentIntegrationTest.invalidReportCannotPublishAndSuggestionRequiresExactExplicitConfirmation` protects this boundary. | **OPEN, Person 3 + Chat 3.** Exact-source/hash confirmation exists; arbitrary client edits do not. Do not weaken validation or relabel editable support as accepted. |
| D05 — UC-08 naming | Catalogue/heading: **Create AI agent**; inner source cell: **Build AI agent**. | Existing Agent registry/configuration/preview/publish/assignment APIs are unchanged; `AgentAdminController` and `AgentRegistryServiceImpl` implement the lifecycle. | **Documentation name resolved by current catalogue: Create AI agent.** Retain the Build source note; no class/API rename or business change. |
| D06 — Report frontend technology | Current DOCX technology table says React 19/Vite 8/**TypeScript**. | `frontend/src` production source is JavaScript/JSX; no `.ts`/`.tsx` source was found. `frontend/package.json` defines the actual toolchain. | **Report correction pending**, not permission to migrate frontend language. Documentation states React/Vite JavaScript/JSX. |
| D07 — UC-07 ingestion/publication | Main flow says a complete document version becomes active/retrievable; implicit publication could be inferred. | `guidance/knowledge/service/impl/KnowledgeStudioServiceImpl` separates upload/retry/READY staging from explicit confirmed `publish`; failed replacements preserve the prior published version. `AgentPlatformIntegrationTest.stagedFailuresAndCancellationNeverReplacePublishedEvidenceAndRetriesDeduplicate` protects the distinction. | Read together with versioned publication and explicit Admin action. **Ingested/READY is not automatically published.** Clarify source wording before changing lifecycle. |
| D08 — Proxy deployment evidence | Prior architecture doc describes dedicated 90-second SSE locations. | Those locations remain pre-existing local changes in `frontend/nginx.conf` and `deployment/nginx/default.conf`. Main `AgentEventStreamController.java:60–61` emits no-store/`X-Accel-Buffering: no`; keepalive exists. Older gateway/browser tests mounted working files. | **As-merged/deployed 90s config NOT VERIFIED.** Both configs are preserved, not modified/merged/tested here. Architecture/deployment indexes now warn about this distinction. |
| D09 — UC-04 authentication/recommendations | Preserved UC-04 source says the actor is authenticated and alternatives 2a/2b highlight recommendations. | The PO explicitly approved a public ordinary catalogue with no recommendations for Feature 1 on 2026-10-24. Checkpoint 1 adds only independent `scenario.industry_code` classification/filtering; it does not use assessment evidence or recommendation ranking. | **Explicit PO decision supersedes implementation behavior, historical source preserved.** Public access and removal of recommendation behavior are implemented only in their approved checkpoints; do not rewrite the imported UC source. |
| D10 — UC-05 pre-enrollment survey review | PO observed no pre-enrollment survey during UC-04 UAT on 2026-10-25. | Current Start reuses existing enrollment; no survey step is implemented by Feature 1. | **UC-05 requirements-review item**, not an automatic UC-04 defect or permission to implement a survey. Review current report/Baseline and code after UC-04 closure; obtain PO approval before changes. |

Other source conditions, proposed English UI messages and supplementary flows are not automatically exact accepted UI strings or complete endpoint coverage. UC-10–13/15–16 retain historical supplementary review status; UC-14 is not implemented/assigned by this import.

## Source/test traceability and acceptance limits

Each selected UC links its matching production controller/component and existing tests in its Verification tracking section. [Traceability](traceability-matrix.md) maps functional Baseline coverage without equating catalogues. Legacy three-story and former thirteen-story materials are labelled historical rather than rewritten or deleted.

Prior local P5/P6 evidence records backend 305 PASS/5 SKIP, frontend 97 PASS per mode and isolated V22 upgrade/regressions. Those dated records are not a fresh run on `5486f9f`. No full suite, application startup, DB migration, paid provider, browser UAT or live Google verification ran in this documentation task. AC-A09/full AC-A17 and relevant live/human/deployment gates remain pending. Twenty deterministic schema/evidence cases are not twenty reviewed live-model cases.

Current rules require at least twenty evaluation cases, expected/actual results and before/after comparison; historical ten-case milestones are preserved as history. No NFR ID, latency target, retention TTL, consent or business permission was invented.

## Git publication boundary

The owner creates the PR. This task does not switch branches, stage, commit, push or deploy. `.gitignore` still re-includes `.agent/**`; ignored root `.env` does not make nested files/logs safe. The tracked debug file `how --format=fuller HEAD` needs metadata review separately; no content or contact value is copied here.

Use an explicit documentation whitelist, inspect pre-existing hunks (especially the already-dirty `.agent/PROJECT_CONTEXT.md`, `TASKS.md`, `DESIGN.md`), and review `git diff --cached --name-status`, `git diff --cached --check` and the staged content before the owner's commit. Never stage the entire repository, all `.agent/`, old raw logs, JSON evidence, screenshots or the source report binaries. Existing deletions must not enter this PR inadvertently.

### Documentation validation

Observed static results (documentation checks only):

- UC-01–16 unique/continuous; ten selected US-01–10; all nine selected UC owner fields match the official Person allocation; UC-14 remains deferred.
- All **16 source flow bodies** (Trigger through Exception flows) match the CORRECTED ZIP exactly; the selected-US document retains the complete original source prefix/ACs. All **54 selected acceptance questions** were also found in the actual current DOCX text after whitespace/quote normalization. Additional source/test notes are separate from the prose.
- **197 current internal link/anchor destinations checked, 0 failures**, including final added links; historical preserved links also had 0 failures.
- All **719 original inventory entries** remain accounted for. Protected digest outside the explicit documentation edit list matches; application code, tests, V1–V22, frozen Baseline, Target v1.2, historical evidence, Nginx changes and original deletions are preserved.
- The **13 documents** given current notices/historical sections retain their original bytes inside preservation markers, including all three pre-existing dirty `.agent` documents.
- `git diff --check` passed; index remains empty (0 staged files). New files are Markdown only. No full test suite or runtime check was executed.
- New/current documentation sections produced **0 provider-token/private-key/JWT/personal-local-path pattern flags** in the scoped check. This is not a guarantee that the entire repository history is secret-free; existing raw evidence/contact metadata must still be reviewed before sharing.

## Exact documentation change list

**21 existing files updated** (the list excludes pre-existing Nginx changes/deletions):

```text
AGENTS.md
README.md
System_Architecture_Analysis.md
.agent/AGENTS.md
.agent/PROJECT_CONTEXT.md
.agent/TASKS.md
.agent/DESIGN.md
.agent/README.md
.agent/references/README.md
.agent/references/SOURCE_INDEX.md
.agent/rules/ai-assisted-development.md
.agent/rules/report-and-evidence.md
.agent/rules/testing-and-quality-gates.md
.agent/templates/feature-plan.md
docs/README.md
docs/api/README.md
docs/database/README.md
docs/deployment/README.md
docs/architecture/student-agent-p5-p6.md
docs/requirements/user-stories.md
docs/requirements/traceability-matrix.md
```

**23 new Markdown files:** sixteen `docs/requirements/use-cases/UC-*.md` specs and their `README.md`; `docs/requirements/user-stories/REPORT_SELECTED_10_US.md`; `docs/TEAM_ASSIGNMENTS.md`; `docs/REPO_DOCS_REFRESH_CHECKLIST.md`; `docs/requirements/README.md`; this alignment register; `.agent/reports/ai/2026-10-22-documentation-synchronization.md`. No source report binaries or JSON evidence were added. The previously untracked root `AI_USAGE.md` was not changed.

## Safe owner-operated PR preparation

1. Review `git status --short`, fetch latest main and preserve unrelated dirty/untracked changes before preparing a documentation branch. This task did not switch the older checkout to main; do not discard the existing root `AI_USAGE.md` or user deletions to make a branch switch work.
2. Inspect the three already-dirty `.agent` files separately. Use `git add -p -- .agent/PROJECT_CONTEXT.md .agent/TASKS.md .agent/DESIGN.md` if the owner's PR must exclude older hunks. Do not automatically include pre-existing user work.
3. Stage **only reviewed paths from the exact list above** plus the new UC directory, selected-US file, team/checklist/requirements-index/alignment Markdown. If including the assistance record, stage that single `.md` path, never all `.agent/reports/`.
4. Run `git diff --cached --name-status`, `git diff --cached --check` and `git diff --cached`; review for contact data, tokens and local paths. The intended PR has only approved Markdown additions/modifications, **no deletions, application/SQL/proxy changes, raw logs, JSON, screenshots or source report binaries**.
5. The repository owner alone commits/pushes/opens the PR against current main after reviewing the staged content. Reconcile main updates without overwriting preserved local work. No `git add -A`, hard reset, clean, valuable DB action or full-suite rerun is needed for this documentation handoff.
