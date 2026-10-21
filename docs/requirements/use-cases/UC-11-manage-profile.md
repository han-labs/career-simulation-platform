# UC-11 — Manage profile

> **Status:** SUPPLEMENTARY / historical draft; NOT part of 9 selected detailed cases
> **Actor:** Student
> **Report-selected User Story mapping:** None directly in report-selected 10 US
> **Specification source:** Prior 2026-10-21 report, original UC-04 (renumbered to current catalogue)
> **Module owner:** NOT ASSIGNED — supplementary supporting UC, not a new feature allocation

## Trigger
The actor wants to maintain exploration information.

## Description
Allows owner-scoped personal context and goals to be reviewed or updated.

## Preconditions
The actor is authenticated.

## Postconditions
Valid profile changes are saved for authorized exploration use.

## Main flow
- 1. The actor opens the profile editor, updates the desired fields, and selects Save.
- 2. The system validates the changed fields, stores permitted values, and displays the updated profile.

## Alternative flows
- 1a. The actor cancels before saving. The system discards unsaved changes and returns the actor to the profile view.
- 1b. The actor leaves an optional field blank. The system saves other valid changes and continues at Main Flow Step 2.

## Exception flows
- 2.1. If a field fails validation, the system highlights it and displays “Please correct the highlighted profile information.” The actor stays in the editor and returns to Main Flow Step 1.
- 2.2. If the update fails, the system displays “Profile changes could not be saved. Please try again.” The previous stored profile is preserved and the actor returns to Main Flow Step 1.

## Source alignment / implementation notes
- **Status:** transferred from an earlier approved working draft. The 2026-10-22 report contains only the catalogue row; this is **not** a newly approved detailed specification. Business owner/Chat 3 should reconcile wording before assigning development.

## Verification tracking
- API/component: **TBD after fresh repository comparison**
- Automated tests: **TBD — link exact path and result**
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**
