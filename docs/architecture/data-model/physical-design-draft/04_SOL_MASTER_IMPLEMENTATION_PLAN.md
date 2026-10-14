# Sol master implementation specification

**Status: PROPOSED — NOT YET AUTHORIZED FOR IMPLEMENTATION.** Approval of this set is the entry gate. This planning session must stop after writing/reviewing these documents. Do not start Sol, code, migrations, commits or deployment from this document alone.

Migration numbering amendment approved in chat on 2026-10-14: preserve merged main/Syn V1–V14; Package A uses V15–V18; Package B remains planned V19–V21. The original planning snapshot had 21 tables; merged main has 24, including three preserved career-quiz tables. No business invariant or Package B implementation is authorized by this numbering amendment.

## 1. Read set, authority and execution contract

After human approval, read repository AGENTS entry chain, then this document and:

1. [01_PHYSICAL_DATABASE_DESIGN.md](01_PHYSICAL_DATABASE_DESIGN.md): normative inventory, every column/type/default/nullability, constraints, identifiers, immutability and locking.
2. [02_LEGACY_MAPPING_AND_MIGRATION.md](02_LEGACY_MAPPING_AND_MIGRATION.md): all 24 source tables, exact migration sequence, mappings, safe rollout and evidence boundaries.
3. [03_BACKEND_AND_AGENT_ARCHITECTURE.md](03_BACKEND_AND_AGENT_ARCHITECTURE.md): module ports, auth/consent, runtime loop, API contracts and compatibility.

Architecture priority: supplied mandatory business rules > approved specification > target diagrams > legacy implementation. No new grading prerequisite, role, tenant-sharing policy, consent inference, resubmission allowance, provider capability or historical version may be invented. If the four documents conflict materially, stop only affected work and report it rather than selecting a different architecture silently.

Initial baseline is branch `codex/syn-core-vector-rag`, commit `f00bd55ddacca3b4f0efa6f17d07de45a2acd0e7`. Four target-design files were already staged before planning. Inspect actual HEAD/diff again; preserve all user changes. User asked for planning files **only in this directory during the architecture session**. This file is a proposal for later implementation permissions, not permission now to modify code or `.agent` files. After implementation approval, create the required feature trace/report artifacts in normal repository locations if that approval allows them; otherwise keep reporting here and explain restriction.

### Recommended execution packaging

Prefer **two major self-contained packages**, fully designed now:

- **Package A — Core database and backend:** stages 0–7, Flyway V15–V18, authenticated identity/enterprise, versioned assessments, scenario authoring, enrollments/multiple deliverables/finalization, completion/evidence/dashboard, safe legacy Syn/consent bridge and minimal coordinated frontend contract changes. Complete non-AI student workflow and continued authorized legacy Syn behavior are the release boundary.
- **Package B — Configurable Agent platform:** stages 8–12, V19–V21, registry/configs, KB integration, conversations/consent/memory, bounded native runtime, approved writes, final integration.

Reason for separation: replacing core evidence semantics and simultaneously introducing durable multi-agent state/tenant KB permissions creates two distinct migration and security risk surfaces. Package A must be demonstrable with AI disabled; B builds on its stable authority ports. Both packages share one approved architecture and can be executed sequentially in **one Sol conversation** if context/runtime permits; use at most two implementation sessions, not stage-by-stage handoffs. Checkpoints are internal tests, not requests for human review. A single consolidated report is required for a single combined run; if executed as two packages, each gets one report and B produces the final combined acceptance matrix. No approval pauses for routine compile/test repairs.

## 2. Fixed engineering conventions

- Java source root `J = backend/src/main/java/edu/hcmute/careersim/`; tests `T = backend/src/test/java/edu/hcmute/careersim/`; migrations `M = backend/src/main/resources/db/migration/`; frontend `F = frontend/src/`.
- Existing Boot 3.5.14/Java17/PostgreSQL17/pgvector line; do not combine with framework upgrade. Use actual BOM-supported APIs. No new microservices/framework, Redis, broker, outbox, workflow engine or JWT issuer.
- New tables are **singular** exactly as 01. Legacy plural entities stay distinct. Explicit schema-qualified JPA/JDBC; no Hibernate create/update. No generic DAO that crosses modules.
- Public use-case service interfaces with ServiceImpl; focused DAO per aggregate/query, immutable DTOs. Do not produce ceremonial facade/service/manager layers that merely forward one call.
- New REST BIGINT IDs are decimal strings, UUIDs strings, UTC timestamps, ApiResponse/ErrorResponse. Actor is principal; If-Match for mutable revisions; UUID/hash for finalization and Agent retries; ownership filtered before details.
- Every external call has timeout/cancellation and current consent, outside DB transaction. Business state commit and audit are atomic. Same datasource transaction spans approved draft action target and proposal state.
- Use exact tests for races and rejected direct SQL; H2/mock-only tests do not prove composite FKs, pgvector, locks, partial indexes or Flyway correctness.
- No production/live model calls needed for deterministic tests. Opt-in provider smoke needs separately authorized provider/environment use and records actual model/profile/status; skipped means skipped.

## 3. Traceability and test identity

The unavailable revised SRS must not be cited as read. Use these local trace IDs for the supplied mandatory rules; reconcile to official FR IDs when the human supplies that artifact.

| Requirement trace | Owning implementation stages | Acceptance IDs |
|---|---|---|
| R-ID: one role, account≠organization, membership/publication/security | 1–2 | AC01–AC04 |
| R-AS: multi-type immutable versions, unlimited retakes, deterministic historical evidence | 3 | AC05–AC08 |
| R-SIM: immutable content, pinned enrollment, multi-deliverable final-only submission | 4–6 | AC09–AC16 |
| R-EV: submission-based completion, fixed snippet, practiced-skill provenance | 6–7 | AC17–AC19 |
| R-AG: configurable Agents/configs, KB grants, durable runs, human-approved writes | 8–11 | AC20–AC32 |
| R-PR: purpose consent, tenant isolation, revocation | 2,7,9–11 | AC03–04,AC23–28,AC31 |
| R-MIG/API: history, idempotency, coexistence, compatibility | 0–1,7,12 | AC33–AC39 |

Each stage below inherits the fixed schema/API definitions; source files named are **expected implementation paths**, not claims that they already exist.

## 4. Package A stages

### Stage 0 — Repository/runtime preflight and executable acceptance skeleton

**Objective:** establish reproducible authority, actual migration baseline and existing callers before changes. **Dependencies:** approved scope; no runtime database changes.

Read `M/V1..V14`, all nine entity mappings, `J/common/config`, existing assessment/simulation/guidance services and `F/features/*/api`, direct fetches/hooks. Record exact SHA, changed files, migration SHA256/checksums and planned path scope. Inspect `T/guidance/dao/GuidancePersistenceIntegrationTest`, `T/architecture/LayerDependencyTest`, Maven/CI settings. Create `T/system/migration/SchemaUpgradeIntegrationTest` fixtures and acceptance checklist based on §7; make no claim before running them.

Runtime access is optional for implementation but **required before real upgrade**. With a known intended connection, run only read-only preflight inventory in 02. If access is unavailable, mark runtime upgrade BLOCKED/PENDING EVIDENCE and proceed against isolated synthetic containers. Never infer target DB from default credentials or clean an owner's database. Valuable-data Flyway conflict stops affected migration work.

**Checkpoint:** current branch/diff captured, all 24 tables accounted for, no V1–V14 edits, runtime evidence labeled. **DoD:** fixture/reproduction plan names clean-install and V14-upgrade targets, not a production URL. **Out of scope:** commit, deploy, cleanup, migration execution on valuable data.

### Stage 1 — Additive schema and preservation scaffolding

**Objective:** install core schema without disrupting legacy mappings. **Dependencies:** stage 0.

Create M/V15 through V18 exactly as 02; include all 01 columns, composite FKs, indexes, guards and delete policies. Create `J/system/migration/LegacyBackfillCommand`, `MigrationRecordDao`, `MigrationPreflightService` and immutable review-manifest DTOs. Command defaults to dry run; actual copy requires reviewed manifest including source hash, enterprise mapping and content review identities. Do not include automatic attempt/consent/completion imports. No provider calls from migration/import.

Implement DDL guards with parent row locks; no parent lifecycle can race an unguarded child update. Create table cycles in two steps and add FKs after both tables exist. Keep legacy JPA validate untouched. DB normalized-email uniqueness must fail visibly on a collision; no merge/delete. Runtime application role cannot migrate schema.

**Interfaces/data:** manifest row `(sourceTable,sourceKey,sourceHash,targetKind,reviewedMapping)`; dry-run report `(counts,conflicts,unmapped,reasons)`; committed ledger written with target transaction. Batches capped (proposed 100 source records/transaction), retry by exact source hash. **Failure:** rollback batch, mark/report conflict without changing source; a schema install must not depend on imported enterprise data.

**Tests:** fresh V1–V18; populated V14→V18; source row hash/count preservation; migration rerun unchanged; interrupted importer; email collision rejection; source task mismatch detection; no default new consent/completion. **Acceptance/DoD:** AC34–AC37 core subset passes in PG17, or explicitly blocked by Docker unavailable; all core tables match dictionary. **Out of scope:** any physical contract/drop, schema namespace change, rewriting seeds.

### Stage 2 — Identity, enterprise and purpose-specific consent

**Objective:** eliminate client-selected authentication and establish ownership authority. **Dependencies:** V15.

Modify `J/common/config/SecurityConfig.java`, guard LocalDemoAuthenticationFilter, extend identity access to stable principal ID. Create `J/identity/service/{IdentityService,ConsentService}`, `service/impl`, controller/dto/dao/domain policies; `J/enterprise/service/{EnterpriseService,EnterpriseAccess}`, policies/DAOs/controllers. Account/profile/membership creation atomic; default enterprise publication disabled. Use proposed session/CSRF/password policies in 03; framework session store initially in-memory, no persistence table.

Implement all `/v2/auth`, `/me`, consent and admin enterprise endpoints in 03. Revalidate account role/status on protected service entry. Harden **existing** assessment/simulation/guidance controllers immediately: authenticated actor, no X-Student-Id/default 1; reject or ignore untrusted header consistently (proposed reject mismatch if sent during transition). Replace legacy consent checks in StudentAccountAccess/Syn lifecycle with effective purpose/provider grants; preserve old timestamp only as history. No automatic admin or enterprise privilege from registration request. Add consent review UI contract and profile/enterprise data projections.

**Transaction:** user lock before consent grant/revoke; increment privacy_version; revoke blocks new dispatch/replay; cancel affected in-flight legacy/new runs through owning runtime interface, not identity importing Agent DAOs. Use a local `ConsentRevocationListener` port implemented by runtime adapters; commit-sensitive checks remain authoritative even if best-effort cancellation notification fails. Enterprise status/publish changes same transaction with audit.

**Validation/failure:** unique normalized email, password bounds, role enum, explicit confirmation/current policy version, exact recipient enterprise/provider. 401/403/404/409/422 stable errors; never a Standard success for auth failure. Login generic error avoids email existence leakage.

**Tests:** AC01–AC04, AC26, password hash rejection/demo disabled, CSRF/login/logout rotation, session fixation, wrong status, registration rollback, enterprise publishing permissions independently toggled, consent default-off vs explicit confirmation, revoked legacy Syn replay. **DoD:** no production protected route accepts student header as identity; user/tenant/consent ports reusable by later stages. **Out of scope:** OAuth/SSO, email recovery, multi-role accounts, recruitment.

### Stage 3 — Versioned assessment end to end

**Objective:** correct version pinning, response membership and deterministic retakes. **Dependencies:** stages 1–2/V16.

Create `J/assessment/service/{VersionedAssessmentService,AssessmentAuthoringService}` and implementations; `domain/{AssessmentAttemptState,AssessmentPublicationPolicy,RiasecV1Evaluator,AssessmentEvaluatorRegistry}`; explicit new entities `AssessmentType`, `AssessmentVersion`, `AssessmentQuestion`, `VersionedAssessmentAttempt`, `AssessmentResponse`, `AssessmentResult`; own repositories. Retain old entity/repositories for legacy reads. Modify AssessmentEvidenceAccess to return typed legacy/new result descriptors; no source DAO import in guidance.

Implement start/save/get/submit/result/admin publication endpoints exactly in 03. New start UUID means a new retake; same UUID + same version replays. Persist evaluator key/version + canonical input hash. Put comparison/scoring on pure domain policy. Pin current reviewed RIASEC bank as a **new** approved version, not historical reconstruction. Duplicate question IDs rejected before persistence. Save/submit validates against attempt's version even if active version changed meanwhile.

**Transaction:** attempt FOR UPDATE; submitted same canonical hash returns stored result, different hash 409; responses/result/state atomic. Publication/edit serializes on assessment_version. **Failure:** unknown evaluator cannot publish; invalid answer leaves draft; DB failure leaves no submitted attempt without result. Standard assessment works with all AI disabled.

**Tests:** AC05–AC08, min/max RIASEC, tie order, out-of-range/duplicate/missing/wrong-version question, all six dimensions, parallel submit, changed active version, rollback between response/result writes, legacy results unchanged. **DoD:** v2 API service/controller/PostgreSQL tests pass; schema/DTO/evaluator version provenance demonstrable. **Out of scope:** psychometric normalization, new unapproved assessment scoring methodology, averaging incompatible tests.

### Stage 4 — Scenario authoring, immutable version publication and assets

**Objective:** enterprises author reviewed version-specific tasks, definitions/resources/answers/skills. **Dependencies:** stage 2 and V17.

Create `J/simulation/service/ScenarioAuthoringService`, `ScenarioCatalogAccess`, domain publication/definition policies, new content entities and DAOs with names avoiding existing SimulationTask collision. Create `J/common/storage/{FileAssetService,PrivateFileStorage}` with local private-directory adapter for single-host deployment; random server storage key, normalized path under configured root, no public static serving. Implement no remote content fetching. Create `J/simulation/adapter/ScenarioDraftCommandAdapter` in B later, not a second authoring engine.

Implement create/clone/update/publish/availability APIs and v2 public catalog metadata. Add `catalog/service/impl` implementation consuming ScenarioCatalogAccess; keep v1 legacy public catalog until cutover data approved. Draft full version DTO includes definitions, instructions, resource file IDs and model answer separately; no answer fields in student catalog/task DTO. Validating file upload/attach includes ownership, size/type/hash, readiness and reference release policy. Proposed defaults: max individual file 10 MiB, allow TXT/PDF/PNG/JPEG and ZIP only if explicitly enabled later; no executable uploads, no inline HTML serving; safe download Content-Disposition attachment. Definition can further narrow allowed MIME/size. Knowledge initial ingestion TXT/Markdown only; not all asset types are parsable KB documents.

**Transaction:** parent lock + revision for every child edit; clone uses new IDs, source immutable; publish checks ≥1 required task and ≥1 required definition each, complete allowed formats, licensed/original provenance, ready assets, valid publisher. Upload bytes staged outside transaction; failed metadata leaves orphan staged object eligible for later safe cleanup, never ready attachment; no approved historical asset overwritten.

**Tests:** AC03, AC09, cross-enterprise CRUD/publication, parent/child concurrent edit vs publish, public answer-key leak check, wrong-owner file attach/download, malformed URL/no SSRF fetch, invalid asset keeps publication draft. **DoD:** published graph hash and version immutable through API and direct SQL guards. **Out of scope:** generic CMS, employer scraping, automatic publication, organization reassignment of published scenario.

### Stage 5 — Enrollment, multiple deliverables and finalization

**Objective:** one enrollment per student/version and one draft→final submission per enrollment/task. **Dependencies:** stages 3 optional as implementation order only; actual business dependency stage 4 + identity. Assessment completion is never required.

Create `J/simulation/service/{EnrollmentService,TaskSubmissionService}`, `domain/{DeliverableValidator,SubmissionState}`, new participation entities/DAOs. Implement v2 enrollment/read/draft/finalize routes and payload contracts in 03; return required definitions and saved draft for reload. Required TEXT nonblank after normalization; URL valid HTTPS reference; FILE ready/owned/matching MIME/size; JSON validated against immutable schema/options. A supplied optional item must validate; absent optional item allowed. Unknown/duplicate definitions rejected.

**Transaction:** exact finalization algorithm 01 §8, locks enrollment before submission; canonical hash covers normalized content and pinned file checksum. SQL unique conflict on concurrent draft/enrollment returns existing owned resource when equivalent, not 500. Draft writes require If-Match. Final retry same request/hash returns prior result; any different final payload gets FINAL_SUBMISSION_LOCKED/409. Do not reuse old evaluated-attempt creator.

**Failure:** missing requirement 422 with field errors; stale draft 409; invalid asset/payload does not lock draft; crash rollback leaves consistent state; lost response recovers by GET or same UUID. Persist all deliverables as one accepted set, never partial finalization. **Tests:** AC10–AC15 plus property/parameterized format cases, concurrent two finals, concurrent last-two tasks, changed payload retry. **DoD:** PostgreSQL race tests prove uniqueness/version membership, draft reload and final read are truthful. **Out of scope:** resubmission, grading/code execution, completion percentage as grade.

### Stage 6 — Model-answer access, completion, certificate and fixed snippet

**Objective:** deterministic all-required-work completion, artifact creation and answer entitlement. **Dependencies:** stage 5.

Create `J/simulation/service/{ModelAnswerAccess,CompletionService}`, `domain/{CompletionPolicy,ResumeSnippetFactory,CertificateSnapshotFactory}`. Completion creation invoked inside same submission transaction; proposed deterministic snippet template version `completion-v1` names simulation and practiced required-task skill labels without competency claims. Certificate data snapshots student display name/title/enterprise/completed time with completion-only wording. Render authenticated HTML/JSON certificate from snapshot; PDF/binary signing deferred. No LLM, no certificate queue or independent snippet table.

Model answer read requires same student/enrollment/task FINAL, then access-check each resource; authoring API permits owning enterprise author review. No answer embedded in catalog/enrollment before finalization. Optional remaining tasks may still be submitted after completion, but cannot update completed_at/snippet/certificate. Runtime failure of rendering cannot undo completion.

**Tests:** AC16–AC18, final task rollback contains no orphan completion; two finalizers exactly one completion; no grade dependency; wrong version/student/enterprise denied; before/after release; later display-name/skill-name changes do not alter fixed snapshot; snippet has no mutation route. **DoD:** complete work with provider disabled, certificate/snippet retry stable. **Out of scope:** professional certification, editable persisted snippets, public certificate lookup.

### Stage 7 — Evidence, dashboard, plans and Package A compatibility

**Objective:** new participation evidence, preserved legacy history, confirmed plan and usable frontend contracts. **Dependencies:** stages 2–6/V18.

Extend `J/assessment/access` and `J/simulation/access` with separate typed legacy/new views; move new cross-module joins out of guidance/JdbcSynAgentDao. Implement `J/guidance/service/{GuidanceEvidenceQuery,ExplorationPlanService}` and `PlanCommands`; retain StandardGuidancePolicy, reviewed career_path_profiles/learning_resources. New dashboard returns typed evidence; practices are derived from FINAL task_submission + immutable task_skill, including incomplete enrollments. Completion count is not graded score; no student strength certification from legacy correctness flags.

Keep legacy Syn orchestration operational within new auth/consent boundary: it can use legacy score cards and separate safe new participation context, cannot serialize new completion as numeric grade. Pending-plan v1 explicit save remains human confirmation with server-known draft match. Current legacy sessions/turns recover/cancel under current policy; no conversion to new long-term memory. Core direct confirmation writes exploration_plan and archives earlier plan under user lock. Legacy plan copy only when confirmation provenance proven, otherwise explicit reconfirmation.

**Frontend integration scope to approve/coordinate:** `F/shared/api/httpClient.js` session-CSRF/errors; add auth/consent controls; `features/assessment/hooks/useAssessment.js` attempts-first v2 and authenticated error states; `features/simulations/api/{simulationApi,simulationCatalogApi}.js`, hook/workspace/result for enrollment/multiple deliverables/final lock/model answer/completion; `features/guidance/api/guidanceApi.js`, GuidancePage/result cards for typed evidence; preserve EN/VI, accessibility and user's drafts. No unrelated landing redesign. Feature owners are the named owners in `.agent/TASKS.md`; backend ownership remains unassigned. Human approval must include this cross-owner coordination before those frontend edits. Backend-only implementation may finish tests, but release/cutover stays blocked until callers migrate.

Old v1 assessment and graded simulation writes now return 409 WORKFLOW_UPGRADE_REQUIRED with new route hint after coordinated switch. Keep legacy reads; current v1 numerical result contracts represent legacy only. Error/fallback must preserve draft and show unsaved state; remove production use of local simulation evaluator on failed request. No local mock can unlock answer/completion. Do not call any frontend story complete without service persistence evidence.

**Tests:** AC19, AC33, AC38–AC39; current API fixtures plus v2 adapter tests; auth/CSRF/401/409/422 flow; dashboard with only legacy, only new, mixed, none; direct plan vs proposal future path; no-AI full workflow; EN/VI labels and keyboard/reduced motion. **DoD:** Package A backend verify/frontend check/Compose validate + real isolated gateway smoke pass or failures explicitly recorded; no irreversible production cutover without runtime evidence. **Out of scope:** full new Agent admin UI, recruitment, automatic import of unproven content.

## 5. Package B stages

### Stage 8 — Agent registry/configs and KB management schema

**Objective:** durable explicit Agent configurations with independently usable KB administration. **Dependencies:** Package A authority ports; V19.

Create `J/agent/service/AgentAdminService`, config domain policy/DAOs/controller; `J/knowledge/service/{KnowledgeBaseService,KnowledgeIngestionService,KnowledgeSearchService,KnowledgeSourceAccess}` plus ownership policies/DAOs. Move reusable chunker/hybrid abstractions through bounded edits, preserving legacy wrappers/tests. Configuration publication stores hash, role array/tool codes/full execution limits/memory policy and explicit KB junctions. Seed Syn/Enterprise Assistant **as disabled drafts** until reviewed config and provider capabilities are known; no credentials in rows.

Create code registries `AgentPolicy`, `AgentTool`, `AgentModelClient`, `ApprovedActionExecutor` interfaces; register supported canonical codes from 03. Provider/model keys resolved through server config allowlist, never arbitrary URL. Published config freezes payload and junctions; REVOKED disables old pinned runs. New config has no implicit KB/tool/audience grants. Admin create/edit/preview makes zero model calls.

**Tests:** AC20–AC24 schema subset, role/tool enum validation, unknown tool/provider/native capability rejected, config immutability and pointer same-agent composite FK; save config without any provider credentials. **DoD:** CRUD/publication tests + isolation constraints pass; UI API exposes explicit usable/disabled state. **Out of scope:** tool plugin marketplace, arbitrary code plugins, provider billing automation.

### Stage 9 — Scoped versioned KB ingestion and retrieval

**Objective:** retain good RAG primitives while enforcing KB/config/tenant/profile permissions. **Dependencies:** stage 8.

Refactor existing `guidance/service/KnowledgeIngestionService`, `guidance/dao/JdbcKnowledgeDao`, `KnowledgeRetrievalServiceImpl`, GeminiEmbeddingProvider behind knowledge ports. New JDBC eligible scope joins KB/document/current-version/config grants; both lexical/vector and source inspector use it. Pass explicit AuthorizedKbScope, full embedding profile, bounded query/candidate/observation limits. New cache includes actor/tenant/config/KB/profile/revision; no global same-query cache. Staged ingest and review/publish endpoints from 03; first formats TXT/Markdown, source URI reference only.

Copy existing vectors only through approved profile/corpus mapping from 02. Use vector without fixed dimension + dimension/profile FK/check; query a materialized compatible eligible subset. Keep legacy vector(768) untouched. Reuse exact lexical+RRF behavior with authorized inputs; test before tuning thresholds. Mixed profile new model change requires explicit vectors; LLM-only model change does not re-embed. Embed outside transaction, publish complete chunk set+pointer inside one short transaction; failed replacement retains prior publication.

**Failure:** invalid text/control/MIME/provenance rejected before paid embedding; wrong vector count/dim/nonfinite/zero fails version; no consent or embedding outage returns authorized lexical-only result; unpublished/revoked/foreign chunks never returned. No phantom references/page locations.

**Tests:** AC23–AC25, source revoked in flight, two KBs in same config and a third ungranted; enterprise A/B identical query isolation on lexical/vector/cache/inspector; mixed dimensions; old 768 copy equality; staged replacement failure; deterministic known/unknown retrieval fixtures. **DoD:** independent retrieval endpoint proven without generation, precise passage provenance; live quality remains distinct. **Out of scope:** ANN optimization, crawl/OCR/PDF ingestion, automatic content review.

### Stage 10 — Durable conversations, bounded runtime, consent and manageable memory

**Objective:** actual configurable native tool loop and scoped durable conversation lifecycle. **Dependencies:** stages 8–9; V20.

Create `J/agent/service/{AgentConversationService,AgentRuntimeService,AgentRunLifecycleService,AgentMemoryService}`, state/DTO/DAO/controller components; `infrastructure/AgentModelClient` adapters over retained transport. Extract shared execution budget/deadline/cancellation primitives from Syn without replacing career policy. Implement accept/run/recover/cancel/watchdog flow in 03, bounded task executor, same key/hash semantics, one active run per chat, epoch + privacy_version guards. User message and run acceptance atomic; assistant/result terminal commit atomic. Do not persist chain of thought or provider opaque protocol in public traces.

Integrate runtime policy/tool SPIs from guidance/enterprise adapters using owner ports, not agent→guidance implementation imports. Keep existing Standard quick actions and EN/VI; optional general educational answers clearly labeled; citations require delivered evidence. Store bounded same-chat messages, send recent valid pairs; no old tool-response replay or citation laundering. Record all actually used consent grants in agent_run_consent. External query embeddings/search/generation require respective provider grants.

Memory APIs explicitly display/confirm/edit/delete preferences in owner/agent/enterprise scope; no automatic transcript condensation into cross-chat memory. Consent revoke and clear redact/disable memory and block stale replay; regrant does not resurrect. Expiry cleanup affects only new-policy operational payloads, not core evidence or unapproved legacy history.

**Tests:** AC20–AC22,AC26–AC28,AC32; genuine model protocol double supports direct response, one tool, sequential two tools, wrong native IDs/order, unknown tool, repeats, cap, malformed result, timeout, 429/503 bounded retry, cancellation/late reply, source revoke, account lock, clear/epoch race, restart interrupted run, repeated >10 turns with fresh counters and bounded context. **DoD:** native decision→backend execution→observation→reconsider loop proven, all caps enforced, no-AI fallback honest. Live provider test optional/explicitly recorded, not confused with doubles. **Out of scope:** unbounded autonomy, provider auto-execution, voice, multi-agent collaboration between Agents, cross-tenant memory.

### Stage 11 — Typed proposals, explicit approval and transactional execution

**Objective:** safe proposed draft/plan changes without autonomous business writes. **Dependencies:** stage 10 + core authoring/plan command ports.

Create `J/agent/service/AgentProposalService`, immutable typed payload DTOs (one per action enum), ProposalApprovalPolicy, DAO/controller; `J/simulation/adapter/ScenarioDraftActionExecutor`, `TaskDraftActionExecutor`; `J/guidance/adapter/PlanActionExecutor`. Proposal creation checks current rights and payload schema; freeze hash/target revision/context. User GET sees exact diff, expiry and affected draft. Approve requires matching hash, same requesting user and current rights; execution remains separate from approval.

Execute exact transaction in 03: locks, approved/unexpired/hash/revision/status/tenant checks, invoke owning command, write result/audit/EXECUTED once. Target creation failure rolls back proposal execution; successful response lost -> same proposal returns result. Runtime cannot invoke approve endpoint as a tool. Edits between propose/approve/execute create STALE; never rewrite expected revision. Scenario/task commands operate on draft only, current published version requires explicit human clone to draft first.

**Tests:** AC29–AC31, simultaneous executions, cancellation/consent revoke after proposal, swapped actor/tenant, target deleted/changed/published, payload tamper, expired approval, transaction failure after target insert, plan replacement active-plan race, same success replay. **DoD:** zero write without explicit approval; no duplicates; no mutation of scores/final submissions/completion/permissions. **Out of scope:** automatic publish, email/message sending, external connectors, arbitrary JSON patch to any entity.

### Stage 12 — Syn bridge, integrated migration, security and consolidated evidence

**Objective:** complete compatibility transition and prove entire approved system. **Dependencies:** stages 0–11/V21.

Implement guidance→AgentChat adapter preserving current v1 request UUID/session/recovery/cancel/clear/plan response semantics. New sessions pin initial Syn config; legacy session/turn lookup remains separate and expires naturally only under approved policy. Request ID lookup checks old store before new execution to prevent duplicate actions. v1 source inspection uses old K namespace, new v2 source inspect uses typed references. Update `F/features/guidance/utils/synSources.js` and its tests to allow exactly the new authenticated source route with validated decimal configurationId query; retain hostile URL rejection. Enterprise Assistant uses same runtime with different config/tools, no second orchestrator branch.

Extend ArchUnit for module port boundaries, no controller→DAO, no runtime SQL outside owner repository, no cycles. Ensure only domain services write objective data; Agent read tools cannot inject actor/tenant, dynamic SQL or URLs. Complete compatibility React integration for Agent selection/conversation, memory review and exact proposal approval/execute controls within approved scope; keep lower-level admin tooling/API valid without adding an elaborate Agent studio UI.

Run consolidated gates §8 once implementation stabilizes, fix defects autonomously and rerun relevant failed/affected gates. Exercise clean install/upgrade/import dry run and restore rehearsal only in isolated data stores. Run final static secret scan/DTO leak tests, real gateway core no-AI smoke, native-provider smoke only if authorized, and explicit human semantic corpus review pending where unavailable. Remove legacy writes at app boundary; retain history and never run automatic contract/drop.

**DoD:** all applicable ACs pass with concrete evidence or explicit blocked limitation; docs/report/trace updated to actual components; no claim that unavailable live/runtime/human checks passed. One consolidated report, no commit/push/deploy absent explicit authorization. **Out of scope:** destructive cleanup, model quality guarantees, production cutover without restore/runtime approval.

## 6. Internal checkpoint policy and blocker handling

At each stage: implement complete service/domain/persistence/API slice → focused unit/controller/integration tests → fix failures → record checkpoint in working execution log → proceed. Do not ask the architect to approve each file or stage. Maintain a short checkpoint summary of completed stages, migration head, changed interfaces and next tests so compaction does not restart work. No sub-agent delegation is required by this plan.

Stop **affected work only** when: irreversible data loss risk; Flyway history/checksum/version collision; unsupported assumptions about real owner/data/profile/provenance; contradictory mandatory business policies; unclear security/tenant authority; material design contradiction. Continue independent isolated implementation if safe. Blocker report format:

`BLOCKER <id>: affected stage/AC; observed file/query/error (sanitized); exact assumption that failed; risk if continued; alternatives and recommendation; smallest human decision/evidence needed; unaffected work completed; commands actually run.`

Do not stop for ordinary compile errors, race-test failures, DTO mismatches, formatting, local unavailable provider (use doubles/fallback) or lack of production database access when isolated tests remain possible. Do not silently bypass Docker-skipped DB tests, weaken tests, fake embedding vectors in production, invent private evidence or fabricate approval/consent.

## 7. Implementation acceptance matrix

**Current status of every row: NOT IMPLEMENTED/NOT EXECUTED IN THIS PLANNING SESSION.** `DB` rows need real isolated PostgreSQL; `LIVE` rows need actual authorized runtime/provider and are currently **BLOCKED/PENDING EVIDENCE** for such claims. The source contains some legacy tests, but that does not mark the new acceptance criteria passed.

| ID | Test setup/action | Required observable result / evidence |
|---|---|---|
| AC01 | Unauthenticated request; forged X-Student-Id; student B reads A attempt/run/file | 401 or scoped 404/403; no ID-based authentication or private payload; controller/security tests |
| AC02 | Locked/inactive user; role changed since session; CSRF absent/wrong | Protected use denied using current status; CSRF blocks unsafe browser writes; no fallback success |
| AC03 | Enterprise A edits/publishes B scenario; representative without publish grant | Denied; verification/registration does not imply publication; multi-representative same-org positive test |
| AC04 | Profile grant only, completion grant only, assessment grant only, no submission grant | Field/endpoint-specific disclosure; detailed submission requires both own scenario and separate grant; revoke blocks next read |
| AC05 | Publish assessment V2 while attempt pins V1 | V1 question/evaluator/result retained; content immutable; DB guards exercised |
| AC06 | Same student starts same assessment version with distinct UUIDs then retries one UUID | Unlimited distinct attempts; identical start retries same ID; same UUID other version conflicts [DB] |
| AC07 | Submit wrong-version question, duplicate ID, missing required ID, out-of-range answer | 422 before partial result; composite FK rejects direct cross-version SQL [DB] |
| AC08 | Two submit threads; failure between answer/result writes | Exactly one immutable result, matching hash; atomic rollback; same final body replay, changed body 409 [DB] |
| AC09 | Edit published scenario or task/definition/model answer/skill through API and SQL; race edit/publish | Rejected, published hash unchanged; cloned new version editable [DB] |
| AC10 | Student enrolls in V1 and V2 concurrently without assessment | Two valid pinned enrollments; no prerequisite; old version work still accessible |
| AC11 | Two concurrent enroll requests for same student/version | Exactly one row, both responses resolve same owned enrollment [DB] |
| AC12 | Several deliverables with individual formats/required flags, missing/unknown/duplicate/invalid optional | Correct validation by definition; valid missing optional accepted; mismatched task FK rejected [DB] |
| AC13 | Finalize exact UUID/body twice, then changed body/new UUID | One FINAL row; exact retry same success; further modification conflict, children frozen [DB] |
| AC14 | Concurrent final task payloads, stale If-Match, file becomes unready | One valid final or validation conflict; invalid request leaves draft; no partial final set [DB] |
| AC15 | Task/definition from another version/enrollment inserted directly | Composite FK violations independent of service validation [DB] |
| AC16 | Model answer/attachment before final, after final, different student, wrong version | Only authorized owning final task and authoring owner can read; no leaked keys in public JSON/cache/file URL |
| AC17 | Finalize last two required tasks concurrently; all AI/providers off; no score rows | Exactly one completion/certificate snapshot; completion unaffected by optional tasks/grades [DB] |
| AC18 | Read/copy snippet twice, modify user name, submit optional task, call hypothetical snippet update | Identical stored snippet/template/certificate; no snippet edit route or second completion |
| AC19 | Draft only, one final task in incomplete enrollment, completed enrollment, legacy graded record | Practices only from final new tasks with exact task/version/submission; legacy tagged separately; no inferred competence |
| AC20 | New Agent config V2 after conversation pins V1; cross-agent config ID injection | Old conversation retains V1; new uses V2; mismatched config rejected by FK/service; revoke V1 blocks future run |
| AC21 | Student invokes enterprise-only Agent/tool; attached KB but search tool disabled | Denied; explicit audience+tool grant intersection; no implicit tool access |
| AC22 | Native double returns direct answer, then sequential two tool calls | Correct request/observation correlation, actual bounded tools executed, fresh counters per run; no keyword fake loop |
| AC23 | Config has two authorized KBs and one ungranted/foreign KB | Both granted KBs eligible, no others; deterministic multi-KB provenance; unauthorized requested KB rejects |
| AC24 | Enterprise A/B query identical text across lexical/vector/cache/inspector/replay | No B content in A result or source lookup; scope before ranking; recheck at finish |
| AC25 | 768 and other-dimension profiles; same dimension incompatible model; failed replacement | No mixed-space comparison; wrong dimensions rejected; prior document publication intact [DB] |
| AC26 | No consent, preselected unconfirmed toggle, old timestamp, only one provider grant, revoke in-flight | Zero unauthorized external calls; provider-specific check; fallback local; revoked output not committed/replayed |
| AC27 | Memory off, on with explicit confirmation, different agent/enterprise/user | Only consented matching scope read/write; owner list/edit/delete; no automatic transcript import |
| AC28 | Clear conversation while run in-flight; memory revoke and regrant; expiry | Epoch prevents resurrection, payloads redacted/hidden, old memory not restored; retention tests with injected clock |
| AC29 | Proposal received without approval, model requests approve/write directly | No business-data write; typed proposal only; approve alone does not execute |
| AC30 | Approve and execute twice concurrently; fail after target write before executed marker | One target+result, atomic rollback/retry, audit; no partial successful business write [DB] |
| AC31 | Target edit/publish, changed hash, wrong actor/tenant, permission revoke or expiry after approval | STALE/expired/denied, zero mutation; new proposal/review needed |
| AC32 | Timeout/provider 400/429/503/malformed/unsafe reply, cap, cancel, restart | Bounded calls, classified safe failure, Standard/abstention, no objective change, cancelled late reply discarded |
| AC33 | Replay recorded v1 React contract fixtures; old graded submit after cutover | Compatible legacy reads/Syn IDs/language/recovery, explicit upgrade code for invalid old write behavior; no fake grade |
| AC34 | Clean V1–V21 on fresh PG17+pgvector | Flyway checksums/constraints/counts match dictionary; Hibernate validate; no arbitrary credentials/consent [DB] |
| AC35 | Populated V14 isolated upgrade/import; run twice and crash/retry | Legacy hashes/counts unchanged; reviewed copies only; idempotent ledger; no invented versions/completions [DB] |
| AC36 | Valuable DB history mismatch, normalized email collision, unknown content owner/profile | Dry-run conflict report, affected cutover blocked; no repair/merge/drop shortcut [LIVE evidence pending] |
| AC37 | Backup restore into different isolated DB | Restored head/counts/relationships, core read/write smoke; no original DB overwrite [LIVE evidence pending] |
| AC38 | Real gateway no-AI assessment→enroll→draft→final→answer→completion→dashboard, EN/VI | Persistence proven by reload/readback; failed save keeps draft; keyboard/errors/locale correct [LIVE smoke pending] |
| AC39 | Target corpus held-out reviewed questions and authorized real native provider | Separately report retrieval recall/answer-grounding cases, unsupported answer abstention, protocol success, failures/skips; no fake-model accuracy claim [LIVE + HUMAN pending] |

## 8. Verification commands, final report and definition of done

Run focused test classes at their checkpoint. Final repository gates (use correct working directory):

```powershell
# backend/
.\mvnw.cmd verify
# frontend/
npm run check
# repository root
docker compose config --quiet
git diff --check
```

Before integration tests confirm the datasource points to the isolated test container, never default owner database. Ensure CI actually executes PostgreSQL-required tests: report discovered/executed/skipped and make missing Docker fail the required DB gate rather than turn into a green release. Run new controller contracts independently of service tests. Preserve/extend existing guidance retry/consent/lifecycle/RAG tests; investigate changed business semantics, do not delete tests to hide a regression. Migrate old behavior-specific tests into labeled legacy suites where that behavior is intentionally historical. No routine production provider calls or destructive lifecycle smoke.

Consolidated report must contain: approved scope/package; final SHA and uncommitted diff status; requirement→service→schema→test→evidence mapping; all created/changed files; schema inventory and migration checksums/head; backfill dry-run/conflicts/copied counts; actual test commands/counts/failures/skips; real DB/API/UI/native-provider/human evidence separately; preserved legacy history and API behavior/deprecations; unresolved production runtime/ownership/retention/corpus gaps; exact next human actions. Disclose AI assistance and human review, no fabricated measurements. No secrets/private payloads in artifacts. Do not claim deployment without executing an authorized deployment.

Overall DoD: core non-AI flow correct; identity/tenant/purpose enforcement current; content/config/version provenance intact; one final task and completion invariants proven; Agent cannot mutate protected evidence; authorized KB retrieval and native runtime bounded; approved writes atomic/stale-safe; original V1–V14 unchanged; legacy records preserved; frontend compatibility handled with honest state; required gates passed or clearly blocked; human acceptance remaining items enumerated. A partial package is not the whole target architecture.

## 9. Approval review checklist and planning self-review

Review 03 §7 decisions first, 01 §1/§8 inventory and critical invariants, 02 §3 legacy mapping/§6 cutover, then stages and acceptance matrix here. Approve packages and minimum frontend coordination together; reserve production migration/deployment decisions until runtime evidence exists. No additional approval is needed merely because a stage has many files.

Planning self-review performed against the supplied checklist: retained responses and per-definition payload rows; avoided separate snippet/certificate/evidence tables; same-parent composite FKs cover question/version, enrollment/task/version, definition/task and config/agent/owner; no grade-dependent completion; no consent inference or global tenant cache; no data copy that fabricates old versions; fixed configuration and human-approved hash/revision-bound actions; dependency SPIs prevent circular module implementation imports; migration schema install is independent from uncertain real-data import; v1 grading contract explicitly deprecated rather than falsely preserved. These are design review outcomes, **not runtime proofs**.

Remaining approval/evidence items are explicit: retention defaults and new raw transcript policy; identity/publication/content validation defaults; legacy content ownership/profile/provenance; actual database history; missing revised SRS reconciliation; cross-owner frontend changes and backend implementer assignment. No silent decision on those real-data facts is delegated to Sol.
