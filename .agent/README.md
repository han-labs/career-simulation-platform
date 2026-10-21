# Career Simulation Platform `.agent` knowledge base

This folder keeps coding agents aligned with current requirements, architecture,
work allocation, quality gates and evidence. Product catalogue and official
allocation live in `docs/`; do not duplicate or renumber them here.

Read in this order:

1. `AGENTS.md` - binding workflow and scope rules.
2. `PROJECT_CONTEXT.md` - concise current snapshot and source hierarchy; older content is labelled historical.
3. `TASKS.md` - current queue/progress and blockers; ownership links to `docs/TEAM_ASSIGNMENTS.md`.
4. `DESIGN.md` - visual and interaction system.
5. Relevant files in `rules/`.
6. [Current requirements](../docs/requirements/README.md), matching selected UC/US/AC and [official assignment](../docs/TEAM_ASSIGNMENTS.md), then relevant as-built API/source/tests and a plan in `.agent/plans/`.

Report catalogue is 10 selected US / 16 UC; selected work is UC-01–09, supplementary
UC-10–16 has no new allocation, and UC-14 is deferred. Baseline v1/DR1–DR2 govern
business rules; code/contracts/tests establish as-built P1–P6/V22. Consult
[the alignment register](../docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md)
instead of silently resolving report/code conflicts.

Folder map:

- `plans/` - catalogue-qualified feature plans; retain earlier plans as historical references.
- `references/` - the supplied proposal, architecture image, and source index.
- `reports/` - implementation/evidence audits, not marketing summaries.
- `rules/` - architecture, GRASP, facade, AI, testing, report, security, and data rules.
- `templates/` - repeatable plan, trace, AI log, and report-update formats.

Never store `.env`, secrets, private student data, unrestricted model traces, or unlicensed third-party task content here. The current `.gitignore` whitelist re-includes `.agent/**`; it is not a guarantee that logs/JSON/screenshots are safe or ignored. Stage only reviewed explicit paths. Do not rewrite existing historical reports to imply fresh verification.
