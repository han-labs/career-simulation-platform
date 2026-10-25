# US-04 Browse Simulations Evidence

## Final verification and PO-reported UAT — 2026-10-25

The PO reported completing manual browser UAT on 2026-10-25: **14 PASS, 1 PARTIAL, 1 BLOCKED, 0 FAIL**. Codex received these results; Codex did not directly observe the PO's full UAT session or independently certify all cases.

| Case | PO-reported result | Boundary |
|---|---|---|
| TC-04-04A Career Track dropdown | PASS, all 8 acceptance checks | API-derived dropdown; Industry IT is fixed |
| TC-04-05 Cursor Pagination | PARTIAL | Nine scenarios fit on one page; browser next-page navigation not exercised |
| TC-04-12 Google OAuth | BLOCKED | Disabled locally; mocked return is not a real Google login |
| Remaining cases | Included in PO's aggregate 14 PASS | Individual case results beyond those above were not supplied; do not invent them |

No pre-enrollment survey was observed by PO: **UC-05 requirements-review item** (alignment D10), not an automatic UC-04 defect or implementation approval. Current PO scope is UC-04/05/06 (selected US-04, US-05/06, US-07); UC-09/US-10 now belongs to a teammate.

### Current automated verification executed by Codex

Frontend, from `frontend/`:

```powershell
.\node_modules\.bin\vitest.cmd run src/features/simulations/pages/coreBrowse.test.jsx
.\node_modules\.bin\eslint.cmd src/app/App.jsx src/features/identity/AccountPage.jsx src/features/simulations/components/SimulationCatalog.jsx src/features/simulations/pages/CoreCatalogPage.jsx src/features/simulations/pages/CoreSimulationPage.jsx src/features/simulations/pages/coreBrowse.test.jsx src/shared/i18n/coreMessages.js
$env:VITE_CORE_V2_ENABLED='true'
$env:VITE_AGENT_PLATFORM_ENABLED='false'
$env:VITE_API_BASE_URL='/api'
npm run build
```

Results: **19 PASS / 0 FAIL / 0 SKIP**; scoped ESLint PASS; build PASS (1931 modules). Coverage includes dropdown uniqueness/later API pages, URL/cursor reset, public card/detail states, regular login return, mocked OAuth-session return and rejection of external destinations. Build outputs `index-D9k6GUns.js` and `index-CBV6bjAl.css` match both asset names and SHA-256 bytes served after the earlier PO-approved frontend-only refresh. No production code changed during this closure.

Backend, from `backend/`, with JDK 17:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
.\mvnw.cmd '-Dtest=VersionedCatalogControllerTest,ScenarioClassificationServiceTest,ScenarioAuthoringControllerTest,ScenarioIndustryMigrationIntegrationTest,CorePackageAIntegrationTest#filteredCatalogueUsesStableCursorAndEnterpriseStatisticsNeverReturnPrivateWork' test
```

Results: **14 PASS / 0 FAIL / 0 ERROR / 0 SKIP** (controller 3, classification service 5, authoring controller 4, isolated migration 1, isolated Core catalogue 1). Surefire outputs are local generated artifacts, not staging candidates. Public-detail controller coverage checks DTO serialization; the selected Core case additionally exercises service/DAO/public projection and rejects locked fields. No full regression or fresh backend package was run; historical Docker packaging skipped tests.

**Cursor verification PASS:** the Core test creates three synthetic published scenarios in Testcontainers, traverses three limit-1 pages, asserts unique IDs/exact count and rejects a cursor after changing the query. The industry test verifies filtering before pagination and rejects IT/unfiltered cursor reuse. TC-04-05 browser UAT remains PARTIAL. Existing frontend tests cover cursor reset and track-option pagination, not catalogue Next/First button interaction; an attempted additional UI-test patch was rejected by the tool and was not applied.

Both integration tests use their own disposable PostgreSQL containers and `PG.getJdbcUrl()`, with dynamic datasource overrides for Core. No test inserts into the existing CareerSim database. Fresh V1–V27 PASS in Core; separate upgrade/mapping coverage is **V22→V25**, not populated V26/V27 correction regressions.

### Migration, integration and publication boundaries

- V23–V27 are unique and ordered. Source Flyway CRC32 matches read-only local history: **V23 -680765044; V24 1570157704; V25 -583669794; V26 1284167014; V27 743639303**, all success=true. No applied SQL was edited/rerun.
- Live `git ls-remote` confirms cached `origin/main` at `fdb85625191518135907122f86efd04aedfde1e3`; no fetch/ref write. Main currently contains V1–V22, with no V23–V27 numbering collision. Recheck before integration.
- Main has newer UC-03 `App.jsx` navigation (NavigationScroll), Core integration-test and alignment changes. Preserve them during integration; no merge/rebase or post-integration validation occurred here.
- V24–V26 disable the version guard while modifying published media; V27 disables the task guard and changes workplace context on current published versions. V27 leaves content_hash unchanged although workplace context participates in the published draft hash. PO/lead must reconcile this integrity risk; do not edit already-applied SQL.
- V24–V27 contain Forage CDN/Wistia mappings and copied taglines. Local study approval is not documented redistribution permission. No compatible license/written permission was found in reviewed changes; [Forage terms](https://www.theforage.com/terms) do not establish such permission. Hold these files and screenshots containing employer images/logos/text/video from public/shared publication pending rights review. Generic DTO/CSP support is distinct from rights to specific content.
- Eleven supplied PNGs were visually reviewed as a contact sheet; no obvious password/token/account details were visible. This is not forensic metadata certification. Several show the earlier text Career Track field; TC-04-04A shows the final dropdown. Preserve that distinction; screenshots alone do not establish all eight checks/full latest-build coverage. Third-party-content screenshots remain outside the text/source staging whitelist.

`git diff --check` and Compose config validation PASS; checked local Markdown links have zero broken targets. Final read-only GETs succeeded for all nine public details, with zero restricted-field-name findings (instructions/resources/deliverables/modelAnswer/rubric/quizScoringConfig/password_hash). All four application containers remain running with unchanged IDs/start times. A targeted changed/untracked text scan found no private-key/provider-key/JWT-token patterns. Generated dist/target, .env secrets, browser-auth artifacts and raw logs are excluded from staging; `.env.example` is already tracked. This is a scoped review, not an exhaustive secret-history audit.

**Recommendation: NOT READY for a public/shared Draft PR** until redistribution and published-version integrity decisions are resolved and main integration is reviewed. Exact conditional staging groups/PR body are in the existing Vietnamese verification report. No stage/commit/push/merge/rebase occurred.

The sections below preserve earlier checkpoint evidence. Current results above supersede prior pending/blocked statements where explicitly stated; earlier observations are not newly executed checks.

## UAT correction — 2026-10-25: career-track selector

PO approved replacing the exact-match text field with a labelled native select. Options come from the public IT catalogue, deduplicated and sorted across numeric-cursor pages of 100, independently of current search. Existing URL selections remain; submit/clear resets pagination. Loading/error/retry have EN/VI messages. No backend/API/database changes.

Current verification: focused `vitest run src/features/simulations/pages/coreBrowse.test.jsx` **19/19 PASS** outside sandbox (sandbox attempt failed before tests with cache-rename EPERM); scoped ESLint **PASS**; production build **PASS**. Existing tests now read CSS by filesystem path and distinguish review/rating headings from approved self-review explanatory text. Historical blocked Vitest evidence below describes the earlier run.

At this earlier correction checkpoint Docker refresh/UAT were pending. Subsequent PO-approved frontend-only refresh and PO-reported UAT above supersede that status. Industry remains fixed to IT, with no industry selector.

## Evidence identity

- Story/use case: report-selected US-04 / UC-04.
- Branch: `feature/browse-simulations`.
- Approved base: `62bbe1012e9e5c03dad1719b84cd40fff5fce499`.
- CP1 commit: `9ee96e6ae9da3d63d100cebd99ee862dccbc938d`.
- Final feature commit: pending PO review and manual commit.
- Environment: local Docker Compose; PostgreSQL persistent volume retained.

## Reproducible checks

From `backend/`:

```powershell
.\mvnw.cmd -Dtest=VersionedCatalogControllerTest test
```

Observed result: 3 tests passed; 0 failures, errors or skips.

From `frontend/`:

```powershell
.\node_modules\.bin\eslint.cmd src/features/simulations/components/SimulationCatalog.jsx src/features/simulations/pages/CoreCatalogPage.jsx src/features/simulations/pages/CoreSimulationPage.jsx src/features/simulations/pages/coreBrowse.test.jsx src/shared/i18n/coreMessages.js
```

Observed result: exit code 0.

From repository root:

```powershell
git diff --check
docker compose up -d --build backend frontend gateway
```

Observed result: diff check passed; frontend Vite production build transformed 1931 modules; backend package built successfully with tests skipped by its Dockerfile; backend/frontend/gateway started. PostgreSQL and its volume were not recreated.

## Verified behavior

- Public Core catalogue/detail use database-backed published/available scenarios.
- Catalogue supports server-side search, career track, difficulty, industry, sort and cursor pagination.
- Industry is filtered in SQL before pagination and participates in cursor validation.
- Nine authorized local study simulations were visible before the final layout refinement.
- Cards use one keyboard-accessible link, compact four-column desktop layout, two-line descriptions and icon/value metadata.
- Detail renders Overview, Video and Tasks as one continuous page; anchors jump to sections; tasks disclose public descriptions.
- Public detail contains skills from published task-skill snapshots.
- Public API inspection found no model answers, rubric, private submissions or locked resources.
- Start stores only a safe internal simulation path before entering the existing login flow.

## Evidence boundaries

- Frontend targeted Vitest source exists, but the local Manager run was blocked by sandbox `spawn EPERM`; do not report it as passed.
- PostgreSQL/Testcontainers verification was blocked from the Manager sandbox; rerun on an approved isolated test database.
- Docker backend build uses `-DskipTests`; build success is not automated test success.
- Browser evidence before the final refinement is valid for the nine-item catalogue and responsive grid. Browser UAT after the final refinement remains pending because Computer Use could not establish the active Edge URL.
- The imported reference content is approved for the PO's local non-commercial study environment. Public redistribution/licensing review is outside this evidence.
- No final acceptance or deployment claim is made until PO UAT and final commit SHA are recorded.

## Manual UAT checklist

1. Open `http://localhost:8080/simulations` without signing in. Expect nine published simulations and no demo fallback.
2. At desktop width, expect four compact cards per row. At 320 px, expect one column and no horizontal scrolling.
3. Use keyboard Tab/Enter on a card. Expect the whole card link to open its detail page.
4. Expand Search and filters. Select an API-derived Career Track, search, and change difficulty/sort; Industry IT remains fixed. Expect URL state to update and stale cursor reset. Nine-item browser pagination remains PARTIAL; automated pagination uses isolated synthetic data.
5. Search for an impossible term. Expect a no-match message and Clear filters action.
6. Open a detail. Expect top CTA, Overview, Video and Tasks on one scrollable page. Click each anchor and verify it jumps to the section.
7. Inspect Learning objectives and task descriptions. Expect readable text, no raw HTML/RSC payload and no model answer.
8. Expand and collapse tasks. Expect public descriptions only.
9. Click Start while signed out. Complete regular login, then Google OAuth separately if configured. Expect return to the same simulation and no duplicate enrollment.
10. Refresh the catalogue/detail and use browser Back. Expect route, filters and direct links to remain usable.
11. Switch EN/VI and repeat key labels/states.
12. Temporarily simulate an API error only in a safe test environment. Expect error and Retry without static fixture fallback.
