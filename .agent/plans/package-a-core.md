# Package A core implementation

Status: APPROVED by the project lead in this chat, 2026-10-14.
Implementer: Codex; human backend ownership remains unassigned.

## Scope and authority

Stages 0–7 and V13–V16 of the five Astra physical-design-draft documents.
The human explicitly approved the consolidated defaults, minimum cross-owner
frontend integration, isolated databases/testing and normal evidence artifacts.
Missing revised SRS is a traceability limitation. No inferred FR identifiers.
Package B, V17–V19, valuable-data migration, production cutover, live providers,
commit, push and deployment are excluded. Preserve the four staged design files,
five untracked specification files and all original V1–V12 bytes.

## Actors, flows and responsibility mapping

| Trace | Actor / use case | Owning facade / domain | Persistence | Acceptance |
|---|---|---|---|---|
| R-ID | register/login/profile/consent | identity IdentityService, ConsentService, ActorAccess | retained identity + V13 | AC01–04,26 |
| R-ID | organization/member/publish permission | enterprise EnterpriseService, EnterpriseAccess | V13 enterprise | AC03–04 |
| R-AS | publish/start/save/submit/results/retake | assessment authoring/versioned service + frozen evaluator | V14 | AC05–08 |
| R-SIM | author/clone/publish scenarios/assets | simulation authoring, common storage | V14 assets/V15 content | AC09,16 |
| R-SIM | enroll/save/finalize | simulation enrollment/submission domain/services | V15 | AC10–15 |
| R-EV | required-work completion/answer/certificate/snippet | simulation completion/domain factories | V15 | AC16–19 |
| R-PR | owned/shared evidence, confirmed plans | source evidence ports + guidance | V16 | AC04,19,26 |
| R-MIG/API | coexistence, reviewed imports, frontend/Syn | system migration + owning module ports | V13 ledger | AC33–38 core subset |

Controllers perform HTTP/DTO/principal handling; services own authorization,
transactions and lifecycle; DAOs own SQL; pure policies validate values/scoring.
Main core flow: authenticated student -> pinned assessment -> optional enrollment
-> draft deliverables -> FINAL -> all-required completion -> immutable artifacts.
Alternates: retake, V1/V2 parallel work, optional tasks after completion, exact retry.
Exceptions: wrong actor/version, stale revision, missing payload, invalid assets,
duplicate UUID changed body, locked account, denied sharing, rollback and races.
AI is outside objective transactions; legacy Syn retains bounded infrastructure
and fallback, with provider-specific explicit grants and no consent inference.

## Change surface / contracts

New module-owned controller/service/dao/domain/dto packages under identity,
enterprise, assessment, simulation, guidance; common storage/audit/API primitives;
system migration tooling. New SQL is schema-qualified. Existing v1 history stays
distinct; old write endpoints return WORKFLOW_UPGRADE_REQUIRED at core cutover.
New IDs decimal strings, UTC instants, If-Match revisions, canonical SHA256 final
hashes. See 03 §6 for exact v2 route/DTO contracts. Minimum frontend changes only:
shared session/CSRF, auth/consent, attempts-first assessment, enrollment/deliverables,
immutable completion and typed dashboard. Keep drafts/EN-VI/accessibility.

## Security, privacy and evidence

Session+CSRF, current account/tenant status, private files with use-specific access,
purpose/provider consent, safe audit metadata. Passwords/hashes/secrets never in
responses/logs. No public model answers, professional certification or fake scores.
No uncertain content import, historical version reconstruction or default grants.

Internal checkpoints: focused unit/controller tests, PostgreSQL migration/constraint/
race tests, frontend tests; final Maven verify, npm check, Compose config, diff check,
isolated gateway no-AI smoke and isolated restore if available. Report actual results
and skips; Docker unavailable is a DB verification blocker, never a pass.

## Execution checkpoint

- Stages 0–7 code is present on codex/package-a-core (HEAD unchanged).
- V13–V16 additive migrations and 25 new tables installed in disposable PG17 tests;
  V12 legacy fingerprints and 46-table coexistence inventory verified. New constraint
  naming pass was also tested. No owner/runtime database accessed.
- Identity/session/CSRF, enterprise, versioned assessments, scenario authoring,
  enrollment, four deliverable formats, finalization/completion, private assets,
  typed evidence, confirmed plans, reviewed import and minimum frontend are present.
- Preserved Syn transport/orchestration, added purpose/provider consent and single
  instance dispatch/revoke admission lock; new final-task context remains ungraded.
- Focused core + architecture tests passed, including real temporary Nginx gateway
  no-AI workflow and pg_dump/pg_restore into another isolated database. Additional
  security/FK/publication concurrency and importer interruption cases also passed.
- Final backend verify passed: 186 discovered, 182 passed, 4 opt-in live skips,
  zero failure/error. Frontend core-enabled check: 65 tests plus lint/build.
- Compose config and worktree whitespace passed. Consolidated report:
  .agent/reports/package-a-core-verification.vi.md; test totals in package-a-test-summary.json.
- No implementation blocker remains. Runtime inventory/valuable-data rollout,
  human content/UAT, missing revised SRS and all Package B work remain separately gated.
- Preserve original staged docs and untracked five-spec directory. Baseline JSON
  remains the byte/hash reference. Never commit/push/deploy or run default Compose up.
