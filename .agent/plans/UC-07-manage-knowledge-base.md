# UC-07 — Manage knowledge base / selected US-08

Status: **Approved — execution plan; implementation and acceptance evidence pending**  
Owner: **Person 3**; implementer: **Sol**; human reviewer: **Team Lead**  
Date: **2026-10-23**  
Approval: Team Lead approved the complete C1–C5 plan and **Reindex option A** in the planning conversation. This document records that decision; it does not claim implementation completion.

## 1 Scope

### Approved delivery and baseline

- Complete **all UC-07 / selected US-08**, one feature branch and one PR. C1–C5 are internal dependency/quality checkpoints, **not separate approval cycles and not permission to stop after C1**.
- Sole repository: `C:\Users\huynh\Study\Exercises\Team\MTSE\career-simulation-platform`.
- Verified branch at plan creation: `feat/uc07-admin-knowledge-base`.
- Verified base HEAD: `62bbe1012e9e5c03dad1719b84cd40fff5fce499` (`62bbe10`). Current application code is the merged P1–P6 foundation, not a proposal to rebuild it.
- Owner has 20 staged documentation changes: updated report/reference/architecture assets and intentional obsolete-document deletions. Preserve their index and working-tree contents. Do not restore deleted documents, use another checkout, or include unrelated Owner changes by staging everything.
- This save task creates only this plan. It does not change application code, tests, migrations, other documentation, `AI_USAGE.md`, data, or Git state.
- Future implementation must first verify its actual branch/HEAD/status. Do not switch branches, reset, clean, stash, create another worktree, commit, push or merge merely because this plan exists; follow explicit Owner instructions for those operations.

### Sources and authority

Read the relevant sources in this repository. CareerSim requirements, confirmed decisions and actual contracts take precedence over the prior-project blueprint.

1. [Root AGENTS](../../AGENTS.md), [.agent AGENTS](../AGENTS.md), [PROJECT_CONTEXT](../PROJECT_CONTEXT.md), [TASKS](../TASKS.md), [DESIGN](../DESIGN.md), [feature-plan template](../templates/feature-plan.md).
2. Rules: [architecture](../rules/architecture-and-patterns.md), [security/privacy/AI](../rules/security-privacy-and-ai.md), [testing](../rules/testing-and-quality-gates.md), [report/evidence](../rules/report-and-evidence.md), [AI assistance](../rules/ai-assisted-development.md).
3. [Latest report DOCX](../references/MTSE-FinalProjectReport.docx), especially §1.3.8 US-08 and §1.5.7 UC-07; [UC-07](../../docs/requirements/use-cases/UC-07-manage-knowledge-base.md); [selected US catalogue](../../docs/requirements/user-stories/REPORT_SELECTED_10_US.md); [requirements index](../../docs/requirements/README.md).
4. [Frozen Requirements Baseline v1 + DR1–DR2](../../docs/requirements/CareerSim_Requirements_Baseline_v1_Chat2_Handoff.md), [team assignments](../../docs/TEAM_ASSIGNMENTS.md), [traceability](../../docs/requirements/traceability-matrix.md), [alignment register](../../docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md).
5. [P3/P4 API](../../docs/api/agent-platform-p3-p4.md), [P5/P6 API](../../docs/api/student-agent-p5-p6.md); [Target Core v1.2](../../docs/architecture/data-model/CareerSim_Core_Domain_Model_ReqBaseline_v1_2.mmd), [Target Agent v1.2](../../docs/architecture/data-model/CareerSim_AI_Agent_Domain_Model_ReqBaseline_v1_2.mmd), [Target ERD v1.2](../../docs/architecture/data-model/CareerSim_Target_Logical_ERD_ReqBaseline_v1_2.mmd), [v1.2 notes](../../docs/architecture/data-model/CareerSim_v1_2_Design_Notes.md).
6. Prior-project [SKILL](../references/build-rag-agent/SKILL.md), [implementation guide](../references/build-rag-agent/references/implementation-guide.md) §15–17, §19–22 and [lessons learned](../references/build-rag-agent/references/lessons-learned.md). These are reference material, not independent execution authorization.
7. [AI_USAGE](../../AI_USAGE.md), [CI](../../.github/workflows/ci.yml), source and tests linked below.

Catalogue qualification: selected US-08 maps to UC-07, Person 3; it maps to **Baseline US-09**, not Baseline US-08. The report body and current UC/US agree. Some report TOC/overview numbering remains historical; do not silently renumber the catalogue. Record the scoped alignment clarification during implementation. No Final Project Assignment was located in the audited repository; do not invent its contents or rely on removed documents. Target diagrams are already mapped by P1–P6; they are not instructions to recreate schema.

### Actor, ownership and exclusions

- Actor: authenticated, active Administrator; current code resolves the `ADMIN` role through existing actor utilities. Preserve existing role naming/contracts.
- Person 3 owns UC-07. UC-06 Student Agent and UC-08 Agent Builder are regression consumers, not features to implement here. UC-09 usage/dashboard belongs to Person 2; coordinate any shared-contract impact without taking over dashboard work.
- Preserve modular monolith, existing KB grants, current platform KB scope, Syn/provider adapters, pgvector retrieval, explicit publication and account/CSRF conventions.
- Exclude enterprise KB authoring expansion, OCR, additional file formats, Python/second backend, new Agent framework, background queue, batch embedding, arbitrary parser/chunk/profile/dimension changes, forced duplicate versions, broad UI redesign and unrelated feature work.
- No migration is planned. Do not modify applied V1–V22, rewrite historical chunks, or discard source/version/job history. A newly demonstrated need for schema/security/scope changes requires a narrow Owner decision; do not preemptively create a migration.

### Acceptance criteria and verified gap matrix

AC1–AC6 below are local labels for the six selected US-08 acceptance bullets, not new catalogue IDs. `EXISTS` means source-verified implementation, not a fresh test PASS or human acceptance.

| AC / UC flows | Current implementation | Classification and approved work |
|---|---|---|
| AC1: create/manage a KB within permitted scope; Main 1–2, Alt 1a | Admin list/create/update/archive, empty KB, service authorization and revision checks; existing KB page | EXISTS + PARTIAL/BUG: introduce focused Create/Edit dialogs; pin draft revision. Refresh currently can replace `base.revision` while retaining an older draft; prevent lost-update behavior. |
| AC2: upload TXT/PDF/DOCX/MD/XLSX and inspect ingestion status; Main 3–4 | Five-format parser, private source files, durable jobs, retry/cancel, embeddings, staging and attempts | EXISTS + PARTIAL: accurate failed-stage diagnostics, snapshot before embedding, and resume job observation after reload. Local UI polling currently depends on local ingestion state. |
| AC3: review publication status and provenance before retrieval; Main 5, Alt 5a | Version/chunk/provenance inspection, READY separate from confirmed publish | EXISTS + MISSING: normalized extracted-text inspector and warnings. Current parser output does not persist a complete normalized extraction snapshot. |
| AC4: test passage retrieval separately from generated answer; Main 6, Alt 6a | Independent persisted KB_TEST, retrieval-only mode, scoped evidence | EXISTS + PARTIAL UX: two explicit modes, submitted-query snapshot, distinct retrieval/generation results and async-state safety. |
| AC5: clear invalid-upload/no-authorized-evidence feedback; Alt 6b, E3.1 | Parser validation, bounded files, no-evidence/refusal paths | EXISTS: verify format/MIME browser behavior and actionable errors; do not weaken signature/content validation. |
| AC6: retain published material when replacement fails; Alt 3a, E4.1–4.2 | Complete staging, atomic explicit publication, generation-fenced cancellation and deduplication | EXISTS + PARTIAL: active version separate from latest attempt, frozen reviewed revision on publish, truthful Retry/Replace/Reindex A outcomes. |
| E6.1 provider or output-validation failure | Execution retains authorized retrieval evidence on generation failure | EXISTS + PARTIAL UX: preserve passages while separately showing answer failure. |
| E6.2 revoked/deleted source | Service/runtime recheck active published source and authorization | EXISTS + PARTIAL UI: invalidate/revalidate stale results; source denial must not leave stale content presented as current evidence. |
| Preconditions: Admin/account/scope/CSRF | `actors.admin(p)`, active-account service checks, shared HTTP client; App shell remounts on account generation | EXISTS: regression required; do not replace auth or incorrectly claim missing account isolation. |

## 2 Flow inventory

### Main success flow and sequence-to-code

1. Admin opens KB Studio, creates or selects a permitted KB: feature page -> shared API client -> `KnowledgeAdminController` -> `KnowledgeStudioService`/implementation -> DAO.
2. Workspace displays metadata, documents, active publication and permitted actions. Metadata editing captures the revision when the draft begins.
3. Admin uploads a supported source and starts ingestion through existing asset/job contracts. Source remains private; reviewed metadata and request identity are validated.
4. Service claims an attempt generation, parses/normalizes with existing parser behavior, persists the attempt snapshot before embedding, invokes the existing embedding adapter, and atomically stages a complete READY version. No external provider call is made inside the final long-lived database transaction.
5. Admin inspects attempt diagnostics or version text/chunks/provenance. READY is not published. Explicit confirmation plus the reviewed revision controls publication.
6. Admin opens Test KB, submits a query in Retrieve passages or Answer with sources mode. Execution service retrieves within authorized scope; optional generation is independently validated. The UI binds results to the submitted query, KB and execution, not the editable draft.

### Alternate flows

- Alt 1a: create an empty KB; no fabricated searchable content.
- Alt 3a: Retry/Replace/Reindex use confirmed option A below. A new distinct fingerprint stages a new version while the old active version remains available until successful explicit publication.
- Alt 5a: inspect available extracted text, chunks, provenance and warnings for the selected attempt/version. Missing historical snapshots are explicitly unavailable; never reconstruct and label joined chunks as original normalized text.
- Alt 6a: retrieval-only does not call answer generation. Query embedding may still occur and must be accounted for separately.
- Alt 6b: no useful authorized evidence -> clear no-evidence result; no unsupported answer.

### Exception and fallback flows

- E3.1: unsupported/oversize/invalid/empty content -> safe actionable error, never publish it.
- E4.1: extraction, snapshot storage, embedding, staging or publication failure -> record/return the appropriate safe stage/error; preserve old published content. A successfully stored extraction remains inspectable after embedding failure. If snapshot persistence itself fails, report unavailable extraction and stop before embedding.
- E4.2: conflicting revision -> reject stale publication/update and require reload/review; do not silently retry with the newest revision.
- E6.1: provider timeout/quota/transport or generated-output validation failure -> answer fails/refuses according to existing runtime contract; valid authorized passages remain usable.
- E6.2: source unavailable/revoked -> no new retrieval; invalidate denied displayed source and return to permitted context. Revalidate after local lifecycle actions and relevant focus/reload/actions; do not promise instantaneous out-of-tab notification without infrastructure.
- Cancel/superseded attempt: late work may record its actual usage, but cannot stage content or overwrite another generation.

### Retry / Replace / Reindex A — CONFIRMED

| Action | Approved semantics |
|---|---|
| Retry | Retry an incomplete/failed ingestion with the same input identity using the existing generation mechanism. It is not a promise of a new content version. READY/PUBLISHED retry returning existing state must never be presented as a successful reindex. |
| Replace | Supply replacement source for the same asset. New fingerprint -> new staged version and explicit publication. Duplicate fingerprint -> truthful existing/unchanged outcome, not a fabricated new version. |
| Reindex A | With the same fingerprint/profile and a valid existing index, retain deduplication: **no new version, no claimed rebuild**. Report that the index is unchanged and identify the reused version. A genuinely new processing fingerprint follows normal staging/publication. Do not change profiles just to force this path. |

This is the Team Lead's confirmed clarification of UC-07 Alt 3a. **Do not reopen it as an unresolved blocker.** Preserve the existing fingerprint identity (source input hash + parser/embedding profile) and unique constraints. Do not relabel an invalid/retired index as valid unchanged content or silently resurrect retired versions.

Reuse upload/ingestion contracts when the Admin selects a source again; provide an explicit action/outcome in the UI. A one-click retained-server-source reindex contract is not implicitly approved or required by option A. Do not add a new route merely to rename retry. If a real inability to fulfill the approved flow is demonstrated, document that narrow gap before requesting a contract change.

## 3 Responsibility design

| Responsibility | Existing expert/controller/facade | Approved direction |
|---|---|---|
| HTTP, actor extraction, headers and DTOs | [KnowledgeAdminController](../../backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/controller/KnowledgeAdminController.java) | Keep controller thin; no lifecycle policy or direct DAO access. |
| Authorization and lifecycle orchestration | [KnowledgeStudioService](../../backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/service/KnowledgeStudioService.java), [KnowledgeStudioServiceImpl](../../backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/service/impl/KnowledgeStudioServiceImpl.java) | Own attempt/snapshot/publication decisions and short transaction boundaries. |
| Atomic persistence/projection | [KnowledgeStudioDao](../../backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/dao/KnowledgeStudioDao.java) | Generation-conditional writes, merge attempt evidence, immutable version metadata, lean lists. |
| Extraction and source-linked chunks | [KnowledgeParser](../../backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/infrastructure/KnowledgeParser.java) | Add normalized snapshot output without changing existing chunk text/profile semantics. |
| Provider integration | [GeminiEmbeddingProvider](../../backend/src/main/java/edu/hcmute/careersim/guidance/infrastructure/GeminiEmbeddingProvider.java) | Reuse existing adapter/profile/dimension and vector validation; no new batching framework. |
| Independent KB execution | [ExecutionServiceImpl](../../backend/src/main/java/edu/hcmute/careersim/guidance/agent/service/impl/ExecutionServiceImpl.java) | Preserve KB_TEST inputs, sources, validated results, authorization and actual usage. |
| UI orchestration | [KnowledgeStudioPage](../../frontend/src/features/admin/knowledge/KnowledgeStudioPage.jsx) | Focused dialogs/panels; keep policy on backend. |
| Shared API/result/source behavior | [platformApi](../../frontend/src/features/admin/agentPlatform/platformApi.js), [ExecutionResult](../../frontend/src/features/admin/agentPlatform/ExecutionResult.jsx), [SourcePassages](../../frontend/src/features/admin/agentPlatform/SourcePassages.jsx), [useExecution](../../frontend/src/features/admin/agentPlatform/useExecution.js) | Narrow compatible changes; UC-08 regression mandatory. |
| Usage consumer | [AgentUsageDao](../../backend/src/main/java/edu/hcmute/careersim/guidance/agent/dao/AgentUsageDao.java), [AgentUsageQuery](../../backend/src/main/java/edu/hcmute/careersim/guidance/agent/service/AgentUsageQuery.java) | Verify compatibility only; no UC-09 dashboard expansion. |

## 4 Change surface

### Target UX

- KB list with Create dialog; focused Edit dialog in selected KB workspace. Preserve existing design tokens, localization patterns and accessible labels/focus handling.
- Workspace: Documents and Test KB. Do not place every form and detail panel on screen at once.
- Documents: published version distinct from latest ingestion attempt; state-appropriate upload/replace, process/retry, cancel, inspect, publish and retire/archive actions. Resume observation of active jobs after reload; do not invent percentage progress or a background worker.
- Inspector: normalized Extracted text and Chunks views; provenance/warnings expand on demand. Bind requests to KB + attempt generation/version. Reject stale responses after switching selection.
- Test KB: explicit Retrieve passages / Answer with sources modes, captured query/mode for each execution, separate retrieval/generation status and errors.
- Loading, empty, validation, error, disabled/action-in-progress and narrow-viewport states are part of completion, not follow-up scope.

### Extraction snapshot persistence — approved no-migration approach

Source evidence: [V21](../../backend/src/main/resources/db/migration/V21__knowledge_studio_and_agent_execution_foundation.sql) defines `knowledge_asset_metadata.locator_metadata` as an object, tied by FK to a version. `guard_scoped_knowledge_content` permits only publication status/time updates after insertion. Current DAO staging inserts version/chunks/metadata after embedding succeeds; therefore **locator_metadata alone cannot preserve pre-version extraction on embedding failure**.

`knowledge_ingestion_job.attempt_history` is a JSONB array explicitly updateable by `guard_ingestion_job`. `metadata_snapshot` is immutable ingestion input and must not receive parsed output. Reuse these existing storage boundaries:

1. After successful parsing, store an extraction snapshot in the attempt entry keyed by `generation`, before calling embedding. Use a short, generation/state-conditional transaction.
2. Proposed namespaced snapshot shape: `extractionSnapshot` with its own representation version, input identity, parser profile, ordered normalized blocks, locators and warnings. Use existing extraction bounds; no unbounded raw source/binary copies. Representation versioning must not change parser/chunk fingerprint semantics.
3. On failure/cancel retain that attempt snapshot with accurate diagnostics. An old generation's snapshot must never be presented as the new attempt's output.
4. On successful staging copy the matching snapshot into a namespaced field of the new version's `locator_metadata` in the same atomic staging transaction. Preserve existing metadata keys and consumers.
5. Keep full snapshots private to authorized Admin detail inspection. Job/asset list projections omit full text. Do not add snapshots to usage summaries, ordinary logs, Agent prompts or unrestricted public/source DTOs.
6. Old jobs/versions without snapshots remain readable. Show missing snapshot honestly; no destructive backfill and no mutation of existing immutable version metadata.

Required implementation details and invariants:

- Current `finish()` appends metrics and `recordAttempt()` skips a generation already present. They must merge/complete the same attempt entry while retaining extraction and actual final usage; otherwise an early snapshot could suppress late usage or duplicate entries.
- Enforce generation fencing, immutable snapshot content within one attempt, atomic entry merge and no lost updates. Schema currently permits attempt-history updates but does **not** itself enforce all nested snapshot invariants: implement conditional DAO operations coordinated by the service and test them.
- Cancelled/superseded workers may attach their actual resource observations to their own attempt only; they cannot stage or overwrite a newer generation.
- Snapshot-write failure stops before embedding. Parsing failure without a valid output exposes diagnostics, not fabricated text.
- Preserve vectors, chunk ordering/content, source locators, embedding profile and current retrieval policy. Do not add offset highlights unless actual offsets and a consistent Unicode convention are available.
- No modifications to applied Flyway history. A schema change is not authorized by this approach; escalate only a demonstrated inadequacy, not stylistic preference for another table.

### Format behavior to preserve/test

| Format | Required behavior |
|---|---|
| TXT / MD | Existing UTF-8/content validation and limits, NUL rejection before provider, valid Unicode; browser MIME consistency. Do not casually alter paragraph/fence/chunk behavior. |
| PDF | Text/page provenance; safe failures for encrypted, image-only or unprocessable files; no OCR. |
| DOCX | Paragraph/table provenance and Office archive validation. |
| XLSX | Sheet/row/cell provenance and existing formula/archive policy; no spreadsheet evaluator. |
| All | Safe invalid/empty/oversize feedback, snapshot/version identity, old published content survives failed replacement, explicit publication only. |

### Blueprint mapping

| Reference | Decision |
|---|---|
| K-01 list/create/edit | ADOPT focused dialogs and revision-safe drafts; ADAPT to existing CareerSim fields/validation. |
| K-02 upload/status | ADOPT receipt/status/errors; ADAPT existing synchronous bounded retry and persisted jobs, without a new queue. |
| K-03 ingestion | ADOPT pre-provider validation and atomic staging; ADAPT explicit confirmed publication. Keep actual parser/profile/dimension, not blueprint numeric defaults. |
| K-04 inspection | ADOPT continuous normalized text, chunks/provenance/warnings; ADAPT exact version/attempt identity and Unicode representation. |
| K-05 Test KB | ADOPT distinct modes, query snapshot, evidence retained on answer failure. `topK` is a maximum, not a required result count. |
| K-06 lifecycle | ADAPT existing retry/cancel/replace/retire and confirmed Reindex A. No blanket hard delete or forced duplicate version. |
| Python, old routes/schema, batching, OCR, new framework | DEFER/IGNORE; outside approved UC-07. |

Historical lessons become tests, not claims of observed CareerSim defects: NUL is already rejected; batch ordering is not a reason to add batching; guard atomic publication/late completion, Unicode correctness, stale citations and simple layouts.

## 5 API contract

Preserve the [P3/P4 contract](../../docs/api/agent-platform-p3-p4.md), `ApiResponse`, shared error mapping, session/current actor and CSRF conventions. Paths below are backend mappings; keep the existing frontend/gateway API prefix rather than introducing `/api/v1`.

| Method/path under `/v2/admin/knowledge-bases` | Request / success | Error/compatibility requirement |
|---|---|---|
| GET /; POST / | Existing BaseInput; list/create Base | Current Admin and input validation; empty KB supported. |
| GET /{kb}; PATCH /{kb}; DELETE /{kb} | Existing metadata/archive; PATCH/DELETE use If-Match | Preserve existing revision conflict/auth mappings; do not auto-resubmit stale drafts with latest revision. |
| GET /{kb}/assets; POST /{kb}/assets | Existing summary list; multipart metadata + file with requestId | File validation/idempotency unchanged. List must not include full snapshots. |
| GET /{kb}/ingestions; GET /{kb}/ingestions/{job} | Summary jobs / detailed job and attempt evidence | Additive snapshot/diagnostic projection only; missing legacy data is valid. |
| POST /{kb}/ingestions/{job}/retry or /cancel | Existing processing/cancel response | Do not misrepresent synchronous processing as a new queued-worker API; maintain generation fences. |
| GET /{kb}/assets/{key}/versions/{version} | Existing version/chunks plus additive extraction detail | Current Admin/scope/version checks; immutable snapshot and legacy availability. |
| POST /{kb}/assets/{key}/versions/{version}/publish | If-Match + existing confirmation body | Explicit review/confirm; stale revision rejects, READY alone not searchable. |
| POST /{kb}/assets/{key}/versions/{version}/retire | Existing retire behavior | No resurrection or hard deletion; old citations become unavailable. |
| POST /{kb}/tests | Existing requestId/query/retrievalOnly/topK/model/config fields; accepted persisted execution | No fake Student session/config; config if supplied must grant KB; idempotency and errors unchanged. |
| GET /{kb}/sources/{chunk} | Existing authorized current passage | Unavailable/revoked source denied; no full extraction disclosure. |

Execution polling/reading uses the existing Admin execution contracts. No new error-code family, scoring logic, route namespace or persistence identity is approved. Additive DTO fields belong in [KnowledgeContracts](../../backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/dto/KnowledgeContracts.java); modify controller/facade signatures only if genuinely needed.

## 6 Security privacy and AI

- Active Admin is resolved server-side on every protected request; client IDs never grant access. Preserve KB scope checks, existing configuration grants and source authorization throughout execution and replay.
- Preserve CSRF for JSON and multipart mutations through shared API client; test missing/invalid CSRF, unauthenticated, wrong-role and deactivated accounts.
- Existing App shell account-generation remount and shared API stale-response protections remain. Verify logout/login and late requests; do not build a second identity system.
- No cross-KB retrieval, draft/retired source leakage, consent bypass or additional Student context for independent KB tests.
- Backend selects minimized authorized context. Keep current output schema validation, provider timeouts and safe error categories. Retrieved text is untrusted data, not executable instructions.
- Retrieval-only invokes no answer generation; query embedding is a separate resource observation. No-evidence must not generate unsupported answers. On answer failure, keep only still-authorized passages.
- API keys remain backend secrets. Evidence/logs are sanitized; snapshots are not ordinary telemetry. Use synthetic fixtures for testing.

### KB_TEST → UC-09 compatibility (regression only)

Read [UC-09](../../docs/requirements/use-cases/UC-09-view-ai-usage-dashboard.md). Existing `AgentUsageQuery` accepts KB_TEST; `AgentUsageDao` aggregates `agent_execution`, not ingestion attempts. Preserve that boundary; do not build/extend the dashboard in UC-07.

Required checks:

1. Persist KB_TEST mode, initiating Admin, KB/input identity, status, sources, validated output, actual calls, timing and observed usage without fake Student session/config.
2. Same requestId/input is idempotent; conflicting input rejects. Reload/replay does not trigger another provider call or duplicate execution count.
3. Retrieval-only generation tokens/cost remain null/NOT_APPLICABLE; embedding resources remain separately labeled.
4. Available cost is estimated, not billing truth. Unknown/partial observations do not become zero. Successful, failed/refused/cancelled cases preserve applicable current status semantics.
5. Late cancellation may retain actual usage without returning an unauthorized late answer.
6. UC-09 KB_TEST mode/time filters produce the corresponding execution counts/statuses/finished-latency samples. Ingestion retries/snapshot writes do not increment execution counts.
7. Summary does not expose query bodies, extraction text, citations or Student transcripts. Server retrieval is not mislabeled as provider-requested native tool dispatch.

## 7 Tests and evidence

### Dependency-ordered execution checkpoints

#### C1 — KB workspace and revision safety

- Dependency: approved plan and verified checkout. Begin here, then continue through C5.
- Files: `frontend/src/features/admin/knowledge/KnowledgeStudioPage.jsx`; new focused `KnowledgeBaseDialog.jsx` in that directory if useful; existing `platformApi.js` only as needed; feature tests/current localization/styles where genuinely touched by this UC.
- Scope: list/create/edit dialogs; stable draft revision; request identity on KB change; loading/error/empty/responsive/accessibility states.
- Gates: empty KB created; metadata saved once; two-tab edit conflicts rather than lost update; refresh does not silently overwrite draft/revision; late response cannot replace selected KB; create succeeded but refresh failed must not encourage duplicate resubmission.
- Tests: frontend component/route mocks plus controller revision contract where changed. Check account-generation behavior instead of assuming missing isolation.
- Browser: create/select/edit, two tabs, slow response and narrow viewport.

#### C2 — Ingestion diagnostics and extraction persistence

- Dependency: C1 workspace contracts stable; preserve existing ingestion APIs.
- Files: `KnowledgeParser.java`, `KnowledgeStudioServiceImpl.java`, `KnowledgeStudioDao.java`, `KnowledgeContracts.java`; facade only when needed. Extend `KnowledgeParserContractTest.java` and `AgentPlatformIntegrationTest.java` with focused service/DAO tests as useful.
- Scope: snapshot before embedding in attempt_history; safe immutable snapshot in staged version metadata; failed-stage diagnostics; generation-conditional evidence merging and lean list/detail projections.
- Gates: all five formats; invalid/NUL before provider; embedding unavailable/failure still permits reload-inspection of successful extraction; storage failure stops before provider; cancellation/retry/late worker preserves actual usage and correct attempt identity; no chunk/profile drift and no partial publication.
- Tests: deterministic parser fixtures; temporary-DB integration for JSONB merge, immutability and transactions; failure/cancel concurrency; legacy rows without snapshot. Assert one coherent final entry per attempt generation rather than duplicate metrics.
- Browser: inspect failed ingestion after reload; distinguish parsing failure with no text from embedding failure with saved extraction.

#### C3 — Attempt/version inspector

- Dependency: C2 detail projections and snapshot invariants.
- Files: `KnowledgeStudioPage.jsx`; proposed `KnowledgeDocumentInspector.jsx`; `platformApi.js` only for additive detail mapping; existing/new KB component tests.
- Scope: readable Extracted text/Chunks views with provenance/warnings; exact attempt/version binding; honest historical availability; avoid unnecessary metadata panels.
- Gates: Unicode displays correctly; chunks/provenance match selected version; switching versions rapidly rejects late results; old snapshots missing never become fabricated full text; list payloads stay summary-only.
- Tests: frontend async-selection races and Unicode fixtures; backend DTO projection/privacy and list size behavior; no provider needed for inspection.
- Browser: all format locators, failed attempt inspection, old version, long but bounded text and narrow viewport.

#### C4 — Publication and lifecycle completion

- Dependency: C1 revision safety + C2 persistence + C3 review/inspection.
- Files: `KnowledgeStudioPage.jsx`; optional `KnowledgeDocumentList.jsx`; service/DAO/DTO/controller only for verified contract gaps. Extend backend lifecycle/controller and frontend workflow tests.
- Scope: resume observation after reload; active version vs latest attempt; state-correct retry/cancel/replace/retire/archive; publish bound to reviewed version/revision; confirmed Reindex A with explicit unchanged outcome.
- Gates: failed/cancelled replacement retains old retrievable source; READY not searchable; confirmed publish swaps atomically; stale publish rejects; retry idempotency; same valid fingerprint creates no duplicate and no claimed rebuild; retired content not resurrected; local lifecycle actions invalidate stale results.
- Tests: two Admin concurrency, revision conflicts, stage/publish failures, cancellation fences, duplicate fingerprint and source revocation; scope/grant regression. Do not run destructive DB cases on Owner database.
- Browser: replace/fail/retry/review/publish and two-tab conflict, unchanged result, retire/archive behavior.

#### C5 — Independent KB Test and whole-feature release gates

- Dependency: C1–C4; it is not a separate dashboard project.
- Files: `KnowledgeStudioPage.jsx`; optional `KnowledgeBaseTestPanel.jsx`; narrow compatible changes to `ExecutionResult.jsx`, `SourcePassages.jsx`, `useExecution.js`, `platformApi.js` only as needed. Execution service/DAO only for demonstrated regression; tests below. CI may add focused Agent Platform flag coverage with shared-owner coordination, not redesign the pipeline.
- Scope: explicit modes, submitted-query snapshot, independent retrieval/generation status, no-evidence/provider-error UI, source invalidation and KB_TEST usage compatibility.
- Gates: no generation in retrieval-only; unknown question no fabrication; invalid output/provider failure retains authorized passages; revocation/account switch safe; UC-06/08 and UC-09 aggregate contracts regressions pass; Docker/Chrome checks and human UAT recorded.
- Evidence/docs during implementation: update relevant API/traceability/alignment and existing `AI_USAGE.md` with actual changes/commands/results. Do not recreate AI_USAGE or copy reference failures as CareerSim observations. Any new diagram/report change must reflect actual implementation, not broaden scope.

### Automated suites and commands

Use these existing suites as the baseline, extending behavior tests rather than mirroring implementation:

- [KnowledgeParserContractTest](../../backend/src/test/java/edu/hcmute/careersim/guidance/knowledge/KnowledgeParserContractTest.java): five formats, invalid inputs, limits, Unicode and provenance.
- [AgentPlatformIntegrationTest](../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentPlatformIntegrationTest.java): independent KB_TEST, source scope, stage/publish, cancellation, idempotency, actor/CSRF, provider failure and honest usage.
- [StudentAgentIntegrationTest](../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/StudentAgentIntegrationTest.java): UC-06 compatibility and existing usage query coverage; add focused KB_TEST aggregate assertions without changing dashboard scope.
- [GeminiEmbeddingProviderTest](../../backend/src/test/java/edu/hcmute/careersim/guidance/infrastructure/GeminiEmbeddingProviderTest.java): provider/profile/vector contract regression.
- [platformIntegration.test.jsx](../../frontend/src/features/admin/agentPlatform/platformIntegration.test.jsx): existing KB and Builder workflows; add focused KB test file under the feature when separation helps.
- [httpClient.test.js](../../frontend/src/shared/api/httpClient.test.js): shared CSRF/account behavior; preserve rather than replace.

For each checkpoint run relevant targeted tests. At feature completion run:

```powershell
# From backend/
.\mvnw.cmd verify

# From frontend/
npm run check

# From the sole repository root, following Owner's existing environment
docker compose config --quiet
docker compose up -d --build
```

- Use existing test profiles/isolated database infrastructure for PostgreSQL-specific and destructive fault tests. Never point them at valuable/Owner data.
- Ordinary Docker/UAT uses the Owner's repository and Compose workflow. **No second worktree or Compose project by default.** A separate stack requires a stated reason and Owner approval. Never `down -v`, reset or reseed Owner data.
- Destructive tests must use temporary databases/containers. Ordinary UAT creates synthetic KB content; it does not modify unrelated existing KBs.
- Check `/api/health` and affected routes through the existing Nginx gateway in Chrome. Preserve current ports/configuration; align backend/frontend Agent Platform flags and cookie/origin configuration with Owner's environment. Never disclose environment secrets.
- Existing CI runs backend verify, frontend check with the Core flag and Compose config validation. It is not evidence of actual Compose build, Chrome UAT or live model quality. Test Agent Platform on/off and Core compatibility; do not weaken existing gates.

### Chrome and human acceptance matrix

| Case | Expected result / human check |
|---|---|
| KB create/edit/empty | Focused dialog, clear empty state, usable keyboard/narrow viewport, two-tab revision conflict, no lost draft. |
| Five supported formats | Small synthetic TXT/PDF/DOCX/MD/XLSX ingest; accurate status, text/chunks and source locators. |
| Invalid/extraction/provider failures | Safe errors; no partial publication; saved extraction survives embedding failure and reload; parsing failure does not invent text. Fault injection belongs in isolated tests. |
| Replace/publish/conflict | Old content remains available through failed attempt and READY staging; explicit confirmed publication changes it; stale review rejects. |
| Retry/Reindex A | Failed attempt can retry; same valid fingerprint is unchanged with no new version or fake rebuild success. |
| Two Test KB modes | Retrieval-only has no answer call; query/mode snapshot matches results; answer-with-sources validates output; no-evidence cannot fabricate. |
| Generation failure/source revocation | Valid passages survive generation failure; unavailable source is denied and not displayed as current evidence; switch KB/version quickly without stale response leakage. |
| Authorization/account | Student/Enterprise/inactive access denied; CSRF enforced; logout/login does not mix state; existing granted Agent flows still work. |
| Usage compatibility | Existing UC-09 view/API reflects KB_TEST mode/count/outcome with honest known/unknown/not-applicable metrics and no private content; no dashboard feature added. |

Browser automation/checks and Team Lead human sign-off are separate evidence. Use deterministic provider stubs for failure scenarios; do not consume live quota to reproduce every injected fault.

### Evidence levels and live checks

| Label | Required evidence |
|---|---|
| Source-verified | Actual source/schema/guard reviewed; not an executable PASS. |
| Deterministic-test PASS | Actual successful test command/output tied to commit/environment; mock/wire fixtures clearly identified. |
| Live-provider PASS | Owner-authorized provider/model/config, synthetic corpus, bounded calls/cost, observed output/usage for the specific scenarios. |
| Human PASS | Named human reviewer/Team Lead actually reviews expected/actual Chrome behavior and signs off. Automation is not human acceptance. |

At plan creation, tests/build/browser/live/human acceptance are **NOT RUN/PENDING** for this implementation. Historical reports do not change those statuses. Live checks require Owner-approved credentials/model, synthetic data and a small explicit call/cost budget before execution. Keep them opt-in; pending live evidence does not become PASS because mocks pass. Project-level final AI evaluation requirements remain separate; this small UC UAT matrix does not claim the required full product evaluation is complete.

### Whole-feature Definition of Done

- C1–C5 completed; all UC main/alternate/exception flows mapped to evidence, with confirmed Reindex A applied.
- All relevant backend/frontend and security/data-integrity regressions pass; snapshot and actual usage invariants proven in deterministic tests.
- No unauthorized API/schema/profile changes, data loss, cross-KB exposure or regression to UC-06/08/09 contracts.
- Owner Compose build/start and Chrome workflows observed; Team Lead human acceptance recorded. Any live acceptance not authorized/executed remains explicitly pending, not a claimed full live PASS.
- API/traceability/alignment/evidence and existing AI_USAGE updated truthfully during implementation; sanitized evidence carries actual commit/commands/config.
- One reviewable feature PR for the whole UC-07, with relevant tests and limitations. Git publishing steps require their own applicable Owner authorization. Do not stop at C1 or silently include Owner's pre-staged documentation changes in an unrelated commit.

## 8 Risks and decisions

| Decision/risk | Resolution / required action | Status |
|---|---|---|
| Whole-feature scope | C1–C5, one branch/PR, Person 3. No S1-only restriction. | CONFIRMED |
| Reindex same valid fingerprint/profile | Option A: retain deduplication, no new version and truthful unchanged outcome; never claim rebuild. | CONFIRMED — no remaining semantic blocker |
| Snapshot failure before version exists | Attempt-history snapshot before embedding; immutable version snapshot at successful staging; merge/fence invariants tested. | APPROVED approach; implementation/tests pending |
| Applied migrations/data | Reuse existing JSONB/guards; no migration planned, no destructive backfill. | CONFIRMED |
| Owner Docker workflow | Sole checkout, `docker compose up -d --build`; temporary DB/container only for destructive tests; no second Compose project without reason/approval. | CONFIRMED |
| Live provider cost/data | Opt-in synthetic-only checks with agreed provider/model/call budget; no live provider in plan-save task. | Authorization pending when live checks are requested; not a reason to stop deterministic implementation |
| UC-09 ownership | Regression of KB_TEST producer/aggregate contract only, coordinate any actual shared change with Person 2. | CONFIRMED boundary |
| Historical report numbering | Preserve current selected catalogue and confirmed decision; record scoped clarification during implementation. | Non-blocking documentation discrepancy |

There is **no remaining planning blocker** for the approved deterministic implementation. If actual code reveals a materially different persistence/security/ownership requirement, document the concrete evidence and seek only that decision; do not reopen confirmed Reindex A or redesign P1–P6.

### Standalone Sol handoff

Read this approved plan and its linked CareerSim instructions/requirements/API sources. Verify the sole repository, feature branch, current HEAD and Owner's changes. Implement **all C1–C5** in dependency order, reusing existing P1–P6 and preserving data, guards and shared contracts. Apply Reindex A exactly; implement snapshots through the approved existing JSONB paths with generation/usage tests. Run deterministic gates, Owner's Compose/Chrome workflow and human acceptance; live checks require separate opt-in. Record truthful evidence in existing documentation/AI_USAGE when implementing. Deliver one complete UC-07 change for review; do not stop after C1, build another KB system, or treat reference defaults as CareerSim requirements.
