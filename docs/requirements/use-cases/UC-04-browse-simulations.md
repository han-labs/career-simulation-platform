# UC-04 — Browse simulations

> **Status:** SELECTED / present in current report
> **Actor:** Student
> **Report-selected User Story mapping:** US-04
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 2

## Trigger
The actor wants to explore available career simulations before deciding whether to experience one.

## Description
This use case allows the actor to browse, search, filter, inspect, and compare available simulations without starting a simulation.

## Preconditions
The actor is authenticated and published simulations are available.

## Postconditions
The actor views the available simulation catalogue or the details of a selected simulation.

## Main flow
- 1. The actor opens the simulation catalogue.
- 2. The system displays published simulations with information such as IT area, role, difficulty, duration, skills, and summary.
- 3. The actor searches, browses, filters, and selects a simulation.
- 4. The system displays the selected simulation details and available actions.

## Alternative flows
- 2a. The actor has sufficient exploration evidence; the system highlights several recommended simulations while continuing to display other available simulations.
- 2b. The actor has limited exploration evidence; the system displays the general catalogue and marks recommendations as preliminary.
- 3a. The actor filters by IT area, role, level, difficulty, or another supported criterion; the system updates the displayed simulation set and returns to Main Flow Step 3.
- 3b. The actor enters a search term that matches no simulation; the system displays “No simulations match your search.” and allows the actor to modify or clear the search at Main Flow Step 3.
- 3c. The actor clears the search or filters; the system restores the default catalogue and returns to Main Flow Step 3.

## Exception flows
- 2.1. Simulation catalogue data cannot be loaded; the system displays “Simulation catalogue could not be loaded. Please refresh the page or try again later.” and returns to Main Flow Step 1.
- 4.1. The selected simulation is no longer published or accessible; the system displays “This simulation is no longer available.” and returns to Main Flow Step 2.
- 4.2. Simulation details cannot be loaded; the system displays “Simulation details could not be loaded. Please try again later.” and returns to Main Flow Step 3.

## Source alignment / implementation notes
- **Review point — recommendations:** source mentions highlighted recommendations (alternative flow). Treat as desired behavior until route/UI/test evidence confirms implementation.

## Verification tracking
- API/component: [VersionedCatalogController.java](../../../backend/src/main/java/edu/hcmute/careersim/catalog/controller/VersionedCatalogController.java); [CoreCatalogPage.jsx](../../../frontend/src/features/simulations/pages/CoreCatalogPage.jsx).
- Automated tests: [CorePackageAIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/system/migration/CorePackageAIntegrationTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

Catalogue/filter/sort/pagination source exists. Highlighted/preliminary recommendations in alternatives 2a/2b remain target-only or NOT VERIFIED for this route; see alignment D03.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
