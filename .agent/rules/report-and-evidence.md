# Report and Evidence Rules

## Evidence before prose

Do not write “implemented,” “tested,” “deployed,” “accurate,” or “fast” until evidence exists. Every report claim should point to a commit, command output, test, API capture, screenshot, migration record, or measured evaluation result.

## Required feature report sections

1. Scope and source requirement.
2. Implemented main, alternate, exception, and fallback flows.
3. Sequence-to-code and design-to-code mapping.
4. API and database impact.
5. Security/privacy and AI-boundary decisions.
6. Tests and exact commands run.
7. Manual/integration evidence.
8. Deviations, unresolved gaps, and next action.
9. Files/artifacts changed.

## Architecture diagram rule

The diagram must match the running deployment and real code boundaries. It must show:

- browser actors;
- Nginx, React, Spring Boot, PostgreSQL, and persistent volume;
- backend-owned AI adapter and external LLM;
- direction of HTTPS/REST/JPA-provider calls;
- no direct frontend-to-database, frontend-to-LLM, or LLM-to-database path;
- deterministic fallback;
- deployment host/container boundary when used in the report.

When code/topology changes, update `docs/architecture/README.md` and the report figure together.

## AI evidence

- Record executed prompt, actual response/result, exact model identifier, timestamp, measured latency method, and pass/fail criteria.
- Preserve the minimum necessary sanitized evidence.
- Separate smoke-test success from product-quality evaluation.
- Use at least ten product evaluation cases and summarize failure analysis.
- Never copy the proposal's planned or example measurement into the final result as if observed.

## Final submission checklist

- Repository URL and exact commit SHA.
- Clean clone/setup reproduction.
- Passed backend/frontend/Compose gates.
- Persistent database and migration evidence.
- Complete student vertical-slice evidence.
- AI failure and fallback evidence.
- Architecture and sequence diagrams reconciled.
- AI assistance log and human review evidence.
- Ubuntu VM/Docker Compose evidence or explicit documented blocker.

Use `templates/report-update.md` and store audits under `reports/`.
