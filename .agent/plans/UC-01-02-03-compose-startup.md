# UC-01/02/03 Docker startup repair

Status: Implemented and locally verified; human review/live Google UAT pending.
Date: 2026-10-24.

## 1 Scope

Person 1 identity/assessment maintenance, requested by the project lead; human
review remains required before publication. Actors: local developer and Student.
Trace: current selected US-01 registration, US-02 login and US-03 saved quiz
results; UC-01/02/03; frozen P1/P2 authentication contracts. Preserve all scoring,
ownership, OAuth, CSRF, JWT and V1–V22. No feature expansion or UI changes.

## 2 Flows

Main: initialize ignored local configuration, supply required credentials, validate
Compose, build, start PostgreSQL/backend/frontend/gateway, exercise auth and quiz.
Alternative: existing .env and stable signing key remain unchanged. Google is
optional in the template and remains enabled in the user's configured runtime.
Exception: missing key fails Compose early; malformed/short keys still fail closed
in Spring. Missing Google credentials remain an error when Google is enabled.

## 3 Responsibility design

Compose owns readiness/dependencies; local setup owns cryptographic key generation
without printing secrets. Existing BrowserAuthenticationImpl and GoogleOAuthFlow
remain security authorities; no new service or configuration abstraction.

## 4 Change surface

Compose, .env.example, local setup script, focused startup regression tests and
deployment instructions/evidence. No controller, facade, DAO, schema, migration,
assessment logic, frontend component, API or architecture topology changes.

## 5 API contract

Unchanged: /api/health, /api/v2/auth/providers, registration/login/CSRF/current actor,
and versioned assessment attempt/result APIs. Existing authorization errors remain.

## 6 Security/privacy/AI

Restore the previous private stable JWT key to the blank local setting; preserve
Google credentials in .env and remove them from the public template. Never log
secrets or render resolved Compose config to shared output. No ephemeral-key,
CSRF, authorization or DB validation bypass. AI stays as configured; no paid calls.

## 7 Tests/evidence

Test JWT startup boundaries, Google configuration and setup idempotence; execute
Compose config/build/up/ps, logs/health, isolated fresh volumes and registration,
JWT/CSRF/ownership/assessment persistence smoke. Preserve valuable volumes and
record exact executed outcomes, full-suite failures and external OAuth limits.

## 8 Decisions

User selected .codex-tmp/us01-us03 as the long-term Compose directory. Root checkout
and original US-03 branch stay intact. No automatic commit, push or merge.
