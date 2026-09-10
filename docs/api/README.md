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
