# AI assistance — report-selected US-01 / UC-01

Date: 2026-10-23. Student / Person 1; human security review and UAT sign-off pending.

Read the required `.agent` workflow/rules, selected US/UC sources, frozen identity rules, assignments, P1/P2 architecture and existing code/tests. Codex assisted with the plan, registration form/responsive CSS, EN/VI feedback, session lookup/retry, duplicate-email feedback/null-role validation, tests and a loopback-only gateway smoke script. Existing JWT/OIDC/CSRF, consent, scoring, assessment code and V1–V22 are reused.

Rejected: replacing P1 authentication; branching from US-03; switching/stashing/committing the original checkout; using its database; claiming fixture OIDC as live Google. No product AI calls.

Executed before implementation commit: frontend `npm run check` PASS (107 tests, lint/build); focused Maven `spotless:apply verify` PASS (15 registration, OIDC/assessment, service and existing gateway tests); Compose config, JS syntax and whitespace checks PASS. Initial sandbox worker timeouts, field-focus and translation issues, a wrong new result-field assertion and missing built-bundle prerequisite were investigated and corrected. One overlapping local build failed EBUSY; the subsequent sequential complete frontend check passed. No tests weakened/disabled. Combined-feature evidence is recorded in the completion report.

Agent code/evidence review performed; no human sign-off claimed. Raw logs and credentials remain ignored. Security ownership remains with Person 1/project lead under official assignments.
