# Current Use Case Catalogue — Career Simulation System (2026-10-22)

**Source priority for this dossier**

1. Current final-report working specification `MTSE-FinalProjectReport (1).docx`: **10 selected US**, **16 UC catalogue**, **9 detailed UC-01–09**.
2. Frozen Requirements Baseline v1 + approved DR1–DR2: **unchanged business-rule authority** (15 historical baseline US IDs). A report-selected US identifier must never be treated as the same identifier in Baseline v1 without explicit mapping.
3. Prior report `MTSE-FinalProjectReport_Requirements_Design_Ready_2026-10-21 (1).docx`: historical source for **UC-10–16 only** after remapping IDs, not authority to expand the 9 selected cases.
4. Source code/API tests on `origin/main`: **implementation evidence**; check current HEAD before development. P1–P6/V22 are already merged (user-reported and corroborated by historical evidence), so do not restart implementation.

**No silent renumbering:** report UC-01–09 are the nine actively assigned use cases; UC-10–16 remain in the catalogue for complete documentation and traceability. Only UC-14 (recruitment invitation) is explicitly **DEFERRED**.

| Use Case | Actor | Related *selected report* US | Owner | Scope |
|---|---|---|---|---|
| [UC-01 — Register account](UC-01-register-account.md) | Student | US-01 | Người 1 | **Selected** |
| [UC-02 — Log in](UC-02-log-in.md) | Student / Enterprise Representative / Administrator | US-02 | Người 1 | **Selected** |
| [UC-03 — Complete career quiz](UC-03-complete-career-quiz.md) | Student | US-03 | Người 1 | **Selected** |
| [UC-04 — Browse simulations](UC-04-browse-simulations.md) | Student | US-04 | Người 2 | **Selected** |
| [UC-05 — Complete simulation](UC-05-complete-simulation.md) | Student | US-05, US-06 | Người 2 | **Selected** |
| [UC-06 — Consult career agent](UC-06-consult-career-agent.md) | Student | US-07 | Người 3 | **Selected** |
| [UC-07 — Manage knowledge base](UC-07-manage-knowledge-base.md) | Administrator | US-08 | Người 3 | **Selected** |
| [UC-08 — Create AI agent](UC-08-create-ai-agent.md) | Administrator | US-09 | Người 3 | **Selected** |
| [UC-09 — View AI usage dashboard](UC-09-view-ai-usage-dashboard.md) | Administrator | US-10 | Người 2 | **Selected** |
| [UC-10 — View dashboard](UC-10-view-dashboard.md) | Student | None directly in report-selected 10 US | Unassigned | Supplementary |
| [UC-11 — Manage profile](UC-11-manage-profile.md) | Student | None directly in report-selected 10 US | Unassigned | Supplementary |
| [UC-12 — Manage scenario](UC-12-manage-scenario.md) | Enterprise Representative | Prerequisite/support for US-04/US-05 (not a dedicated selected US) | Unassigned | Supplementary |
| [UC-13 — View analytics](UC-13-view-analytics.md) | Enterprise Representative | None directly in report-selected 10 US | Unassigned | Supplementary |
| [UC-14 — Send recruitment invitation](UC-14-send-recruitment-invitation.md) | Enterprise Representative | DEFERRED — no selected US / not MVP | Unassigned | **Deferred** |
| [UC-15 — Manage account](UC-15-manage-account.md) | Administrator | Supports account security; not a dedicated selected US | Unassigned | Supplementary |
| [UC-16 — Manage platform content](UC-16-manage-platform-content.md) | Administrator | Supports assessment content; not a dedicated selected US | Unassigned | Supplementary |

## Historical UC renumbering (2026-10-21 → current)

| Prior report | Current 2026-10-22 |
|---|---|
| UC-03 View dashboard | UC-10 View dashboard |
| UC-04 Manage profile | UC-11 Manage profile |
| UC-09 Manage scenario | UC-12 Manage scenario |
| UC-10 View analytics | UC-13 View analytics |
| UC-11 Send recruitment invitation | UC-14 Send recruitment invitation (DEFERRED) |
| UC-12 Manage account | UC-15 Manage account |
| UC-13 Manage platform content | UC-16 Manage platform content |

## Scope, acceptance and ownership

- 9 detailed selected UC: UC-01–09; their source is current report §1.5.
- 7 supplementary UC: UC-10–16; their source is the 2026-10-21 report, **relabelled under current catalogue**; subject to business review before any new development.
- Selected report US-05 and US-06 both map to **UC-05 Complete simulation**.
- Team persons 1/2/3 are official role slots per the latest user allocation; confirm legal names/Git identities separately.
- No human-reviewed live AI acceptance implied by P1–P6 tests; keep AC-A09/A17 pending live/human validation.
- No secret, credentials, raw logs, local absolute paths or private Student data should be committed.

## Known alignment decisions to confirm

1. **UC-05 completion:** the detailed prose can imply final self-review is a prerequisite to completion, while approved DR2/BR-06 treats completion as all required *final submissions*; self-review unlocks next task. Reconcile text rather than change Core scoring.
2. **UC-08 naming:** catalogue uses **Create AI agent**; details cell says *Build AI agent*. Use catalogue label and preserve note.
3. **UC-06 plan editing:** editing a proposal needs compatibility with source/content-hash validation before confirmation; do not claim implementation supports arbitrary edits.
4. **UC-04 recommendations:** selected UC mentions highlighted recommendations. Confirm actual implementation and scope, or mark target-only.
5. **UC-05 async grading/rubrics:** old-style flow references evaluation pending; don't introduce automated grading of open/practical work without explicit approval.
6. **All old materials:** legacy 13/15-US and 16/19-UC references must be labelled historical, not silently overwritten or deleted.

## Repository integration check (2026-10-22)

Checked `origin/main@5486f9f`; P1–P6/V22 source is present. The official allocation was confirmed directly by the user, not inferred from the ZIP instructions. Read [requirements/source order](../README.md), [functional traceability](../traceability-matrix.md) and [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md). UC flows retain source wording; implementation tracking and discrepancies do not certify acceptance. The source report technology table says TypeScript, while production frontend is JavaScript/JSX; no language migration is authorized.
