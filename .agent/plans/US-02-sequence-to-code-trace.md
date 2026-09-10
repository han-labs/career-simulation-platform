# US-02 Phase 1 Sequence to Code Trace

Source user story: US-02 in the supplied proposal and `.agent/TASKS.md`
Sequence artifact: Phase 1 frontend mock slice
Implementation commit: Pending

| Step/message | Actor/lifeline | Endpoint or frontend action | Method owner | Layer/module | Data/provider interaction | Status | Notes/deviation |
|---|---|---|---|---|---|---|---|
| Browse catalog | Student | Visit `/simulations` | `SimulationCatalogPage` | React feature page | Local mock data | Planned | Loading then ready state only. |
| View simulation | Student | Visit detail route | `SimulationDetailPage` | React feature page | Local mock data | Planned | No answer keys exposed. |
| Open attempt | Student | Visit attempt route | `SimulationAttemptPage` | React feature page | Local mock data | Planned | Reached from detail action. |
| Select answer | Student | Choose radio option | `useSimulationAttempt` | React hook | In-memory state | Planned | Keyboard-native radio controls. |
| Navigate task | Student | Previous or next task | `SimulationNavigation` | React component | In-memory state | Planned | No answer required to navigate. |
| Submit | Student | Submit completed form | `evaluateAttempt` | Mock evaluator | Local task keys | Planned | Explicitly phase-only client mock. |
| Review result | Student | Read score and outcomes | `SimulationResult` | React component | Evaluated result only | Planned | Explanations unavailable before evaluation. |
