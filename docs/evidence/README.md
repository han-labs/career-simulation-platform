# Course report evidence checklist

Every submitted claim should be reproducible from a repository commit.

- Repository URL and exact commit SHA.
- README commands verified on a clean machine or VM.
- Architecture diagram matching the deployed services and backend modules.
- Database migration history and non-sensitive sample records.
- A complete frontend-to-database vertical slice.
- Normal, alternative, invalid, authorization, status-lock, and persistence-failure tests.
- At least ten AI evaluation cases covering normal, ambiguous, unsafe/out-of-domain, malformed, timeout, and provider-failure scenarios.
- Executed LLM smoke-test prompt, actual response, model identifier, measured latency, date, and outcome. Do not fabricate measurements.
- Evidence that deterministic fallback still delivers assessment/simulation results.
- AI assistance log with human review, commands/tests run, rejected suggestions, and known limitations.
- Ubuntu VM or equivalent Docker Compose deployment evidence.

Use `.agent/rules/report-and-evidence.md` for the required report update format.
