<!-- documentation-sync-2026-10-22 -->
> **Source distinction checked 2026-10-22:** the dedicated 90-second Agent SSE locations described near the end of this document remain pre-existing local changes in `frontend/nginx.conf` and `deployment/nginx/default.conf`, not in checked `origin/main@5486f9f`. Main backend already emits `X-Accel-Buffering: no` and keepalives. Keep earlier working-tree smoke separate from as-merged/deployed verification; no proxy change/test is performed here. See [current catalogue and report/code discrepancies](../requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md).

<!-- preserved-content-start -->
# P5/P6 — current implementation boundary

The approved Spring Boot modular monolith and Core BIGINT identifiers remain.
Only additive V22 extends existing P3/P4 UUID platform persistence. No historical
Syn transcript adoption, consent grant, self-review, score or completion backfill.

## Responsibilities

- `StudentAgentController` → `StudentAgentService` → `StudentAgentServiceImpl` owns
  admission, current authority, source lineage, context, late commit and replay.
- Existing `ExecutionServiceImpl` remains Admin Preview / KB Test host. Both hosts
  use **one `BoundedNativeExecution` controller** for round/call budgets, final-round
  tool disabling, native correlation and observation flow. No mode-specific provider
  engine or fake Student context for Admin/KB modes.
- `StudentEvidenceTools` validates model arguments, binds server actor and calls
  Assessment/Scenario/Enrollment/Submission evidence facades. No Core DAO coupling,
  locked answers, raw submissions, certificate identity snapshot or autonomous writes.
- `NativeAgentModelProvider` handles actual Gemini/OpenAI native protocol and provider
  SSE; `SynHttpRequests` shares existing bounded HTTP/admission/deadline infrastructure.
  Opaque Gemini signatures and OpenAI call IDs stay request-scoped, never public traces.
- `AgentTextSegments` emits only decoded root text as bounded safe semantic segments;
  final text must agree with delivered prefix. `StudentOutputValidator` closes report,
  plan, citation, actual score and practiced-skill schemas. Existing P3 validation
  remains the default overload without Student score authority.
- `AgentSessionDao`, `AgentExecutionDao`, `InsightReportDao` persist ordered immutable
  interactions, ledger and reports; `AgentUsageService`/`AgentUsageQuery` authorizes
  Admin aggregate requests, `AgentUsageDao` selects safe operational projections.
- Explicit confirmation calls existing `PlanCommands` in the same transaction and
  binds source execution/canonical content hash/idempotency request. No tool writes
  an exploration plan. Confirmed plans remain historical business records after
  source deletion; new confirmations/replays require current source authority.

## Execution sequence

```mermaid
sequenceDiagram
    actor Student
    participant UI as React / Nginx
    participant Host as StudentAgentService
    participant DB as PostgreSQL
    participant Loop as BoundedNativeExecution
    participant Tools as Read-only Core / scoped KB facades
    participant Model as Native provider adapter
    Student->>UI: Explicit message / report / draft-plan action
    UI->>Host: POST + JWT cookie + CSRF + requestId
    Host->>DB: Short admission tx: owner/assignment/pin/epochs, USER + ACCEPTED
    Host-->>UI: 202 canonical execution identity
    Host->>Loop: Explicit durable execution snapshot + bounded host callbacks
    Loop->>Host: Recheck current authority before each dispatch
    Loop->>Model: Minimized context, native tool declarations, provider consent
    Model-->>Loop: Genuine native calls or incremental structured output
    Loop->>Tools: Host actor + validated closed arguments
    Tools-->>Loop: Current-authorized evidence and source lineage
    Model-->>Host: Decoded semantic segments; raw private protocol excluded
    Host->>DB: Monotonic safe event journal
    UI->>Host: Cookie SSE + Last-Event-ID (read/replay only)
    Host-->>UI: Safe persisted segments / terminal event
    Host->>DB: Recheck + atomic terminal execution/ASSISTANT/valid explicit report
    Student->>Host: Explicit exact-hash/source plan confirmation
    Host->>DB: Existing active-plan revision/confirmation tx + source binding
```

## Authority and memory

The execution row carries owner, request hash, session, immutable config/prompt hash,
privacy/context epochs and mode/task. Async workers explicitly load that snapshot;
SynExecutionScope is only adapter-bound synchronous scope, always closed. Actor →
Agent → session → KB/sources → execution locking serializes authority transitions.
No provider response is awaited under a database admission lock.

Current owner/account/assignment/config, external provider consent, source publication,
context lineage, cancellation and deadline are checked at dispatch, commit and replay.
SSE also revalidates the specific browser JWT each poll; ordinary logout revokes that
credential without automatically revoking every other device. An admitted execution
may finish only under still-effective owner/consent/assignment authority; reading it
still requires a valid authorized credential.

Memory is an explicitly enabled bounded extractive summary, never objective evidence.
It uses recent owned narrative and a light self-declared major/goal/interests projection;
no email/display name/institution or raw private Core uploads. Original interaction refs
are flattened transitively. SQL triggers immediately invalidate dependent summaries,
increment context epochs and mark pending runs for cancellation on source deletion.
Runtime checks enforce invalidation before any cleanup/recompute. No invented TTL or
30/90-day retention; detailed backup/raw-content purge policy remains a real-data gate.

## Persistence and compatibility

V22 adds creation idempotency, `agent_execution_context_source`, `agent_execution_event`,
`insight_report`, optional AI-confirmation source columns on existing `exploration_plan`,
and history/epoch/artifact integrity triggers. V1–V21 remain byte-for-byte immutable.
Legacy manually confirmed plans retain null source linkage; no provenance fabrication.
Current source authorization gates past generated replies/reports. USER history remains
owner-readable after external consent loss. Core assessment/submission/completion is
independent of Agent availability; no Core write depends on provider success.

Both Nginx configurations disable buffering/cache for `/api/v2/agent-executions/` and
allow90-second read timeouts. Provider streams, tool execution and event journal are
bounded. API, UI and exact automated/manual verification are recorded separately in
[API contracts](../api/student-agent-p5-p6.md) and the consolidated completion report.
Live Google/provider quality and course delivery acceptance are not inferred from mocks.

<!-- preserved-content-end -->
