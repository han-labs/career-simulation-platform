# Enterprise participation analytics

The current handoff calls this UC-10 View analytics. The repository catalogue
calls it [UC-13 View analytics](../requirements/use-cases/UC-13-view-analytics.md).

Open `/enterprise/analytics` directly or choose **View analytics** from the
enterprise workspace. The dashboard resolves the signed-in account's enterprise.
Both reads require an active representative of the target enterprise. Publishing
permission is independent.

| Core V2 endpoint | Response |
| --- | --- |
| `GET /api/v2/enterprises/{id}/analytics` | Owned published program options, enrollment counts, detail state, and per-version task counts |
| `GET /api/v2/enterprises/{id}/analytics/evidence` | Consented completion reference, program title, version and completion timestamp, plus a cursor when more data is available |

Both endpoints accept optional `scenarioId`, `from`, `to`, and `status`. Dates
are inclusive registration dates in `Asia/Ho_Chi_Minh`. Valid statuses are
`ENROLLED`, `IN_PROGRESS`, and `COMPLETED`. Evidence additionally accepts
`before` and `limit` (default 20, maximum 50).

- Published versions contribute independently. Counts use enrollments, with
  `started_at` for starts and `COMPLETED` for completions. The rate is completed
  enrollments divided by selected enrollments.
- Zero enrollments return `EMPTY`. A selected segment with fewer than five
  distinct learners returns `INSUFFICIENT`, keeping totals and withholding the
  rate and task detail. The initial threshold comes from the handoff proposal.
- Task buckets are no submission, draft, and final submission. Final submission
  is distinct from self-review and overall completion.
- Evidence is loaded on demand. The database checks the active learner and
  effective `COMPLETION_SHARE` for the owning enterprise on every page read.
  An empty result is identical for absent, expired, or revoked sharing. The
  legacy shared completion read also scopes its result to the receiving enterprise.

The legacy participation-summary response and scenario authoring contracts remain
available. This change uses the existing schema.

For UAT, use an authorized Enterprise account and a synthetic programme. A six
learner selection with two not started, two in progress and two complete should
show 6 enrollments, 4 started, 2 completed and 33.3%. Check filtering across two
published versions, a future date with no activity, and current consent after
refreshing evidence. Use a second Enterprise to check data isolation and a
Student to check route/API access.
