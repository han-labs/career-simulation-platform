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
reasoning.

Plan requests contain a title of at most 120 characters and one to five non-blank
steps of at most 240 characters each. Posting the request is the explicit save
confirmation; receiving a Syn plan draft does not write a plan.

The deterministic router can read owned evidence, compare reviewed path profiles,
identify possible evidence gaps, retrieve published simulations, retrieve reviewed
learning resources, and draft a plan. It cannot browse the web, execute arbitrary
code, write scores, or directly query through the model. Quick actions return
reviewed `STANDARD` guidance without a provider call. An eligible
`FREE_TEXT` request may return `AI` when the backend provider is enabled and the active
student has recorded AI consent. The backend sends only a bounded evidence summary,
structured session summary, and deterministic tool observations,
uses a strict structured-output schema, disables provider-side response storage,
adapts the bounded output budget to the request depth, and applies an HTTP timeout.
The stored memory is a versioned code-only summary of language, stage, goal, covered
topics, open follow-up, proposed next action, and completed exploration steps; raw
chat is not persisted. Missing configuration, missing
consent, unsafe input, timeout, provider rejection, or malformed output returns
`STANDARD` guidance and records fallback provenance. Objective scores are never
changed, and raw answers, answer keys, evaluation rules, prompts, keys, and provider
errors are never returned.

The gateway limits Syn message requests to 10 requests per minute per client address
with a small burst and limits its request body to 16 KiB. Other API traffic has a
separate, higher limit. A gateway rejection may return HTTP `429` before Spring.
