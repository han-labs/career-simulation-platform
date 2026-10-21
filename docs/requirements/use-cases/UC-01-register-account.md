# UC-01 — Register account

> **Status:** SELECTED / present in current report
> **Actor:** Student
> **Report-selected User Story mapping:** US-01
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 1

## Trigger
The actor wishes to open a personal CareerSim account.

## Description
Creates a personal account through the supported registration or identity flow.

## Preconditions
The actor is not authenticated and registration is available.

## Postconditions
A valid personal account is created; cancellation creates no account.

## Main flow
- 1. The actor opens account registration, supplies the required identity details, and requests account creation.
- 2. The system validates account eligibility and uniqueness, creates an authorized account, and displays the next sign-in or onboarding action.

## Alternative flows
- 1a. The actor cancels registration on the registration page. The system discards any unsubmitted values, returns the actor to the landing page, and ends the use case.
- 1b. The actor omits optional profile fields. The system accepts the remaining valid information and continues at Main Flow Step 2.

## Exception flows
- 2.1. If a required field is missing, the system highlights that field and displays the proposed message “Please complete the required fields.” The actor remains on the registration page and returns to Main Flow Step 1.
- 2.2. If the account already exists, the system displays the proposed message “An account with this email already exists. Please sign in.” No duplicate is created; the actor returns to Main Flow Step 1.
- 2.3. If the identity service is unavailable, the system displays the proposed message “Registration is temporarily unavailable. Please try again later.” No partial account is presented as complete.

## Source alignment / implementation notes
- Preserve the source specification; verify exact API/UI behavior and automated tests against current `main` before marking as accepted.

## Verification tracking
- API/component: [IdentityController.java](../../../backend/src/main/java/edu/hcmute/careersim/identity/controller/IdentityController.java); [AccountPage.jsx](../../../frontend/src/features/identity/AccountPage.jsx).
- Automated tests: [AuthenticationContractTest.java](../../../backend/src/test/java/edu/hcmute/careersim/identity/controller/AuthenticationContractTest.java), [BrowserCredentialValidationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/identity/service/BrowserCredentialValidationTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

Local registration/validation source exists. Identity linking and registration never imply automatic email-based role, membership or consent grants.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
