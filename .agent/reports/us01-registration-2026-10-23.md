# US-01 / UC-01 registration verification — 2026-10-23

## Scope and implementation

Current report-selected US-01 / UC-01, Person 1; existing US-02 authentication reused; US-03 compatibility required. Sources: [plan](../plans/US-01-UC-01-register-account.md), [UC-01](../../docs/requirements/use-cases/UC-01-register-account.md), [contract/sequence](../../docs/api/registration-uc01.md), assignments and frozen identity business rules.

Responsive EN/VI registration/sign-in now has required-field summaries/associated messages, invalid-field focus, password visibility, cancel-to-home, cleared secrets on mode change/success, pending guards, duplicate-email recovery and unavailable-service retry. Student omits enterprise information; existing Enterprise and consent UI remain. Session lookup has loading/retry states; signed-in Students can navigate to Career Assessments.

IdentityService retains transactional account/profile/audit creation and existing PBKDF2/normalized unique email. Duplicate insertions now give explicit sign-in guidance; missing/privileged roles fail validation. No new roles, cookie transport, implicit consent, migration, assessment/scoring changes or P1–P6 replacement. Responsibility/sequence mapping is in the contract; topology unchanged.

## Git preservation

Original clean checkout: `codex/us-03-career-quiz-alignment` at `62bbe1012e9e5c03dad1719b84cd40fff5fce499`.
Protected US-03: `feat/us-03-career-quiz` at `69784e8753f4d74fac3354fad85325b91e31d0fa`.
US-01: `feature/us01-register-account`, isolated `.codex-tmp/us01`, from main `62bbe10` (cached origin/main agrees). Remote check failed network resolution; remote freshness not asserted. No switch/stash/commit/reset/cleanup of original checkout. No US-03 commit in US-01 history. No push/main merge.

## Executed feature checks

| Check | Observed result |
|---|---|
| Frontend `npm run check` | PASS: 22 files / 107 tests, lint/build |
| Backend `mvnw.cmd spotless:apply verify -Dtest=RegistrationServiceTest,RegistrationAssessmentIntegrationTest,AuthenticationContractTest,AgentPlatformIntegrationTest#realProductionGatewayKbBuilderAssignmentAndIsolatedBackupRestore` | PASS: 15 tests, disposable PostgreSQL/security filters/Nginx; formatting/package |
| `docker compose config --quiet` | PASS |
| `node --check scripts/verify-us01-registration.mjs`; `git diff --check` | PASS |
| Assessment source and V1–V22 comparison against main | No US-01 changes |

Three PostgreSQL registration tests cover invalid fields, CSRF, normalized/concurrent uniqueness, profile/hash/no implicit consent, new Student login, published Core assessment submission/scoring, missing answers, persistence, foreign/anonymous denial, logout and retrieval after reauthentication. Eight existing/extended OIDC contracts include new signed-fixture identity provisioning and legacy Career Quiz submit/reload. Three service tests isolate registration orchestration/duplicates/roles. Ten new frontend tests cover form and session behavior.

Initial full backend run was **not green**: the new assertion used `scores` instead of `resultData.dimensions`, and the existing Nginx test required a built `frontend/dist`. Both were corrected and pass in the focused rerun; dimension assertions check six codes/raw values. Sandbox frontend worker startup timed out; elevated execution succeeded. One overlapping build hit EBUSY; sequential complete frontend check passed. No unexecuted tests counted as passed.

## Integration status

PENDING combined committed US-01 + US-03 verification. Feature checks alone are not integration-readiness evidence. Exact commits and combined results will be appended after execution.

## Remaining acceptance limits

Real Google OAuth with authorized credentials, deployed HTTPS, human visual/content/UAT and lead security sign-off remain pending. Signed OIDC fixture is not live Google. Course report binary unavailable: this Markdown and the API sequence are the handoff. No valuable database/provider/deployment or supplementary/deferred scope involved.
