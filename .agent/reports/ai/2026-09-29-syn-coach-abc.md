# AI Assistance Log — Syn A/B/C

Date: 2026-09-29. Trace: US-03, user-approved A/B/C continuation. Frontend owner: Huỳnh Gia Hân; backend/operations owner not inferred. Base SHA: `1c53253ee1cc1373a78557c3a790298e00613ea0` plus existing dirty P1/P2 tree and this increment, no commit/push.

Read project entry instructions, .agent context/tasks/design/rules, requirements US-03, earlier P1/P2 architecture/verification, and actual policy/toolbox/provider/DAO/UI/tests. Used official Google Search grounding API documentation for provider metadata and Search Suggestions: https://ai.google.dev/gemini-api/docs/generate-content/google-search . User authorized implementation and basic tests; voice remains deferred.

Codex modified capability composition, Vietnamese/ordinal context, resource fallback, structured plan template, provider prompt boundaries, bounded transient HTTP retry, claim-pattern checks, Google grounded source adapter, consent/cache/trace handling, UI source panel and regression tests. Reused existing SQL/pgvector infrastructure. Added an operator publish script and review-friendly rendering of all 20 draft documents; did not fabricate reviewer metadata or publish unreviewed content.

No subagents. Existing edits preserved. Local ignored environment enabled web and embeddings for verification; keys were not printed or committed. Backend uses generic controlled topic query for web search, not the raw student transcript. Live tests use the existing synthetic local-demo student and do not submit assessments or save plans.

Code and content review by a human remain pending. User approval to implement is not represented as human approval of AI-authored corpus, model output or held-out gold labels. Heuristic guards are not a complete semantic verifier, Google grounding is not independent URL/fact verification, and server planning is not a general adaptive agent loop.

Commands, observed outcomes and known gaps are recorded in [verification report](../us-03-syn-coach-abc-verification.md). Manual acceptance scenarios are in [Vietnamese test guide](../../../docs/testing/syn-coach-manual.vi.md).
