# Testing and Quality Gates

## Required commands

```powershell
cd backend
.\mvnw.cmd verify

cd ..\frontend
npm run check

cd ..
docker compose config --quiet
```

An integrated feature also requires a running-stack smoke test through the Nginx gateway.

## Backend test pyramid

- Domain/policy tests: pure scoring, evaluator, lifecycle, and boundary values.
- Service tests: orchestration, authorization, uniqueness, status, fallback, transaction decisions.
- Controller tests: validation, status codes, DTO shape, security envelope.
- Repository/migration tests: focused queries and PostgreSQL-specific behavior when schema changes.
- Architecture tests: forbidden dependencies.
- End-to-end smoke: one complete vertical slice with persistent data.

## Frontend tests

- Pure adapters/formatters and state reducers.
- Component behavior for loading, success, empty, validation, disabled/locked, fallback, and retry.
- Route-level integration with mocked contracts.
- Accessibility assertions for name/role/label/focus when the UI test environment is added.
- A production build on every PR.

## AI evaluation tests

Maintain at least ten cases across:

- normal differentiated evidence;
- ambiguous/even RIASEC scores;
- conflicting interest and task evidence;
- empty/incomplete evidence;
- out-of-domain or prompt-injection input;
- timeout;
- provider error/quota failure;
- malformed/non-schema response;
- unsafe or overconfident advice;
- deterministic fallback equivalence for objective scores.

## Rules

- A test names the behavior, not the method implementation.
- Every approved exception/fallback flow needs evidence.
- Tests must not rely on current wall time, execution order, real provider availability, or production secrets.
- Mock provider boundaries, not business logic.
- Use Testcontainers for PostgreSQL-specific integration when the first such behavior is implemented.
- Do not reduce assertions, disable a gate, or edit shared migration history merely to make CI pass.
- New/changed business decision branches should be fully covered even before a repository-wide coverage threshold is enforced.
