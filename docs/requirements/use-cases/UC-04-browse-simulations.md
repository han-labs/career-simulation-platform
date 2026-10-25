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
- **PO-approved Feature 1 decision:** catalogue/detail are public, authentication starts at Start, and recommendations are excluded. See alignment **D09**, with D03 pointing to the same decision. Imported source flows above are preserved for provenance.
- Career Track is a native dropdown populated from public API data across pages; Industry is fixed to IT. Filtering remains server-side.

## Verification tracking
- API/component: [VersionedCatalogController.java](../../../backend/src/main/java/edu/hcmute/careersim/catalog/controller/VersionedCatalogController.java); [CoreCatalogPage.jsx](../../../frontend/src/features/simulations/pages/CoreCatalogPage.jsx).
- Automated tests (2026-10-25): focused frontend **19 PASS**; focused backend **14 PASS**, including public DTO checks, isolated PostgreSQL cursor pagination and migration tests. See evidence for exact commands and coverage limits.
- Evidence: [US-04 Browse Simulations evidence](../../evidence/US-04/us-04-browse-simulations.md) and `.agent/reports/us-04-uc-04-browse-simulations-verification.vi.md`.
- Browser/UAT: **PO-reported**, 2026-10-25: **14 PASS / 1 PARTIAL / 1 BLOCKED / 0 FAIL**. TC-04-04A dropdown passed all 8 checks; TC-04-05 next-page navigation was not exercised with nine records; TC-04-12 real Google OAuth remains blocked by local configuration. Codex did not directly observe the PO's full UAT session.
- Human UAT results received; final integration/redistribution review and Git publication approval remain pending.

## Current repository comparison (2026-10-22)

Catalogue/filter/sort/pagination and public detail source now exist on `feature/browse-simulations`. The PO explicitly excluded highlighted/preliminary recommendations in alternatives 2a/2b from Feature 1; see alignment D09 (D03 is resolved by that decision). The PO also approved public catalogue/detail access, which supersedes this UC's authenticated precondition for this feature; authentication begins at Start.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
