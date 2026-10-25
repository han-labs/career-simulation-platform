# Docker Compose startup repair — 2026-10-24

## Scope and source

Explicit urgent maintenance request for existing selected UC-01/02/03 (US-01/02/03),
not new business scope. User selected the integration checkout `.codex-tmp/us01-us03`
as the ongoing Compose directory. Base: `7280a6322c60bfa5d779c2d0ecbec881e4cacb28`
on `codex/integration-us01-us03-registration`, with the uncommitted changes listed
below. No automatic commit, stage, push, merge, reset or stash.

## Observed cause

The failing container's labels identified that checkout's docker-compose.yml.
Its environment had Google enabled and a blank JWT signing key, with ephemeral
keys disabled. The meaningful exception chain was:

```text
identityController -> browserAuthenticationImpl
  -> BeanCreationException -> BeanInstantiationException
  -> IllegalStateException:
     Set JWT_SIGNING_KEY to at least 32 random bytes (base64).
```

PostgreSQL accepted connections. Flyway validated all 22 migrations and reported
schema V22 up to date before the authentication constructor failed. Google was
not the observed crash cause. A separate parent-checkout configuration problem
was stale COMPOSE_FILE pointing to a missing docker-compose.us03-local.yml. That
parent checkout was preserved; the user chose the integration checkout instead.

## Smallest repair and unchanged boundaries

- Restore the previously used private stable JWT key into the blank local setting;
  preserve Google ID, secret, enabled flag, callback and all unrelated .env settings.
- Require nonempty JWT_SIGNING_KEY during Compose interpolation. Spring still
  independently validates base64, minimum decoded length and token lifetime.
- Add backend HTTP readiness and frontend/gateway healthy dependencies. Dependency
  restart propagation refreshes Nginx after explicit Compose updates. Canonical
  `build` followed by `up -d` was verified without a separate manual Nginx restart.
  Semantics: [Docker startup order](https://docs.docker.com/compose/how-tos/startup-order/).
- Add Initialize-LocalEnvironment.ps1: copy a template only when .env is absent;
  create a cryptographically random stable 32-byte JWT key only when missing/blank;
  never replace a nonblank key or print secrets. Existing invalid keys are rejected.
- Remove real Google values from the pre-existing modified .env.example, retaining
  them privately in ignored .env. Document required setup, optional Google and flags.
- Enable both existing Core runtime/build flags in the local integration .env for
  versioned assessment. No application/business rule or UI implementation changed.
- Retire the old ignored custom runtime helper in favor of canonical Compose.

Production JWT/OAuth implementation, CSRF, Secure/HttpOnly/Lax cookie policy,
authorization, ownership, database validation and V1–V22 remain unchanged. Google
is enabled, not disabled to conceal the failure. No frontend design changes.

## Executed automated gates

| Check | Actual result |
|---|---|
| Test-LocalEnvironment.ps1 | PASS: fresh/random/stable key, missing key, preservation of OAuth/settings, invalid-key refusal without mutation |
| AuthenticationStartupConfigurationTest | 7 PASS: independent missing JWT, invalid base64, short key, valid JWT+Google, missing Google ID/secret, wrong callback |
| backend mvnw.cmd verify | BUILD SUCCESS; 353 discovered, 347 PASS, 6 opt-in SKIP, 0 failures/errors; Spotless PASS |
| Existing identity suites | 23 PASS within the full gate; new startup tests are an additional 7 |
| Existing assessment suites | 29 PASS within the full gate; opt-in local fixture initially skipped |
| Frontend npm run check | Lint, 158 tests/25 files, production build PASS after retry outside Windows sandbox |
| Frontend with explicit VITE_CORE_V2_ENABLED=true / VITE_AGENT_PLATFORM_ENABLED=false | Lint, 158 tests/25 files, production build PASS |
| Compose blank-key regression | config --quiet rejects missing JWT before creating containers |
| Canonical Compose | config --quiet, build, up -d, ps and gateway health PASS |
| Final diff/secret hygiene | No production business-code or migration changes; real configured Google values/JWT key absent from tracked/nonignored source candidates |

The initial sandboxed frontend gate could not load tests because Windows denied
temporary-cache renames (EPERM). It did not pass; the full rerun outside that
sandbox did. The full backend gate this time passed, including the previously
reported Student Agent race test; no test was weakened or disabled.

## Docker and end-to-end evidence

Main project `career-simulation-platform`: existing PostgreSQL container and data
volume retained, PostgreSQL/backend healthy, frontend/gateway running, backend
restart count 0. Observed Spring initialization and successful Flyway validation.
`/api/health` returns UP and `/api/v2/auth/providers` returns google=true through
localhost:8080. Original counts remain 6 accounts, 21 assessment results and 4
published assessment versions. Private pre-repair pg_dump was taken, with no reset,
volume deletion, history repair or historical migration edit.

Fresh-install project `careersim-startup-clean` used separate new PG/asset volumes
and loopback ports 18090/18091/13090/15483. It reused the just-built application
images with a fresh private JWT key and supplied Google configuration. Initial
schema V22 had 1 migration-seeded user, 0 results and 0 published versions. Its
four-service startup and Google provider flag passed. Only then, the existing
explicit LocalCareerQuizBootstrapTest created synthetic fixture content in that
verified isolated volume: 1 PASS, four published assessments.

`US01_ISOLATED_TEST=true`, `US01_SMOKE_ORIGIN=http://localhost:18090`,
`node scripts/verify-us01-registration.mjs` passed all six check groups:

1. Gateway health.
2. Registration validation, CSRF, uniqueness and separate sign-in.
3. New Student login, JWT cookie and current authenticated session.
4. Published assessment selection, missing-answer validation, scoring, persisted
   result and result reload.
5. Anonymous and foreign-account result isolation.
6. Logout denial, reauthentication and durable result retrieval.

The isolated database contained four published versions and one saved result after
the smoke. None of its fixture users/results were inserted into the existing DB.
The disposable project was then stopped without deleting volumes; the main
localhost:8080 project remains running. Final main backend restart count is 0;
the served frontend contains the Core-enabled bundle index-DnfEVSgY.js (HTTP 200).
Both localhost gateways passed a manual-redirect HTTP check for Google authorization
URL, exact callback, state, nonce, PKCE S256, minimal scopes and Secure/HttpOnly/Lax
handshake cookie; no client secret was present in the redirect. Browser inspection
of localhost:8080/account confirmed the enabled Continue with Google link.

Raw build/test logs, backups, fixture exports and environment files remain ignored
under tmp/. Sanitized exact command outcomes are recorded here, not raw credentials.

## Preservation and remaining setup

- Original US-03 `feat/us-03-career-quiz` remains at
  `69784e8753f4d74fac3354fad85325b91e31d0fa`.
- US-01 `feature/us01-register-account` remains at
  `7db61785c912f0cc4b0e0dc072b781a244732047`.
- Parent checkout `codex/us-03-career-quiz-alignment` remains clean at
  `62bbe1012e9e5c03dad1719b84cd40fff5fce499`.
- No production Java/frontend source or immutable migration was edited. Changes
  are Compose/template, setup and setup tests, startup regression tests, runbook,
  plan, task/AI log and this report.
- A full read of the pre-existing modified template displayed a real secret in
  tool output during diagnosis. This was an agent error. The template is now clean;
  the user must replace that Google client secret in Google Cloud and ignored .env,
  then run `docker compose up -d`. No secret value is reproduced in this report.
- Real Google account authorization/token exchange/callback has **not** been
  completed. google=true and authorization redirect are configuration evidence,
  not proof of live Google login. Google Cloud callback/test-user configuration
  and real account UAT remain required.
- Human review/publication and deployed HTTPS acceptance remain separate. No main
  merge or integration-ready claim is made.

## Reproduction

Use [the canonical PowerShell runbook](../../docs/deployment/local-compose.md).
Initialize the required private JWT key before config/build/up. Optional Google
credentials must be supplied privately; no generated sample can substitute for
them. The runtime no longer depends on a custom ignored Compose override or JAR.
