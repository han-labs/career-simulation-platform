# US-04 — Explore simulations: Traceability Matrix

**Selected use case:** [UC-04 — Browse simulations]
**Owner:** Person 2  
**Purpose:** Trace the current report acceptance criteria to the implementation and executable evidence. This matrix records implementation coverage; it is not human UAT sign-off.

## Status legend

| Status | Meaning |
|---|---|
| Implemented | Current production code supports the criterion and automated evidence exists. |
| Partial | A meaningful part is implemented, but a requirement or final evidence remains incomplete. |
| Pending UAT | Automated coverage exists, but current human/browser acceptance is not complete. |

## Acceptance-criteria trace

| ID | US-04 acceptance criterion | API / UI behavior | Primary implementation | Automated evidence | Current status / notes |
|---|---|---|---|---|---|
| US04-AC01 | Browse available simulations without first completing a career quiz. | Public `GET /v2/scenarios`; catalogue route `/simulations`; authentication begins only when Start is selected. | `backend/src/main/java/edu/hcmute/careersim/catalog/controller/VersionedCatalogController.java`; `backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/ScenarioAuthoringServiceImpl.java`; `frontend/src/features/simulations/pages/CoreCatalogPage.jsx`; `frontend/src/features/simulations/pages/CoreSimulationPage.jsx` | `VersionedCatalogControllerTest`; `coreBrowse.test.jsx` — public metadata and unauthenticated Start/return-path cases; `NineScenarioSeedMigrationIntegrationTest` — nine AVAILABLE/IT/PUBLISHED scenarios. | **Implemented.** Matches PO-approved public catalogue/detail decision D09. |
| US04-AC02 | Search, filter, sort, and navigate paginated catalogue results. | `GET /v2/scenarios?paged=true` accepts `q`, `careerTrack`, `difficulty`, `industry`, `sort`, `cursor`, and `limit`; UI keeps criteria in URL, fixes Industry to IT, derives Career Track options from API pages, and resets cursor when criteria change. | `VersionedCatalogController.java`; `backend/src/main/java/edu/hcmute/careersim/simulation/dao/ScenarioReadDao.java`; `CoreCatalogPage.jsx`; `frontend/src/features/simulations/api/simulationCatalogApi.js` | `CorePackageAIntegrationTest.filteredCatalogueUsesStableCursorAndEnterpriseStatisticsNeverReturnPrivateWork`; `VersionedCatalogControllerTest.optionalIndustryIsForwardedWithoutChangingAbsentFilter`; `coreBrowse.test.jsx` URL/filter/career-track/pagination cases. | **Implemented; pagination UAT partial.** Cursor behavior is automated; the nine-record browser dataset does not exercise a next page at the default page size. |
| US04-AC03 | Open a simulation and see its role, expected tasks, resources, and difficulty. | `GET /v2/scenarios/{slug}` returns allowlisted public metadata, public media, task outline, and public skill names; detail page renders role, duration, difficulty, overview, tasks, skills, and available video. | `VersionedCatalogController.java`; `backend/src/main/java/edu/hcmute/careersim/simulation/dto/ScenarioContracts.java`; `backend/src/main/java/edu/hcmute/careersim/simulation/dao/ScenarioDao.java`; `CoreSimulationPage.jsx`; `frontend/src/features/simulations/components/TaskRichText.jsx` | `VersionedCatalogControllerTest.publicDetailMapsMediaAndWorkplaceDescriptionWithoutLockedFields`; `coreBrowse.test.jsx` public-detail, task-outline, media and learning-objective cases; `TaskRichText.test.jsx`. | **Implemented.** Public resource visibility follows release policy; protected answer resources remain excluded before final submission. |
| US04-AC04 | Receive a useful empty state when no simulation matches. | Catalogue distinguishes loading, no-match, no-published-data and retryable load error states; filters can be cleared. | `CoreCatalogPage.jsx`; `frontend/src/features/simulations/components/SimulationCatalog.jsx` | `coreBrowse.test.jsx` — `distinguishes loading, no-match, and retryable error states`. | **Implemented.** |
| US04-AC05 | Browse without seeing locked model answers. | Public catalogue/detail DTO contains metadata, public task outline, skills and media only. Model answers use the authenticated enrollment endpoint and require a valid FINAL submission. | `ScenarioContracts.java`; `ScenarioDao.java`; `backend/src/main/java/edu/hcmute/careersim/simulation/controller/EnrollmentController.java`; `backend/src/main/java/edu/hcmute/careersim/simulation/service/impl/TaskSubmissionServiceImpl.java` | `VersionedCatalogControllerTest.publicDetailMapsMediaAndWorkplaceDescriptionWithoutLockedFields`; `CorePackageAIntegrationTest.answerAttachmentsAreAssociationAuthorizedAndReleaseOnlyAfterFinal`; `coreBrowse.test.jsx` public projection checks. | **Implemented.** |

## Use-case flow and exception trace

| UC-04 flow | Implementation / evidence | Status |
|---|---|---|
| Main 1–4: open catalogue, view published cards, search/filter/select, view detail/actions | `CoreCatalogPage.jsx` → versioned catalogue API → `CoreSimulationPage.jsx`; covered by `coreBrowse.test.jsx` and controller/integration tests. | Implemented |
| Alt 2a/2b: highlighted or preliminary recommendations | Excluded from Feature 1 by PO decision D09; general catalogue remains available. | Out of approved US-04 delivery scope |
| Alt 3a/3c: apply or clear supported filters | URL-backed filters and server query in `CoreCatalogPage.jsx`; automated filter/reset coverage. | Implemented |
| Alt 3b: no search match | Explicit no-match state and clear-filter action. | Implemented |
| Exception 2.1: catalogue load failure | Retryable catalogue error UI. | Implemented |
| Exception 4.1: simulation unavailable | Detail request maps missing/unavailable content to an unavailable message and catalogue return path. | Implemented; browser revalidation pending |
| Exception 4.2: detail load failure | Retry control in `CoreSimulationPage.jsx`. | Implemented |

## Data and release trace

| Concern | Current implementation | Evidence |
|---|---|---|
| Published catalogue data | `V32__seed_nine_versioned_scenarios.sql` installs exactly nine approved IT scenarios only on an empty versioned catalogue and fails closed on partial/conflicting reference state. | `NineScenarioSeedMigrationIntegrationTest` fresh-install and rollback cases. |
| Public-data boundary | Catalogue projection strips provenance and excludes submissions, model answers and private evidence. | Public DTO/controller tests and owner-only integration tests. |
| Current browser evidence | Screenshots and execution notes are stored under `docs/evidence/US-04/`. | `docs/evidence/US-04/us-04-browse-simulations.md`. |

## Remaining verification

- Exercise cursor Next/Previous in browser with more than one page of published results.
- Re-run unavailable-detail and transient API-error cases against the deployed gateway.
- Record final human reviewer approval separately; automated evidence is not UAT sign-off.

