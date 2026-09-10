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
