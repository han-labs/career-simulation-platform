# TEAM_ASSIGNMENTS — Current allocation (2026-10-25)

> **Status:** **OFFICIAL UC/person and selected-US allocation**, updated directly by the Team Lead on 2026-10-25, superseding the 2026-10-22 allocation. Exact name/Git identity mapping and the proposed reviewer rotation still require group sign-off. This allocation covers integration/UAT work on **already merged P1–P6**, not permission to rewrite Core/AI modules. The current `feat/uc07-admin-knowledge-base` branch implements **UC-07 / US-08 only**. The Team Lead has authorized Phase 2 finalization and a minimal common Admin Studio shell exposing UC-07/08/09 routes; this does not authorize UC-08/09 feature implementation.
>
> **Source of truth:** current `docs/requirements/use-cases/README.md` and `docs/requirements/user-stories/REPORT_SELECTED_10_US.md`, supported by the frozen Baseline v1 + DR1–DR2.

## Assignment for the nine selected use cases

| Person | Functional area | Owned UC | Covering selected report US | Delivery responsibility |
|---|---|---|---|---|
| **Person 1** | Identity & Assessment | UC-01 Register account; UC-02 Log in; UC-03 Complete career quiz | US-01, US-02, US-03 | Inspect/complete assigned UI and API integration, auth/assessment correctness, role isolation, test evidence, UAT |
| **Person 2** | Simulation & Student Agent | UC-04 Browse simulations; UC-05 Complete simulation; UC-06 Consult career agent | US-04, US-05, **US-06**, US-07 | Catalogue/tasks/results/self-review and Student Agent conversations/SSE/tools/report/plan; integration, tests and UAT |
| **Person 3 (project lead)** | Admin AI Platform & Usage | UC-07 Manage knowledge base; UC-08 Create AI agent; UC-09 View AI usage dashboard | US-08, US-09, US-10 | KB ingestion/test, Builder preview/publish/assignment and privacy-safe Admin usage; integration/release coordination |

**Reviewers (proposed; confirm):** Person 2 reviews Person 1; Person 3 reviews Person 2; Person 1 reviews Person 3. For security/privacy/schema changes, also request project-lead review even where this repeats a reviewer.

## Name & Git alias sign-off (do not infer from commit count)

| Person | Legal team-member name | GitHub alias / email as approved for sharing | Status |
|---|---|---|---|
| Person 1 | **TO CONFIRM** | **TO CONFIRM** | Pending team confirmation |
| Person 2 | **TO CONFIRM** | **TO CONFIRM** | Pending team confirmation |
| Person 3 | **TO CONFIRM** | **TO CONFIRM** | Pending team confirmation |

## Supplementary UC catalogue — not assigned for new feature work

- UC-10 Student dashboard; UC-11 Student profile; UC-12 Enterprise scenario authoring; UC-13 Enterprise analytics; UC-15 Admin account management; UC-16 Admin platform content management: preserve as **supporting existing capabilities**. No separate current selected US or assigned person. Report exact maintenance owner after joint review; do not silently transfer to one of the three.
- UC-14 Send recruitment invitation: **DEFERRED**, excluded from MVP implementation/acceptance. No new feature assignment.

## Definition of Done per assigned UC

1. Current selected report US acceptance criteria reviewed and checked against **existing** behavior on updated main.
2. Relevant API/React component matched to exact source files and tests; missing gaps logged without fabricating functionality.
3. No unauthorized scope changes, unreviewed schema rewrite, modified historical Flyway migration or secret/PII exposure.
4. Automated checks relevant to changed files PASS; UAT evidence linked separately. AI live/human AC-A09/A17 remain pending until actually completed.
5. Human reviewer approves PR; owner updates shared `AI_USAGE.md` accurately for AI-assisted changes; project lead coordinates integration.

## Coordination model

- Each person creates a feature/fix/docs branch from **latest `main`**, works only within allocated scope and agrees on backend API contracts before changing shared interfaces.
- Prefer narrow PRs. CI → review → merge → delete remote feature branch after confirmed merge; history on main/PR is preserved.
- **Shared or cross-cutting tasks** (JWT/OAuth, DB migrations, Nginx, security, global navigation) require project-lead review and identified owner before work starts.
- Do not use the old 13-US or frozen Baseline 15-US ID numbers as direct identifiers for selected report US without an explicit mapping.
