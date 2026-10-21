# UC-14 — Send recruitment invitation

> **Status:** DEFERRED / supplementary historical specification
> **Actor:** Enterprise Representative
> **Report-selected User Story mapping:** DEFERRED — no selected US / not MVP
> **Specification source:** Prior 2026-10-21 report, original UC-11 (renumbered to current catalogue)
> **Module owner:** NOT ASSIGNED — supporting/deferred UC

## Trigger
The actor wishes to invite a learner into a downstream recruitment process.

## Description
Future optional recruitment workflow, retained in the full domain catalogue but outside the approved final-project MVP.

## Preconditions
DEFERRED: approved recruitment workflow and required user sharing scope are not active in the MVP.

## Postconditions
DEFERRED: no invitation is created by this release.

## Main flow
- 1. The actor attempts to open the recruitment invitation capability.
- 2. The system identifies the capability as unavailable in the current scope and directs the actor to supported enterprise functions.

## Alternative flows
- 1a. The actor selects available simulation analytics instead. The system opens aggregate analytics and ends the deferred use case.

## Exception flows
- 2.1. The system displays the proposed message “Recruitment invitations are not available in this release.” No participant information is disclosed and no invitation is created.

## Source alignment / implementation notes
- **Deferred:** not to be implemented, demonstrated or counted toward 9 selected UC acceptance without explicit new scope approval.
- **Status:** transferred from an earlier approved working draft. The 2026-10-22 report contains only the catalogue row; this is **not** a newly approved detailed specification. Business owner/Chat 3 should reconcile wording before assigning development.

## Verification tracking
- API/component: **No implementation assignment — DEFERRED; proposed unavailable-message flow is not claimed as shipped UI.**
- Automated tests: **No new feature acceptance claimed for this deferred use case.**
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**
