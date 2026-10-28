# P5/P6 — Student Agent, Reports, Plans and Usage API

Status: implemented on `feat/student-agent-and-ai-reporting`; verification evidence is
in `.agent/reports/p5-p6-completion.vi.md`. Feature flags remain opt-in:
`app.agent-platform-enabled=true`, `VITE_AGENT_PLATFORM_ENABLED=true` with Core V2.
HTTP prefix `/api`; all UUIDs and Core BIGINTs are serialized as strings.
Existing Core, legacy Syn, KB Studio and Admin Builder contracts stay available.

Current UC-08 correction (2026-10-26, V31): role publication supersedes the historical
per-Student assignment wording below. `/v2/agents` resolves at most one current published
Agent through the authenticated `STUDENT` or `ENTERPRISE` role. It includes
`runtimeAvailable:true` for Student and `false` for Enterprise, which has no Student
session/private-tool access. New Student sessions pin the role Agent's current published
configuration. Re-publishing that same Agent preserves existing valid pins; replacing
the role Agent removes old-Agent runtime authority. Historical per-user assignments are
not an access grant. Session owner, consent, privacy, source authorization and execution
fencing remain mandatory. See [role API](agent-platform-p3-p4.md#uc-08-role-publication-correction--2026-10-26).

## Browser contract

Backend-issued short-lived HttpOnly JWT cookie; existing same-origin CSRF bootstrap
`GET /v2/auth/csrf`, header `X-CSRF-TOKEN` on mutations. No bearer token storage or
credentials in stream URLs. Existing 900-second JWT/session revocation semantics
are unchanged. SSE revalidates that exact credential on every replay poll.
Active Student role, current assignment, Agent and immutable pinned configuration
are server-side authority. Foreign private IDs are404; wrong role403; missing or
expired authentication401. Authorized USER history remains readable after external
consent/assignment loss; unavailable AI replies become a labeled placeholder.

| Method / path | Payload / result |
|---|---|
| GET `/v2/agents` | Existing assigned active published Agents; no prompt/tool configuration leak |
| POST `/v2/agents/{agent}/sessions` | `{requestId,title,contextSessionIds:[]}`; new session pins current published configuration; up to3 own eligible contexts only when memory enabled |
| GET `/v2/agent-sessions` | `agentId?`, keyset `before` ISO Instant + `beforeId` UUID together, `limit=1..100` |
| GET / DELETE `/v2/agent-sessions/{sid}` | Owner-authorized session / idempotent soft-delete, invalidation and cancellation intent |
| GET `/v2/agent-sessions/{sid}/interactions` | `afterSequence=0&limit=100`; server sequence ascending; content and unavailable flag |
| POST `/v2/agent-sessions/{sid}/messages` | `{requestId,text}`;202 `Run`, task CHAT; natural explicit artifact requests may return validated `requestedAction` plus report/draft |
| POST `/v2/agent-sessions/{sid}/insight-reports` | Same payload, explicit action;202 INSIGHT_REPORT; ordinary chat creates no unsolicited report |
| POST `/v2/agent-sessions/{sid}/plan-suggestions` | Same payload;202 PLAN_SUGGESTION; draft execution output only |
| GET `/v2/agent-executions/{eid}` | Owner/current-authority Run projection; no internal input/prompt/native protocol |
| POST `/v2/agent-executions/{eid}/cancel` | CSRF protected, same-owner cancellation intent |
| GET `/v2/agent-executions/{eid}/events` | Cookie SSE; `Last-Event-ID` integer; monotonic durable events; never admits a new run |
| GET `/v2/insight-reports` / `/{id}` | Current-authority owner-only saved report; zero model calls on reload |
| POST `/v2/exploration-plans/confirm-suggestion` | `{sourceExecutionId,requestId,contentHash,expectedActivePlanId,expectedRevision}`; exact server-validated draft, existing plan facade transaction |
| GET `/v2/admin/agent-usage` | Admin only, `from` inclusive UTC Instant, `to` exclusive, optional mode; max366-day query window (not retention policy) |

Message request IDs are owner/mode-unique. Same request and semantic hash returns
original execution; changed payload409. One active execution/session; retries do
not append duplicate USER messages or create model cost. Different task actions
need distinct request IDs. Server sequences never come from clients. Interrupted
runs become explicit failures rather than silently regenerating after restart.

## SSE and Run

Events: `execution.started`, `tool.status`, `answer.segment`, terminal
`execution.completed|failed|refused|cancelled`. Each has `id`, `event`, JSON `data`.
Segments are actual native-provider deltas buffered into safe semantic sentences;
no client typewriter, raw tool arguments, protocol thoughts or prompt streaming.
A partial report is never a valid report. Final object is schema/evidence validated
before atomic ledger/ASSISTANT/report publication. Disconnect stops the browser
stream, not canonical execution. Reconnect/read resumes identity without generation.
Cancellation, assignment/consent/account/source changes block late publication.

Run includes id/session/task/status/error, output, authorized actual tool traces,
configuration ID/hash, nullable input/output tokens, estimatedCost, costStatus,
latencyMs, cancellationRequested, lastEventSequence, reportId, planContentHash.
Local fallback provenance is distinct from validated model output. Missing metrics
stay null; NOT_APPLICABLE and UNKNOWN are distinct. Admin usage never returns these
private Run objects, Student identifiers, transcripts, arguments or reports.

## Native tools

`assessment.lookup({assessmentType?})`: public catalogue + independently stored own
versioned results. `simulation.explore({query?,careerTrack?,difficulty?})`: published
catalogue/outline only. `simulation.myEvidence({})`: own progress, finalized quiz
results, completed-task practiced skills and truthful completion projections.
`kb.search({query,topK?})`: explicit pinned grants + current published PLATFORM KBs.
No student/tenant selectors. Every dispatch rechecks current authority. Embeddings
reuse existing infrastructure only with separate effective provider consent;
otherwise retrieval stays lexical. Admin preview exposes public Core catalogue,
and explicitly denies private Student tools instead of impersonation.

## Structured artifacts

Common JSON has exactly `schemaVersion:1,outcome,text,citations,suggestions`.
Citations are `{referenceId,quote}` and must quote current-run delivered evidence.
KB_ONLY policy requires actual KB citations, not substituted Core references.
INSIGHT_REPORT ANSWER additionally has `report` with closed schema:
`schemaVersion:careersim-insight-v1,summary,hypotheses[{direction,reason,evidenceRefs}],
strengthsPracticed,difficulties,limitations,nextSteps,disclaimer,evidenceRefs`.
Practiced strengths must exactly match completed-task skill evidence, never a
proficiency inference. Unknown refs, score fabrication, HTML/URLs, certainty claims
and unsupported fields fail validation. Disclaimer is the server-required English
statement; original AI/source content is not translated by UI labels.

PLAN_SUGGESTION ANSWER has `planSuggestion` with closed schema:
`schemaVersion:careersim-plan-v1,title,steps[1..5],limitations,evidenceRefs`.
Server adds canonical `planContentHash`. Confirmation accepts source/hash only,
rechecks live owner/assignment/consent/lineage and uses existing optimistic active
plan revision. Ignore/reject does no plan write; duplicate confirmation is idempotent.
Confirmed business plans remain historical records after source conversation deletion.

## Frontend handoff

`/agents` → `/agents/:agentId?session=...` provides session creation/history,
per-account preserved message drafts, explicit report and plan actions, source
quotes, stream resume/cancel and plan confirmation. `/admin/ai-usage` consumes only
safe aggregate API. Shared account generation aborts/rejects delayed SSE data;
AppShell key resets all private route state. EN/VI labels, original source/model
content, existing routes and legacy Syn are retained. No private Student upload UI.

## Model reliability increment — 2026-10-27

Pinned configuration v2 may include optional ordered same-provider
`experienceConfig.fallbackModelKeys` (max3). Existing single-model configurations
remain readable without changing their sealed hash. A new chat still pins the exact
current published configuration; fallback is part of that tested/published version.

The existing native loop retains authorized observations across bounded generation
retry/failover. It never re-executes plan confirmation. Schema validation precedes
report persistence; at most one bounded output repair is allowed. Configured
multi-model streaming buffers deltas until validation, preserving authoritative
replay/cancellation/consent/source semantics. Terminal unavailability is a failure
with a clear message, not a fake answer. The submitted question remains in the
account-scoped frontend draft for retry. Compact Quick Actions populate only.

Safe `MODEL_ATTEMPT` metadata uses the same execution/audit storage and UC-09
projection. See [model reliability](../architecture/agent-model-reliability.md).

## Student admission correction — Owner decision, 2026-10-27

Explicit STUDENT_CHAT messages, report requests and draft-plan requests no longer
require a separate EXTERNAL_AI consent grant. This supersedes the earlier consent
prerequisite for this feature only. No consent records are fabricated. Other
consent purposes and consumers remain unchanged. Actor/ownership, active role
publication, pinned configuration, privacy revision/epoch and current evidence
authorization still fence dispatch, history, replay and late output.

Session adds safe provider enum metadata from the pinned model, never credentials.
Modern Gemini requests project the existing closed output/report/plan schemas;
host schema/evidence validation remains authoritative. Invalid artifacts are never
saved. OUTPUT_VALIDATION stores safe categories without rejected content.
Plan persistence still requires explicit source/hash confirmation and idempotency.

Final Student citation schemas bound current reference IDs. Literal reference/quote
examples appear separately in currentRunCitationOptionsUntrustedData, never in
platform instructions. Host validation still rejects unmatched/duplicate/undelivered
references or quotations. There is no silent candidate rewrite. Max20 references
with at most3 short quote windows each; this is transient request data, not another
persisted event system. Student reserves one output-repair opportunity inside the existing
round cap when possible, with tools disabled and current observations retained.
Its versioned central repair-policy hash is safe execution audit metadata.

### Student natural action contract — 2026-10-28

The Student host now supplies versioned `student-actions-v1.txt` alongside the unchanged pinned Agent prompt. The model interprets EN/VI requests and chooses a closed `requestedAction` (`NONE`, `INSIGHT_REPORT`, `PLAN_SUGGESTION`) inside the existing CHAT final output; there is no keyword router, additional model pass, new endpoint or new domain tool. Informational/negated requests use NONE; missing evidence and security refusal remain distinct.

A matching artifact is checked by the existing report/plan validator against current authorized evidence before persistence. V33 adds only CHAT-with-matching-action alternatives to the existing owned-input/report and exact-source/confirmation DB guards. Original task/input/configuration and request identity remain immutable. Reports require a successful validated execution; plans remain drafts until the existing explicit owner/hash confirmation. Admin Preview and KB schemas are unchanged. Client buttons send their own explicit task text, never unrelated composer contents; retry keeps the original request payload even across locale changes.


### Student chat usability and discovery follow-up — 2026-10-28

New sessions persist exactly one opening greeting from their pinned experience configuration (including existing variable rendering/defaults), without a model execution. Idempotent session creation does not duplicate that greeting. The popup initially restores the last selected session if it is still in the accessible history, otherwise resumes the latest returned accessible session, or creates one clean session when history is empty. Accepted messages clear only the submitted composer draft; newer edits and unsent account/session-scoped drafts survive navigation/reload.

The Student-only host policy is now `student-actions-v2.txt`; pinned Agent templates/configuration hashes remain unchanged. Public simulation/assessment discovery requires no prior participation. `simulation.explore` exposes only allowlisted public metadata (including target role, introduction, objectives and duration), with query/track bounds matching the existing catalogue service. Personal progress/results remain separate owner-scoped evidence. Draft plans may use current public evidence with honest limitations when no personal results exist, and still require explicit exact-source/hash confirmation.

A premature INSUFFICIENT_EVIDENCE final before any lookup may be reconsidered once only when another tool-enabled round remains within the original saved budget. The unaccepted preliminary text is buffered; no fake tool observation is added, no tool is forced, and actual tool execution/authority/citations remain enforced. Greetings, security refusals, spent tool budgets and completed lookups do not trigger this correction. Admin Preview/KB execution behavior is unchanged.


### Published assessment discovery / chat alignment — 2026-10-28

`assessment.lookup` now uses the assessment subsystem's internal `publicCatalog()` projection, preserving the existing REST catalogue contract and tool arguments. It returns each ACTIVE type's current PUBLISHED version, SQL-counted question inventory and a reviewed public overview (purpose, when to try it, result meaning and limitations). Explanations/indicative durations align with existing `assessmentCatalog.js` / `profileMetadata.js`; estimates are explicitly NOT measured and omitted when the published inventory differs from the reviewed count. Unknown/custom types retain real counts with unavailable explanatory metadata. No question prompts, response/scoring config, keys or other owners' results are projected. Public discovery works without prior attempts, in Admin public Preview as well as authorized Student runtime. Student host policy `student-actions-v3.txt` distinguishes these active catalogue facts from general handbook examples.

Student and Admin Preview share a transcript-only viewport hook: a newly submitted user turn is positioned once near the top, with temporary tail space for the response; generation/completion never follows automatically. Admin Preview uses the same compact composer, hidden history drawer, per-answer collapsed evidence/run details and bounded expanded panel. Its existing saved-configuration conversations, polling, Clear/Delete confirmation/revision and Stop/idempotency remain Admin-only; no Student runtime/session is used. The UI does not simulate streaming for an API that returns validated turns by polling.

### Student self-evidence and citation repair — 2026-10-28

`assessment.lookup` accepts optional `scope: ALL|CATALOG|PERSONAL` (default ALL).
PERSONAL returns the requesting active Student's exact attempt/result/type totals,
then recent saved results; optional assessmentType filtering happens before the
bounded result window. CATALOG reads only the existing public projection and does
not fetch personal results. Admin Preview remains public-only and rejects PERSONAL.
`simulation.myEvidence` also supplies exact accessible enrollment totals by status
and distinct simulation count, excluding suspended resources as in the existing
owner service. Detail lists remain bounded; their length is not a total. Aggregate
counts are execution-time snapshots, not a reason to hide an old conversation when
its owner starts another quiz. Current role/owner and underlying-source checks remain.

Student host policy `student-actions-v4.txt` explains natural EN/VI self-data requests,
submitted-versus-started and enrollment-versus-completion distinctions, and minimal
relevant tool use. Citation examples group alternative literal windows once per
referenceId. Duplicate citations still fail validation; the one existing bounded
repair receives the backend-owned validation category instead of replaying tools or
receiving rejected output/private diagnostics. Profile-quiz category raw scores are
recognized only from delivered own results. Model-attempt audit records allow only
numeric provider HTTP status/class, never raw provider bodies or headers. No new
REST endpoint, migration, provider setting, model grant or Student identity is added.
