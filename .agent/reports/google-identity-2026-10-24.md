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

## Verification observed so far

| Check | Observed outcome |
|---|---|
| Final focused identity backend (`AuthenticationContractTest`, `RegistrationAssessmentIntegrationTest`, `RegistrationServiceTest`, `BrowserCredentialValidationTest`) | 23 passed, 0 failed/errors/skipped |
| Frontend initial run | Failed: pending assertion checked the button's own disabled property although fieldset provided disabled state; two worker startup timeouts. Corrected to effective `:disabled`; no behavior/assertion removed. |
| Frontend one-worker rerun (`npm run check`) | 119 passed in 22 files, lint and production build passed |
| Standard + isolated runtime Compose validation | Both passed with `config --quiet`; no secrets printed |
| Final feature frontend gate (`VITEST_MAX_WORKERS=1 npm run check`) | 119 passed, 22 files; lint and build passed on final source |
| Complete feature backend `mvnw verify` | 323 discovered: 317 passed, 1 failed, 5 skipped, 0 errors; existing StudentAgentIntegrationTest line 545 failure |
| Complete combined integration backend gate | Pending final execution summary below |
| Combined US-03 regression/gateway/browser | Pending final execution summary below |

The final source also aligns action height and limits workspace links to enabled routes;
combined verification is recorded before handoff below. Feature full verify reproduced
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

Pending final gate, browser and commit evidence.
