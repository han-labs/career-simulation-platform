# Assessment API — current US-03 / UC-03

2026-10-23. Additive alignment to the existing P1/P2 assessment implementation.
Base `/api/v2`; assessment routes require `CORE_V2_ENABLED=true`. Browser routes
choose legacy/Core through `VITE_CORE_V2_ENABLED`; neither default is changed.
The shared authenticated HTTP/CSRF transport and role rules remain unchanged.

| Method/path | Actor / input | Result |
|---|---|---|
| GET `/assessment-types` | Public | Active types with a published active version; no assumed seed content |
| GET `/assessment-attempts` | Active Student, no owner argument | New: latest 100 own attempt summaries, ordered by startedAt DESC/id DESC |
| POST `/assessment-versions/{id}/attempts` | Student; `{requestId: UUID}` | Idempotent start, pinned content version |
| GET `/assessment-attempts/{id}` | Owner Student | Pinned questions/responses, lifecycle status and revision |
| PUT `/assessment-attempts/{id}/responses` | Owner, If-Match; `{items}` | Valid partial draft and incremented revision |
| POST `/assessment-attempts/{id}/submit` | Owner, If-Match; `{items}` | Atomic persisted deterministic result |
| GET `/assessment-attempts/{id}/result` | Owner Student | Existing stored result, no rescoring |

Attempt summary: `{id, assessmentTypeCode, assessmentVersionId, status, startedAt,
submittedAt}`. IDs are decimal strings; submittedAt is null for drafts. No private
answers are included in the list. Foreign owner filters are not accepted as identity.
The list includes drafts and submitted attempts so it does not duplicate the existing
guidance result projection. It does not read guidance, simulation, or AI data.

Responses remain `{questionId, answerData:{schemaVersion:1,value:integer 1..5}}`.
Unknown/duplicate/wrong-version questions, extra fields, strings/fractions/out-of-range
answers and missing required final responses are rejected. No evaluator semantics
or scoring configuration changed. RIASEC dimensions and profile categories retain
their own server-provided raw/min/max and methodology. No composite score is created.

New writes require a PUBLISHED pinned version and an ACTIVE type, rechecked under
the type lock. Publishing a newer version does not invalidate a previously published
pinned version. Archived-type attempts remain readable; an identical retry of an
already submitted payload returns the original result. A new/unfinished write on an
unavailable type returns 404. Historical SQL/results and V1–V22 are untouched.

Errors: 401 absent authentication, 403 wrong role/inactive actor/invalid CSRF,
404 foreign attempt or unavailable content, 409 stale revision/different final payload,
422 invalid schema/required responses, 428 missing If-Match, 500 failed persistence.
Transactions retain rollback guarantees; the UI only presents a result after a
successful stored-result response. Network failures retain answers and allow retry.

Browser drafts use authenticated owner + attempt ID and carry the server revision.
A different revision requires explicit choice of server answers or local answers
before writing. Completed attempts ignore later local drafts. Retake rotates the
start request ID and uses the currently selected published type. Existing unowned
and legacy drafts are not migrated. The old v1 quick-demo profiles still use normal
validation; v2 demo links explain why those profiles are not mapped to arbitrary
published question sets. No content is automatically reviewed or published.

Trace/tests and runtime limitations: [UC-03 verification](../../.agent/reports/us-03-career-quiz-alignment.vi.md).
