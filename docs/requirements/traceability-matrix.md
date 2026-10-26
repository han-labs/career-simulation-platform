<!-- documentation-sync-2026-10-22 -->
# Current catalogue traceability

Use [the requirements index](README.md), [selected report stories](user-stories/REPORT_SELECTED_10_US.md), [UC catalogue](use-cases/README.md) and [official ownership](../TEAM_ASSIGNMENTS.md). IDs are catalogue-qualified. The table is **functional scope coverage**, not equality or silent renumbering of Baseline IDs.

| Selected report US | Current UC | Person | Frozen Baseline v1 scope references |
|---|---|---|---|
| US-01 Register account | UC-01 | 1 | US-01 identity scope; separate registration flow |
| US-02 Log in | UC-02 | 1 | US-01 |
| US-03 Complete career quiz | UC-03 | 1 | US-04 |
| US-04 Explore simulations | UC-04 | 2 | US-06 |
| US-05 Complete simulation | UC-05 | 2 | US-07 |
| US-06 View simulation results | UC-05 | 2 | US-08 |
| US-07 Consult career agent | UC-06 | 2 | US-11/12/13/14; Agent/KB access prerequisites remain |
| US-08 Manage knowledge base | UC-07 | 3 | US-09 |
| US-09 Create AI agent | UC-08 | 3 | US-10 |
| US-10 View AI usage dashboard | UC-09 | 3 | US-15 |

UC-09 implementation follow-up (2026-10-26, human review pending): the existing
AgentUsageController/Query/Dao now exposes an additive safe dashboard view over
agent_execution, with server aggregates, UTC trends, filters and bounded keyset
pagination. AgentUsagePage reuses the current Admin Studio shell. Contracts and
metric/privacy semantics: [UC-09 API](../api/ai-usage-uc09.md). Focused evidence:
AgentUsageDashboardTest and frontend usageDashboard.test.jsx. No new migration
or analytics producer; runtime/model quality acceptance is recorded separately.

Baseline profile/dashboard US-02 is documented by supplementary UC-10/11; Enterprise US-05 by UC-12/13; Admin assessment US-03 by UC-16; account administration also supports UC-15. These are supporting relationships, not new selected stories or person assignments. UC-14 is deferred. The exact prior-report UC renumbering comes from the supplied [UC index](use-cases/README.md#historical-uc-renumbering-2026-10-21--current).

For each selected UC, the imported spec now links production source/tests and distinguishes existing automated evidence from pending UAT. [The alignment register](REPORT_CODE_ALIGNMENT_2026-10-22.md) records unresolved report/code differences; do not mark a full flow accepted from an endpoint or a mock.

## US-04 / UC-04 implementation trace (feature branch)

| Acceptance criterion | API or component | Source files | Test/evidence |
|---|---|---|---|
| Browse published simulations without a prior quiz | `GET /v2/scenarios`, `CoreCatalogPage` | `backend/src/main/java/edu/hcmute/careersim/catalog/controller/VersionedCatalogController.java`; `backend/src/main/java/edu/hcmute/careersim/simulation/dao/ScenarioDao.java`; `frontend/src/features/simulations/pages/CoreCatalogPage.jsx` | `VersionedCatalogControllerTest`; `docs/evidence/US-04/us-04-browse-simulations.md` |
| Search, filter, sort and cursor pagination | Paged v2 catalogue; API-derived Career Track dropdown; fixed IT; URL state | `VersionedCatalogController.java`; `ScenarioDao.java`; `ScenarioReadDao.java`; `CoreCatalogPage.jsx` | Isolated industry and Core cursor tests PASS 2026-10-25; PO browser TC-04-05 PARTIAL (nine items, no next page) |
| View role, expected tasks, difficulty and supported public metadata | `GET /v2/scenarios/{slug}`, `CoreSimulationPage` | `ScenarioContracts.java`; `ScenarioAuthoringServiceImpl.java`; `CoreSimulationPage.jsx` | Public DTO/Core integration tests PASS; PO-reported UAT recorded separately |
| Useful empty/no-match/error states | `SimulationCatalog` | `frontend/src/features/simulations/components/SimulationCatalog.jsx`; `CoreCatalogPage.jsx` | `coreBrowse.test.jsx`: focused file 19 PASS on 2026-10-25 outside sandbox |
| Browse without locked model answers | Public DTO allowlist; protected model-answer endpoint remains separate | `ScenarioContracts.java`; `ScenarioAuthoringServiceImpl.java`; `EnrollmentController.java` | Public DTO serialization and real isolated Core projection negative checks PASS |
| Start after login and return to selected simulation | Existing account flow with internal pending path | `CoreSimulationPage.jsx`; `frontend/src/features/identity/AccountPage.jsx` | `coreBrowse.test.jsx`; live Google OAuth UAT pending |

Feature report: `.agent/reports/us-04-uc-04-browse-simulations-verification.vi.md`. PO-reported UAT 2026-10-25: 14 PASS / 1 PARTIAL / 1 BLOCKED / 0 FAIL; TC-04-04A passed all 8 checks. Codex did not observe the full PO session. Final commit SHA, redistribution/published-content integrity decision and integration review remain pending. Pre-enrollment survey is a UC-05 requirements-review item (D10), not a UC-04 defect.

## Historical initial matrix

### Approved Admin Studio repair — 2026-10-26, human UAT pending

| Selected UC / US | Implementation and focused evidence |
|---|---|
| UC-07 / US-08 | KnowledgeStudioPage card grid/native links, direct document actions, exact READY review, top publication summary and Inspector. `knowledgeStudio`/`knowledgeDocuments` tests retain revision/draft/publication invariants. |
| UC-07 reprocess alternative | KnowledgeAdminController → KnowledgeStudioServiceImpl → existing DAO/parser/embedding pipeline; job If-Match, stored source, generation fence, UNCHANGED and READY. Four new AgentPlatformIntegrationTest tests prove authorization, no duplicate upload/version, parser invocation, failure/cancel preservation and retired-source denial on isolated PostgreSQL. No migration. |
| UC-08 / US-09 | AgentBuilderPage/ConfigurationForm/Preview, PublishAssignPanel: coherent partial Save, four supported tabs, explicit grants, independent saved-config Preview, bounded UI-only history, source revalidation, exact publish/assign/revoke. `agentBuilder`, `publishAssign`, `previewSources`, `platformIntegration` tests. |
| UC-09 / US-10 | Existing aggregation unchanged; compact KPI/filter/history presentation, explicit cost statuses and zero estimate. `usageDashboard` and unchanged AgentUsageDashboardTest cover metadata privacy, scope/pagination and cost coverage. |

Browser evidence is separate from provider/human acceptance. Read-only canonical
runtime checks do not prove live embedding, generation, publication or assignment.

### UC-07 / selected US-08 implementation evidence — 2026-10-23

Approved plan: `.agent/plans/UC-07-manage-knowledge-base.md`; baseline `62bbe10`.
This supplements the existing mapping to **Baseline US-09**, without renumbering.

| Selected US-08 AC | Implementation / executable evidence |
|---|---|
| AC1 create/manage | `KnowledgeBaseDialog.jsx`, `KnowledgeStudioPage.jsx`; `knowledgeStudio.test.jsx` covers pinned revisions, preserved conflict drafts, late KB reads and write-success/list-failure. |
| AC2 five formats/status | Existing parser/chunker/profile retained; `KnowledgeParserContractTest` checks extraction blocks plus unchanged chunks/locators across all formats; `AgentPlatformIntegrationTest` checks pre-embedding snapshots, storage failure and retry. |
| AC3 publication/provenance | `KnowledgeDocumentInspector.jsx`; exact version/attempt detail, Unicode, missing historical snapshots and rapid-selection tests; full text omitted from lists. |
| AC4 independent retrieval/answer | `KnowledgeBaseTestPanel.jsx`; separate modes, accepted-query snapshot and revalidation; existing ExecutionService remains authority. |
| AC5 invalid/no-evidence/errors | Parser invalid-file tests; platform no-evidence/provider-failure tests; visible failure stage and snapshot availability. |
| AC6 failed replacement preservation | Platform tests inject staging/publication rollback, cancellation and stale revision; Reindex A verifies unchanged version and zero new embedding calls, with no retired resurrection. |
| UC-06/08/09 compatibility | Student/Builder regressions; `kbTestUsageCountsExecutionsNotIngestionOrReloadAndPreservesUnknownMetrics` verifies KB_TEST producer/aggregate contract without a dashboard expansion. |

See `.agent/reports/UC-07-completion.vi.md` for commands actually executed and
browser/live/human limitations. Source/test presence alone is not Human PASS.

The original 3-US matrix follows unchanged; it does not define today's selected IDs.

<details>
<summary>Historical content preserved unchanged</summary>

<!-- preserved-content-start -->
# Initial traceability matrix

| Requirement | Frontend area | Backend facade | Data impact | Required evidence |
|---|---|---|---|---|
| US-01 assessment | `features/assessment` | `AssessmentService` | questions, attempts, answers, scores | scoring tests, API tests, completed-flow capture |
| US-02 simulation | `features/simulations` | `SimulationService` | simulations, tasks, attempts, submissions, evaluation results | evaluator tests, hidden-answer-key test, vertical-slice capture |
| US-03 Dashboard and Syn | `features/guidance` | `GuidanceService` | dashboard summaries, plan drafts, messages, and provenance | adapter tests, score-stability test, confirmed-plan and fallback smoke proof |
| Catalog foundation | landing and simulation catalog | `SimulationCatalogService` | published simulations | backend tests, API response, running landing page |
| Identity foundation | auth/profile UI | `IdentityService` or `AuthService` | users, profiles, consent | role tests, inactive/locked tests, secret scan |
| Enterprise content | enterprise workspace | `SimulationManagementService` | simulation/task lifecycle | ownership and publish-state tests |
| External catalog research | future content-review tooling; no public runtime crawler | none until assigned | draft factual metadata plus per-record provenance | current terms/robots review, rate-limit evidence, field allowlist test, human publication approval |

Create a sequence-to-code trace from `.agent/templates/sequence-to-code-trace.md` before implementing each vertical slice.

<!-- preserved-content-end -->

</details>


Revised Admin Studio (2026-10-26): UC-07 counts/card navigation reuse the ingestion/publication
model; UC-08 experience v2 + owned Preview conversations use additive V29/V30 and the existing
execution/audit ledger. `AgentExperienceTest`, `AgentExperienceUpgradeTest`,
`AgentPlatformIntegrationTest`, `agentBuilder.test.jsx`, `agentPreview.test.jsx` cover the
new contracts. UC-09 aggregation/cost semantics are unchanged. Automated evidence and runtime
limitations are recorded separately; human UAT remains pending.

UC-08 explicit role-publication correction (2026-10-26): V31 / `AgentRoleBindingDao`,
`AgentRegistryServiceImpl.activateRoles`, role-aware `/v2/agents` and the compact Publish
panel replace per-Student assignment UX/access. `AgentPlatformIntegrationTest` verifies
both roles, atomic concurrent replacement/rollback, stale revisions, readiness, archive
and same-Agent re-publication; `StudentAgentIntegrationTest` retains ownership/private
evidence regressions. `AgentExperienceTest` and `agentBuilder.test.jsx` cover dynamic
bounded variables and legacy v2 readability. Native provider wire tests classify safe
HTTP/network failures. `RegistrationAssessmentIntegrationTest` exercises real auth-test
login/refresh/logout/relogin for Student, Enterprise and Administrator; AppShell tests
separately verify the local-workspace indicator. No UC-07/UC-09 architecture change.
