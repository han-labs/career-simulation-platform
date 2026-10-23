# Report-selected US-01 / UC-01 — Register account

Date: 2026-10-23. Status: authorized implementation; acceptance requires executed evidence.
Owner: Person 1 (Identity & Assessment); human identity/reviewer pending official sign-off.
Cross-cutting identity owner: this explicitly requested US-01 task; lead security review pending.

## Sources and acceptance

Read `.agent/AGENTS.md`, PROJECT_CONTEXT, TASKS, DESIGN, architecture/security/testing/report/AI/facade/GRASP rules; requirements index, selected 10-US catalogue, UC-01 and UC-03, TEAM_ASSIGNMENTS, frozen Baseline v1/DR1–DR2 and P1/P2 architecture.
Current report ACs: eligible registration/identity flow; missing/invalid fields; duplicate account message; cancel without account creation. UC-01 optional fields may be omitted; unavailable identity must not appear successful. Baseline authentication uses existing server roles, JWT cookies, CSRF and owner isolation.

## Git preservation

Original checkout: `codex/us-03-career-quiz-alignment` at `62bbe10`, clean at inspection.
US-03: `feat/us-03-career-quiz` at `69784e8753f4d74fac3354fad85325b91e31d0fa`.
US-01: separate ignored `.codex-tmp/us01` worktree, `feature/us01-register-account`, from main `62bbe10` (cached origin/main matches). Remote freshness check failed network resolution; no claim of fresh remote verification. No original-checkout switch, stash, commit, reset or cleanup. US-01 contains no US-03 commit. After focused US-01 commit, use a separate temporary integration branch/worktree to merge committed US-03; never merge into main.

## Flows and responsibilities

1. Anonymous Student opens registration, enters display name/email/password; optional enterprise field omitted for Student. Existing Enterprise path retained.
2. UI identifies required/invalid fields, associates messages and focuses first invalid input. IdentityController validates DTO; IdentityService owns eligibility, unique transactional creation/profile/audit; IdentityDao persists normalized identity.
3. Success returns to sign-in with clear status. Login uses existing HttpOnly JWT and CSRF rotation. Account UI offers Career Assessments; unchanged US-03 facade owns selection, validation, scoring, saving and result reads.
4. Cancel discards unsubmitted state and navigates home, with no request. Alternate sign-in mode also clears registration secrets.
5. Duplicate email produces explicit 409 feedback; server uniqueness remains authoritative, including concurrency. Network/service failures preserve in-memory input and permit retry without claiming success.
6. Google uses existing server-configured OIDC authorization-code/PKCE/state/nonce and verified issuer/subject path; collisions require explicit existing-account login/link. Disabled provider is explained. Never fabricate a provider login.

## Change surface and sequence

- AccountPage delegates anonymous form to feature-local AuthenticationForm; feature CSS and EN/VI dictionary, focused component tests. Existing consent, shared account-generation invalidation, app/router and assessment code retained.
- IdentityServiceImpl: duplicate-email message and field-level service validation where appropriate; service and MockMvc contracts independently tested.
- Existing API: GET `/v2/auth/{providers,csrf}`, POST `/v2/auth/{register,login,logout}`, GET `/v2/me`; no new token transport, roles or consent grants. No migration, no edits to V1–V22, no scoring changes, no P1–P6 rewrite.
- Add repeatable authenticated registration-to-versioned-assessment integration coverage using isolated PostgreSQL; synthetic published content only. Cover missing CSRF, absent/foreign actor, logout and stored-result reload.
- Evidence in `.agent/reports/`, API/UC-01/alignment notes and AI-assistance log. Course report binary unavailable; Markdown report handoff provided.

## Security/privacy and boundaries

Passwords stay in memory until submit, clear on mode change/success/cancel, never localStorage/logs. Preserve PBKDF2 policy (12–128 characters), current status/version checks, per-token revocation and account-scoped assessment drafts/results. Identity/profile/audit transaction must roll back on failure. No AI used by product registration and no external Student evidence. No external provider/valuable database access required for synthetic tests.

## Verification and open decisions

Run backend `mvnw.cmd verify`, frontend `npm run check`, `docker compose config --quiet`; then repeat feature/regression gates in integration worktree and run isolated gateway/UI registration/login/assessment/result flow when tooling permits. Preserve original branch hashes/status and migration checksums. Record exact commits, actual counts, skips and blockers. Real Google credentials/human provider verification remain a distinct pending acceptance gate; local signed OIDC fixture tests are not live Google evidence. No unresolved business-rule change is implemented.
