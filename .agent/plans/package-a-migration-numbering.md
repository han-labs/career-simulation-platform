# Package A migration numbering after main merge

Status: APPROVED in this chat; the user said "continue" after the V15–V18 / V19–V21 proposal.
Owner: User-directed repository maintenance. Date: 2026-10-14.

## 1 Scope

Foundation item: resolve duplicate V13 and V14 without changing any SQL bytes.
Source: user instructions, AGENTS.md and relevant rules, all 18 migration scripts,
the merged main history, and the five physical-design documents.
Acceptance: main/Syn V1–V14 stay unchanged; Package A becomes V15–V18 in the same
dependency order; Package B remains planned only at V19–V21; clean backend verify,
unique Flyway versions and original SQL hashes pass.
No frontend feature, API, business rule, deployment, shared database, Flyway
repair/baseline/clean, data deletion, commit or push is authorized by this fix.

## 2 Flows

Main: capture hashes/status -> rename four files -> update head check, upgrade and
restore fixtures, documentation/references -> verify fresh/upgrade isolated tests.
Alternate: main includes both V13 vector knowledge and V14 trace, so V14–V17
would still collide. The accepted replacement is Package A V15–V18.
Exception: if evidence shows a renamed Package A script was applied to a shared
or valuable database, stop; do not rewrite its history. Repository evidence only
records isolated Package A tests, not deployment. Shared runtime: NOT VERIFIED.

## 3 Responsibility design

Flyway keeps schema ownership. MigrationPreflightService checks the Package A
head. Migration tests verify discovery, preservation, rollback and restore.
No domain or persistence responsibility moves.

## 4 Change surface

Four SQL filenames only; MigrationPreflightService head constant/diagnostic;
SchemaUpgradeIntegrationTest and restore head assertion; a classpath version
uniqueness regression test; current architecture/migration/package plans and
references; task/evidence/AI log. Historical reports retain their original test
claims with a current-numbering note rather than fabricated new measurements.

## 5 API contract

No API or DTO changes.

## 6 Security, privacy and AI

No product authorization or AI behavior changes. Tests use synthetic PG17
Testcontainers databases only. No valuable database connection or provider call.
Keep SQL contents including the old Package B/V18 comment; document that its
runtime/approval migration is now planned V20 instead of changing the SQL hash.

## 7 Tests and evidence

Run clean backend verify (Docker available), frontend check for the gateway build
artifact and repository gates, Compose config, duplicate-version and SHA256 checks,
and diff review. Upgrade baseline is merged main V14 with all 24 source tables;
Package A adds the same 25 tables, yielding 49 during coexistence.

## 8 Risks and decisions

The V13/V14 conflicts and alternative numbering were reported before changes.
Current application head becomes V18; future Package B implementation is excluded.
The original Package A baseline/report describes an earlier V12/21-table source.
Do not mutate baseline hashes or rewrite historical verification as current proof.
Real deployment history remains unverified and separately gated.

## Completed checkpoint

All four renames keep their original SQL hashes; main/Syn V1–V14 stay unchanged.
Source/JAR inventory: 18 unique versions. Focused tests: 3 passed. Final backend
clean verify: 196 discovered, 192 passed, 4 opt-in live skips, zero failures/errors.
Frontend core-enabled check: 76 passed plus lint/build. Compose/diff checks passed.
Full evidence: ../reports/package-a-migration-numbering-verification.vi.md.
