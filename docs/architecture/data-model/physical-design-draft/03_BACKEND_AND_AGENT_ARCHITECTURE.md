# Backend and configurable Agent architecture

Status: **PROPOSED DESIGN**, for one consolidated human approval. Mandatory business policies are those in the supplied request; implementation evidence is separately labeled below. Schema authority is 01, migration authority 02, execution authority 04. No backend code was changed.

## 1. Architecture and responsibility ownership

Keep one Spring Boot application and one PostgreSQL transaction manager. Retain existing packages, controllers, DTO/envelope conventions, JPA/JDBC, test framework and provider adapters where behavior fits. Add `agent` and `knowledge` modules; do not reorganize the entire repository. `catalog` remains the public query adapter. New scenario authoring belongs to `simulation`, while `enterprise` owns organizations/membership/publication authorization.

Layer flow: Controller → service interface → ServiceImpl → owning DAO + domain expert + approved cross-module port. Domain objects implement local rules; services coordinate authorization and transactions; DAOs perform queries only. Public module contracts return immutable value DTOs, never JPA graphs.

| Module / roots | Domain experts and application services | Repository ownership / public contracts | Authorization + transactions |
|---|---|---|---|
| identity / UserAccount, StudentProfile, ConsentGrant | `IdentityService`, `ConsentService`; AccountStatusPolicy, ConsentGrant effectiveness | Extend StudentAccountAccess to stable principal ID; add `ActorAccess.requireActive`, `ConsentAccess.requireEffective(user,purpose,recipient,instant)`; owns app/profile/consent | Registration/profile/consent revoke are short transactions. Resolve principal, never accept caller identity from studentId/header |
| enterprise / Enterprise, Representative | `EnterpriseService`; `EnterprisePublicationPolicy` evaluates organization + representative | `EnterpriseAccess.requireMember/requirePublisher(actor,enterpriseId)`; owns enterprise/representative | Account role/status, membership active, exact ownership; publishing needs separate permissions |
| assessment / AssessmentType, AssessmentVersion, AssessmentAttempt | `AssessmentService` legacy façade; `VersionedAssessmentService`; `AssessmentAuthoringService`; frozen evaluator registry | Own six new assessment DAOs; `AssessmentEvidenceAccess` becomes typed owned legacy/new read port | Attempt submit atomic answers/result/status; author publication admin-only; no simulation prerequisite |
| simulation / Scenario, ScenarioVersion, Enrollment, TaskSubmission, Completion | `ScenarioAuthoringService`, `EnrollmentService`, `TaskSubmissionService`, `CompletionService`; DeliverableValidator, CompletionPolicy, SnippetFactory | Own new content/participation tables; `ScenarioCatalogAccess`, `SimulationEvidenceAccess`, `ScenarioDraftCommands` | Enterprise-owned drafts; publication policy; students own enrollments; finalize locks enrollment then submission; completion synchronous |
| catalog / no write aggregate | retain `SimulationCatalogService` and mapper; add versioned queries | Depends on ScenarioCatalogAccess only for new public data; legacy CareerSimulationDao stays inside legacy slice until removal | Public published/available metadata only; no tasks requiring enrollment, answers or private resources in summary |
| guidance / ExplorationPlan | retain `GuidanceService`, StandardGuidancePolicy and Syn-specific coach policy; add `ExplorationPlanService`, `GuidanceEvidenceQuery` | Own plans, reviewed career paths/resources; consumes evidence ports. `PlanCommands` contains direct user-confirmed and approved proposal commands | Student ownership; direct confirmation or approved typed action; AI never writes through DAO |
| agent / Agent, Configuration, Conversation, Run, Memory, Proposal | `AgentAdminService`, `AgentConversationService`, `AgentRuntimeService`, `AgentMemoryService`, `AgentProposalService`; RunState, ProposalApprovalPolicy, ToolRegistry | Own registry/runtime/proposals; `AgentChat` façade for Syn adapter; `ApprovedActionExecutor` SPI uses typed commands | Admin config; owner/tenant scoped runtime; current tool grants; consent; no network inside DB transaction |
| knowledge / KB, Document, Version, Profile | `KnowledgeBaseService`, `KnowledgeIngestionService`, `KnowledgeSearchService`, `KnowledgeSourceAccess`; chunker, HybridRetrievalPolicy | Own new knowledge tables; search returns passages/provenance only, no generated answer | KB owner/config/actor intersection for lexical, vector, inspector and replay; atomic publication |
| common / no business roots | API errors/envelopes, clock, FileAssetService/storage adapter, AuditWriter | Own file metadata/audit; file use needs module-specific access policy callback | No SQL-by-tool, no general-purpose business facade; safe storage URLs and logs |
| system / migration/health | existing HealthController; reviewed LegacyBackfillCommand | migration ledger only | Operator-only, dry-run default, explicit DB identity and review manifest |

### Dependency direction and GRASP

Identity is foundational. Enterprise → identity. Assessment → identity. Simulation → identity/enterprise/common. Catalog → simulation read contract. Knowledge → identity/enterprise/common. Agent core → identity/enterprise/knowledge and its **own** tool/action SPIs. Guidance → evidence ports + AgentChat. Guidance and simulation implement agent SPI adapters in their adapter packages; agent runtime does not import guidance/simulation implementations. Spring injects collections of tools/policies/executors. This avoids guidance→agent→guidance cycles; no root `GlobalService`.

- Information Expert: attempt checks answer lifecycle; deliverable definition validates format; submission owns final lock; completion policy evaluates required tasks; consent object evaluates time/scope.
- Creator: owning services construct aggregates after policy checks; version creates tasks; submission creates payload items; mappers create DTOs only.
- Controller: one primary use-case facade per endpoint; actor extraction and request validation only.
- Low coupling: source modules expose small immutable evidence/read/command ports; agent receives minimized DTOs, never source DAOs.
- High cohesion: grading legacy, versioned assessment scoring, submission completion, AI explanation and enterprise membership remain separate responsibilities.

Keep Spring/JPA mapping outside pure policies when it simplifies tests; no obligatory second domain/entity graph for every row. Use JPA for ordinary roots and JDBC for locked/composite bulk persistence and pgvector. A service may combine own JDBC/JPA under the same datasource transaction, but explicitly flush before JDBC reads where needed; avoid two representations of the same mutable aggregate in one transaction.

## 2. Source evidence and specific reuse

Paths are repository-relative and line anchors refer to the inspected SHA.

| Evidence | Verified source finding / consequence |
|---|---|
| `common/config/SecurityConfig.java:31,56–64` under backend Java root | CSRF disabled/stateless, assessment and simulation submit permitted. This is prototype policy, not production authentication |
| `assessment/controller/AssessmentController.java:33–50` | X-Student-Id defaults to 1. Replace with principal on all protected routes |
| `assessment/service/AssessmentServiceImpl.java:79` vicinity | Uses active question bank at submission, not attempt-pinned version; replace new workflow, retain historical repository reads |
| `simulation/service/impl/SimulationAttemptServiceImpl.java:43` vicinity | Evaluates all tasks and creates new evaluated attempt each submit; cannot be renamed into finalization |
| `guidance/service/SynTurnLifecycleService.java:49,101,145,181` | Short begin/finish, ownership/consent recheck, clear/cancel patterns worth retaining |
| `guidance/dao/JdbcKnowledgeDao.java:18–23,68–119,129` | Published/active-version filtering and atomic publication exist; vector validation fixed at 768 and no tenant predicate |
| `identity/access/JdbcStudentAccountAccess.java:22–25` | Account/status and legacy consent loaded by email; change principal resolution to stable account ID |
| `simulation/access/JdbcSimulationEvidenceAccess.java:28–43` | Guidance evidence derives legacy evaluated score; new participation projection must remain distinct |
| `guidance/dao/JdbcSynAgentDao.java:213–224` | Direct legacy simulation joins inside guidance; move new evidence query to owning simulation port |
| `guidance/dao/JdbcGuidanceDao.java:41–43` | One plan/student upsert; new plan preserves archived confirmation instead |
| `backend/src/test/java/edu/hcmute/careersim/architecture/LayerDependencyTest.java:14` | Layer ArchUnit rules exist; extend with module ports/cycle checks |
| `backend/src/test/java/edu/hcmute/careersim/guidance/dao/GuidancePersistenceIntegrationTest.java:23–34` | Real PG17/pgvector Testcontainers test exists, can skip without Docker; a skip is not database verification |

Java root above = `backend/src/main/java/edu/hcmute/careersim/`. Anchors are navigational, not assertions about unexecuted methods.

| Existing component | Decision | Exact adaptation boundary |
|---|---|---|
| SynHttpRequests, GeminiRequestRetry | KEEP | Preserve bounded transport, classification/cancellation; no provider URLs from model/admin free text |
| GeminiSynGuidanceProvider, OpenAiSynGuidanceProvider | KEEP adapters / REFACTOR protocol port | Existing structured guidance API remains until bridge verified; new AgentModelClient wraps shared transport and implements actual native call correlation |
| SynExecutionScope, SynUsagePricing | REFACTOR shared execution context | Fresh per-run budgets/deadline, explicit immutable context; no leaked thread-local state across async worker; actual vs estimated usage distinguished |
| SynTurnLifecycleService, JdbcSynTurnDao | KEEP legacy; REUSE algorithms | New lifecycle uses agent_run/config/consent set/epoch, not renamed SQL against syn_turns |
| SynAgentOrchestrator, SynAgentPolicy, SynReplyValidator | KEEP Syn-specific policy | Extract shared protocol/safe validation primitives only; career policy/cards/fallback stay guidance |
| SynAgentToolbox | REFACTOR into approved SPI tools | Remove new cross-module SQL; bind actor/scope at executor; legacy behavior protected by regression tests |
| KnowledgeChunker, HybridRetrievalPolicy | KEEP | Add explicit normalization/chunk profile and locator conventions; measured settings, no blind rechunk |
| KnowledgeIngestionService/JdbcKnowledgeDao | REFACTOR into knowledge module | Stage full versions; owner/context guards; scoped search and dimension/profile-aware persistence; preserve atomic pointer switch |
| GeminiEmbeddingProvider | KEEP adapter | Existing exact 768 profile remains supported; registry maps explicit full profiles, no universal 768 assertion in new DAO |
| StandardGuidancePolicy, EN/VI formatting | KEEP | Distinct legacy evaluations/new completion-only evidence; no fabricated grades or strength certification |
| LocalDemoAuthenticationFilter | KEEP localhost demo-only tests | Never substitute for production auth; guarded explicit profile, off by default; no default principal in new controllers |

## 3. Authentication, enterprise and consent

### Proposed identity default

Use Spring Security **server-side session authentication** for the same-origin React/Nginx monolith. HttpOnly session cookie, Secure outside explicit localhost, SameSite=Lax, session fixation protection, proposed 30-minute inactivity timeout. Single application instance initially; restart requires sign-in again. No custom JWT issuer/refresh-token subsystem or browser localStorage credentials. Keep Java 17/current Boot dependency line; use the Spring Security 6 version supplied by that BOM, not a latest-only DSL copied from documentation.

CSRF remains enabled for browser state-changing requests including login/logout; GET `/api/v2/auth/csrf` exposes the session token, React supplies `X-CSRF-TOKEN`, refresh after login/logout. Restrict CORS to configured origins, use same-origin gateway normally. Spring's [CSRF documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript) explains the SPA token lifecycle; implementation must use APIs available in the pinned dependency version. Use DelegatingPasswordEncoder with an explicit approved hash implementation; reject legacy malformed/demo hashes. Proposed password policy: 12–128 characters, never log/replay plaintext. Registration supports STUDENT and ENTERPRISE only; admin provisioning is explicit operator action. Registration of a new enterprise creates an organization and representative atomically, publishing disabled. Existing-enterprise representative membership is admin-managed; no join-by-name or supplied enterprise ID self-enrollment.

Every protected service calls ActorAccess for current role/status (not stale session role alone). Account IDs derive from trusted authenticated principal. Return 401 unauthenticated, 403 wrong role/status/CSRF, 404 for foreign-owned resource to avoid enumeration; 409 lifecycle/version/idempotency conflict; 422 domain validation; 429 bounded rate limit; 503 unavailable dependency. Preserve existing ApiResponse/ErrorResponse envelope and map errors centrally. Security failures do not become successful Standard guidance.

Enterprise authoring requires active ENTERPRISE account + active representative + matching enterprise owner. Publish additionally requires enterprise VERIFIED/ENABLED and representative publishing_permission. Admin may approve organization/configs but does not automatically read private student submissions; private access still requires declared operational purpose and applicable consent. For this scope no administrative impersonation/private-data browsing endpoint.

### Consent access matrix

| Operation | Required authority / scope |
|---|---|
| Student sees own profile/results/work/completion | Authenticated active owner; no sharing grant required |
| Enterprise sees student's profile | Active rep, recipient enterprise, PROFILE_SHARE |
| Enterprise sees completion list/details | Active rep, recipient enterprise, COMPLETION_SHARE; show completed version metadata only |
| Enterprise sees detailed submitted deliverables/assets | SUBMISSION_SHARE for that enterprise **and** scenario.enterprise_id equals rep.enterprise_id; no cross-enterprise delegation |
| Enterprise sees assessment result | ASSESSMENT_SHARE for that enterprise; no assumption profile/completion grant includes it |
| External generation, external query embeddings or grounded search | EXTERNAL_AI for the actual configured provider; minimal context, rechecked before each dispatch |
| Cross-conversation memory read/write | LONG_TERM_MEMORY effective plus owner/agent/enterprise context; new memory explicitly user-confirmed |
| Recruitment/contact sharing | Separate RECRUITMENT_CONTACT; workflow deferred, no implied authorization from other grants |

Grant request contains purpose, recipient, policyVersion and explicit `confirmed:true`; server validates current policy version and records timestamp. Preselected toggle until confirmation produces zero grant rows. Denying/revoking consent leaves assessment/enrollment/finalization usable. Enterprise profile/completion sharing remains separate from contact discovery; no broad student search/recruitment feature is added.

Revoke transaction and finalization serialize on app_users; dispatch authorization is a short per-user admission step. For the initial single-instance runtime, coordinate dispatch admission and revocation with the same per-user lock: recheck DB grant/version, begin the bounded outbound request, release admission lock without waiting for provider. Do not hold a DB transaction over the response. A request already admitted before revoke may finish remotely, but discard its protected output locally if grant/version changed. Test this ordering with barriers. Multi-instance admission coordination is deferred and must be redesigned before horizontal scaling, not falsely guaranteed by local locks.

Sharing exports/cache URLs use short lifetime and access checks; no permanent public file links. Revocation blocks subsequent reads and cached response replay; it cannot retract copies already lawfully delivered. Retention proposed: conversations/messages 30 days, retry response body 24 hours, minimal run metadata 30 days, memory 30 days renewable only on explicit user action. Core academic evidence remains history-preserving pending a reviewed retention/erasure policy. No automatic purge of existing legacy history in this change.

## 4. Core workflows and contracts

### Assessment

`VersionedAssessmentService.start(actor,versionId,startRequestId) -> AttemptView`; `saveResponses(actor,attemptId,expectedRevision,items) -> AttemptView`; `submit(actor,attemptId,expectedRevision,items) -> ResultView`; `result(actor,attemptId) -> ResultView`. Items contain unique questionId + typed answerData. Start validates published version, creates new attempt per new request UUID; retry returns existing. Questions returned from pinned version, never global active questions after start. Submit hashes canonical ordered input, locks attempt, validates required set/ranges/version, deterministically computes result with registered evaluator and atomically freezes responses/result. Repeated same final payload returns result; changed payload conflicts. No limit on retakes; results report scales/type/version separately.

RIASEC initial evaluator extracts existing approved six-dimension sum behavior into a pure versioned policy. Explicitly reject duplicate IDs, missing IDs, unknown IDs and out-of-scale values. Tie presentation uses canonical R/I/A/S/E/C order, not HashMap iteration; no new psychological score normalization or predictive claim. Existing incomplete/inconsistent historical score data stays unchanged and labeled legacy.

### Scenario authoring and publication

`ScenarioAuthoringService.create(actor,enterpriseId,metadata)` creates root+draft; `clonePublishedVersion(actor,scenarioId,sourceVersionId)` copies full task/definition/resource/skill/answer content into new draft IDs; `updateDraft(actor,versionId,expectedRevision,command)`; `publish(actor,versionId,expectedRevision)` validates complete content/provenance/authorization and freezes it with content hash, timestamp and current pointer. Task child edits increment parent scenario_version.row_version; immutable published content cannot be updated through child repositories. Lock parent for edits and publication. Archived scenario allows existing pinned participants; suspended scenario has explicit content denial until resolved. New version does not migrate an enrollment.

### Enrollment/finalization/model answer/completion

`EnrollmentService.enroll(actor,scenarioVersionId)` returns existing or new unique enrollment, independently of assessment. `TaskSubmissionService.saveDraft(actor,enrollmentId,taskId,expectedRevision,items)` allows one mutable draft. `finalizeTask(actor,enrollmentId,taskId,requestId,expectedRevision,items)` executes exact transaction in 01 §8. Hash includes enrollment/task/version and canonically ordered definition payload identities/checksums. First successful commit locks final work forever; identical retry is replay, any changed finalization is conflict. Validate all supplied optional items, but only required tasks determine completion.

`ModelAnswerAccess.read(actor,enrollmentId,taskId)` verifies owner and that exact task has FINAL valid submission, then returns answer and authorized AFTER_SUBMISSION resources. Never derive entitlement from browser result or score. `CompletionService.read/renderCertificate/readSnippet` retrieves immutable completion snapshot, not live mutable enterprise/user names. Rendering is deterministic and repeatable; no LLM or provider call. Optional task submitted later adds participation evidence only, not a new completion or snippet.

`SimulationEvidenceAccess.readOwnedParticipation(actor,studentId,limit)` returns `{submissionId,taskId,scenarioVersionId,skillCode,skillNameSnapshot,submittedAt}`; a separate `readOwnedLegacyEvaluations` contract preserves old scores. Dashboard joins through these ports and returns per-version progress counts and evidence type. No persisted dashboard or inferred professional competency. Task resources and file downloads use the same task access/release checks.

### Plan confirmation

`PlanCommands.confirm(actor,expectedActivePlanId,expectedRevision,title,steps,approvalContext?)` locks the student, checks exact active plan, archives previous plan and creates confirmed plan atomically. Direct user endpoint is explicit human confirmation, allowed without AI. An Agent proposal contains the same bounded command and may execute only after approval and current checks. Agent outputs cannot update plan on their own.

## 5. Configurable Agent platform execution

### Configuration and critical ports

`AgentAdminService.createAgent/publishConfiguration/revokeConfiguration`; `AgentConversationService.create/get/list/clear/archive`; `AgentRuntimeService.accept(actor,conversationId,clientRequestId,message,language) -> RunAccepted`; `readRun/recoverByRequest/cancel`; `AgentMemoryService.list/confirm/update/disable/delete`; `AgentProposalService.list/approve/reject/execute`.

`AgentModelClient.decide(RunContext,NativeProtocol,ToolDeclarations) -> Final | ToolCalls | Clarification`; provider-native call IDs/order/opaque fields stay within adapter. `AgentTool.execute(ActorContext,TypedArguments,RunBudget) -> BoundedObservation`; registry exposes canonical codes, JSON schemas, applicable roles, side-effect classification. `AgentPolicy` SPI composes contextual instructions, allowed response shape and deterministic fallback for an agent policy_key. `ApprovedActionExecutor.execute(ApprovedProposalContext) -> TypedResult` dispatches to owning domain command, never arbitrary SQL. `KnowledgeSearchService.search(AuthorizedKbScope,query,profileKeys,limits)` returns passages only. `KnowledgeSourceAccess.revalidate(actor,configId,reference)` independently supports inspector/replay/finalization.

Proposed canonical read tools: `assessment.results.read`, `simulation.participation.read`, `simulation.legacy_results.read`, `catalog.search`, `guidance.paths.compare`, `guidance.resources.search`, `knowledge.search` (the application code for conceptual kb.search), `plan.read`. Proposal tools: `scenario.draft.propose`, `task.draft.propose`, `plan.propose`. Tools obtain actor from backend context; input schema does not accept arbitrary studentId/tenantId/SQL/URL. Arguments may contain owned resource IDs that are still reauthorized. Memory management is a direct user control, not an automatically enabled write tool. A new Agent/config has no grants until admin explicitly adds them.

Initial Syn config: STUDENT audience, reviewed career policy, existing bounded read capabilities and plan proposal; default Standard quick actions preserved. Initial Enterprise Assistant config: ENTERPRISE audience, draft proposal + own draft inspection + own-tenant/platform authorized KBs; no student-evidence tool enabled by default. Config editing can choose registered policies/tools/provider settings, not arbitrary server code/network endpoints. Admin can create further Agents with those capabilities without adding hard-coded agent-name branches.

### Bounded runtime, not a renamed Syn orchestrator

1. **Accept transaction:** current actor/agent/config/audience/context checks; lock owner/conversation; deduplicate `(actor,requestId)` using canonical request hash; check active-run uniqueness; insert run + user message + required consent ledger snapshots; commit before HTTP provider work. Config pinned from conversation, never client-selected per turn.
2. **Prepare:** retrieve bounded prior successful user/assistant pairs from that conversation only; optional memory only if currently consented and same owner/agent/enterprise; fetch authoritative evidence via ports. Prior assistant text is context, never current evidence or a new citation ledger. Reset all counters for every new run.
3. **Decide/execute loop:** model may answer directly or request declared tools; backend validates tool code, arguments, role/status, ownership, current config grants, KB permissions, consent and remaining budgets before each execution. Return bounded observation correlated to native call ID; model can reconsider and request a second sequential tool. Reject arbitrary code/SQL/unapproved writes. No provider SDK automatic tool executor bypass.
4. **Conclusion:** reserve a model opportunity for final/abstention; at cap remove tool declarations and disallow more execution. Validate schema, citation membership/quotes, evidence permissions and competence-claim limits. One bounded repair may consume remaining call allowance; cannot extend total cap. Knowledge-dependent assertions need actual passages; general educational explanation is clearly labeled and does not fabricate K references.
5. **Finish transaction:** lock owner/conversation/run; recheck epoch, state, expiry, privacy version, agent/config status, sources/KB rights; persist assistant message, safe response/provenance/trace and terminal state atomically. Changed permissions cause cancellation/fallback/redaction as appropriate; no stale personalized answer committed. Failure of persistence never returns a falsely durable success.
6. **Recover/cancel:** same key/hash reads existing outcome, different hash 409; client reload polls accepted run, not resends with new ID. Cancel is owner-scoped conditional terminal transition and propagates cancellation token. Late provider result cannot resurrect run. Deadline watchdog marks abandoned active runs FAILED with INTERRUPTED; no replay of tools/writes after restart.

Proposed **server caps**: 20-second whole run, at most 4 provider invocations total including retries/repair, 4 tool executions, 2 distinct retrieval queries, topK 5 across all authorized KBs, 20 candidates per retrieval branch, 8 recent user/assistant pairs, 32 KiB serialized context ceiling, 12 KiB total tool observation ceiling, 1,200 output tokens. Configuration can lower but not exceed server caps. These are conservative proposed engineering bounds, not measured latency/quality claims. Use current Syn façade's lower existing response/token limits where required by its DTO; never silently widen legacy budgets. Request rate limit initially 10/minute/actor plus gateway limit. Distinguish input bytes, estimated token reservation and provider-reported usage; absent usage is unknown, not zero. Transport retry only for existing classified transient failures within remaining deadline/call budget.

Syn v1 can initially continue server-planned execution under its existing bridge; it must not be advertised as generic native-agent capability. New `/v2/agents/...` runtime must pass direct response, tool request, observation, reconsideration and sequential-tool tests. Provider adapters without native support are rejected for NATIVE config publication or explicitly Standard-only; do not simulate native agency with keyword routing.

### Independent KB pipeline and retrieval

Create KB -> upload/reference reviewed content -> normalize/preflight storage-safe text -> persist STAGED version -> review provenance -> chunk deterministically -> embed with explicit full profile outside transaction -> atomic chunk/embedding publication + document pointer update. Failed new version leaves previous active version retrievable. Restartable ingestion can retry a STAGED/FAILED version with source hash comparison; no queue cluster needed. TXT/Markdown initial ingest plus legacy approved chunk copy; PDF/OCR/remote crawling deferred until separately designed parser provenance exists. No background arbitrary URL fetching.

Allowed KB set = configuration attachments ∩ active KBs ∩ actor tenant/platform access. If request names a subset, it must be within this intersection; unauthorized explicit KB ID yields 403, never silently searches another KB. Lexical and vector queries share the same SQL eligible set. Query embeddings require provider-specific consent even if answer generation provider differs. Regex removal of email/numbers in old code is not a privacy guarantee; minimize raw query and apply consent. No consent/embedding outage => authorized lexical retrieval and local Standard response. External failure cannot block catalog/assessment/submission/completion.

Full embedding profile identity controls comparisons; current 768 vectors stay associated with their real profile. A second profile requires complete corresponding vectors and exact compatible query encoder; do not compare scores across spaces as if equivalent. RRF combines independently ranked branches only after authorized filtering. Cache keys include user/tenant, config, KB set/status revision, profile, query and run; no global query-only cross-tenant cache. Source inspector checks current authorization, active version and reference namespace. Returned citations contain source ID/version/hash/excerpt/location from actual delivered data; URLs from approved stored metadata only. Retrieval success does not prove answer accuracy; held-out semantic review is separate.

### Human-approved typed writes

Proposal includes exact typed payload, immutable hash, target IDs, target parent revision/content hash, actor/context and expiration (proposed 15 minutes). It can only create/update **draft** scenario/task or confirm a plan. No publish, assessment score, final submission, completion, permission, consent or certificate tools.

Approval UI/API displays the exact diff and hash. `approve` requires same authenticated requesting user for this scope and current domain edit permission; record approver/time, but make no domain mutation. `execute` performs a second check and atomic transaction: lock actor/target/proposal in common order, require APPROVED and unexpired, recompute payload hash, match expected revision/content hash and current status/tenant; invoke owning command; record result + EXECUTED with audit and plan proposal linkage; commit together. Concurrent execute retries return same result_reference, with no duplicate target. Any edit since proposal => STALE/409, require a new proposal and review; never apply to the latest target opportunistically. Approval after revoke/disable/tenant change fails. Revoking a grant used by its source run makes an unexecuted proposal STALE; direct human draft editing remains available under ordinary authorization. Draft task/scenario expected revision binds the parent version and a calculated draft graph hash; it does not rely on a published-only content_hash. Service-level authorization alone, independent of agent, still protects target writes. No external side effects are included, so no distributed transaction/outbox is required.

## 6. API and React compatibility specification

All paths include existing `/api` gateway context. New IDs serialized as strings; timestamps ISO-8601 UTC, language EN/VI, existing success/error envelope. `If-Match` carries integer row_version for mutable drafts; missing required revision is 428, stale is 409. List endpoints use cursor+limit (default 20, max 100), stable ID/time order. No frontend mock is an authenticated saved result.

### Current consumed endpoints

| React consumer | Current route/shape | Cutover decision |
|---|---|---|
| `features/assessment/hooks/useAssessment.js:51,66,96` | GET `/v1/assessments/questions`; POST `/attempts`; POST `/attempts/{id}/submit`; hardcoded student header | Preserve legacy reads with safe DTO; protected results owner only. Replace start/submit with v2 versioned flow. Old writes return WORKFLOW_UPGRADE_REQUIRED, no permissive fallback |
| `features/simulations/api/simulationCatalogApi.js`, `simulationApi.js` | GET `/v1/simulations`, GET `/{slug}/tasks`; POST `/{slug}/attempts` returns graded response | Public old summary can remain on legacy catalog; new catalog/detail v2 adds version/deliverables. Old graded submit explicitly deprecated, cannot adapt by fabricating score |
| `features/simulations/hooks/useSimulationAttempt.js:30–49` | Catch branch locally evaluates and shows result after failed backend | Coordinated frontend fix preserves draft and shows unsaved/retry, never completion/certificate/model answer. Offline fixture kept only explicit demo with visible PREVIEW label |
| `features/guidance/api/guidanceApi.js` | GET `/v1/guidance/dashboard`; score-based recentResults and skills | Keep legacy response for legacy evaluations; v2 dashboard supports tagged evidence/completions. Migrate renderer before showing new completion evidence; no score=100 shim |
| same file + `SynWidget.jsx` | POST `/v1/guidance/syn/messages`, UUID clientMessageId; GET `/syn/requests/{id}` recovery; DELETE `/syn/sessions/{id}` | Preserve response fields/text/language/provenance/activity/cards/planDraft and ownership/idempotency. Bridge new conversations to new runs; existing legacy requests recovered from legacy store with current consent/source checks |
| Syn source/recovery controls; `features/guidance/utils/synSources.js:1–7` | GET `/v1/guidance/syn/turns/{id}`, POST `/{id}/cancel`; GET `/v1/guidance/knowledge/chunks/{id}`; current URL helper allows only old relative source route or safe HTTPS | Keep legacy IDs/namespaces, authorization and TTL; new references use v2 inspector and narrowly extend helper for `/api/v2/knowledge/chunks/<decimal-id>?configurationId=<decimal-id>`. No arbitrary relative URL allowlist expansion |
| `guidanceApi.js saveExplorationPlan` | POST `/v1/guidance/plans` title/steps confirmation returns saved | Preserve authenticated explicit confirmation; if based on agent draft require known session pending-plan match/proposal hash via bridge. Arbitrary model-generated request cannot self-approve; direct human plan remains allowed |
| `shared/api/httpClient.js` plus direct assessment fetch | /api envelope; currently no session-CSRF setup | Consolidate CSRF/login handling, remove X-Student-Id, preserve error/retry and EN/VI; coordinate two feature owners before rollout |

The current simulation query/submit controller and assessment result endpoint exist even when a particular React screen does not currently call them; protect them too. Public route wildcard must not accidentally permit model-answer/file/submission reads. Direct assessment fetch bypasses shared client today and must be addressed explicitly.

### New core endpoints and minimum DTOs

| Operation | Contract |
|---|---|
| Auth | GET `/v2/auth/csrf`; POST `/v2/auth/register` {email,password,displayName,role,enterpriseName?}; POST `/v2/auth/login`; POST `/v2/auth/logout`; GET `/v2/me` -> id/role/status/profile/consent summary; no password field returned |
| Enterprise management | GET `/v2/enterprises/me`; POST `/v2/admin/enterprises/{id}/verification` {status}; POST `/v2/admin/enterprises/{id}/publishing` {status}; PUT `/v2/admin/enterprises/{id}/representatives/{userId}` {membershipStatus,publishingPermission}; PATCH `/v2/admin/accounts/{id}/status` {status}; If-Match required for existing roots; no public role-elevation endpoint |
| Consent | GET `/v2/me/consents`; POST `{purpose,enterpriseId?,recipientKey,policyVersion,confirmed:true}`; POST `/{grantId}/revoke`; owner-only, grant list shows effective state |
| Assessment catalog/start | GET `/v2/assessment-types` -> active published version IDs/scales; POST `/v2/assessment-versions/{id}/attempts` {requestId} -> id/version/status/revision/pinned questions |
| Assessment draft/result | GET `/v2/assessment-attempts/{id}`; PUT `/{id}/responses` {items}; POST `/{id}/submit` {items}; GET `/{id}/result` -> attemptId/type/version/evaluator/scores/scales/calculatedAt; If-Match on writes, no actor ID |
| Assessment authoring | POST `/v2/admin/assessment-types`; POST `/v2/admin/assessment-types/{id}/versions`; PUT `/v2/admin/assessment-versions/{id}` full draft questions/config; POST `/v2/admin/assessment-versions/{id}/publish`; bounded registered evaluator only |
| Scenario authoring | POST `/v2/enterprise/scenarios`; POST `/{id}/versions` {sourceVersionId?}; GET/PUT `/v2/enterprise/scenario-versions/{id}` draft DTO includes tasks/definitions/resources/modelAnswers/skills; POST `/{id}/publish`; POST `/scenarios/{id}/availability` |
| Public catalog | GET `/v2/scenarios` and `/{slug}` -> current published version ID/title/summary/difficulty/duration/enterprise; no model answers, evaluation or student data |
| Enroll | POST `/v2/scenario-versions/{id}/enrollments` -> {id,versionId,status,revision}; GET `/v2/enrollments` and `/{id}` -> pinned tasks/definitions/progress; natural unique replay |
| Draft/finalize | GET/PUT `/v2/enrollments/{eid}/tasks/{tid}/submission`; POST `.../finalize` {requestId,items}; response {submissionId,status,submittedAt,completionId?,revision}; same key/hash replay, changed payload 409 |
| Answers/completion | GET `/v2/enrollments/{eid}/tasks/{tid}/model-answer`; GET `/v2/completions/{id}`; GET `/{id}/certificate`; GET `/{id}/resume-snippet` -> fixed text/templateVersion; no snippet edit endpoint |
| Assets | POST `/v2/files` multipart staged metadata/upload; GET `/v2/files/{id}/download?useType=...&useId=...`; backend validates the exact referenced task/submission/version; no arbitrary storage path |
| Evidence/plans | GET `/v2/guidance/dashboard` -> interests by type/version, enrollment progress, completions, task participation, legacyEvaluations, activePlan; POST `/v2/plans/confirm` {expectedActivePlanId,expectedRevision,title,steps}; GET `/v2/plans` |
| Enterprise shared reads | GET `/v2/enterprise/students/{id}/profile`, `/completions`, `/assessments`; GET `/v2/enterprise/submissions/{id}`; deny absent purpose grant; submissions additionally require scenario ownership |

### New Agent/KB endpoints

| Endpoint family | Required behavior |
|---|---|
| `/v2/admin/agents` POST/GET; `/{id}` PATCH; `/{id}/configurations` POST; `/configurations/{id}/publish` POST; `/revoke` POST | Validate registered policy/provider/tools/budgets and explicit KB IDs; config read/write makes zero generation/embedding calls |
| `/v2/agents` GET | Only usable current configurations for actor/context; hide inaccessible admin metadata/instructions |
| `/v2/agents/{id}/conversations` POST; `/v2/conversations` GET; `/{id}` GET; `/{id}/clear` POST; `/{id}/archive` POST | Pin config, retain scoped durable messages, clear epoch/redaction/cancellation; explicit new chat to adopt new version |
| `/v2/conversations/{id}/runs` POST {clientRequestId,message,language}; `/v2/runs/{id}` GET; `/v2/runs/by-request/{id}` GET; `/{id}/cancel` POST | Accept returns 202 {runId,state}; bounded in-process worker; no unbounded queue. Poll recovery yields terminal DTO, safe trace/citations, not provider protocol |
| `/v2/me/agent-memories` GET/POST; `/{id}` PATCH/DELETE | Explicit user-reviewed summary/key/agent/context; deny missing memory consent; DELETE redacts and disables rather than silently preserving usable content |
| `/v2/proposals/{id}` GET; `/approve`, `/reject`, `/execute` POST | Approve requires displayed payloadHash; execute is separate explicit operation/confirmed UI action, rechecks all conditions; one result |
| `/v2/knowledge-bases` GET/POST; `/{id}` PATCH; `/{id}/documents` POST; `/documents/{id}/versions` POST; `/versions/{id}/review`, `/publish` POST; `/documents/{id}/revoke` POST | Authorized owner/admin; reviewed staged publication, preserve prior active version; no private remote crawl |
| `/v2/knowledge-bases/search` POST {configurationId,kbIds?,query}; `/v2/knowledge/chunks/{id}` GET with configurationId | Independent passage search and source inspection; current actor/config/KB checks; no generation; missing profile consent falls back lexical |

For asynchronous acceptance use a small bounded Spring task executor with explicit rejection (429/503 before acceptance if no capacity). Persist accepted run first, handle dispatch rejection by marking FAILED; do not leave orphan PENDING indefinitely. Single-process restart marks expired/abandoned runs INTERRUPTED; user retries only through defined new request after observing failed state. v1 bridge can await result within original HTTP timeout while retaining recovery contract.

## 7. Decisions for consolidated approval and boundaries

Approve together: same-schema additive architecture and BIGINT strategy; 40 new/44 active/64 coexistence tables after preserving main's three additional career-quiz tables (the original baseline counted 61); session+CSRF auth (no JWT subsystem); ADMIN storage alias; publication requires verified+enabled organization and representative grant; content requires ≥1 required task and deliverable; proposed transcript/memory retention and provider-specific consent; completion/certificate/snippet embedded immutable record; typed draft-only proposal execution; explicit v1 write deprecation and coordinated minimum frontend migration; two execution packages if needed.

Migration-numbering amendment approved separately in chat on 2026-10-14: main/Syn V1–V14 unchanged, Package A V15–V18, planned Package B V19–V21. No module, API, business invariant or Agent capability changes accompany this numbering fix.

High-risk facts requiring evidence rather than a guessed default: actual Flyway/checksum/runtime data; case-colliding identities; legitimate enterprise ownership of legacy content; missing historical version/confirmation provenance; exact deployed embedding profiles; corpus license/reviewer validity; production retention/deletion policy and deployment credential provisioning. Sol can implement/tests with synthetic fixtures without those facts, but cannot promote uncertain real records or enable affected production paths.

No implementation may infer authority from old consent timestamp, client user ID, model-specified tool/tenant, known chunk ID, administrator label alone or browser scorer. Existing source/infrastructure is reusable evidence of design, not proof that the target runtime is safe or complete.
