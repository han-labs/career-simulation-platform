# UC-15 — Manage account

> **Status:** SUPPLEMENTARY / historical draft; NOT part of 9 selected detailed cases
> **Actor:** Administrator
> **Report-selected User Story mapping:** Supports account security; not a dedicated selected US
> **Specification source:** Prior 2026-10-21 report, original UC-12 (renumbered to current catalogue)
> **Module owner:** NOT ASSIGNED — supplementary supporting UC, not a new feature allocation

## Trigger
The actor wants to manage a CareerSim account.

## Description
Enables approved account status and role/enterprise access management with audit constraints.

## Preconditions
The actor has an active Administrator role.

## Postconditions
Permitted account changes are applied and audited; prior valid state persists on failure.

## Main flow
- 1. The actor searches for an account and confirms an authorized change.
- 2. The system validates the requested action, updates account permissions or status, and records the administrative result.

## Alternative flows
- 1a. The actor cancels before confirmation. The system leaves the account unchanged and returns to the account list.
- 1b. The actor selects an account already in the requested state. The system reports that no update is necessary.

## Exception flows
- 2.1. If the action is unauthorized or targets an invalid account, the system displays “This account action is not permitted.” No state change occurs; the actor returns to Main Flow Step 1.
- 2.2. If saving fails, the system displays “Account changes could not be saved. Please try again.” The previous account state remains effective.

## Source alignment / implementation notes
- **Status:** transferred from an earlier approved working draft. The 2026-10-22 report contains only the catalogue row; this is **not** a newly approved detailed specification. Business owner/Chat 3 should reconcile wording before assigning development.

## Verification tracking
- API/component: **TBD after fresh repository comparison**
- Automated tests: **TBD — link exact path and result**
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**
