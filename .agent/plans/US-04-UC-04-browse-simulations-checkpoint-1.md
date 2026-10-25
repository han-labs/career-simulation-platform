# US-04 / UC-04 Browse simulations — Checkpoint 1

## Final verification checkpoint — 2026-10-25

PO reported UAT 14 PASS / 1 PARTIAL / 1 BLOCKED / 0 FAIL; dropdown TC-04-04A passed eight checks. Codex final focused checks: frontend 19 PASS with lint/build; backend 14 PASS with isolated PostgreSQL cursor/public API checks. No application/SQL/runtime/Git changes authorized by this closure were made. Main integration, content redistribution, V27 published-content hash integrity and final commit require review. See the current report and relocated `docs/evidence/US-04/us-04-browse-simulations.md`; earlier approval/status entries below are checkpoint history. Survey absence is a UC-05 review item, not implementation scope.

## PO-approved UAT correction — 2026-10-25

Replace the exact-match career-track text field with a native labelled select (selected US-04 search/filter AC, UC-04 flow 3a). Reuse the public unpaged catalogue API, following its numeric cursor in batches of 100 to collect unique published IT tracks independently of search filters. Preserve URL values, reset pagination on submit/clear, and expose loading/error/retry. Files: existing CoreCatalogPage, coreMessages, index.css, coreBrowse tests and existing evidence/AI log. No backend, API contract, database or runtime change. Verify focused frontend tests, scoped lint/build and diff hygiene; PO separately approves container refresh and performs browser UAT. No AI runtime or private data involved.

Status: CP1 conditionally accepted; CP2 implementation approved  
Owner: Person 2 / this chat; reviewer: Manager  
Date: 2026-10-24

## Scope and sources

- Current report US-04/UC-04; frozen BR-05, BR-06, BR-14; explicit PO handoff.
- Public AVAILABLE + PUBLISHED Core catalogue supports optional independent `industry=IT` before pagination, without requiring assessment.
- Missing/blank industry preserves existing behavior and old no-industry cursor fingerprints. `careerTrack` is never treated as industry.
- Owner Enterprise representatives may read/write nullable classification with `If-Match`; V23 adds nullable `scenario.industry_code varchar(32)` constrained to `NULL` or `IT`, with no default/backfill.
- Excluded: UI, recommendations, detail/login/enrollment/alias work, admin UI, new taxonomy, restricted resources/model answers, runtime-stack migration, and CP2+.

## Flows and ownership

1. `VersionedCatalogController` accepts optional industry for paged and unpaged routes.
2. Existing catalogue service normalizes/validates industry once; DAOs apply AVAILABLE/PUBLISHED and industry SQL predicates before pagination.
3. Public summary exposes nullable `industryCode` only.
4. `ScenarioAuthoringController` delegates classification read/write to the existing service; service enforces active ENTERPRISE role, tenant ownership, root lock and revision; DAO persists and increments revision.
5. Unsupported filter returns validation 422; cursor/query mismatch returns 400; missing `If-Match` returns 428; stale revision returns 409; unauthorized/foreign ownership retains existing 401/403/404 handling.

## API and security

| Method | Path | Contract |
|---|---|---|
| GET | `/api/v2/scenarios` | Optional `industry=IT`; public summaries only |
| GET | `/api/v2/enterprise/scenarios/{id}/classification` | Owner-only `{scenarioId,industryCode,revision}` |
| PUT | same | `If-Match`; body `{industryCode:"IT"}` or null |

No AI/provider, private submission, rubric, answer, configuration, dependency, or frontend change.

## Tests and evidence

- Controller: paged/unpaged forwarding; authentication; validation 422; missing/stale revision.
- Service: ownership, stale revision, `IT`/null writes, shared paged/unpaged validation.
- Isolated PostgreSQL Testcontainers: V22→V23 no-backfill preservation, constraint, visibility/filter-before-pagination, old and cross-query cursor behavior.
- Never connect to or migrate the existing Compose database. Record only commands actually observed.

## Checkpoint 2 — public browse UI

- Scope: existing Core catalogue and public scenario detail only; EN/VI copy and scoped responsive styling.
- Catalogue: public `industry=IT` API query, URL-owned search/career-track/difficulty/sort/cursor state, cursor reset on filter changes, semantic whole-card links, and honest loading/empty/no-match/error/retry states.
- Detail: actual public summary metadata only—enterprise name, overview, role, duration, difficulty, and ordered task outlines—with the existing enrollment action preserved.
- Security: no task resources, model answers, rubrics, locked content, private results, or fabricated/demo fallback.
- Explicit PO deviation: public browsing and no recommendations supersede UC-04's authenticated precondition and recommendation alternatives; the historical source remains unchanged and the alignment register records this decision.
- Excluded: login-return/OAuth/Identity, enrollment semantics, submissions/completion, backend contracts, migrations/data, admin UI, recommendations/reviews, and CP3.
- Evidence gate: scoped lint, focused browse/Core integration tests, and production build once; responsive browser UAT remains separate unless a safe runtime is already available.
