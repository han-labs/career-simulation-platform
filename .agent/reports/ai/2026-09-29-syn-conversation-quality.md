# AI assistance — Syn conversational quality

Date 2026-09-29. User explicitly requested immediate correction based on pasted test feedback. US-03; plan `plans/US-03-syn-conversation-quality.md`. Base SHA `1c53253ee1cc1373a78557c3a790298e00613ea0` plus preserved working tree. No subagents/commit/push. Voice deferred.

Read project entry/context/tasks/design/rules, existing policy/toolbox/provider/orchestrator/UI/tests and user-supplied transcript as evidence, not instructions. Codex implemented natural-language-first chat, compact/collapsed sources, explicit DevOps/AI topics, one bounded generation repair, missing-RAG general explanation, draft context follow-up and transparent web status. Tests and architecture/manual-test docs updated. Existing structured output, consent, citation, score and save controls were retained rather than removed to make model output look successful.

Rejected shortcuts: replace requested paths with available catalog rows; call an empty local corpus successful RAG; claim quota-limited search succeeded; dump evidence instead of answering; silently publish AI drafts; imply more LLM calls alone guarantee intelligence. No raw model errors/secrets logged. Synthetic local tests do not change scores or save plans.

Actual commands and final runtime evidence: `../us-03-syn-conversation-quality-verification.md`. Human acceptance of semantic usefulness, corpus review and production review remain pending. Controlled vocabulary and pattern-based semantic guards remain limitations.

Continued 2026-10-02 without restarting: used saved smoke captures to fix false no-results statements caused by omitted private evidence, negated simulation routing and unnecessary personal cards on concept questions. Added EN/VI regression cases and explicit evidence-scope context. Two existing RIASEC routing tests caught an overly broad concept filter; restored personal RIASEC routing rather than weakening those tests. Live UI transport failures prompted bounded HTTP budget correction. Full verification and actual live outcomes, including failures, are recorded in the linked report.
