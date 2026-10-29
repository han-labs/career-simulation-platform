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
| GET `/admin/agents/{agent}/configurations/{config}/publication-readiness` | read-only persisted exact-hash Preview check, current Agent revision/editability, KB availability and publication eligibility; no provider call. Publish rechecks all guards under lock. |
| GET `/admin/agents/{agent}/eligible-students?q=&afterId=0&limit=20` | Admin-only active Student candidates with required Student profile; literal display-name search (0–80 characters), ID cursor ≥0, limit 1–50. `{items:[{id,displayName}],nextAfterId}` only; no consent/private evidence. Picker grants no authority. |
| GET/POST `/admin/agents/{agent}/assignments` | list includes minimal `studentDisplayName`, state and timestamps; assign `{studentUserId}` rechecks active Student/profile under lock; duplicate active assignment idempotent, no assign-all. Assignment history is Agent-scoped, without a separate config pin. |
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

## UC-07 completion — 2026-10-23

Additive Admin inspection behavior on the existing routes; no migration, parser or
embedding-profile change. UC-06/08 source passages and UC-09 aggregates do not gain
extraction text.

- Ingestion detail (`GET .../ingestions/{job}`, processing responses) retains one
  merged `attempts` entry per `generation`. `stage`, safe `errorCode` and
  `outcome: STAGED|UNCHANGED` describe the actual attempt. Late resource usage is
  merged into its own generation, preserving its immutable extraction.
- Successful parsing saves `extractionSnapshot` **before embedding** in existing
  attempt-history JSONB. Representation v1 contains `inputHash`, `parserProfile`,
  `textUnit: UTF-16`, ordered `blocks[{section,text,locator}]`, and `warnings`.
  Blocks are normalized parser units before chunking, not reconstructed chunks;
  there are no invented offsets/highlights. Existing extraction limits apply.
- Successful atomic staging copies that snapshot to
  `AssetVersion.provenance.extractionSnapshot` in version detail. Existing version
  metadata remains immutable. Job and asset **lists omit full snapshots**.
  Legacy attempts/versions without snapshots remain readable and explicitly lack
  extraction; no backfill/reconstruction is performed.
- Snapshot-storage failure stops before embedding. Parsing, source storage,
  snapshot storage, embedding and staging storage diagnostics are distinct.
  Failed/cancelled replacements preserve the prior publication and saved extraction.
- **Reindex A:** upload the source again through the existing multipart route.
  Same input/parser/embedding fingerprint with a valid stored index reuses its
  version and reports `UNCHANGED`; no new embedding, version or claimed rebuild.
  READY/PUBLISHED retry is a state read/check, not rebuilding. Retired or mismatched
  indexes cannot be reused or resurrected. No new reindex route is introduced.
- Metadata dialogs preserve their draft revision. Publication uses the revision
  captured when the exact version was inspected, even after refresh; conflict
  requires review again. Lifecycle actions remain explicit.
- Test KB offers **Retrieve passages** and **Answer with sources**, binding displayed
  results to the accepted request's saved input. Changing a draft does not relabel
  old results. Focus/reload/local lifecycle actions revalidate current execution
  authority; revoked sources are removed instead of displayed as current evidence.
  Retrieval-only still has no answer call; embedding accounting stays separate.

Implementation evidence and remaining browser/human gates:
[`UC-07-completion.vi.md`](../../.agent/reports/UC-07-completion.vi.md).

## Admin Studio final repair — 2026-10-26

This supplements the dated reupload/retry description above. Existing retry behavior
is retained; an explicit Reprocess operation now runs the pipeline from stored source.

`POST /api/v2/admin/knowledge-bases/{kb}/ingestions/{job}/reprocess`

- Active Admin and existing CSRF/authentication apply. Send the **job revision**
  in `If-Match` and an empty JSON body. Missing precondition: HTTP428; invalid
  revision: HTTP422; stale revision/running attempt: HTTP409. Existing `Job` response.
- Reuses stored source file, job and assetKey. Claim increments existing generation;
  extraction/chunking/embedding/staging reuse the existing pipeline and profiles.
  An audit event records only the prior revision.
- Parses again even for READY/PUBLISHED jobs. Matching content/profile and complete
  valid index returns attempt outcome UNCHANGED without another upload, embedding
  or version. A changed profile/result stages READY for explicit review/publication.
  It never changes the published pointer implicitly.
- Retired produced versions cannot be reprocessed. After failure clears the produced
  pointer, original-file version history is checked. If bounded history cannot
  establish safety, fail closed and require replacement upload. No DAO/schema change.
- Failure/cancel retains previous published content and attempt extraction/usage.
  Generation fencing rejects stale staging. Lost responses require reading job
  state before another attempt with a fresh revision; no automatic retry.
- `/retry` is unchanged, including terminal READY/PUBLISHED fast-return.

One Builder Save Changes validates draft, PATCHes changed identity, then POSTs the
configuration using the updated revision. Partial success is explicit; draft and
revision are retained for recovery. Preview may test a selected saved version while
a draft exists, but never submits the draft or previous Preview turns. At most ten
runs are held in React state for continuity, not conversation memory; reload retains
only the existing accepted-execution receipt. Citation chips join same-run reference
IDs and revalidate exact source version/text through the existing source API.

Publication/readiness/hash, assignment eligibility/revoke, Student sessions/SSE/
consent, tool grants and usage accounting contracts remain unchanged.


## Revised Agent Studio contract — 2026-10-26

Additive V29/V30 preserve all existing sealed v1 configurations and publication proofs.
`experienceConfig` is nullable; omitted/JSON null keeps the v1 renderer and hash algorithm.
New saves from Agent Studio use schemaVersion 2 with `name`, `description`, `audience`
(personal/business: response orientation only), `language` (en/vi/bilingual), `greeting`
(up to 1000), IANA `timezone`, `variables: {name}` (plain text, optional, up to 100), and
`chat: {includeHistory,maxTurns,showSourceExcerpts,responseStyle}`. Bounds: maxTurns 1–6,
responseStyle concise/balanced/detailed; existing Top K 1–10 and runtime/output caps remain.
`modelKey` remains canonical; provider/model choices are derived from model options.
Availability means server configuration only, never quota, billing status or live success.

`POST /api/v2/admin/agents/{agent}/configurations` plus `If-Match` validates then saves
mutable identity and immutable configuration in ONE transaction. No separate metadata PATCH
is needed. Save does not execute, publish or assign. Names/purposes are pinned in v2 configs.
Raw templates + variables participate in the configuration hash; rendered instructions
participate in promptHash. Only literal single-pass `{{name}}` is accepted in custom
instructions/greeting; unknown or nested templates are rejected. Platform prompt, schemas,
tool arguments and credentials are never templated. Blank greeting is deterministic.
DB timestamps stay UTC; timezone supplies response/date context, not a clock tool.

### Dedicated Admin Preview conversations

All routes below are `/api/v2/admin/agents/{agent}/preview-conversations` and require the
current ACTIVE Admin owner. These tables never reuse Student sessions or private evidence.

| Method / suffix | Contract |
|---|---|
| GET collection | `limit` 1–50 (default 20), `offset` 0–5000; items + nextOffset |
| POST collection | `{requestId,configurationId}`; pins saved config, no model call |
| GET `/{chat}` | `before` positive sequence, `limit` 1–100 (default 50); pinned greeting/config, current-epoch turns referencing executions, nextBefore |
| POST `/{chat}/messages` | `{requestId,query}`; accepted existing execution DTO, idempotent within exact chat/epoch/input; one active run per chat |
| POST `/{chat}/clear` | `If-Match`, `{confirmed:true}`; cancels active work, advances epoch, hides prior transcript/context |
| DELETE `/{chat}` | `If-Match`, `{confirmed:true}`; cancels, advances epoch, soft-deletes chat; normal read/resume denied |

Clear/Delete retain immutable executions, usage and exact-configuration publication evidence.
Workers fence against current chat epoch before dispatch and final commit. Resume performs
GET only. History is bounded complete successful pairs from this same chat, max 6 and 12KB;
it is untrusted narrative, never grounding evidence. History off retains the visible transcript.
Current source authorization still governs replay and citations. Revoked turns cannot display
cached source text as current evidence. Stop uses the exact accepted execution's cancel route.

Native Agent transport now uses effective remaining execution time, a 2-second safety margin,
a maximum 60-second transport cap and independent 5-second connect cap. Legacy Syn bounded
call caps remain unchanged. No automatic provider fallback or retry is added.

### Knowledge Base list counts

`GET /api/v2/admin/knowledge-bases` returns flat BaseSummary plus `documentCount` and
`publishedChunkCount`, using one aggregate query over at most 200 bases. Documents follow
logical active-document grouping, including processing attempts without duplicating replacements.
Chunks belong only to current active PUBLISHED versions with non-null embeddings.
The UI reports unavailable counts honestly when an older server omits these fields.

## UC-08 role publication correction — 2026-10-26

This explicit Team Lead correction supersedes the per-Student assignment UX and the
fixed-name variable limit described above. Existing historical assignment records remain;
they no longer authorize discovery, new sessions or execution. HTTP assignment/revoke
writes return 409 after the Admin check. Historical reads are compatibility endpoints only.

### Role availability (V31)

| Endpoint | Contract |
|---|---|
| `GET /api/v2/admin/agent-role-bindings` | Admin only; two slots: `role, agentId, agentName, revision, updatedAt` |
| `PUT /api/v2/admin/agents/{agent}/roles` | Admin only; Agent `If-Match`; `{roles:[STUDENT,ENTERPRISE], expectedRoleRevisions:{STUDENT:n,ENTERPRISE:n}, confirmed:true}` |
| `GET /api/v2/agents` | Current ACTIVE Student/Enterprise role resolves zero or one published Agent; `id,name,purpose,runtimeAvailable` |

Role slots are unique and locked in deterministic order. A stale snapshot fails atomically;
selecting both roles replaces both bindings in one transaction. Unselected roles are removed
only when currently bound to this Agent. Activation requires ACTIVE Agent, current published
sealed configuration, available KB grants and successful exact-configuration Preview proof.
Archive, inactive lifecycle or unavailable publication/KB clears its bindings. No inferred
backfill from per-user assignments occurs; after V31 an Admin must explicitly activate roles.

Bindings point to Agent identity. New Student chats pin its current published configuration;
same-Agent re-publication preserves existing valid immutable pins and the role binding.
Replacing the role with another Agent denies old-Agent runtime access. Session ownership,
consent and Student-private tool checks remain server-side. Enterprise discovery returns
`runtimeAvailable:false`: Enterprise cannot use Student sessions/private evidence. No Syn
runtime or popup is restored. Save, Preview, Publish and role activation remain distinct.

### Dynamic variables and diagnostics

`experienceConfig.variables` now accepts up to 20 `{key,value}` entries. New UI saves an
array; existing v2 `{name: "..."}` objects remain valid without rewriting sealed records.
Keys are unique ASCII identifiers, non-digit start, 1–40 characters; reserved/credential
names are rejected. Values are optional plain text up to 500 UTF-16 characters, without
control characters, credentials or nested placeholders. Only instructions/greeting accept
defined `{{key}}` references. Unknown/malformed expressions fail Save; substitution is
literal and single-pass. Raw templates/variables remain part of the immutable config hash;
old v1/v2 sealed hashes and publication proofs are not recalculated.

Admin options include safe `providerState` booleans/selected-provider enum and
`embeddingAvailable`. Native generation and embedding have separate clients/enable flags,
although Gemini credential/base URL property paths are shared. Failures retain only safe
`PROVIDER_DIAGNOSTIC` metadata: configured booleans, outbound-attempt flag, numeric HTTP
status/class or network class. No raw bodies, URLs, keys, prompts or request headers are
recorded. Native generation distinguishes not-configured, disabled/mismatched model,
authentication rejection, model unavailable, rate limit, HTTP unavailability and transport
failure/deadline; no provider fallback or automatic retry is added.

`GET /api/v2/auth/providers` also returns `localWorkspace` using the existing local-demo
profile predicate. Local workspace shows an explanatory indicator instead of misleading
Sign out. Real-auth sign-out and JWT/session revocation semantics are unchanged.

## Bounded same-provider model failover — 2026-10-27

This increment supersedes the earlier "no automatic retry/fallback" statement.
Configuration v2 optionally accepts `experienceConfig.fallbackModelKeys`, an ordered
array of at most3 unique allowlisted same-provider keys, distinct from `modelKey`.
The existing immutable hash includes this field; absent legacy fields/hashes remain
unchanged. Save/Preview/Publish/role activation remain separate operations. No DDL.

Admin model options now include the supported stable Gemini3.6/3.5-lite/3.8 and
legacy3.1-lite identifiers plus the explicitly configured legacy model. `available`
continues to mean server configuration, not a successful live inference or quota.

Existing execution `toolStepsAndSources` may include safe `MODEL_ATTEMPT` rows:
`requestedModel,effectiveModel,attempt,fallbackFrom,reason,status,elapsedMs`.
Usage dashboard executions add nullable `effectiveModel` and bounded/allowlisted
`fallbackReasons`; aggregate request/outcome/cost/latency definitions do not change.
HTTP401 authentication,402 billing and403 permission are distinct safe categories;
these and400 do not trigger fallback. Safety refusal is never bypassed.

See [measured decision and retry bounds](../architecture/agent-model-reliability.md).

### Owner UAT quality correction

New Agent defaults are4rounds/4tool calls/60s and GENERAL_ALLOWED. Existing saved
limits/policies are not overwritten; independent KB Test remains KB_ONLY. Admin
Save creates a new configuration using platform prompt v2. Known v1 references
remain valid for reading/executing existing pinned versions; old resource bytes,
hashes and publication proofs are retained. Old known references are accepted on
new Save, which explicitly creates a new current-template immutable version.

Stream body and headers share one absolute per-attempt deadline. Eligible timeout
switches to the next configured model without repeating the timed-out endpoint.
While another candidate remains, an attempt receives at most12s/40% of remaining
execution time; final fallback at most18s. Existing single-model budget is retained.

Local output rejection remains MALFORMED_OUTPUT and is distinct from provider
unavailability. Safe `OUTPUT_VALIDATION` audit may include only the enum-like
`validationCheck`: OUTPUT_SCHEMA_OR_LIMIT, DUPLICATE_CITATION,
CITATION_NOT_DELIVERED or CITATION_QUOTE_MISMATCH. No rejected provider response
body is persisted/exposed. A two-round config may have no remaining repair round;
the runtime never silently exceeds the saved limit.

### Bounded conclusion correction — 2026-10-29

Gemini requests that conclude a run with prior native observations explicitly use
`toolConfig.functionCallingConfig.mode=NONE`, without new declarations, and request
a final JSON answer or honest insufficient-evidence response. Existing function
signatures/responses, source validation, authorization and saved round/call/time
limits remain intact. Tool-enabled rounds retain native model choice. OpenAI
transport and pinned prompt files are unchanged; no extra retry is added.

Rounds are a per-execution budget, not a conversation-message limit. Every new
execution gets fresh counters. Admin Preview conversations retain their exact saved
configuration: editing the form does not change an existing conversation. Save the
new configuration and start a new Preview conversation to test the revised limits.
The frontend distinguishes execution-limit failures from generic provider failures.
