# UC-12 — Manage scenario

> **Status:** SUPPLEMENTARY / historical draft; NOT part of 9 selected detailed cases
> **Actor:** Enterprise Representative
> **Report-selected User Story mapping:** Prerequisite/support for US-04/US-05 (not a dedicated selected US)
> **Specification source:** Prior 2026-10-21 report, original UC-09 (renumbered to current catalogue)
> **Module owner:** NOT ASSIGNED — supplementary supporting UC, not a new feature allocation

## Trigger
The actor wants to author or publish a simulation.

## Description
Manages authorized enterprise-owned scenario drafts, ordered tasks, answer examples, preview and published versions.

## Preconditions
The actor has current enterprise membership and scenario publication rights.

## Postconditions
Valid scenario versions are published; incomplete or unauthorized drafts remain unpublished.

## Main flow
- 1. The actor opens an owned scenario, edits content, tasks, resources and allowed evaluation settings, and requests Preview or Publish.
- 2. The system validates the scenario version, preserves the draft or publishes an eligible immutable version, and displays its current status.

## Alternative flows
- 1a. The actor saves incomplete content as Draft. The system persists the draft without making it visible in the published Student catalogue, then returns to Main Flow Step 1.
- 1b. The actor reorders tasks before publication. The system validates the new order and returns to Main Flow Step 2.

## Exception flows
- 2.1. If required instructions, deliverables or model answers are missing, the system highlights the affected task and displays “This scenario cannot be published. Complete the required task information.” The actor stays in the editor and returns to Main Flow Step 1.
- 2.2. If the scenario belongs to another enterprise, the system displays “You do not have permission to manage this scenario.” No content changes are saved.
- 2.3. If publication fails, the system displays “Scenario publication failed. Your previous published version remains available.” The actor returns to the draft editor.

## Source alignment / implementation notes
- **Status:** transferred from an earlier approved working draft. The 2026-10-22 report contains only the catalogue row; this is **not** a newly approved detailed specification. Business owner/Chat 3 should reconcile wording before assigning development.

## Verification tracking
- API/component: **TBD after fresh repository comparison**
- Automated tests: **TBD — link exact path and result**
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**
