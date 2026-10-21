# UC-13 — View analytics

> **Status:** SUPPLEMENTARY / historical draft; NOT part of 9 selected detailed cases
> **Actor:** Enterprise Representative
> **Report-selected User Story mapping:** None directly in report-selected 10 US
> **Specification source:** Prior 2026-10-21 report, original UC-10 (renumbered to current catalogue)
> **Module owner:** NOT ASSIGNED — supplementary supporting UC, not a new feature allocation

## Trigger
The actor wants aggregate information about its simulations.

## Description
Shows only enterprise-scoped participation/completion metrics rather than private Student evidence.

## Preconditions
The actor is authenticated and authorized for the selected enterprise.

## Postconditions
The actor sees scoped aggregate data or an empty-state explanation.

## Main flow
- 1. The actor opens the enterprise analytics page and selects owned scenarios or supported filters.
- 2. The system calculates and displays permitted aggregate participation and completion information.

## Alternative flows
- 1a. The actor clears filters. The system restores the default authorized analytics view and returns to Main Flow Step 1.
- 2a. No completed activities match the selected filter. The system displays “No activity is available for this selection.” The actor may change filters at Main Flow Step 1.

## Exception flows
- 2.1. If the actor selects another enterprise’s scenario, the system displays “You do not have permission to view these analytics.” No private participant data is returned; the actor remains on the analytics page.
- 2.2. If aggregation fails, the system displays “Analytics are temporarily unavailable. Please try again.” The actor may retry Main Flow Step 2.

## Source alignment / implementation notes
- **Status:** transferred from an earlier approved working draft. The 2026-10-22 report contains only the catalogue row; this is **not** a newly approved detailed specification. Business owner/Chat 3 should reconcile wording before assigning development.

## Verification tracking
- API/component: **TBD after fresh repository comparison**
- Automated tests: **TBD — link exact path and result**
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**
