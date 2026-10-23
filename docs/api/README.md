<!-- documentation-sync-2026-10-22 -->
# Current API index

Base context is `/api`. Core/Auth/Agent contracts use `/api/v2`; legacy catalogue/assessment/Syn `/api/v1` routes remain under their compatibility/authorization flags. This index does not rename endpoints or infer completion from route existence.

- [P1/P2 authentication and Core contracts](../architecture/p1-p2-security-core.md): JWT HttpOnly/Secure/Lax cookie, default 900 seconds, CSRF writes, per-jti logout/current actor, explicit safe linking; no identity chosen by client user IDs.
- [US-01 / UC-01 registration contract and sequence](registration-uc01.md): registration feedback, cancellation, sign-in and assessment handoff.
- [US-01/02 Google identity setup and callback sequence](../testing/google-identity.md): shared Google action, safe outcomes, role restoration and external acceptance limits.
- [P3/P4 KB Studio / Builder / Preview / Assignment](agent-platform-p3-p4.md).
- [P5/P6 Student sessions / SSE / reports / plan confirmation / safe usage](student-agent-p5-p6.md).
- [EN/VI contract](site-localization.md).
- [Current UC/US](../requirements/README.md), [report/code discrepancies](../requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md).

Wrong-role denial and foreign private-resource masking have distinct 403/404 contracts. Model answers are owner/final/version/release gated. Exact-source plan confirmation is not an arbitrary plan editor; no autonomous write tool is implied. Published historical scenario versions retain their semantics.

## Historical v1 inventory

The previous envelope/inventory is retained below for legacy context. It is not a complete current endpoint list or evidence that identity is still pending.

<details>
<summary>Historical content preserved unchanged</summary>

<!-- preserved-content-start -->
# API conventions

Base path: `/api/v1`

Successful responses use:

```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "timestamp": "2026-09-10T00:00:00Z"
}
```

Errors use:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "One or more fields are invalid.",
  "path": "/api/v1/example",
  "fieldErrors": [
    { "field": "answer", "message": "Answer is required." }
  ],
  "timestamp": "2026-09-10T00:00:00Z"
}
```

Rules:

- Controllers adapt HTTP and delegate; they do not query DAOs or implement business rules.
- Request validation errors must be stable enough for frontend field mapping.
- IDs, timestamps, enum strings, pagination, and error codes must be documented before frontend integration.
- Evaluation rules, answer keys, provider credentials, internal prompts, and raw model traces are never returned by public DTOs.

Current endpoints:

| Method | Path | Authentication | Purpose |
|---|---|---|---|
| GET | `/api/health` | Public | Service health |
| GET | `/api/v1/simulations` | Public | Published catalog |
| GET | `/api/v1/simulations/{slug}` | Public | Published simulation summary |
| GET | `/api/v1/guidance/dashboard` | Authenticated student | Concise owned assessment, simulation, skill, progress, and plan summary |
| POST | `/api/v1/guidance/syn/messages` | Authenticated student | Structured Syn reply with visible `AI` or `STANDARD` provenance |
| DELETE | `/api/v1/guidance/syn/sessions/{sessionId}` | Authenticated student | Clear the current student's bounded Syn memory |
| POST | `/api/v1/guidance/plans` | Authenticated student | Confirm and save the student's current exploration plan |

## US-03 guidance contract

Syn message request:

```json
{
  "message": "Review my latest result",
  "actionType": "REVIEW_LATEST",
  "sessionId": "87829935-b74f-45a2-99c1-0b02bec264f1",
  "responseLanguage": "AUTO",
  "context": { "attemptId": 42 }
}
```

Supported actions are `FREE_TEXT`, `EXPLAIN_RIASEC`, `REVIEW_LATEST`,
`NEEDS_EVIDENCE`, `EXPLORE_NEXT`, `DRAFT_PLAN`, `COMPARE_PATHS`,
`LEARNING_RESOURCES`, `CAREER_QUESTION`, and `GET_STARTED`. Messages are limited to 600
characters. A requested attempt must be an evaluated attempt owned by the current
student. `sessionId` is optional on the first message; the returned ID should be
reused for contextual follow-up. A foreign or expired ID creates an empty owned
session instead of exposing whether another session exists. `responseLanguage` is
optional and accepts `AUTO`, `EN`, or `VI`; `AUTO` detects Vietnamese conservatively
and reuses the bounded session language for ambiguous follow-up messages.

The response includes `intent`, visible `activity`, bounded `evidenceReferences`,
and optional server-owned `comparisonCard`, `resourceCards`, `resultCard`, and
`planDraft`. It also reports the resolved `responseLanguage` and current
`explorationStage`. Activity is a concise operation summary, not hidden model
reasoning. Each suggested simulation card keeps its existing `title` and
`careerTrack` fields and may also include the reviewed catalog `summary`,
`difficulty`, and `estimatedMinutes` metadata.

Plan requests contain a title of at most 120 characters and one to five non-blank
steps of at most 240 characters each. Posting the request is the explicit save
confirmation; receiving a Syn plan draft does not write a plan.

The approved tools can read owned evidence, compare reviewed path profiles, identify
possible evidence gaps, retrieve published simulations, retrieve reviewed learning
resources, and draft an unsaved plan. They cannot browse the web, execute arbitrary
code, write scores, or directly query through the model. Quick actions return
reviewed `STANDARD` guidance without a provider call. For an eligible consented
Gemini `FREE_TEXT` request, the reliable default lets `SynAgentPolicy` select up to
four typed read tools, executes them on the backend, and makes one structured
generation request. Experimental native Gemini planning can be enabled by
`AI_GEMINI_TOOL_PLANNING_ENABLED=true`; that path uses at most one planning and one
synthesis call. Unknown, repeated, parameterized, or synthesis-pass tool calls fail
closed to the static policy and `STANDARD` response. The backend sends only bounded
session/evidence context and tool observations, applies a strict output schema and
HTTP timeout, and never gives the provider database or arbitrary URL access.
The stored memory is a versioned code-only summary of language, stage, goal, covered
topics, open follow-up, proposed next action, and completed exploration steps; raw
chat is not persisted. Missing configuration, missing
consent, unsafe input, timeout, provider rejection, unavailable tool, iteration-limit,
or malformed output returns
`STANDARD` guidance and records fallback provenance. Objective scores are never
changed, and raw answers, answer keys, evaluation rules, prompts, keys, and provider
errors are never returned.

The gateway limits Syn message requests to 10 requests per minute per client address
with a small burst and limits its request body to 16 KiB. Other API traffic has a
separate, higher limit. A gateway rejection may return HTTP `429` before Spring.


## Syn P1/P2 contract additions (2026-09-28)

The message endpoint accepts optional UUID `clientMessageId`. Clients should always provide it and reuse it for a retry of the exact same payload. Responses use the persisted turn UUID as `id` and include `reasonCode`. Changing content for a reused request ID, a pending session or a revoked source can return 409; per-student limits return 429. Ownership/status checks remain server-side.

- `GET /api/v1/guidance/syn/turns/{turnId}`: owner-only `{id,state,response}`; expired/cleared payloads are null.
- `GET /api/v1/guidance/syn/requests/{clientMessageId}`: recover a turn after a lost HTTP response; PENDING/FAILED/CANCELLED are distinct from COMPLETED.
- `POST /api/v1/guidance/syn/turns/{turnId}/cancel`: cancel an owned turn. Not a tool exposed to the LLM.
- `DELETE /api/v1/guidance/syn/sessions/{sessionId}`: idempotent owner-scoped clear; clears turn payloads and prevents late finalization.
- `GET /api/v1/guidance/knowledge/chunks/{id}`: authenticated active student may inspect a currently published source; revoked/inactive versions return 404.

A KNOWLEDGE evidence reference contains `label`, `sourceType`, `referenceId` (K + chunk ID), `version`, `section`, `url`, `excerpt`. Personal references use A + assessment attempt ID or S + simulation attempt ID from the authorized snapshot; `version` is null for completed personal attempts. Dashboard adds nullable `assessmentAttemptId`; existing labels and result cards remain compatible. Citation URLs come from reviewed metadata or the bounded source endpoint, never model text. Accepted responses are retained 24 hours, turn metadata and bounded sessions 30 days. Raw user dialogue is not stored as session memory; accepted response storage is a separate retry mechanism.

See [implementation and fallback map](../architecture/syn-core-rag-implementation.vi.md) and [verification evidence](../../.agent/reports/us-03-syn-core-vector-rag-verification.md). Voice endpoints are not implemented.
# Syn A/B/C additive response fields

Conversation-quality update: `webSearch.status` additionally distinguishes RATE_LIMITED and ACCESS_DENIED. Free text can return AI_READY with no K references when explaining clearly labeled general knowledge; this is not evidence of successful RAG. The frontend collapses comparison/score details and uses compact citation labels. Default generation allows one bounded validation repair; API request shape is unchanged.

Syn message responses optionally include `webSearch: {status, retrievedAt, sources, searchSuggestionsHtml}`. Each source contains `referenceId` (W1–W3, turn-local), `title`, `url`, `excerpt`. These are provider-grounded live sources, not reviewed catalog entries or published K citations. Status may be LIVE_WEB, DISABLED, NO_CONSENT, NEEDS_TOPIC, UNAVAILABLE, NO_SOURCES, INCOMPLETE or INVALID_METADATA. `retrievedAt` is the search time, not a freshness guarantee. Provider search HTML is rendered only in an isolated sandboxed iframe; never inject it into the app DOM.

Primary `intent` remains for compatibility, but `toolTrace` can reflect several composed capabilities. Quick actions remain deterministic. Provider failure can preserve catalog resources, simulations, plan drafts and already returned web sources with Standard provenance. Plan save authorization is unchanged.

<!-- preserved-content-end -->

</details>
