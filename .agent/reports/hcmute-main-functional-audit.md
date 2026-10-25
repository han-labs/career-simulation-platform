# Latest-main functional flow audit — HCM-UTE rebuild

Date: 2026-10-26. Audit completed before frontend edits.
Baseline: `608e81274ce78ed071bcdf4c0a889a03dae916e3` (`origin/main`, PR #11 included).
Workspace: fresh managed Git worktree, branch `feature/hcmute-frontend-flow-alignment`.
Rejected branch `feat/hcmute-landing-redesign@887e0d1` is preserved, unmerged and not a code source. No cherry-pick, merge or source copying from it.

## Authority and traceability

Current report 10 selected US / 16 UC; Person 1 UC-01–03, Person 2 UC-04–06, Person 3 UC-07–09. Frozen Baseline v1 + DR1/DR2 own rules. Current source/API/tests own implemented behavior. This cross-cutting rebuild authorizes presentation and existing-flow integration only, never new business logic. D01/D02 completion/evaluation wording and D04 arbitrary plan editing remain unresolved: preserve required FINAL completion, explicit next-task self-review and exact-source/hash plan confirmation. UC-14 remains deferred.

Read: root/.agent entry, context/tasks/design and all eight rule files; current requirements/assignments, selected ACs and UC-01–09; Baseline/DR decisions; alignment; identity/Core and Agent API/architecture; existing foundation/localization/assessment/simulation plans and regression tests. Dated source checkpoint statements are not current runtime evidence.

## 1 Application routing and authentication (UC-01/02)

| Field | Current implementation |
|---|---|
| Feature / actor | Account access; Guest, Student, Enterprise, Admin |
| Entry / components | `App.jsx` → `/account` → `AccountPage` / `AuthenticationForm`; `ApplicationLayout` → `AppShell` |
| Navigation / conditions | Account restores `/v2/me`; Student → validated pending simulation detail or `/assessment`; Enterprise with Core enabled → `/enterprise`; server role ADMIN → `/admin`. No new global ProtectedRoute exists. Main protected pages use backend errors; only simulation Start explicitly hands guests to account. |
| State / hooks | AccountPage owns loading/provider/oauth/pending-destination state. AppShell independently owns server account, menu, logout error/pending lock. ApplicationLayout subscribes to shared account generation and remounts account-scoped shell on login/logout/invalidation. |
| API / backend | HTTP client credentials + CSRF; `/v2/auth/providers`, `/v2/auth/csrf`, `/v2/auth/register`, `/v2/auth/login`, `/v2/me`, `/v2/auth/logout`; configured `/api/oauth2/authorization/google`; IdentityController/IdentityService and current ActorAccess/JWT policy. |
| Preconditions | Active eligible account; server role/ownership; valid CSRF on writes. Registration supports Student and Enterprise; no Admin registration UI. Enterprise registration does not grant publishing permission. |
| Main / alternatives | Accessible validation → registration success → explicit sign-in → current role handoff. Existing local password and configured Google identity; cancel registration returns home. Pending simulation path is strict same-app validated data, not automatic enrollment. |
| Failure / fallback | Provider availability unknown/offline has retry and local login; callback cancellation/link/disabled/unavailable states; auth loading/error/retry; duplicate-email fields; duplicate submit/logout locks; logout error preserves retry. No fabricated session. |
| Tests | `AuthenticationForm.test.jsx`, `AccountPage.test.jsx`, `AppShell.test.jsx`, `httpClient.test.js`, `coreBrowse.test.jsx`; backend AuthenticationContract/credential/startup tests. |
| Allowed UI impact | Brand/layout, compact language labels, one combined link to existing `/account`, approved renamed labels. Preserve direct sign-out, its callback/pending/error behavior, current actor effect, Home/Guidance/Admin links and all destinations. No replacement account menu/state mechanism. |

## 2 Assessments / career quizzes (UC-03)

| Field | Current implementation |
|---|---|
| Actor / entry | Student; `/assessment`, `/assessment/:assessmentType`; core flag selects VersionedAssessmentPage, otherwise existing AssessmentPage. |
| Components / state | VersionedAssessmentPage → AssessmentWorkflow → `useVersionedAssessment` → AssessmentForm → AssessmentResult/ProfileAssessmentResult; URL `attempt` selects authorized history. `useAssessmentScroll` controls committed navigation. Legacy `useAssessment` owns account-scoped local draft and backend result refresh; LandingPage already consumes its status/result for Hero. |
| API / backend | assessmentApi → actor, `/v2/assessment-types`, `/v2/assessment-versions/{version}/attempts`, `/v2/assessment-attempts`, `/{id}`, `/responses`, `/submit`, `/result`; VersionedAssessmentService authorizes, validates, scores and stores. Legacy `/v1/assessments` remains separately gated. |
| Preconditions | Server identity; published active version; owned attempt/revision; required reviewed responses. Assessment is optional and never gates simulations. |
| Main / navigation | Select type → instructions → Start/resume pinned version → answer → Save draft or submit → server result → history/reload/retake. Result types/versions remain separate. Assessment action remains `/assessment`; no scores computed by redesign. |
| Alternatives | Type deep links; postpone; owner history; browser back; local/server revision conflict with explicit choice; request UUID reused after lost Start response. Legacy quick-demo helper remains in legacy feature. Core `?demo` only explains that published questions require actual answers. |
| Failure / fallback | Loading, empty published catalogue, unknown/unavailable type, invalid/missing answers, 404/409/422/network, storage failure, failed result read and retry; preserved draft, no fake result. |
| Tests | `VersionedAssessmentPage.test.jsx`, `useAssessment.test.jsx`, result tests, coreIntegration; backend versioned evaluator/controller/service/persistence tests. |
| Allowed UI impact | Header label/entry and Hero presentation only; preserve LandingPage's legacy status/result props and assessment/resume action. Do not change assessment hooks/pages/forms/results or demo behavior. |

## 3 Published simulation catalogue and landing discovery (UC-04)

| Field | Current implementation |
|---|---|
| Actors / entry | Guest and Student; `/simulations`; singular aliases preserve query and redirect. CoreCatalogPage is selected only with Core flag; legacy page remains supported. |
| Components / state | CoreCatalogPage owns request path, URL query/filter/sort/cursor, retry and independently loaded API career tracks. `SimulationCatalog` renders shared loading/empty/error/retry and real cards. LandingPage currently uses `getPublishedSimulations` and separate reviewed sample fallback plus a three-record slice. |
| API / backend | Public `/v2/scenarios?paged=true&limit=12&industry=IT&sort=...`; API-loaded tracks across cursor pages; getPublishedSimulations currently reads `/v2/scenarios` or `/v1/simulations`. VersionedCatalogController/service returns published public metadata only. |
| Navigation / preconditions | Core cards map `id = item.slug`, link `/simulations/{slug}`. No prior quiz or sign-in needed for catalogue/detail (approved D09). Unpublished data/answer keys are absent. |
| Main / alternatives | Browse → search/filter/sort → detail; next/first page; clear criteria; ordinary published catalogue, no personalized recommender. |
| Failure / fallback | Main SimulationCatalog has loading skeletons, useful empty/no-match, error and retry. Landing's existing samples can incorrectly remain after a successful empty response; this conflicts with the current explicit actual-published-only/no-fake-data request. |
| Tests | `coreBrowse.test.jsx`, `simulationCatalogApi.test.js`, landingPage tests; VersionedCatalogController/classification/catalogue migration/pagination tests. |
| Allowed UI impact | Add a compact presentation option to the existing landing SimulationCard, default off; reuse SimulationCatalog unchanged for loading/empty/error/retry. Existing link/state/retry machinery remains. Reuse getPublishedSimulations and normalize its already-public media/provider fields. No second catalogue/card domain implementation. Landing may stop slicing returned records and show truthful error/empty instead of invented published content, explicitly authorized by this correction. |

## 4 Simulation workspace / completion (UC-05, selected US-05/06)

| Field | Current implementation |
|---|---|
| Actor / routes | Student; `/simulations/:slug`, `/:slug/attempt`, `/:slug/results`; enrollment query selects pinned owned work. |
| Components / hooks | CoreSimulationPage owns catalog/enrollment/task/submission/values/result/retry state; SubmissionConfirmationDialog and TaskRichText; CoreSimulationResultsPage owns results read/retry. Legacy detail/attempt components are selected when Core is off. |
| API / backend | Public `/v2/scenarios/{slug}`; current actor → POST `/v2/scenario-versions/{id}/enrollments`; owned `/v2/enrollments/{id}`, task `/submission` PUT, `/finalize` POST with revision/request UUID, `/model-answer`, `/review`, `/results`; `/v2/files` upload/download; Enrollment/TaskSubmission/Completion facades. |
| Preconditions / navigation | Public detail is not enrollment. Guest Start stores validated detail destination and opens `/account`; successful login returns to detail, then explicit Start. Server owns role, pinned version, status and ordering. |
| Main | Details → Start/resume → select accessible task → enter deliverables → save draft → explicit final confirmation → backend validates/persists → released model answer → explicit self-review → next task → results/completion. |
| Alternatives | TEXT/URL/JSON/FILE formats, resources, incomplete draft, cancel final dialog, local recovery, same request retry and revision reload; zero quiz score is still valid FINAL; practical work has no mandatory numeric grade. |
| Failure / fallback | Missing/unpublished detail, owned/foreign enrollment masking, expired auth, invalid/oversize file, malformed JSON/422, final lock, stale revision/409, network failure, independent result retry; local work preserved, no fabricated final/completion. |
| Tests | `coreIntegration.test.jsx`, `coreBrowse.test.jsx`, TaskRichText; backend TaskProgressionPolicy/CorePackageA/SimulationWorkflow/DeliverableValidator and file policy tests. |
| Allowed UI impact | Existing card/detail destinations and shared theme only. No changes to these pages, hooks, validation, model answers, task order, persistence or results. |

## 5 Dashboard / Guidance / assigned Student Agents (UC-06 and supporting dashboard)

| Field | Current implementation |
|---|---|
| Actor / routes | Student; `/dashboard` → CoreDashboardPage with Core flag, otherwise GuidancePage; `/guidance` always GuidancePage. `/agents` and `/agents/:agentId` exist only with Agent flag. |
| Components / state | CoreDashboardPage owns server data/plan form. GuidancePage owns load/retry/history expansion. AssignedAgentsPage lists assigned active agents; StudentAgentPage/Workspace/Conversation own actor, sessions, account-scoped draft, request/execution IDs, stream cursor, report/plan state. |
| API / backend | `/v2/guidance/dashboard`, explicit `/v2/plans/confirm`; legacy getExplorationDashboard → `/v1/guidance/dashboard`. Student `/v2/agents`, agent sessions/interactions/executions/events, insight-reports, plan-suggestions and exact `/v2/exploration-plans/confirm-suggestion`. Backend owns context/assignment/consent/source validation and persistence. |
| Preconditions | Owner/current role/assignment; external processing needs consent. Dashboard never grants tool/data access. The old global Syn widget is absent from latest main; do not restore it. |
| Main / navigation | Dashboard reads actual owned results/enrollments/practiced skills/completions/plan. Existing resume URL is `/simulations/enrolled/attempt?enrollment={id}`. Guidance reads legacy evidence. Assigned Agent → create/resume conversation → CSRF POST → read-only SSE → canonical saved execution/history. Report only on request, plan only explicit exact hash/revision confirmation. |
| Alternatives | Empty evidence, no assigned agent, history pagination/delete confirmation, reconnect same execution without repost, cancellation, saved reports. Legacy preview data is available only behind existing explicit preview flag, never silently on 401/403. |
| Failure / fallback | Dashboard error/empty; Agent failed/refused/source-revoked/consent errors, interrupted stream and retry; validated canonical text only, unknown metrics stay unknown. Legacy Standard guidance and Agent failed/refused outcomes are distinct. |
| Tests | studentAgentIntegration/studentAgentApi/guidanceApi/coreIntegration; backend StudentAgentIntegration, NativeStudentStreaming, AgentStreamCredential and provider/fallback tests. |
| Allowed UI impact | Preserve direct `/guidance` and `/dashboard`; do not repoint Guidance to another feature or invent chat. Existing assigned-Agent entry may be exposed with its existing label/flag/Student role in footer, no new flow. All feature sources stay unchanged. |

## 6 Enterprise authoring / participation (supporting UC-12/13)

| Field | Current implementation |
|---|---|
| Actor / entry | Enterprise; `/enterprise` exists only with Core enabled; AccountPage already redirects Enterprise there. |
| Components / state | EnterpriseWorkspace + ScenarioTaskEditor + JsonField; server me/membership/versions/stats/skills, owner/version-scoped draft/slug, busy/error/dirty/preview state. |
| API / backend | `/v2/me`, `/v2/enterprises/me`, `/v2/enterprise/scenario-versions`, skills, enterprise participation-summary; scenario create/save/clone/preview/publish via existing ScenarioAuthoringService. |
| Preconditions | Backend membership/ownership and separate publishing permission; registration alone does not authorize publication. Published versions are immutable; new work is a draft/clone. |
| Main / navigation | Account role handoff → owned scenario selection or new draft → metadata/tasks/resources/provenance → save → preview exact saved version → publish only eligible reviewed draft. Aggregate participation excludes private evidence. |
| Alternatives / failures | Local owner draft restore, clone, resources upload, immutable published form, permission/role denial, validation/revision/network errors; busy/dirty guards preserve work. |
| Tests | Existing scenario authoring/workflow/ownership/file tests and full backend suite; live isolated Enterprise UI/API smoke to be recorded separately. |
| Allowed UI impact | For Enterprise links only to the existing Core-enabled route; retain its server guards and role landing, never create an enterprise marketing/auth flow. No editor or publishing-rule changes. |

## 7 Administration (UC-07/08/09)

| Field | Current implementation |
|---|---|
| Actor / entry | Server ADMIN role (business ADMINISTRATOR); `/admin` → `/admin/knowledge`, `/admin/agents`, `/admin/usage`, enabled by Agent flag. Legacy usage alias preserves query/hash. |
| Components / state | AppShell keeps focused Admin chrome/direct shared logout; AdminStudioLayout owns existing sidebar/collapse/mobile Dialog. KnowledgeStudioPage/useKnowledgeData/Builder/Usage own feature state and current actor access. |
| API / backend | `/v2/admin/knowledge-bases`, scoped assets/ingestions/retry/publish/tests; admin agents immutable config/preview/publish/assignment; aggregate usage; existing guarded controllers/facades. |
| Preconditions / main | Current Admin authority. KB upload is staging; READY is not publication. Explicit publication/reindex guards and saved immutable config preview/publish/assignment remain. Admin preview/KB tests never create Student sessions. Usage excludes private transcripts. |
| Alternatives / failures | Empty KB/agent lists, parse/provider/retrieval failures, unchanged reindex, source replacement, revision conflict, cancellation/retry and unknown metrics; prior published version survives failure. |
| Tests | uc07Cleanup, platformNavigation/Integration, knowledgeStudio/Documents/Polling, ui Dialog tests; backend AgentPlatform/KnowledgeParser and usage privacy tests. |
| Allowed UI impact | Shared logo/tokens only; retain existing focused shell, direct sign-out, role condition and all feature routes/components. No admin feature editing or unrelated navigation restoration. |

## 8 Internationalization

| Field | Current implementation |
|---|---|
| Actor / entry / state | All actors; root I18nProvider above router, `careersim_ui_language_v1` validated preference; `useI18n`, LanguageSwitcher, existing translator/dictionaries. |
| Main / alternatives | VI/EN changes text/metadata in place, persists best-effort and syncs tabs. Account/assessment/simulation state is not remounted by locale. Server content uses supplied translation projection where available. |
| Failure / API | Invalid/blocked storage remains usable; unknown future/user/model prose is not remotely translated; no new translation API. |
| Tests | i18n/translate tests include answer preservation, storage failure, tab sync and evaluated explanation/score invariance. |
| Allowed UI impact | Compact option labels and new dictionary entries in existing translator only. Preserve preference key/provider/lifecycle. |

## Rejected-redesign differences and prevention

These are source-observed differences, not fabricated live failure claims:

1. Rejected AppShell moved direct Home/Guidance/sign-out into a new closed account menu and restructured actor state. Rebuild edits main's markup/navigation labels only; direct destinations, actor effect, logout callbacks/locks/error/invalidations remain.
2. Rejected LandingPage removed useAssessment and Hero status/result handling. Rebuild retains that main data flow, completed-result entry and secondary assessment action, without claiming legacy state is a v2 score.
3. Rejected custom PreviewCard/carousel bypassed the shared SimulationCatalog. Rebuild retains the original landing SimulationCard with an opt-in compact view and reuses SimulationCatalog state views; default catalogue/workspace behavior is preserved.
4. Rejected Journey removed existing assessment/simulation/dashboard actions. Rebuild retains those actual destinations in the four-step presentation. Hero/CTA retain existing secondary destinations too.
5. Rejected evidence used empty real catalogues plus populated browser fixtures rather than complete real student journeys. Rebuild establishes clean-main tests and a disposable, explicitly synthetic API-backed stack; live journeys and mocks are reported separately.
6. Main itself has landing samples retained after empty/API failure, a three-record slice, and legacy assessment state in its Hero. Current request explicitly requires actual published data: reuse shared loading/error/empty/retry instead of making samples look published; preserve legacy state as a separate existing assessment link. No backend rule change.
7. Main has distinct Guidance, Dashboard and assigned Agents, feature-flagged routes and page-level server guards. Do not invent a universal auth redirect, repoint Guidance, restore deleted Syn UI or enable gated routes by visual assumption.

## Verification and implementation boundary

Clean-main frontend baseline: `VITEST_MAX_WORKERS=2; npm run check` PASS, 249 tests / 31 files, lint/build. Backend baseline PASS: 404 discovered, 398 passed / 6 skipped / zero failures or errors, full verify/Spotless/build. Enabled frontend baseline also PASS: 249 tests, lint/build. Isolated Compose validation/build/up succeeded on unchanged main, health-ready backend and fresh PostgreSQL. No UI source has been modified at this audit checkpoint. Plan follows only after this audit. Live stack: unique local project/ports/volumes, synthetic accounts/content only; no valuable/shared DB mutation, real Google or paid AI claim.

## Live baseline follow-up (after the pre-implementation checkpoint)

The local-profile Administrator fallback is intentional and was disabled only through a separate flow-verification runtime profile to make real guest and role checks meaningful. Contrary to the source-intended pending return described above, actual unchanged-main password login returns Student to /assessment: account-generation remount consumes the pending path twice. This is separately reproduced in the new boundary-mocked route integration test and tracked as D13. Feature-owned identity and HTTP/router source remains unchanged; the public catalogue workaround is exercised explicitly, never described as a passing pending-return journey. Enterprise classification PUT also fails with 500 on main (D12); only disposable synthetic fixtures were assigned IT through guarded SQL. No production business logic is changed.
