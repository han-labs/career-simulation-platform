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
| US-07 Consult career agent | UC-06 | 3 | US-11/12/13/14; Agent/KB access prerequisites remain |
| US-08 Manage knowledge base | UC-07 | 3 | US-09 |
| US-09 Create AI agent | UC-08 | 3 | US-10 |
| US-10 View AI usage dashboard | UC-09 | 2 | US-15 |

Baseline profile/dashboard US-02 is documented by supplementary UC-10/11; Enterprise US-05 by UC-12/13; Admin assessment US-03 by UC-16; account administration also supports UC-15. These are supporting relationships, not new selected stories or person assignments. UC-14 is deferred. The exact prior-report UC renumbering comes from the supplied [UC index](use-cases/README.md#historical-uc-renumbering-2026-10-21--current).

For each selected UC, the imported spec now links production source/tests and distinguishes existing automated evidence from pending UAT. [The alignment register](REPORT_CODE_ALIGNMENT_2026-10-22.md) records unresolved report/code differences; do not mark a full flow accepted from an endpoint or a mock.

## Historical initial matrix

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
