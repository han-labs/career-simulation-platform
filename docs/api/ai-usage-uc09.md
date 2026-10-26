# UC-09 / selected US-10 — Admin AI Usage

Additive dashboard projection of existing Agent Platform executions. No schema,
producer, provider pricing or execution contract change. Existing
`GET /api/v2/admin/agent-usage?from=...&to=...&mode=...` remains compatible.

## Dashboard

`GET /api/v2/admin/agent-usage?view=dashboard&from=...&to=...`

Active ADMIN required via the existing actor/security boundary. The existing
loopback-only local-profile principal behavior is preserved by reusing the
exact usage path; production JWT/role enforcement is unchanged. Standard
ApiResponse envelope, validation422, unauthenticated401, wrong-role403.

| Query | Contract |
|---|---|
| from / to | Required ISO Instant, inclusive/exclusive. Positive elapsed range <=366 days; not a retention policy. |
| agent | Optional UUID, linked configuration's Agent ID; archived history included. |
| provider | Optional persisted model-key prefix, max40 identifier characters. |
| model | Optional exact persisted model_key, max180 identifier characters. |
| outcome | ACCEPTED, RUNNING, SUCCEEDED, FAILED, REFUSED, CANCELLED. |
| mode | Optional STUDENT_CHAT, ADMIN_PREVIEW, KB_TEST. |
| limit | Default20, integer1..50. |
| cursor | Opaque base64url timestamp/UUID position, max256 characters. Use only the returned nextCursor with unchanged filters/from/to. |

Response data: `from,to,bucketUnit,summary,trend,options,executions,nextCursor`.
One read-only REPEATABLE_READ transaction gives a consistent summary/trend/page
within a response. Ordering is started_at DESC,id DESC; keyset paging fetches
limit+1. No offset or unbounded client-side execution-history download. Later
responses can reflect completion of running executions; this is live monitoring,
not an exported immutable report.

## Metrics and coverage

- `summary.requests`: persisted agent_execution count, including admitted
  no-model executions; not provider HTTP attempts, ingestion jobs or reloads.
  STUDENT_CHAT, ADMIN_PREVIEW, KB_TEST only; legacy Syn is excluded.
- `succeeded,failed,refused,cancelled,pending`: original status counts; pending
  combines ACCEPTED/RUNNING for display only. Provider/transport failure remains
  FAILED; evidence/policy refusal remains REFUSED. No new production state.
- `recordedEstimatedCostUsd`: SUM estimated_cost for ESTIMATED/PARTIAL only.
  Null if no recorded amount; UNKNOWN and NOT_APPLICABLE never become zero.
  Coverage: estimatedCostExecutions, partialCostExecutions, unknownCostExecutions,
  notApplicableCostExecutions. Partial is a known subtotal, not a complete total.
  Existing versioned SynUsagePricing estimates are reused unchanged; not billed
  cost, no retroactive provider pricing. Embedding resource costs remain separate
  and excluded from the generation subtotal.
- `averageLatencyMs`: AVG persisted latency_ms for finished executions, excluding
  null samples, not avg-of-group-averages. latencySamples counts measured finished
  executions; missingLatencyExecutions counts finished rows missing latency.
  In-flight latency does not enter the average. UI uses milliseconds throughout.
- `trend`: actual nonempty UTC buckets with requests and the same outcome counts.
  <=48h uses hours, otherwise days. Explicit UTC SQL avoids server timezone shifts.
  The UI labels sparse buckets and provides exact numeric values; no fake activity.
- `options`: distinct Agent names/IDs and provider/model choices in the requested
  time window, independent of other filters, max100 each; truncated is explicit.

## Safe recent execution projection

Allowed fields: execution ID/timestamps/mode; optional Agent ID/name, configuration
ID/version, persisted promptHash fingerprint; selected provider/model; outcome,
allowlisted failureCategory; nullable latency/tokens/cost and costStatus;
nativeToolCount/tools/serverRetrievalCount/modelAttempts.

Model identifies the selected persisted model_key, not proof of dispatch. Actual
generation attempts come from the existing generation.calls array; missing legacy
telemetry remains null. Native attempts count ACTUAL_NATIVE_CALL, including denied
attempts; only kb.search, assessment.lookup, simulation.explore,
simulation.myEvidence names are returned (others UNKNOWN). SERVER_RETRIEVAL is
separate. Tool arguments/results and raw trace JSON are never returned. Error
categories use a finite allowlist; unknown codes become OTHER, not raw messages.

Prompt fingerprint + immutable configuration version provides audit identity;
prompt template text/internal references are not returned. Left joins tolerate
unlinked historical metadata; current FK RESTRICT guards prohibit destructive
deletion, and archived Agents/configurations remain inspectable as metadata.
KB_TEST without configuration has no fake Agent identity.

No Student IDs/emails/names, prompts, transcripts, input/output snapshots, reports,
private passages/evidence, assessment answers, raw provider requests/responses,
tool arguments, pricing snapshot JSON, reasoning or credentials are in the DTO.
Do not substitute the private Agent execution-read DTO for this projection.

## Frontend

Canonical route `/admin/usage`; existing `/admin/ai-usage` redirect retained.
Admin Studio sidebar is unchanged. Presets24h/7d/30d/366d are elapsed UTC windows;
Apply captures fixed instants, resets pagination, and makes a single bounded
dashboard request. Pending filter edits do not relabel a submitted result.
Next/Previous reuse the same interval; Retry explicitly reloads. Late/unmounted
or previous-account responses are fenced. Empty, unavailable and in-flight states
are distinct. EN/VI and shared Button/FormField/StatusBadge/FeedbackPanel reused.

Verification: AgentUsageDashboardTest (isolated PostgreSQL),
frontend usageDashboard.test.jsx, existing Student/KB/Builder/navigation regressions.
See the dated UC-09 evidence report for actual commands/UAT and limitations.

### Presentation repair — 2026-10-26

Order: header/applied scope, four compact KPIs, trend/outcomes, filters, recent
executions and pagination. Filters still apply to the **entire dashboard** through
the same server request. Desktop uses eight columns; mobile uses stacked records.
Safe audit metadata/tool names remain in closed Details disclosures.

Cost display uses the existing status and persisted amount only:

| State | Display |
|---|---|
| ESTIMATED, positive valid amount | Persisted USD value · Estimated |
| PARTIAL, valid amount | Persisted subtotal · Partial estimate |
| UNKNOWN, invalid/missing amount | Not available, never zero |
| NOT_APPLICABLE | N/A |
| Known zero estimate | $0.00 · Recorded estimate (PARTIAL retains its partial label) |

Tiny positive amounts retain precision instead of rounding to a fake zero. No
Free tier/billing inference, pricing table, recomputation or aggregation change.
The total KPI still uses recordedEstimatedCostUsd and separate coverage counts.
