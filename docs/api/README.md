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
| POST | `/api/v1/guidance/plans` | Authenticated student | Confirm and save the student's current exploration plan |

## US-03 guidance contract

Syn message request:

```json
{
  "message": "Review my latest result",
  "actionType": "REVIEW_LATEST",
  "context": { "attemptId": 42 }
}
```

Supported actions are `FREE_TEXT`, `EXPLAIN_RIASEC`, `REVIEW_LATEST`,
`NEEDS_EVIDENCE`, `EXPLORE_NEXT`, and `DRAFT_PLAN`. Messages are limited to 600
characters. A requested attempt must be an evaluated attempt owned by the current
student.

Plan requests contain a title of at most 120 characters and one to five non-blank
steps of at most 240 characters each. Posting the request is the explicit save
confirmation; receiving a Syn plan draft does not write a plan.

Quick actions return reviewed `STANDARD` guidance without a provider call. An eligible
`FREE_TEXT` request may return `AI` when the backend provider is enabled and the active
student has recorded AI consent. The backend sends only a bounded evidence summary,
uses a strict structured-output schema, disables provider-side response storage,
limits output tokens, and applies an HTTP timeout. Missing configuration, missing
consent, unsafe input, timeout, provider rejection, or malformed output returns
`STANDARD` guidance and records fallback provenance. Objective scores are never
changed, and raw answers, answer keys, evaluation rules, prompts, keys, and provider
errors are never returned.

The gateway limits Syn message requests to 10 requests per minute per client address
with a small burst and limits its request body to 16 KiB. Other API traffic has a
separate, higher limit. A gateway rejection may return HTTP `429` before Spring.
