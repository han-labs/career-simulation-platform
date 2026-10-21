# UC-16 — Manage platform content

> **Status:** SUPPLEMENTARY / historical draft; NOT part of 9 selected detailed cases
> **Actor:** Administrator
> **Report-selected User Story mapping:** Supports assessment content; not a dedicated selected US
> **Specification source:** Prior 2026-10-21 report, original UC-13 (renumbered to current catalogue)
> **Module owner:** NOT ASSIGNED — supplementary supporting UC, not a new feature allocation

## Trigger
The actor wants to manage published platform content.

## Description
Maintains authorized assessment definitions and other administered platform content.

## Preconditions
The actor has current Administrator privileges.

## Postconditions
Valid content changes are stored within their permitted lifecycle state.

## Main flow
- 1. The actor selects an assessment or platform content item, edits it, and requests Save or Publish.
- 2. The system validates versioned content and access rights, saves an eligible draft or makes a valid version available.

## Alternative flows
- 1a. The actor saves incomplete content as Draft. The system records the draft state and returns to Main Flow Step 1.
- 1b. The actor cancels the edit. The system retains the previous version and returns to the content list.

## Exception flows
- 2.1. If publication requirements are missing, the system displays “Complete the required content fields before publishing.” The invalid fields are highlighted; the actor returns to Main Flow Step 1.
- 2.2. If the actor lacks permission, the system displays “You do not have permission to change this content.” No update is stored.

## Source alignment / implementation notes
- **Status:** transferred from an earlier approved working draft. The 2026-10-22 report contains only the catalogue row; this is **not** a newly approved detailed specification. Business owner/Chat 3 should reconcile wording before assigning development.

## Verification tracking
- API/component: **TBD after fresh repository comparison**
- Automated tests: **TBD — link exact path and result**
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**
