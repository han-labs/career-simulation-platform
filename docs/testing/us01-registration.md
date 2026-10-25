# US-01 / UC-01 registration verification

Use `feature/us01-register-account` for registration work. Preserve the active US-03 checkout and do not stash, reset, switch, merge into, or commit its unfinished work. Combined acceptance uses the separate `codex/integration-us01-us03-registration` worktree/branch containing committed US-01 and `feat/us-03-career-quiz`; it is never merged into main automatically.

## Repeatable automated checks

Docker Desktop, Java 17, Maven wrapper dependencies and frontend dependencies must be available. Build the frontend before the complete backend gate: the existing Agent Platform gateway test serves `frontend/dist`.

```powershell
cd frontend
$env:VITE_CORE_V2_ENABLED='true'
$env:VITEST_MAX_WORKERS='2' # limits worker startup pressure without changing tests
npm run check
cd ../backend
.\mvnw.cmd verify
cd ..
docker compose config --quiet
```

Focused feature command:

```powershell
cd backend
.\mvnw.cmd verify '-Dtest=RegistrationServiceTest,RegistrationAssessmentIntegrationTest,AuthenticationContractTest'
```

`RegistrationAssessmentIntegrationTest` creates disposable PostgreSQL and synthetic published RIASEC content. It exercises real security filters, JWT cookies, CSRF, account/profile uniqueness (including a race), missing answers, deterministic scoring, result persistence, ownership, logout and reauthentication. `AuthenticationContractTest` uses a local signed OIDC provider fixture; it never calls Google. No opt-in live/fixture tests should be relabeled as ordinary passes.

For a separately prepared **disposable loopback stack only**, with a published assessment:

```powershell
$env:US01_ISOLATED_TEST='true'
$env:US01_SMOKE_ORIGIN='http://127.0.0.1:18080'
node scripts/verify-us01-registration.mjs
```

The smoke script creates synthetic accounts/results and revokes its credentials. It refuses non-loopback hosts and requires explicit disposable-test opt-in. Do not point it at the existing valuable database. Use separate Compose project/container/network/ports and storage; the 2026-10-23 run used `careersim-us01-us03-check`, loopback gateway 18080 / PostgreSQL 15473, tmpfs database, local HTTP cookie override, process-local JWT key, no demo actor and disabled AI/Google providers. Its guarded US-03 content bootstrap ran only against that new instance. No local HTTP flag or ephemeral key is a production deployment setting.

## Browser checklist

1. Open Account and privacy; choose Register. Submit empty/invalid fields; confirm summary, associated messages, keyboard focus and retained entries.
2. Cancel before submitting; verify return home and no registration request. Open registration again.
3. Register a new Student without optional enterprise data. Verify success and a cleared sign-in form; sign in with the new credentials.
4. Follow the authenticated Career Assessments link, select an available quiz, read instructions and start.
5. Submit missing answers; verify required-question feedback. Complete answers and submit; verify a saved summary, category scores and history.
6. Reload, log out, sign in again and open the saved history entry; scores and stored result must match. Test anonymous/other-account denial through automated contracts.
7. Check 320 px and desktop widths, visible focus, labels, error messages, password visibility and pending buttons. Check EN/VI text without changing submitted identifiers/scores.
8. When Google is disabled, explain email/password availability. Test real Google separately with authorized credentials and callback configuration; fixture OIDC is not live acceptance.

Observed results, tested commit hashes, failures/reruns and limits: [dated report](../../.agent/reports/us01-registration-2026-10-23.md).

The full suite currently has a separately reproduced baseline failure in `StudentAgentIntegrationTest.deletedSourceDuringCallCannotResurrectDerivedContent:545`. An isolated run also fails on unchanged main `62bbe10`. Do not disable it or increase its timeout to claim registration acceptance; retain the successful feature evidence and report the global gate separately to Person 3/project lead.
