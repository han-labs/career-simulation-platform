# HCM-UTE frontend rebuild from latest main

Date: 2026-10-26. Foundation/presentation integration; verdict READY WITH CAVEAT for review, human acceptance pending.

## Baseline, authority and isolation

Baseline origin/main: 608e81274ce78ed071bcdf4c0a889a03dae916e3 (PR #11 included). Fresh managed worktree: C:/Users/admin/.codex/worktrees/careersim-frontend-flow-alignment/career-simulation-platform. Branch: feature/hcmute-frontend-flow-alignment. The rejected feat/hcmute-landing-redesign at 887e0d1 remains in the original checkout; it was not merged, cherry-picked or used as functional source. Its approved visual appearance and the supplied original archive are the visual reference only.

Initial inspection executed git status, branch -vv, remote -v, fetch origin --prune, current/main logs and ancestry/diff checks. There were no inherited changes in this worktree. The [functional audit](hcmute-main-functional-audit.md) and [template-based plan](../plans/foundation-hcmute-frontend-flow-alignment.md) were completed before UI edits; clean-main tests were observed first. Authority: current report 10 selected US/16 UC; frozen Baseline v1 and DR1–DR2 rules; main/API/tests as implementation state. Official Person 1/2/3 business ownership is preserved.

## Existing flow and responsibility mapping

The audit provides actor, entry/component, hook/state, API/backend, preconditions, main/alternative/failure behavior and tests for identity, assessment, public catalogue, simulation, Dashboard/Guidance/Agents, Enterprise, Admin and i18n. Browser → existing App/AppShell/feature pages → shared HTTP/API adapters → unchanged Spring services/DAO → PostgreSQL remains the sequence. No architecture, schema, API, provider, authorization, scoring or plan semantics changed; existing diagrams remain valid.

| Modified source | Presentation/integration and preserved responsibility |
|---|---|
| shared/layout/AppShell.jsx | Compact university header, approved labels, one guest /account Link, core-gated /enterprise Link; retains main direct Home/Guidance/Admin and sign-out, actor lookup, logout pending/error/invalidation/navigation, focused Account/Admin shell. Same existing mobile menu plus Escape/focus return. No new account menu/auth ownership. |
| shared/ui/BrandMark.jsx | Supplied logo plus separate localized institutional name; same shared component/compact footer role. No fabricated partner badge. |
| shared/i18n/LanguageSwitcher.jsx | EN/VI display options in existing provider/state/callback; preference keys and lifecycle unchanged. |
| shared/i18n/translate.js and landingRedesignMessages.js | Additional natural VI presentation strings in existing dictionary. Existing interpolation, content translation and unknown-prose behavior retained. |
| features/landing/pages/LandingPage.jsx | Same published adapter/request cleanup and legacy useAssessment/status/result props. Approved section composition; explicitly authorized removal of sample substitution and three-row limit; real loading/error/empty/retry with all returned published rows. |
| landing/components/HeroSection.jsx | Split supplied entrance photo/light-blue blend and Explore Now → /simulations. Retains original assessment → /assessment action, completed-result label and dominant trait derived from existing status/result props. No authoritative score calculation. |
| landing/components/CatalogSection.jsx | Horizontal controls/keys/reduced motion over original SimulationCard; existing real detail links. Unchanged SimulationCatalog owns loading/error/empty/retry views. No second preview-card or simulation domain implementation. |
| simulations/components/SimulationCard.jsx | Compact=false default; compact opt-in adds optional existing public media/provider display and whole-card link styling. Existing track/duration/level/title/summary/localization/detail destination retained; noncompact view preserved. |
| simulations/api/simulationCatalogApi.js | Same feature-flag-selected endpoint and normalization; retains already-public enterpriseName/media for view use. No API contract or visibility rule change. |
| landing/components/CompaniesSection.jsx | Pure view of named providers with actual public logo metadata, deduplicated; broken logo falls back to provider text; no metadata shows neutral catalogue copy. No fixed brands/affiliation claims. |
| landing/components/JourneySection.jsx | Four approved steps; original assessment/simulation/Dashboard actions preserved. Employer step is explanatory and creates no deferred employer-matching route. |
| landing/components/CtaSection.jsx | Split aerial campus mission; existing assessment and simulation destinations remain, approved primary label. |
| main.jsx, styles/index.css and styles/hcmute.css | Add stylesheet after existing styles; same providers/router. Shared blue tokens and scoped landing layout; desktop major spacing 56–72px, mobile 40px. Existing feature layouts/state untouched. |
| public/images/hcmute | Exactly three supplied original files plus provenance README. SHA256 comparison against ZIP entries passed for each. No generated asset or rejected-branch artwork used. |

Feature-owned source for Account/HTTP/App routes, assessment/hooks, Core catalogue/workspace/results, Dashboard/Guidance/Student Agent, Enterprise/Admin and ALL backend files is unchanged. FeaturesSection/TracksSection remain in source; duplicate landing sections are unmounted for approved composition and their valid entry actions remain elsewhere.

## Automated verification (unit/component and mocked boundaries)

Commands executed from this worktree:

~~~powershell
cd frontend
npm ci
$env:VITEST_MAX_WORKERS='2'; npm run check
$env:VITE_CORE_V2_ENABLED='true'; $env:VITE_AGENT_PLATFORM_ENABLED='true'; npm run check
cd ../backend
.\mvnw.cmd verify
cd ..
docker compose -p careersim-flow-alignment --env-file tmp/flow-alignment/runtime.env -f docker-compose.yml config --quiet
~~~

Clean-main baseline: default and enabled frontend checks each 249 tests/31 files, lint/build PASS. Backend 404 discovered, 398 passed, six skipped, zero failures/errors, verify/Spotless/build PASS. Final executed backend also PASS: same 404/398/6, 5:53 minutes. Existing skips include opt-in fixtures/live providers; no skipped case is counted passed.

Final default/enabled checks each PASS: 260 tests/32 files, lint/build; the last runs took57.32s/54.72s for tests respectively. Exact application commit is recorded below. Existing 249 test cases remain; visual text/logo assertions were updated to approved copy. New route/component cases verify actual AccountPage and feature components through explicitly mocked shared API boundaries, main's observed pending-return gap, direct logout and role nav, published rows/metadata, real assessment hook validation/save/result/VI–EN state, truthful error/retry/empty, all rows, provider fallbacks, compact opt-in, carousel keys/reduced motion and menu focus. No tests, guards or lint rules were disabled/deleted. Initial generated-test selector whitespace/ambiguous Title errors were corrected; a browser Home assertion accepts the two-pixel CSS scroll-snap padding. These were harness issues, not product fixes. Existing jsdom scrollTo warnings and enabled-build bundle advisory are nonfatal and also bounded by existing implementation.

## Live isolated API/UI verification (no mocked responses)

The existing Compose configuration was built/started under project careersim-flow-alignment, fresh named volumes, PostgreSQL localhost:15483, backend18181, frontend13080 and Nginx gateway18180. Runtime config stays ignored in tmp/flow-alignment/runtime.env. Generated local-only credentials/JWT are not included here or staged. HTTP cookies are insecure only for this isolated local HTTP stack; no shared service/database was changed. SPRING_PROFILES_ACTIVE=flow-verification disables main's intentional local-profile loopback Administrator fallback; LOCAL_DEMO_MODE is not an implemented switch. AI and Google providers are disabled; no live-provider claim.

Opt-in existing LocalCareerQuizBootstrapTest published reviewed synthetic local quiz versions on the explicitly guarded disposable DB; it passed separately. Synthetic Enterprise/Admin/other-Student accounts and five original three-task PRACTICAL scenarios were created with real registration/authoring/publish APIs. Narrow SQL fixture grants supplied Enterprise publication permission and synthetic Admin role. Classification PUT failed on main; only these five synthetic scenario rows were classified IT through guarded fixture SQL. This is fixture preparation, not a passing classification API or production data.

Browser evidence uses bundled Playwright + installed headless Chrome, real cookies/CSRF and gateway APIs, no route/response mocks. Baseline frontend image was built from untouched main before UI edits; only frontend/gateway were rebuilt for the new UI, retaining backend/DB. Gateway health 200 and all five published rows were checked. Captures/logs/scripts/raw JSON remain ignored locally under tmp/flow-alignment; only this sanitized report is staged.

Observed baseline student checks: actual registration/password sign-in; draft simulation without prior assessment; next task locked; 42 required RIASEC questions and invalid-submit prevention; all 42 answers preserved across VI/EN; actual server save/reload/submit and six backend dimension raw 21 results; saved result reload; Dashboard same-draft resume; public detail idempotent Start; three actual finals, cancel/confirm, released model answer, explicit self-review/next; direct published-slug completion and three-task result/completion record; assigned-Agent empty state; actual Guidance; direct logout followed by me 401. Enterprise clone/save/preview and publish eligibility passed; unpublished draft stayed out of public results. Admin real role landing, KB/Builder and aggregate Usage passed. Baseline combined harness stopped after those role checks due rapid automation429 before the extra restriction check; pacing was increased without changing rate limits. Earlier failed runner attempts and selector corrections remain in ignored logs and are not passed journeys.

Final real gateway runner completed 10 checks with zero page exceptions and no 429 or 5xx. Login return and Dashboard alias results reproduced D13/D14 exactly as main. Student actual result attempt 3/enrollment 2 persisted; direct published-slug results: 3 tasks and completion record passed. Enterprise/Admin and distinct Student role/ownership negative APIs passed 403/404. Captured browser HTTP errors were expected guest me 401 and the recorded baseline scenarios/enrolled 404; negative API statuses were asserted separately. No invented AI/model result or professional practical score appeared.

Final visual runner passed 10 combinations: widths 1440,1280,768,390,320 × EN/VI, each with five actual API-backed synthetic rows, document width equal viewport, zero broken images and no page exceptions. Menu open/Escape/focus return, actual previous/next/Home scrolling and real Student/Enterprise/Admin header states were checked. Admin link was absent for Student/Enterprise and present for Admin; one direct sign-out and zero guest auth links while signed in. Only expected anonymous me 401 appeared; no asset 404 or unexpected HTTP failure. Representative 1440 EN/VI,390 VI and320 VI captures were visually inspected by the agent (not a human sign-off). Mobile major gap 40px; desktop computed clamp 56–72px.

The final visual image includes the small VI label/mobile-spacing corrections made after the full 10-check functional run. Functional callbacks/state/routes/backend source are byte-identical across those builds; both final frontend gates and final visual/role checks used the corrected source. Raw evidence is separate from the sanitized report.

Additional exact runtime commands:

~~~powershell
docker compose -p careersim-flow-alignment --env-file tmp/flow-alignment/runtime.env -f docker-compose.yml build frontend
docker compose -p careersim-flow-alignment --env-file tmp/flow-alignment/runtime.env -f docker-compose.yml up -d --no-deps frontend
docker compose -p careersim-flow-alignment --env-file tmp/flow-alignment/runtime.env -f docker-compose.yml up -d --force-recreate --no-deps gateway
node tmp/flow-alignment/live-flows.cjs final
node tmp/flow-alignment/visual-check.cjs
~~~

The live reproduction helpers remain ignored beside local generated fixture credentials and depend on the installed bundled Playwright path; they are not production routes or shipped dependencies. Final running frontend image: sha256:4a5593075add57b66ae2d49c4115df9ad0b984da4753073b36c93ea1242cd111. Backend image remains sha256:6c238628c7d321bba3513ec7442876e2a53e9afcc828d1126c11a3fbc668acee built from main. Final asset bundle CSS index-CQAaonrG.css and enabled JS index-Bly_l8So.js match the enabled build. The isolated stack remains available for local review at http://localhost:18180; no shared deployment occurred.

## Baseline limitations and acceptance boundary

- D12: unchanged Enterprise classification PUT returns 500 with returned revision; fixture-only IT setup unblocks other tests. No backend correction authorized.
- D13: unchanged integrated password login consumes pending simulation destination twice across account-generation remount/AccountPage, landing at /assessment. Both new mocked route test and real unchanged-main browser reproduce it. Existing catalogue → detail → explicit Start is exercised as an explicit workaround; pending return is not declared passed.
- D14: unchanged Dashboard alias /simulations/enrolled/attempt can resume the owned draft, but completion leads to /simulations/enrolled/results requiring public /scenarios/enrolled 404. Direct published-slug results pass. Person 2 business source is preserved; alias results are not declared passed.
- Intentional local-profile Administrator fallback is configuration behavior; real-identity checks use verification profile. Expected guest/session/negative-test 401/403/404 are separated from unexpected failures. An initial fast runner hit 429; rate policy stayed intact and slower final checks are reported independently.
- No external Google/OAuth callback, assigned live AI conversation/stream/tool/plan, provider quality/latency, Ubuntu deployment or human sign-off is claimed. Existing frontend/backend tests cover mocked/deterministic denial/provider/fallback/state boundaries; empty real Agent state is a narrower live check.

The approved redesign is implemented around main's functions, with these reproduced baseline gaps left for their business owners. This is not a claim that every pre-existing main defect or external-provider UAT is resolved.

## Git integration and review

Tested application commit: 5f526358763bb93d02a1eeeff0ec1885ea9b8902; frontend tree: deaaab23da60e5e9eea1fab0b0147253091704fa. The final automated suites used exactly the committed application content; post-commit diff verification found no frontend/backend changes. Post-commit gateway health 200 and served enabled JS/CSS byte equality with the final build passed. JS SHA256 c45941d7d2ed8d2d006a48d56be2ba9b7ce74259c3ea50fec398cd23abda5d4e; CSS SHA256 3d935446e4ad4e4879735187c118eae0cb7ae1cd200bd23bd2755f90a0732728. Final fetch origin --prune left main at 608e812; merge-base equals baseline. At application commit comparison was0 behind/1 ahead; the following documentation-only evidence commit adds no application changes. No integration conflicts occurred. Original checkout still clean on rejected branch 887e0d1, which is not an ancestor of main. No push, PR publication, force operations, shared rebase or main merge authorized/performed. High-overlap review areas are AppShell/shared i18n/theme and landing/shared card adapter. No backend or feature business file changes. [AI assistance record](ai/2026-10-26-hcmute-main-rebuild.md); human owner/reviewer remains unassigned, not agent-signed.

## Complete changed-file inventory

26 application/assets/tests plus 9 documentation files; no migrations/config/backend/business-page changes.

~~~text
.agent/DESIGN.md
.agent/TASKS.md
.agent/plans/foundation-hcmute-frontend-flow-alignment.md
.agent/reports/ai/2026-10-26-hcmute-main-rebuild.md
.agent/reports/hcmute-main-functional-audit.md
.agent/reports/hcmute-main-rebuild-verification.md
AI_USAGE.md
docs/requirements/REPORT_CODE_ALIGNMENT_2026-10-22.md
docs/requirements/traceability-matrix.md
frontend/public/images/hcmute/README.md
frontend/public/images/hcmute/first section.webp
frontend/public/images/hcmute/last section.webp
frontend/public/images/hcmute/logo.jpg
frontend/src/features/landing/components/CatalogSection.jsx
frontend/src/features/landing/components/CompaniesSection.jsx
frontend/src/features/landing/components/CtaSection.jsx
frontend/src/features/landing/components/HeroSection.jsx
frontend/src/features/landing/components/JourneySection.jsx
frontend/src/features/landing/landingFlows.test.jsx
frontend/src/features/landing/landingPage.test.js
frontend/src/features/landing/pages/LandingPage.jsx
frontend/src/features/simulations/api/simulationCatalogApi.js
frontend/src/features/simulations/api/simulationCatalogApi.test.js
frontend/src/features/simulations/components/SimulationCard.jsx
frontend/src/main.jsx
frontend/src/shared/i18n/LanguageSwitcher.jsx
frontend/src/shared/i18n/i18n.test.jsx
frontend/src/shared/i18n/landingRedesignMessages.js
frontend/src/shared/i18n/translate.js
frontend/src/shared/layout/AppShell.jsx
frontend/src/shared/layout/AppShell.test.jsx
frontend/src/shared/ui/BrandMark.jsx
frontend/src/shared/ui/BrandMark.test.js
frontend/src/styles/hcmute.css
frontend/src/styles/index.css
~~~

Recommended PR title: Rebuild HCM-UTE landing from current main while preserving existing frontend flows. Description: reuse main shared navigation/identity/state/cards and approved supplied presentation, replace fake landing fallback with actual published lifecycle states, preserve feature-owned sources, and provide baseline versus final gates/real workflows. Explicitly retain D12–D14 owner follow-ups and external OAuth/live-model/human UAT caveats. No PR was created or published.
