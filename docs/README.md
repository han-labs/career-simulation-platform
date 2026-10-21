# Project documentation

This folder contains human-facing requirements, design, contracts and evidence.
The `.agent` folder defines coding workflow; it is not a competing requirements catalogue.

## Current entry points (2026-10-22)

1. [Requirements and source authority](requirements/README.md): current report **10 selected US / 16 UC**, with UC-01–09 assigned and UC-14 deferred.
2. [Official team assignments](TEAM_ASSIGNMENTS.md): Person 1 UC-01–03; Person 2 UC-04/05/09; Person 3 UC-06–08. Exact names/Git aliases/reviewer rota are not inferred.
3. [Report/code alignment and unresolved decisions](requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md).
4. [As-built P1–P6/V22 architecture](architecture/README.md), [API](api/README.md), [database](database/README.md), [deployment](deployment/README.md).
5. [Root AI_USAGE](../AI_USAGE.md) and [documentation refresh checklist](REPO_DOCS_REFRESH_CHECKLIST.md).

Frozen Baseline v1 + DR1–DR2 remain the business-rule authority. Legacy 3-US,
Baseline 15-US and former 13-US report IDs must not be treated as current report IDs.
Detailed supplementary UC specs are historical transfers, not new work allocations.

| Area | Purpose |
|---|---|
| `architecture/` | Implemented system structure, module boundaries, and dependency direction |
| `requirements/` | User stories, acceptance criteria, and traceability matrix |
| `api/` | API envelope, endpoint inventory, and contract rules |
| `database/` | PostgreSQL schema, migration, and data-handling rules |
| `deployment/` | Local and Ubuntu VM deployment procedures |
| `data/` | Simulation-content provenance and research rules |
| `evidence/` | Reproducible evidence expected for the course report |

Do not treat a diagram or report paragraph as complete when the code behaves differently. Record the discrepancy, resolve it with the team, and update the full traceability chain.
