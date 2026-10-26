# HCM-UTE frontend flow alignment — latest-main foundation plan

Status: Implemented and verified for review with documented main baseline gaps; human acceptance pending
Owner: Cross-cutting frontend integration authorized by requester; feature business owners remain Person 1/2/3, human review pending
Date: 2026-10-26

## 1 Scope

Current 10-US/16-UC catalogue; existing UC-01–09 and supporting Enterprise/Dashboard entry points, no supplementary/deferred implementation. Sources: current rebuild request, supplied approved screenshots/archive, root/.agent rules, requirements/Baseline DR1–DR2, official assignments, existing APIs/tests and [pre-implementation flow audit](../reports/hcmute-main-functional-audit.md).

Exact relevant ACs: US-01 eligible registration/validation/duplicate/cancel; US-02 current-role auth/logout; US-03 optional published version selection/required response/backend result/separate histories; US-04 public published search/detail/empty/no locked answers; US-05/06 draft/final/idempotency/model answer/review/zero-score/owned results; US-07 authorized assigned Agent/history/stream/report/exact plan/AI unavailable; US-08–10 existing Admin staging/publication/config/assignment/privacy-safe usage. Preserve all, do not reimplement their feature code. The audit maps each actor/route/component/state/API/backend/test/main/alternative/failure and permitted visual change.

Acceptance: approved compact bilingual institutional header and one combined signed-out account entry; split real-photo Hero; honest company strip; shared catalogue as one compact carousel; four-step journey with existing actions; split aerial-photo mission; existing footer. Moderate responsive spacing, focus/reduced motion, no 320px page overflow. Every action enters an actual main route and existing server guards.

## 2 Main, alternate and failure flows

Main: public landing → existing catalogue/detail → guest Start/account → existing sign-in/role/pending-detail handoff → explicit Start → pinned tasks/draft/final/review/results → Dashboard/Guidance → direct shared logout. Quizzes use existing core/legacy routing, published questions, backend results and resume/history. Enterprise/Admin keep their actual pages and permissions.

Alternates: VI/EN preserves state; legacy completed Hero assessment action remains; cursor catalogue and flag variants; native horizontal scrolling plus buttons/keys; no assigned Agent/empty content remains truthful. Failure: existing shared catalogue loading/error/empty/retry; no fake published rows, auth expiration and wrong-role boundaries unchanged; existing draft/conflict/error/fallback mechanisms untouched.

## 3 Responsibility and change classification

| Class | Files / purpose | Existing responsibility preserved |
|---|---|---|
| 1 Presentation only | BrandMark, LanguageSwitcher options, shared theme and new scoped landing CSS | Branding/localization remain shared; provider/key/storage lifecycle unchanged |
| 1 Presentation only | HeroSection, JourneySection, CtaSection | Keep main props and assessment/simulation/dashboard actions; approved new copy/photos/four-step layout |
| 2 Existing flow integration | AppShell markup/navigation labels and combined `/account` link; Core-enabled `/enterprise` link | Main account/menu/logout state and callbacks stay in place; Home/Guidance/direct sign-out and focused Admin shell remain |
| 2 Existing flow integration | Existing SimulationCard compact option (default false), unchanged SimulationCatalog state views, CatalogSection controls; existing adapter retains already-public media/provider fields | Single shared card/link/lifecycle implementation; no separate domain, route, scoring or task flow |
| 2 Existing flow integration | LandingPage uses existing published adapter/shared state UI; Company strip pure metadata view | Preserve request cleanup; all returned rows rather than slice; no fictional providers |
| 3 Explicitly authorized correction | Replace main landing's hardcoded sample substitution on empty/failure with truthful shared catalogue states | Current user explicitly demands actual published-only/no fake data; existing retry/error UI used, no backend change |
| 3 Not authorized | Auth/security/scoring/schema/provider/feature business changes | Excluded; document any discovered issue separately |

FeaturesSection/TracksSection implementations remain; their duplicate sections are unmounted only for the approved composition. Existing routes and assessment/simulation/dashboard entry actions remain reachable via Hero/journey/CTA/header/catalogue. No old redesign functional code/routing/state is copied.

## 4 Change surface and sequence-to-code

Browser → unchanged App/AppShell/account and feature routing. Landing effect → existing getPublishedSimulations → shared HTTP client → existing public catalogue → normalized metadata → existing landing SimulationCard compact presentation/detail link with unchanged shared SimulationCatalog state views. New company strip only consumes that successful metadata. Hero retains status/result props from existing useAssessment; never grades or synthesizes a v2 result.

Frontend: shared chrome/branding/i18n, landing components/CSS, existing landing SimulationCard opt-in view, catalogue public-field normalization, focused new integration tests. Feature-owned assessment/workspace/dashboard/guidance/enterprise/Admin sources/hooks remain byte-identical. Backend controllers/services/DAOs/entities/migrations/config: no changes. Architecture/API topology: unchanged. Docs: audit, plan, alignment/design approval, verification and AI log/status.

## 5 API contracts

Unchanged GET `/v2/me`, existing auth/account endpoints; GET `/v1/simulations` or `/v2/scenarios`; existing public `/simulations/{slug}` route; all feature contracts and ownership described in audit. No new API/DTO, persistence or environment requirement. Runtime test stack uses existing documented local flags, separate ports/volumes and no external provider credentials.

## 6 Security privacy and AI

Server remains identity/role/owner/score authority. No fake users/metrics/content in production code. Test fixtures are explicit synthetic data in disposable resources. No provider calls, new consent, prompt, score, plan semantics or private evidence. Preserve Standard guidance versus Agent failed/refused contracts. Existing external OAuth/live-model quality remains unverified unless actually observed.

## 7 Tests and evidence

Before UI edits: clean-main `npm run check` and backend verify; Compose config; establish baseline actual routes. During/after: preserve original assertions; add route-level tests covering guest account and public catalogue/detail/Start handoff, actor navigation, direct logout duplicate/failure, locale answer/progress persistence, compact card/shared failure states and keyboard scroll. Existing comprehensive feature tests remain active. Run default and enabled frontend checks, backend verify, Compose; isolated gateway health and real Student assessment/save/result + simulation draft/final/review/result + Enterprise/Admin role smoke; separate mocked Agent/provider outcomes from live service availability. Capture representative 1440/1280/768/390/320 VI/EN visuals and console/HTTP/route/asset errors.

## 8 Risks, approval and Git strategy

| Risk | Decision / approval |
|---|---|
| Prior branch is rejected | Fresh main worktree at `608e812`, requested branch; old `887e0d1` preserved untouched, no merge/cherry-pick/code copying |
| Historical design no-gradient/serif versus approved screenshots | Current explicit approval owns HCM-UTE blue/sans/photo blend/four steps; record new scoped alignment/design note on this branch |
| All existing navigation versus screenshot simplicity | Functional priority wins: preserve Home/Guidance/direct logout, responsive layout may accommodate extra links; do not hide them in a new account menu |
| Main sample catalogue versus no-fake-data correction | Explicit latest request authorizes shared truthful loading/error/empty/retry; no fabricated partners or published simulations |
| Shared files may change upstream | Fetch before commits/PR, inspect overlaps, resolve only clear intent; explicit whitelist staging; no force push/rebase/shared overwrite or main merge |
| Human review and live third-party access | Report pending accurately; no synthetic/mock test is described as live OAuth/AI acceptance |

All new commits belong to this branch. Keep application/evidence commits justified; preserve exact tested commit SHA, report baseline versus final and all failures. No unsolicited publishing/push/deployment to shared services.

## 9 Execution outcome

[Final verification](../reports/hcmute-main-rebuild-verification.md): clean-main249 frontend cases versus final260, default and both flags enabled; backend404 discovered/398 passed/six existing skips both baseline/final; isolated Compose and10 actual gateway workflow checks;10 VI/EN viewport combinations plus real role headers. No backend or feature-owned business source changes. D12 classification500, D13 pending simulation return and D14 Dashboard alias results remain reproduced main limitations; source ownership is respected. Final commit identity, exact commands, changed files, mocked/live boundaries and remaining UAT are in the report and AI record. No push/main merge.
