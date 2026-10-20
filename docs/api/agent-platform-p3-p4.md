# P3/P4 integration contracts — Agent Platform Foundation

Implementation contract, 2026-10-20, based on approved Target v1.2/Astra P3/P4.
Implementation/test status is recorded in the final report, not implied by this file.

Base `/api/v2`. Existing `ApiResponse.data` and ErrorResponse apply. UUID platform
IDs and decimal-string Core/version/chunk/file IDs; UTC times; mutable edits use
quoted `If-Match: "revision"`. JWT HttpOnly cookie and unsafe-request CSRF via shared
httpClient. Anonymous401, wrong role/locked403, private foreign objects404,
malformed JSON/type400, domain validation422, missing If-Match428, stale revision/replay-conflict409. Flag
`AGENT_PLATFORM_ENABLED`/`VITE_AGENT_PLATFORM_ENABLED` defaults false; no fallback
to legacy/global Syn when new platform permission fails.

## KB Studio (active Admin only)

| Method/path | Request / response |
|---|---|
| GET/POST `/admin/knowledge-bases` | list; create `{name}` → KB `{id,name,scopeType,visibility,status,publicationRevision,revision,...}`; only PLATFORM/INTERNAL creation. |
| GET/PATCH/DELETE `/admin/knowledge-bases/{kb}` | detail; patch `{name,status:ACTIVE|HIDDEN|ARCHIVED}` + If-Match; delete archives, never removes versions/vectors/history. |
| GET `/admin/knowledge-bases/{kb}/assets` | stable asset key + version records, publication status, provenance/parser/profile/source-file metadata. |
| GET `/admin/knowledge-bases/{kb}/ingestions` | last 100 durable jobs for reload/resume, including failed and queued attempts. |
| POST `/admin/knowledge-bases/{kb}/assets` | multipart `file` and JSON `metadata`: `{assetKey,title,license,sourceUri?,language:EN|VI,track,reviewed:true,requestId}`. Genuine explicit review confirmation required; reviewer derived from current Admin. Returns durable ingestion job. No JSON Content-Type on whole multipart body. |
| GET `/admin/knowledge-bases/{kb}/ingestions/{job}` | state QUEUED/PARSING/EMBEDDING/READY/FAILED/PUBLISHED/CANCELLED, producedVersionId, safe errorCode, actual attempt/resource usage and timing. |
| POST `/admin/knowledge-bases/{kb}/ingestions/{job}/retry` | starts queued/failed ingestion, retries idempotently; bounded parse/embed outside DB transaction; returns resulting job. A READY/PUBLISHED retry does not duplicate indexing. |
| POST `/admin/knowledge-bases/{kb}/ingestions/{job}/cancel` | prevents late staging/publication; does not delete source history. |
| GET `/admin/knowledge-bases/{kb}/assets/{key}/versions/{version}` | exact version plus immutable chunks with `sourceLocator`, text, profile, hash and review provenance. |
| POST `/admin/knowledge-bases/{kb}/assets/{key}/versions/{version}/publish` | `{confirmed:true}`; atomic publication, current source pointer + KB publicationRevision; parent revision via If-Match. |
| POST `/admin/knowledge-bases/{kb}/assets/{key}/versions/{version}/retire` | explicit access revocation, not destructive deletion. |
| POST `/admin/knowledge-bases/{kb}/tests` | `{requestId,query,retrievalOnly,topK,modelKey?,configurationId?}` → persisted KB_TEST execution and actual ranked evidence; optional configuration must grant this KB. No fake config/session/Student. |
| GET `/admin/knowledge-bases/{kb}/sources/{chunk}` | exact currently authorized active published passage/source/version/locator; hide/revoke takes effect immediately. |

Upload formats: UTF-8 TXT/MD, text PDF, DOCX, XLSX; exact MIME/signature/extension
checks. No scanned-PDF OCR, legacy DOC/XLS, encrypted files, macro archives,
external relations or evaluated spreadsheet formulas. Limits/parser profile are
server-defined and returned with metadata. Source URI is provenance only, never
fetched. Old unscoped V13 sources remain in legacy Syn; no automatic adoption.

Jobs are staged; UI calls retry to ingest then inspects before explicit publication.
Version paths use the decimal **version ID**, while `versionNumber` is the display ordinal.
Ingestion failure preserves existing active version. Retry uses request/job identity,
not an automatic new upload. Browser should preserve metadata/file selection after
recoverable failures and use server state after a lost response.

## Agent Builder (active Admin only)

| Method/path | Request / response |
|---|---|
| GET `/admin/agent-platform/options` | server model/tool allowlist, supported policy/schema/profile limits and prompt template reference; never credentials. |
| GET/POST `/admin/agents` | list; create `{name,purpose}` → Agent `{id,name,purpose,status,revision,publishedConfigurationId}`. |
| GET/PATCH/DELETE `/admin/agents/{agent}` | metadata/edit/archive + If-Match; no physical history deletion. |
| GET/POST `/admin/agents/{agent}/configurations` | list; save `{modelKey,promptTemplate,promptTemplateRef,toolPolicy,memoryPolicy,outputPolicy,knowledgeBaseIds}` + If-Match → immutable SAVED version `{id,versionNumber,contentHash,promptHash,...}`. Empty grants/tools allowed explicitly, no implicit defaults. |
| GET `/admin/agents/{agent}/configurations/{config}` | exact saved content and grant snapshot for Admin. Any edit creates a new version. |
| POST `/admin/agents/{agent}/configurations/{config}/previews` | `{requestId,query}` → actual ADMIN_PREVIEW execution pinned to this saved hash, no session/private Student data. Save itself makes no provider call. |
| POST `/admin/agents/{agent}/configurations/{config}/publish` | `{confirmed:true}` + If-Match; requires successful validated ADMIN_PREVIEW of exact contentHash, atomically sets published pointer/ACTIVE. An older preview cannot qualify a new saved version. |
| GET/POST `/admin/agents/{agent}/assignments` | list; assign `{studentUserId}` explicit active Student only; duplicate active assignment idempotent, no assign-all. |
| POST `/admin/agents/{agent}/assignments/{assignment}/revoke` | idempotent revoke; current assignment checked before later use. |
| GET `/admin/agent-executions/{execution}` | initiating Admin only; exact persisted test result, sources, actual tools, usage/timing; no generic Student-content dump. Reload makes no new model call. |
| POST `/admin/agent-executions/{execution}/cancel` | initiating Admin only, idempotent; late provider result cannot succeed. |

Initial policy shape (all fields validated, unknown keys rejected):

```json
{
  "toolPolicy":{"enabled":["kb.search"],"maxRounds":2,"maxToolCalls":8,"topK":5,"timeoutSeconds":30},
  "memoryPolicy":{"enabled":false},
  "outputPolicy":{"schemaVersion":1,"grounding":"KB_ONLY","maxCharacters":4000}
}
```

P3/P4 supports `kb.search` in preview. Public domain-tool preview capabilities are
advertised only when implemented; Student private evidence tools/history/SSE are P5.
Runtime will reject unknown, disabled or mode-ineligible tools instead of claiming
they ran. A granted KB does not enable its search tool automatically.

Validated output: `{schemaVersion:1,outcome:ANSWER|INSUFFICIENT_EVIDENCE|REFUSED,
text,citations:[{referenceId,quote}],suggestions:[]}`. Exact quotes must occur in
current-run delivered passages; no fabricated references. Retrieval-only output
is explicitly diagnostic, not an LLM answer. Provider unavailable/invalid JSON/
timeout produce a truthful failed/refused execution with safe fallback text.

Execution includes id, mode/taskType, initiator, nullable config/KB/session/input
interaction IDs, minimized input snapshot, status/error, actual tools/sources,
validated output, model/prompt/config hash, nullable reported input/output tokens,
latency and estimated cost/status. Missing usage/pricing remains null/UNKNOWN;
retrieval-only generation tokens/cost null/NOT_APPLICABLE, embedding resource
usage separately labeled. Cost estimates use existing server-pinned Syn pricing,
never client rates or fabricated billing.

Test/preview POST commits acceptance before external work and returns **202** with
the execution ID. Poll GET `/admin/agent-executions/{id}` until terminal; do not
resubmit to reload. Cancellation returns `cancellationRequested=true`; the bounded
worker persists CANCELLED after an in-flight call returns, preserving available
actual usage without allowing late success. Its response uses the same current-source
authorization as GET; hidden/retired sources cannot be replayed through cancel.
Queue time is included in the deadline.
Unexpected process interruption never automatically replays a provider request.

## Student assignment visibility (P4 only)

`GET /agents` returns active assigned Agent `{id,name,purpose}` only for the current
active Student. No prompt, tool policy, config/KB grant or Admin preview content.
An archived/inactive/unassigned Agent is absent. P5 endpoints for session creation,
chat, history and SSE are deliberately not part of this implementation package.

## Integration checklist for teammates

- Use shared httpClient/coreApi command helpers, including multipart CSRF.
- Save before preview; show exact config/version/hash and saved-vs-local state.
- Preview/test reload reads execution; never resend a paid request on refresh.
- Preserve owner-scoped drafts and clear in-flight state on account generation.
- Keep ranked passages visible if answer generation fails; show real unknown usage.
- Treat hidden/retired sources as unavailable; do not cache private source text
  across account changes or use an old trace to bypass current source authority.
- KB Studio and Builder own new Admin folders; coordinated navigation additions
  preserve Core, legacy Syn, EN/VI and existing member-owned features.

## Bounded operation and deployment notes

Parser profile `careersim-source-v1:pdfbox3.0.8:poi5.5.1:units2400` records real
line/page/paragraph/table-row/sheet-row units, split into numbered parts of at
most 2400 UTF-16 characters without cutting surrogate pairs. Limits: raw8MiB,
text100000 characters,80 chunks,PDF100 pages,Office5000 ZIP entries/16MiB expanded/
8MiB per entry/100x expansion after1MiB,30 sheets/10000 row index/20000 cells.
DTD/entities,external relationships,formula andmacro/embedded binaries rejected.
Ingestion budget90s; stale attempt lease180s, generation fencing prevents late
publication. Synchronous retry is bounded; both Nginx layers allow11MiB and110s
for KB paths. Runtime has2 workers +16 queued, preview5–60s,KB Test30s, at most
4 model rounds including final synthesis and8 tool calls. Model HTTP max12s per call
within the shared deadline; context64k request bytes, final JSON16k bytes.

Enabled providers/models come from current server Syn configuration; saving an
unavailable allowlisted model is possible, preview then fails honestly. With
providers disabled, ingestion has no fabricated embeddings and reports failure;
existing compatible indexed sources can still use lexical retrieval-only tests.
No Student personal evidence/consent grant is fabricated for Admin test modes.
Raw-file/audit retention duration remains a release decision under D5. A changed
source-controlled prompt template hash fails closed for old configurations unless
that pinned template is retained by a later release.

Parser dependency sources checked during implementation:
[Apache PDFBox 3.0.8](https://pdfbox.apache.org/download.html),
[Apache POI 5.5.1](https://poi.apache.org/download.cgi),
[POI security guidance](https://poi.apache.org/security.html).
