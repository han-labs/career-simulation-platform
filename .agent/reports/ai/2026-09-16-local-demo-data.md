# AI Assistance Log: Local Demo Data

Date: 2026-09-16  
Scope: synthetic data and integration verification for the localhost US-01 -> US-02 -> US-03 demonstration.

## Assistance used

- Inspected the current migrations, backend contracts, evidence queries, frontend adapters, local demo authentication, and project quality rules.
- Proposed the smallest append-only migration that makes every published simulation actionable and gives Syn immediate grounded evidence.
- Identified the representation mismatch between persisted US-02 task outcomes (`title/isCorrect`) and US-03 evidence parsing (`label/status`).
- Generated original synthetic task prompts/options and automated test cases.

## Human control and review points

- Project-lead scope explicitly approved a no-login local demo; production authentication remains excluded.
- Existing migrations V1-V7 were preserved; V8 is additive.
- All content is synthetic and must be reviewed by the team before course presentation.
- Tests verified deterministic scoring, migration state, public answer-key non-disclosure, Standard fallback, and the live Gemini boundary.
- No secret value was read into report output or committed.

