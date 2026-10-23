# US-01/US-02 Google identity follow-up — 2026-10-24

## Scope and implementation

Person 1, current catalogue UC-01 Register account and UC-02 Log in. The explicit
attached task authorizes completing Google presentation/feedback/session landing.
Sources: [plan](../plans/US-01-UC-02-google-identity.md), current UC/selected-US AC,
P1/P2 architecture and existing auth contracts. Human security/UAT review is pending.

The local runtime disabled Google and had no client credentials/callback. The old
anonymous form showed only an availability note in that state. Both forms now keep
Continue with Google visible, with the unmodified official Google G asset, separator,
matching full-width actions, pending/retry feedback and EN/VI localization. Register
retains its fields and cancellation after Google. Provider lookup errors are distinct
from missing configuration. Two-column HCM-UTE styling remains responsive.

The Google action also uses official Google Sans Medium Latin/Vietnamese WOFF2
subsets (about 29 kB combined), self-hosted with the upstream SIL OFL license and
source/provenance. The rest of the site's typography remains unchanged. The license's
single trailing space is normalized; font binaries are unmodified. A production build
passed with both assets; no remote Google Fonts requests are introduced.

AccountPage restores `/v2/me` before trusting callback success or choosing a role
landing. Student opens Career Assessments; Core Enterprise/Admin open their existing
workspaces; when Core is off they remain on Account. Invalid session and account lock
offer clear feedback without granting access. OAuth failure handling exposes fixed
codes for cancel/provider unavailable/denied/link-required/disabled account only.

Existing Spring OIDC code exchange, state/nonce/PKCE and claim verification, transactional
Student/profile provisioning, issuer+subject identity, explicit credential-bound linking,
JWT HttpOnly/Secure/Lax transport, CSRF, role/status checks and revocation are reused.
No migrations, assessment business rules, tokens in browser storage, auto email linking,
privilege escalation, mandatory domain filter or AI consent are introduced.

## Files and sequence

- Identity `AuthenticationForm`, `AccountPage`, CSS, i18n and tests; official logo asset
  with provenance. Shared contract test updates only the visible Google action name.
- `GoogleOAuthFlow` and `SecurityConfig`: safe failure outcome classification.
- `AuthenticationContractTest`: local signed provider, create/repeat/login/roles,
  cancellation, provider failure, unverified email/audience/expiry and email collision.
- `RegistrationAssessmentIntegrationTest`: Google identity service boundary through
  real Core assessment persistence and subject login after logout.
- `.env.example`, [Google setup/sequence](../../docs/testing/google-identity.md), API,
  architecture, UC-02, alignment/TASKS, plan and sanitized AI/evidence records.

Browser → Nginx `/api` → existing Spring Google authorization → verified OIDC callback
→ identity facade/DAO transaction → JWT cookie → account `/v2/me` → authorized workspace.
Exact API, Google Cloud configuration and local/Docker reproduction are in the setup doc.

## Verification observed

| Check | Observed outcome |
|---|---|
| Final focused identity backend (`AuthenticationContractTest`, `RegistrationAssessmentIntegrationTest`, `RegistrationServiceTest`, `BrowserCredentialValidationTest`) | 23 passed, 0 failed/errors/skipped |
| Frontend initial run | Failed: pending assertion checked the button's own disabled property although fieldset provided disabled state; two worker startup timeouts. Corrected to effective `:disabled`; no behavior/assertion removed. |
| Frontend one-worker rerun (`npm run check`) | 119 passed in 22 files, lint and production build passed |
| Standard + isolated runtime Compose validation | Both passed with `config --quiet`; no secrets printed |
| Final feature frontend gate (`VITEST_MAX_WORKERS=1 npm run check`) | 119 passed, 22 files; lint and build passed on final source |
| Complete feature backend `mvnw verify` | 323 discovered: 317 passed, 1 failed, 5 skipped, 0 errors; existing StudentAgentIntegrationTest line 545 failure |
| Combined frontend default fork-pool attempt | Incomplete: 133 passed in 24 files; worker startup timeout prevented VersionedAssessmentPage.test.jsx from running. Not reported as a pass. |
| Combined frontend lint, `test:run -- --pool=threads --maxWorkers=1`, build | 158 passed in all 25 files, 0 unhandled errors; lint/build passed, Core enabled; existing non-fatal chunk-size warning (about 516 kB) |
| Complete combined backend `mvnw verify` | 346 discovered: 339 passed, 1 failed, 6 skipped, 0 errors. Only failure is the same StudentAgentIntegrationTest line 545 assertion. Gate remains failed. |
| Combined identity / assessment / Core subsets within that full run | Identity 23/23 passed; US-03 assessment 29/29 passed, bootstrap opt-in 1 skipped; CorePackageAIntegrationTest 28/28 passed |
| Combined `mvnw -DskipTests verify` | Packaging/formatting passed (339 Java files clean); no test execution or coverage acceptance implied |
| Latest-local gateway/browser | Actual email registration → login → session reload → Work Activities 9 answers → saved result → reload → logout denial → re-login/result retrieval passed; no live Google call |

The final source also aligns action height and limits workspace links to enabled routes.
Feature and combined full verify reproduced
only the previously recorded unchanged-main Student Agent assertion (`entered.await`
returned false), not an auth or assessment failure; no test was weakened or skipped.
Raw logs/screenshots remain ignored local artifacts and are not staged.

## Preservation, integration and limits

Original checkout `codex/us-03-career-quiz-alignment` remains clean at `62bbe10`.
Protected US-03 branch remains `feat/us-03-career-quiz@69784e8`. Authentication is
developed only on `feature/us01-register-account` in `.codex-tmp/us01`. Existing combined
branch `codex/integration-us01-us03-registration` is used for integration; no main merge,
US-03 rewrite, stash/reset/clean/force push, production migration or external publication.
No US-03 assessment files or V1–V22 migration bytes were changed.

**Real Google acceptance is NOT VERIFIED.** Client ID/secret and callback are absent.
The runtime retains disabled Google feedback until real credentials are supplied privately.
Local signed-provider tests and identity-service fixtures do not imply external Google UAT.
The earlier full-suite Student Agent failure on unchanged main is tracked separately in
[the registration evidence](us01-registration-2026-10-23.md); any recurrence is reported,
never hidden by passing auth tests. Final integration readiness is not claimed while an
applicable gate or real external acceptance remains unresolved.

## Final execution and commits

Application commits on `feature/us01-register-account`: `e591a67` (Google experience,
safe callback outcomes and tests), `ab091be` (self-hosted official fonts), `aa2d4c2`
(asset validation/license whitespace). Combined merges: `561f947` and final application
tree `cfdf0db709c637384c4d2d519e88702bb58ea8f9`. Both merges completed without conflicts;
identity and assessment translation dictionaries remain present. Subsequent evidence
commits contain documentation only and preserve this tested application tree.

The feature full gate ran on the final Java implementation before the font-only
follow-up. The final combined frontend/backend gates and browser QA ran on `cfdf0db`.
Relevant commands and local raw logs (not staged):

- Feature `VITEST_MAX_WORKERS=1 npm run check`: `tmp/google-feature-final-frontend.log`.
- Feature `mvnw verify`: `tmp/google-feature-verify.log`; focused 23-pass run:
  `tmp/google-identity-backend-final.log` in the feature worktree.
- Combined sequential `npm run lint`, `npm run test:run -- --pool=threads --maxWorkers=1`,
  `npm run build`, `VITE_CORE_V2_ENABLED=true`: `tmp/google-combined-threads-check.log`.
- Combined `mvnw verify`: `tmp/google-combined-backend-verify.log`;
  `mvnw -DskipTests verify`: `tmp/google-combined-package.log`. Test reports confirm
  AuthenticationContractTest 13, RegistrationAssessmentIntegrationTest 4,
  BrowserCredentialValidationTest 3 and RegistrationServiceTest 3 passed.
- Standard and latest-local `docker compose ... config --quiet` passed.
- Git diff checks: feature/integration changes versus preserved assessment source/tests
  and V1–V22 migrations are empty. Original branch refs and root checkout remain intact.

The local runtime at `http://127.0.0.1:18080/account` uses the combined build. Generated
private keys stay in its ignored environment file and were retained. JAR/mounted JAR
SHA-256 both equal `D7A4C2170D4FD4ECA04B922A8959F1970AC9E79CED020E0219A9DA6E346293DE`.
Only its backend was stopped/recreated; original services on 8080/3000/8081/5433 were
not restarted. Pre/post refresh counts matched: 4 accounts, 0 results, 4 published
assessment versions. Initial health polling timed out before Spring's 53-second
startup; subsequent gateway health is UP. No seed/reset/volume deletion occurred.

Browser QA then created its own synthetic Student (no consent), restored its JWT
session by reloading Account, opened Work Activities version 4, answered nine questions
with the neutral rating and submitted. Information/People/Things each scored 9/15.
Result and history survived reload and logout/relogin; anonymous result navigation
showed Sign in is required without disclosing the result. Database evidence for this
attempt: `STUDENT|SUBMITTED|1` (exactly one result). The synthetic record remains local.
This does not establish live Google acceptance. The separate full signed OIDC fixture
and Google-identity/Core service-boundary tests cover those mocked integration layers.

Visual/interaction checks at 320, 768 and 1280 px found no horizontal overflow (320 px:
document clientWidth and scrollWidth both 305). Desktop primary and Google controls
share the same x/width and 48 px height. EN/VI rendering, two-column desktop/tablet,
single-column mobile, associated validation, first-invalid focus, effective disabled
state, Tab skipping the unavailable Google action, and cancellation to Home were
observed. Real configured Google keyboard/navigation remains external UAT; enabled
action behavior is covered by component and signed-provider tests. Full-page screenshot
capture was unavailable; documented native viewport capture was used instead.

Reviewed local screenshots in the integration worktree's ignored `tmp/`:
`google-registration-actions.jpg`, `google-registration-mobile.jpg`,
`google-identity-us03-result.jpg`. They show only synthetic/empty UI; no credentials,
provider codes or private records are published. Viewport overrides were reset and
the synthetic browser credential was logged out after QA.

Remaining gates: real Google credentials + complete external callback/session/assessment
UAT, the existing Student Agent baseline assertion (Person 3/lead), human security/UI
review and deployment HTTPS acceptance. Auth/US-03 local regression is observed, but
the full suite and external acceptance are not green. No integration-ready/release
completion claim, main merge, PR, push or production rollout is made.
