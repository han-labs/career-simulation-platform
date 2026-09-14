# US-02 Phase 1 Sequence to Code Trace


| Step | User action | Code |
|------|-------------|------|
| 1 | Open catalog | SimulationCatalogPage → SimulationCatalog |
| 2 | Click card | Link to `/simulations/:slug` |
| 3 | View detail | SimulationDetailPage → SimulationDetail |
| 4 | Start simulation | Link to `/simulations/:slug/attempt` |
| 5 | Answer tasks | SimulationWorkspace → useSimulationAttempt.setAnswer |
| 6 | Navigate tasks | SimulationNavigation → goNext/goPrevious |
| 7 | Click Submit | Open SubmissionConfirmationDialog |
| 8 | Confirm | useSimulationAttempt.submit → evaluateAttempt |
| 9 | View result | SimulationResult |
| 10 | Try again | useSimulationAttempt.restart |
