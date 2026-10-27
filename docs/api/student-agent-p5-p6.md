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
| POST `/v2/agent-sessions/{sid}/messages` | `{requestId,text}`;202 `Run`, task CHAT |
| POST `/v2/agent-sessions/{sid}/insight-reports` | Same payload, explicit action;202 INSIGHT_REPORT; no implicit report from chat |
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
