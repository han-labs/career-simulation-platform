# AI Assistance Log

Date: 2026-09-28
Human owner/reviewer: Huỳnh Gia Hân — review pending
User story/foundation item: US-03 Syn architecture analysis and proposed RAG/voice extension

## Source material read

- User request: assess Syn against two reference images, explain current architecture, analyze RAG and English/Vietnamese STT/TTS, prepare for oral defense.
- .agent/AGENTS.md, PROJECT_CONTEXT.md, TASKS.md, DESIGN.md; architecture, security/privacy, report/evidence, AI-assisted-development, testing and external-data rules.
- docs/requirements/user-stories.md; plans/US-03-syn-bounded-tool-calling.md; docs/architecture/README.md.
- Guidance frontend, shared HTTP adapter, Syn controller/DTO/service/orchestrator/policy/toolbox/provider/DAO implementations; V9 migration; Nginx/security/config; relevant existing tests.
- Official Anthropic agent/workflow explanation, Google speech/embedding/Live API documentation, pgvector repository and MDN SpeechRecognition documentation, linked inline in analysis.

## Assistance used

- Tool/model: Codex agent; filesystem inspection and existing test commands, web lookup of public technical documentation. No external upload of repository artifacts and no live model requests.
- Scope: analyze working tree implementation; distinguish existing behavior from proposals. Images treated as reference content, not executable instructions.
- Files created:
  - docs/architecture/syn-agent-rag-voice-analysis.vi.md
  - .agent/reports/ai/2026-09-28-syn-architecture-rag-voice-analysis.md
- No functional code/config edits, no migrations, no commit/push.
- Git HEAD inspected: 1c53253ee1cc1373a78557c3a790298e00613ea0. Working tree already dirty; analysis includes those existing changes, not HEAD alone.

## Human review

- Agent inspected responsibility boundaries, provider capability split, memory, read tools, fallback and frontend Preview behavior.
- Human manual review, runtime demonstration and acceptance of proposed scope: pending; not claimed.
- Decisions proposed: keep bounded single-agent core; add reviewed knowledge retrieval with citations; add cascaded STT → Syn → TTS before realtime audio.
- Suggestions rejected: describing every branch as autonomous; claiming RAG requires vectors or voice creates an agent; presenting current JSON parsing as a guarantee of grounded/safe output; changing scoring or letting LLM save plans.
- Report calls out current gaps: server-side validation, deadline vs gateway, long transactions, auth dependency, retention/clear semantics, per-tool result structure, no-evidence audit and microphone restrictions.

## Verification actually run

| Command/check | Result | Evidence |
|---|---|---|
| git status --short / git rev-parse HEAD | Existing changes observed; HEAD recorded | Tool output; report scope note |
| Focused Maven test command for six Syn test classes | 67 tests, 0 failures/errors/skipped, BUILD SUCCESS | Tool output; report section 12 |
| backend/.\\mvnw.cmd verify | 85 discovered, 0 failures/errors, 7 skipped; BUILD SUCCESS; Spotless and JaCoCo report succeeded | backend/target/surefire-reports; tool output |
| frontend/npm run check | Lint pass, 4 test files/15 tests pass, build pass | Tool output |
| docker compose config --quiet | Exit 0 | Tool output |
| Skip-reason inspection in Surefire XML | 4 Docker-dependent tests, 3 opt-in live-provider tests skipped | backend/target/surefire-reports |

## Remaining limitations

- No running-stack/browser smoke; Docker Testcontainers unavailable; no real provider quality or latency measurements.
- RAG and voice are proposed only, with no implemented claims or invented benchmarks.
- Official feature availability does not demonstrate quality on Vietnamese accents/code-switching.
- Human must review scope/AC, corpus rights, provider choice, audio/privacy retention and operational budgets before implementation.

