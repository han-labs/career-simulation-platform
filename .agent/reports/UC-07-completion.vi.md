# UC-07 / selected US-08 — implementation evidence

Ngày: 2026-10-23. Baseline/HEAD: `62bbe1012e9e5c03dad1719b84cd40fff5fce499`.
Branch: `feat/uc07-admin-knowledge-base`; sole original checkout. Approved plan:
`../plans/UC-07-manage-knowledge-base.md` (preserved unchanged).

## Delivery boundary

| Checkpoint | Code and self-review evidence |
|---|---|
| C1 | Create/Edit dialogs, persisted per-account/KB drafts, pinned revisions, explicit discard/reload, no duplicate create after successful write/list failure; late-read isolation. |
| C2 | Ordered normalized parser blocks captured before chunking; pre-embedding attempt snapshot, immutable staged-version snapshot; existing JSONB only. Short locked merge keeps one entry per generation and actual late usage. Source/parsing/snapshot/embedding/staging errors separated. Lists exclude extraction text. |
| C3 | Exact attempt generation/version inspector with Extracted text and Chunks views, locators/provenance, Unicode, honest legacy-unavailable state and late-version rejection. No reconstruction from chunks or invented highlights. |
| C4 | Separate published version/latest attempt, resumed job polling after reload, upload/replace/retry/cancel, explicit publish/retire confirmation; reviewed revision is retained across refresh. Reindex A reuses only valid identical indexes without embedding/new versions. Retired indexes cannot resurrect. |
| C5 | Retrieve passages / Answer with sources, accepted input snapshot independent of editable draft, authorized evidence retained on answer failure, focus/reload/lifecycle source revalidation. Existing UC-06/08 and KB_TEST→UC-09 contracts are regression consumers. |

This is implementation and automated engineering evidence, **not Human PASS**.
Browser/live/deployment limitations below remain explicit acceptance gates.

## Contracts, schema and compatibility

- No migration added/edited; V1–V22, chunk text/order, parser profile,
  embedding profile/dimension, public DTOs, auth/CSRF and grant scopes retained.
- Existing ingestion `attempt_history` stores namespaced `extractionSnapshot`,
  keyed by generation. Existing version `locator_metadata` stores the matching
  immutable snapshot under Admin detail `provenance.extractionSnapshot`.
- Snapshot v1: input identity, parser profile, UTF-16 text-unit label, ordered
  normalized blocks with actual locators, warnings. Snapshots are bounded and
  absent from job/asset lists, retrieval tools, usage aggregates and ordinary logs.
- Old data without snapshots stays readable; no destructive backfill. Processing
  remains explicit synchronous bounded retry, not a new queue/background worker.
- Failed snapshot storage prevents embedding; failed staging/publication rolls
  back while preserving prior published sources. Late cancelled/superseded calls
  retain only their own actual observations and cannot stage another generation.
- Existing routes only. Save/publish revisions and upload/test request identity
  remain authoritative; no provider call on inspection, refresh or mode change.
- Admin Preview/Student tools, private evidence, Objective scores and UC-09 usage
  aggregation rules are unchanged. SourcePassages/useExecution changes are narrow
  compatible invalidation safeguards, also covered by Builder/Student regression.

## Commands and observed results

Raw engineering output remains in ignored `backend/target/uc07-*.log` locally;
it is not staged/published or a source of live-model/human claims.

| Command/check | Observed result |
|---|---|
| Frontend targeted C1 + existing platform component suites | PASS 7 tests |
| Parser + existing AgentPlatformIntegrationTest first regression | PASS 37 tests, isolated PG/pgvector and mocked providers |
| C2 snapshot/parser targeted run | Initially 1 test assertion failure (Spring exception translation); corrected without changing the guard |
| Frontend inspector/workspace + platform suites | PASS 9 tests |
| Parser + platform C2/C4 rerun | PASS 42 tests, zero failures/errors/skips; includes snapshot/storage/immutability, reindex, rollback and cancellation |
| Frontend KB/Builder/Student/shared HTTP targeted suites | PASS 24 tests |
| Full frontend first check | 1 failure / 105 passes: missing VI label; corrected |
| `npm run check` default | PASS lint + 106 tests + build |
| `npm run check` with Core V2 + Agent Platform enabled | PASS lint + 106 tests + build; non-failing bundle-size warning (over 500 kB) |
| KB_TEST usage targeted run | Initially rejected the generated test's too-wide date window; fixed fixture to respect the existing 366-day contract. Final full verify passes this test |
| Backend `.\mvnw.cmd verify` | PASS: 316 discovered, 311 passed, 0 failures/errors, 5 skipped (4 opt-in live cases + isolated browser fixture); JAR, Spotless and JaCoCo completed |
| `docker compose config --quiet` | PASS; no startup/restart/data operation |
| `git diff --check` | PASS |

All new test corpus content is synthetic. Native/wire mocks and deterministic
validators do not prove live provider accuracy, account access or corpus quality.
Live-provider test flags explicitly disabled in the final backend command.

## Browser and Owner acceptance checklist

**Chrome workflow: NOT RUN.** Browser inventory contained only in-app/MCP surfaces;
attempting Chrome returned `Browser is not available: chrome`. Gateway health at
`http://localhost:8080/api/health` refused connection; `docker compose ps` returned
no running services. No console/network PASS is claimed; network observation is
limited to that refused health request. No Owner Compose service was started,
restarted, migrated or removed. Human UAT remains **PENDING**.

Team Lead after their own `docker compose up -d --build`:

1. Align existing Core/Agent backend/frontend flags and cookie/origin policy;
   check `/api/health`, then open `/admin/knowledge` in Chrome as active Admin.
2. Create/select/edit empty KB; keyboard focus/Escape/Tab; retain draft over
   close/reload; two-tab stale save conflict; repeat at 320/390 px viewport.
3. With separately authorized provider budget, use only synthetic TXT/MD/PDF/
   DOCX/XLSX fixtures. Inspect normalized text, exact chunks/locators and metadata;
   unsupported/NUL/encrypted/scanned/formula inputs must fail safely. Do not use
   a live provider to manufacture failure cases: those are isolated test evidence.
4. Reload a failed embedding attempt: saved extraction is inspectable. Legacy
   versions without a snapshot explicitly say unavailable. Switch versions/KBs
   quickly without late responses replacing the new selection.
5. Replacement READY remains unpublished; failed/cancelled replacement retains
   old content. Inspect and explicitly publish; stale review must conflict.
   Re-select identical file/metadata/profile: unchanged, no claimed rebuild.
6. Test both modes; edit draft question after submit; results retain accepted
   query/mode. Retrieval-only makes no answer call; generation failure retains
   only authorized passages. Retire/hide then reload/focus: stale source denied.
7. Logout/account switch, Student/Enterprise/locked-account denials and CSRF;
   verify existing Builder and assigned Student Agent retain their scoped grants.
   Check KB_TEST usage filter/count/status/unknown metrics without private text.
8. Inspect actual Chrome console/network for errors and record expected/actual,
   reviewed commit/source/config/model and human reviewer/date. Do not mark
   live-model/human acceptance from this automated report.

## Actual files edited by this task

Repository-relative inventory; Owner's 20 pre-staged reference changes/deletions
and the pre-existing untracked plan are **excluded** from this implementation list.

```text
backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/dao/KnowledgeStudioDao.java
backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/infrastructure/KnowledgeParser.java
backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/service/impl/KnowledgeStudioServiceImpl.java
backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentPlatformIntegrationTest.java
backend/src/test/java/edu/hcmute/careersim/guidance/knowledge/KnowledgeParserContractTest.java
frontend/src/features/admin/knowledge/KnowledgeStudioPage.jsx
frontend/src/features/admin/knowledge/KnowledgeBaseDialog.jsx
frontend/src/features/admin/knowledge/KnowledgeDocumentInspector.jsx
frontend/src/features/admin/knowledge/KnowledgeBaseTestPanel.jsx
frontend/src/features/admin/knowledge/knowledgeStudio.test.jsx
frontend/src/features/admin/agentPlatform/ExecutionResult.jsx
frontend/src/features/admin/agentPlatform/SourcePassages.jsx
frontend/src/features/admin/agentPlatform/useExecution.js
frontend/src/features/admin/agentPlatform/platformIntegration.test.jsx
frontend/src/features/admin/agentPlatform/agentPlatform.css
frontend/src/shared/i18n/agentMessages.js
docs/api/agent-platform-p3-p4.md
docs/requirements/traceability-matrix.md
docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md
AI_USAGE.md
.agent/reports/UC-07-completion.vi.md
```

## Preservation and remaining limits

- Original index hash, staged raw diff/numstat and plan hash compared equal after
  implementation. Owner's 20 staged changes and intentional deletions remain.
- No branch switch, worktree creation, reset/stash/clean/rebase/cherry-pick,
  staging, commit, push, PR or merge. No extra repository/project/Compose stack.
- Schema suitability was demonstrated by existing V21 guards and isolated
  PostgreSQL tests; no schema decision/escalation was needed.
- Snapshot raw-content retention/backup purge, approved real corpus, provider
  credentials/quota, AC-A09/full AC-A17 and deployed NFR/UAT are not newly claimed
  complete. Existing synchronous ingestion/deadline and snapshot size bounds remain.
- At task end review the unstaged implementation diff separately from Owner's
  index. No publication is authorized by this handoff.

## Final git status --short

The 20 `A `/`D ` staged rows below belong to the Owner. The untracked approved
plan also pre-existed; neither is a change produced by this implementation.
All implementation changes remain unstaged.

```text
A  .agent/references/MTSE-FinalProjectReport.docx
D  .agent/references/Proposal_CareerSimulationPlatform.docx
D  .agent/references/Proposal_CareerSimulationPlatform_Updated.docx
A  .agent/references/build-rag-agent/SKILL.md
A  .agent/references/build-rag-agent/references/implementation-guide.md
A  .agent/references/build-rag-agent/references/lessons-learned.md
A  .agent/references/system-architecture-diagram.png
D  .agent/references/system-architecture.png
 M AI_USAGE.md
 M backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/dao/KnowledgeStudioDao.java
 M backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/infrastructure/KnowledgeParser.java
 M backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/service/impl/KnowledgeStudioServiceImpl.java
 M backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentPlatformIntegrationTest.java
 M backend/src/test/java/edu/hcmute/careersim/guidance/knowledge/KnowledgeParserContractTest.java
 M docs/api/agent-platform-p3-p4.md
D  docs/architecture/data-model/CareerSim_Lean_Logical_ERD_v2.mmd
D  docs/architecture/data-model/CareerSim_v3_Agent_Class_Diagram.mmd
D  docs/architecture/data-model/CareerSim_v3_Class_Design_Notes.md
D  docs/architecture/data-model/CareerSim_v3_Core_Class_Diagram.mmd
D  docs/architecture/data-model/physical-design-draft/01_PHYSICAL_DATABASE_DESIGN.md
D  docs/architecture/data-model/physical-design-draft/02_LEGACY_MAPPING_AND_MIGRATION.md
D  docs/architecture/data-model/physical-design-draft/03_BACKEND_AND_AGENT_ARCHITECTURE.md
D  docs/architecture/data-model/physical-design-draft/04_SOL_MASTER_IMPLEMENTATION_PLAN.md
D  docs/architecture/data-model/physical-design-draft/SOL_HANDOFF.md
D  docs/architecture/syn-agent-rag-voice-analysis.vi.md
D  docs/architecture/syn-coach-abc.vi.md
D  docs/architecture/syn-core-rag-implementation.vi.md
 M docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md
 M docs/requirements/traceability-matrix.md
 M frontend/src/features/admin/agentPlatform/ExecutionResult.jsx
 M frontend/src/features/admin/agentPlatform/SourcePassages.jsx
 M frontend/src/features/admin/agentPlatform/agentPlatform.css
 M frontend/src/features/admin/agentPlatform/platformIntegration.test.jsx
 M frontend/src/features/admin/agentPlatform/useExecution.js
 M frontend/src/features/admin/knowledge/KnowledgeStudioPage.jsx
 M frontend/src/shared/i18n/agentMessages.js
?? .agent/plans/UC-07-manage-knowledge-base.md
?? .agent/reports/UC-07-completion.vi.md
?? frontend/src/features/admin/knowledge/KnowledgeBaseDialog.jsx
?? frontend/src/features/admin/knowledge/KnowledgeBaseTestPanel.jsx
?? frontend/src/features/admin/knowledge/KnowledgeDocumentInspector.jsx
?? frontend/src/features/admin/knowledge/knowledgeStudio.test.jsx
```
