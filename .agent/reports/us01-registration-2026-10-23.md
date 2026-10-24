# US-01 / UC-01 registration verification — 2026-10-23

Handoff updated 2026-10-24 (Asia/Saigon); earlier dated execution results remain identified separately.

## Scope and implementation

Current report-selected US-01 / UC-01, Person 1; existing US-02 authentication reused; US-03 compatibility required. Sources: [plan](../plans/US-01-UC-01-register-account.md), [UC-01](../../docs/requirements/use-cases/UC-01-register-account.md), [contract/sequence](../../docs/api/registration-uc01.md), assignments and frozen identity business rules.

Responsive EN/VI registration/sign-in now has required-field summaries/associated messages, invalid-field focus, password visibility, cancel-to-home, cleared secrets on mode change/success, pending guards, duplicate-email recovery and unavailable-service retry. Student omits enterprise information; existing Enterprise and consent UI remain. Session lookup has loading/retry states; signed-in Students can navigate to Career Assessments.

IdentityService retains transactional account/profile/audit creation and existing PBKDF2/normalized unique email. Duplicate insertions now give explicit sign-in guidance; missing/privileged roles fail validation. No new roles, cookie transport, implicit consent, migration, assessment/scoring changes or P1–P6 replacement. Responsibility/sequence mapping is in the contract; topology unchanged.

## Git preservation

Original clean checkout: `codex/us-03-career-quiz-alignment` at `62bbe1012e9e5c03dad1719b84cd40fff5fce499`.
Protected US-03: `feat/us-03-career-quiz` at `69784e8753f4d74fac3354fad85325b91e31d0fa`.
US-01: `feature/us01-register-account`, isolated `.codex-tmp/us01`, from main `62bbe10` (cached origin/main agrees). Initial Git network check failed; a later read-only GitHub connector comparison confirmed remote `main` identical to `62bbe1012e9e5c03dad1719b84cd40fff5fce499` (0 ahead / 0 behind). No switch/stash/commit/reset/cleanup of original checkout. No US-03 commit in US-01 history. No push/main merge.

## Executed feature checks

| Check | Observed result |
|---|---|
| Frontend `npm run check` | PASS: 22 files / 107 tests, lint/build |
| Backend `mvnw.cmd spotless:apply verify -Dtest=RegistrationServiceTest,RegistrationAssessmentIntegrationTest,AuthenticationContractTest,AgentPlatformIntegrationTest#realProductionGatewayKbBuilderAssignmentAndIsolatedBackupRestore` | PASS: 15 tests, disposable PostgreSQL/security filters/Nginx; formatting/package |
| `docker compose config --quiet` | PASS |
| `node --check scripts/verify-us01-registration.mjs`; `git diff --check` | PASS |
| Assessment source and V1–V22 comparison against main | No US-01 changes |

Three PostgreSQL registration tests cover invalid fields, CSRF, normalized/concurrent uniqueness, profile/hash/no implicit consent, new Student login, published Core assessment submission/scoring, missing answers, persistence, foreign/anonymous denial, logout and retrieval after reauthentication. Eight existing/extended OIDC contracts include new signed-fixture identity provisioning and legacy Career Quiz submit/reload. Three service tests isolate registration orchestration/duplicates/roles. Ten new frontend tests cover form and session behavior.

Initial full backend run was **not green**: the new assertion used `scores` instead of `resultData.dimensions`, and the existing Nginx test required a built `frontend/dist`. Both were corrected and pass in the focused rerun; dimension assertions check six codes/raw values. That initial unmerged US-01 run also had the existing Student Agent latch failure discussed below; it was not fixed or hidden. Sandbox frontend worker startup timed out; elevated execution succeeded. One overlapping build hit EBUSY; sequential complete frontend check passed. No unexecuted tests counted as passed.

## Integration status

Tested implementation: `a8b4a24` on `feature/us01-register-account`.
Combined committed tree: `8b081841fde4616a2e43b721b444cdd5a73db0e6` on `codex/integration-us01-us03-registration`, separate `.codex-tmp/us01-us03` worktree. Its parents retain US-01 and protected US-03 `69784e8`. The only textual merge conflict was `frontend/src/shared/i18n/translate.js`; both assessment and identity dictionaries were retained. No US-03 business logic changed for integration.

| Combined check | Observed result |
|---|---|
| Core-enabled frontend `VITEST_MAX_WORKERS=2 npm run check` | PASS: 25 files / 146 tests; lint and production build pass |
| First full `mvnw.cmd verify` | 340 discovered: 333 PASS, 1 FAIL, 6 SKIP; no errors. Existing `StudentAgentIntegrationTest.deletedSourceDuringCallCannotResurrectDerivedContent` timed out at its three-second fixture latch (line 545). Test unchanged. |
| Identity/authentication subset within that full run | 17 PASS: OIDC/security 8, registration-to-Core assessment 3, credential validation 3, registration service 3 |
| US-03 assessment subset within that full run | 29 PASS; opt-in local bootstrap 1 SKIP in normal suite; separately invoked bootstrap PASS |
| CorePackageAIntegrationTest | 28 PASS, including existing/preserved assessment regression |
| Isolated Nginx gateway `verify-us01-registration.mjs` | PASS: health, invalid/duplicate registration, CSRF, fresh Student login/session, assessment selection/submission/scoring/result reload, anonymous/foreign denial, logout and persisted retrieval after reauthentication |
| Browser through real Nginx/PostgreSQL | PASS observations below |
| Base and isolated Compose config | PASS |
| Combined `mvnw.cmd -DskipTests verify` (2026-10-24) | PASS: formatting and application packaging; tests intentionally not executed in this command and are not counted as passes |

The default frontend worker pool initially timed out; limiting concurrency to two workers ran the same complete suite successfully. No assertion/test selection changed. Vite emits a non-fatal >500 kB bundle warning; code-splitting is outside this registration/preservation task. Two EOF blank-line notices come from unchanged committed US-03 data files; they were preserved rather than edited.

Full backend rerun with the disposable demo stack stopped: **same result**, 340 discovered / 333 PASS / 1 FAIL / 6 SKIP / 0 ERROR. The same Student Agent fixture latch timed out; all identity/assessment/Core checks passed again. This is a real remaining global gate failure, not a green suite.

On 2026-10-24, the isolated method also failed on the combined tree, and the exact same command (`mvnw.cmd test -Dtest=StudentAgentIntegrationTest#deletedSourceDuringCallCannotResurrectDerivedContent`) failed at line 545 on **unchanged main `62bbe1012e9e5c03dad1719b84cd40fff5fce499`**, in detached `.codex-tmp/us01-baseline` with a disposable PostgreSQL container (1 executed / 1 failure / 0 errors/skips). The baseline working source remained clean. Therefore this failure predates both feature changes; it is not attributed to US-01/US-03 compatibility. Root cause remains undetermined; this is not described as a passing flaky test. No Student Agent behavior, test assertion or timeout was changed. Person 3/project lead owns the separate diagnosis; global release/DoD remains gated on resolving it.

### Observed eight-step Student flow

1. Registered a fresh synthetic Student through the actual UI; optional enterprise data omitted; clear success and sign-in action.
2. Signed in with that new account.
3. Authenticated account and privacy page appeared with the Student role; independent consent stayed ungranted.
4. Followed its Career Assessments link.
5. Selected Work Activities Test and opened its instructions; four independent published assessment options were visible.
6. Blank submit produced field guidance and focused the first unanswered question; completed nine responses and submitted.
7. Backend validated/scored/stored the result. UI showed Information **12**, People **9**, Things **15**, matching submitted values. A read-only DB check returned `SUBMITTED|1` for this synthetic Student's stored result.
8. Reloaded, logged out, signed in again, reopened history and viewed the same saved result/scores.

The separate repeatable HTTP smoke exercised published RIASEC. Browser registration cancellation returned to `/`; required-field feedback associated errors and focused `identity-displayName`. At 320 px, document scroll width was 305 px (no horizontal overflow); desktop check at 1280 px had scroll width 1265 px. Saved screenshot evidence is retained locally in the integration worktree's ignored `tmp/` and the chat artifact directory. The initial full-page result capture was inspected; an additional screenshot attempt later failed, without affecting the verified DOM/result observations. Viewport overrides were reset.

The isolated runtime used Compose project `careersim-us01-us03-check`, loopback gateway 18080 / PostgreSQL 15473, tmpfs data, process-local JWT key and explicit local HTTP cookie override, no demo actor and disabled AI/Google providers. Its own guarded US-03 bootstrap published all four local versions. Only this disposable stack was removed after evidence; the original stack/DB was not accessed or restarted. [Repeatable checks and browser checklist](../../docs/testing/us01-registration.md).

### Preservation and review handoff

Git comparisons show all US-03 assessment frontend/backend source and tests exactly match `69784e8`; V1–V22 exactly match main. App routes, assessment state/drafts, scoring and persistence were not modified by US-01. Original branch/ref/status remain unchanged; neither feature is merged into main or pushed. The US-01 feature history excludes US-03; the temporary integration branch and clean detached baseline worktree are retained for review. Final evidence-only commits do not change the tested application tree. Security/UAT review still belongs to Person 1/project lead. No authentication compatibility issue was observed in executed checks; the pre-existing Student Agent failure and live Google gate are disclosed separately. Only the original application stack remains running; the disposable integration stack was removed.

## Remaining acceptance limits

Local registration/authentication-to-US-03 integration is verified. Full-suite acceptance remains blocked by the independently reproduced baseline Student Agent failure. Real Google OAuth with authorized credentials, deployed HTTPS, human visual/content/UAT and lead security sign-off remain pending. Signed OIDC fixture is not live Google. Course report binary unavailable: this Markdown and the API sequence are the handoff. No valuable database/provider/deployment or supplementary/deferred scope involved.
