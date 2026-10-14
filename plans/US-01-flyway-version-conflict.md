# US-01 Flyway Version Conflict Plan

Status: Implemented  
Owner: User-directed repository maintenance  
Date: 2026-10-14

## 1 Scope

- Foundation item: resolve duplicate Flyway versions introduced when the Syn V10–V12 migration series met the persistent assessment V10–V11 series.
- Actor: development team maintaining the PostgreSQL schema.
- Source: current migration files, repository migration rules, and live local `flyway_schema_history`.
- Acceptance criteria: preserve all applied migration checksums; assign unique versions to unapplied Syn migrations in dependency order; retain both features; do not delete data, alter migration history, or disable validation.
- Exclusions: no API, scoring, entity, database-content, Docker-volume, or unrelated code changes.
- Ownership: user explicitly requested migration maintenance; no broader backend ownership inferred.

## 2 Flow inventory

### Main success flow

1. Inspect migration SQL, dependencies, Flyway configuration, Compose volume, and applied history.
2. Keep applied assessment V10 and V11 unchanged; move the unapplied Syn lifecycle, vector, and trace migrations after them as V12–V14.
3. Verify discovery/build/tests and, if safe, isolated migration startup.

### Alternate flows

1. If the persistent database cannot be read, do not choose versions until its history is available.

### Exception and fallback flows

1. If tests require unavailable Docker or services, report that limitation without editing validation or database history.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Schema evolution | Flyway | Versioned SQL migrations | Existing schema-change mechanism and append-only compatibility rule |

## 4 Change surface

- Frontend: none.
- API/backend code: none.
- Flyway migrations: rename only three unapplied Syn migration files to unique V12–V14 versions; SQL contents remain byte-for-byte unchanged.
- Configuration: none; retain validation-on-migrate.
- Documentation/report: update current migration map and record verification.

## 5 API contract

Not applicable; no API change.

## 6 Security privacy and AI

No security, privacy, or AI behavior change. The migration-history query is read-only.

## 7 Tests and evidence

- Database history: query the existing persistent Compose PostgreSQL `flyway_schema_history` before edits.
- Migration ordering: verify versions are unique and ordered against dependencies.
- Build/tests: `cd backend; .\mvnw.cmd verify`.
- Database startup: use isolated tests if available; do not run the backend against the persistent database solely to verify startup because Flyway startup applies migrations.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Local persistent database already records assessment V10/V11 | Renumbering those scripts would break Flyway validation | Preserve their exact names and contents; assign Syn migrations V12–V14 | Resolved from live history |
| Syn migrations applied to this persistent database? | Could constrain safe renumbering | History contains only V1–V11, with assessment V10/V11; no Syn migrations have been applied here | Resolved from live history |
