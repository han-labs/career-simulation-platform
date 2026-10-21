# UC-02 — Log in

> **Status:** SELECTED / present in current report
> **Actor:** Student / Enterprise Representative / Administrator
> **Report-selected User Story mapping:** US-02
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 1

## Trigger
The actor wants to access a protected CareerSim function.

## Description
Authenticates a permitted account and applies role-appropriate access.

## Preconditions
The actor has an eligible identity; the authentication service is available.

## Postconditions
The actor receives an authorized session or an explicit unsuccessful-login outcome.

## Main flow
- 1. The actor selects the configured sign-in method and completes the identity-provider authentication.
- 2. The system verifies the identity, account state, and role, establishes protected API access, and opens the authorized landing page.

## Alternative flows
- 1a. The actor cancels at the identity provider. The system creates no authenticated CareerSim session, displays the sign-in screen, and ends the use case.
- 2a. The actor uses a permitted sign-out action. The system invalidates the current access session and returns to the sign-in page.

## Exception flows
- 2.1. If the identity token is missing, invalid, or expired, the system displays the proposed message “Your sign-in could not be verified. Please sign in again.” The actor remains unauthenticated and returns to Main Flow Step 1.
- 2.2. If the account is disabled or the role is not authorized, the system displays “You do not have permission to access this account.” The system denies access; the actor returns to Main Flow Step 1.
- 2.3. If the provider is unavailable, the system displays “Sign-in is temporarily unavailable. Please try again later.” No protected information is exposed.

## Source alignment / implementation notes
- Preserve the source specification; verify exact API/UI behavior and automated tests against current `main` before marking as accepted.

## Verification tracking
- API/component: [IdentityController.java](../../../backend/src/main/java/edu/hcmute/careersim/identity/controller/IdentityController.java); [AccountPage.jsx](../../../frontend/src/features/identity/AccountPage.jsx).
- Automated tests: [AuthenticationContractTest.java](../../../backend/src/test/java/edu/hcmute/careersim/identity/controller/AuthenticationContractTest.java), [BrowserCredentialValidationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/identity/service/BrowserCredentialValidationTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

Google flow/JWT/CSRF and current-account contracts are implemented; real Google/HTTPS callback UAT remains pending. Ordinary logout revokes the current credential, not every device.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
