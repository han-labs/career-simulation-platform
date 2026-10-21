# UC-10 — View dashboard

> **Status:** SUPPLEMENTARY / historical draft; NOT part of 9 selected detailed cases
> **Actor:** Student
> **Report-selected User Story mapping:** None directly in report-selected 10 US
> **Specification source:** Prior 2026-10-21 report, original UC-03 (renumbered to current catalogue)
> **Module owner:** NOT ASSIGNED — supplementary supporting UC, not a new feature allocation

## Trigger
The actor wants to review exploration progress.

## Description
Displays owner-scoped profile, assessment, simulation, and available AI exploration information.

## Preconditions
The actor is authenticated.

## Postconditions
The actor views the latest authorized dashboard information.

## Main flow
- 1. The actor opens the Student dashboard and selects the information or exploration action of interest.
- 2. The system loads the actor’s permitted assessment, simulation, and recent exploration summaries and offers relevant navigation.

## Alternative flows
- 2a. If the actor has no assessment or simulation activity, the system displays an appropriate empty state and the next available actions, then continues at Main Flow Step 2.
- 1a. The actor opens a completed result. The system displays the eligible owner-scoped result and returns the actor to the chosen exploration page.

## Exception flows
- 2.1. If a dashboard data source fails, the system displays “This section is temporarily unavailable. Please refresh or try again later.” The actor remains on the dashboard and can retry Main Flow Step 2.
- 2.2. If a selected result is unavailable or unauthorized, the system displays “This result is no longer available to your account.” No private content is returned; the actor returns to Main Flow Step 1.

## Source alignment / implementation notes
- **Status:** transferred from an earlier approved working draft. The 2026-10-22 report contains only the catalogue row; this is **not** a newly approved detailed specification. Business owner/Chat 3 should reconcile wording before assigning development.

## Verification tracking
- API/component: **TBD after fresh repository comparison**
- Automated tests: **TBD — link exact path and result**
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**
