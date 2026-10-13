# AI Assistance Log

Date: 2026-09-28  
Human owner/reviewer: Huỳnh Gia Hân (US-03 frontend); human code/content/security review pending. Backend/auth/operations ownership remains unassigned.  
User story: US-03, approved Syn P1 core strengthening and P2 vector RAG.

## Source material read

- User's explicit approval of phases 1/2 and instruction to resume unfinished work rather than restart.
- `.agent/AGENTS.md`, PROJECT_CONTEXT, TASKS, DESIGN and relevant architecture, AI, testing, security and reporting rules.
- Approved plan `plans/US-03-syn-core-vector-rag-voice.md`, earlier architecture audit, existing guidance/identity/evidence facades, provider adapters, migrations, frontend and tests.
- Official pgvector and Gemini embedding documentation; links retained in the implementation guide.

## Assistance used

Codex continued the existing working tree: strict validation, bounded HTTP execution, turn lifecycle/concurrency/audit, scoped evidence memory, typed knowledge retrieval, pgvector ingestion/versioning/revocation, provider integration, UI recovery/source details, tests and documentation. Drafted original EN/VI corpus and candidate evaluation cases without marking them human-reviewed. SQL remains authoritative for scores/ownership; embeddings index reviewed knowledge only.

No subagents, production deployment, commit or push. No voice implementation. Existing unrelated/user edits and local `.env` content were preserved. During the runtime correction, `.env` was removed from the Git index and the root ignore rule was corrected so local credentials cannot be committed accidentally.

## Human review

- Permission to implement P1/P2: received from the user.
- Human inspection of final code, source correctness/licenses, semantic support and release acceptance: pending; agent testing is not represented as human review.
- Rejected shortcuts: calling lexical search vector RAG, using draft labels as gold, publishing AI drafts as reviewed, holding DB transactions across network calls, silently faking live fallback on auth/core errors, or treating numeric/citation checks as proof against hallucination.
- Implementation choices: exact vector baseline before HNSW; async bounded/cancellable HTTP; local operator ingestion, fail-closed reviewed manifest; no production corpus automatically published.

## Verification actually run

See [verification report](../us-03-syn-core-vector-rag-verification.md) for commands, counts, synthetic live embedding/vector evidence, browser/API smoke, PostgreSQL concurrency/restore, opt-in skips and corrected failures. Evidence belongs to the stated base SHA **plus the dirty tree**, not a nonexistent final commit.

## Remaining limitations

Full semantic/bilingual held-out benchmark and human corpus review remain open, as do production identity integration and deployment-specific price snapshot configuration. Synthetic smoke proves execution, not production readiness or quality targets. All detailed limits and acceptance deltas are recorded in the verification report.


Continuation at the user's repeated request completed per-phase/model usage, Gemini embedding usage and thinking tokens, decimal cost estimates with operator-pinned rates and honest partial/unknown states. It also corrected plan-save failure handling so an unconfirmed write cannot discard the draft or display a saved claim. Fresh backend/frontend and final runtime evidence supersede the preceding checkpoint counts/digests in the verification report.

On 2026-09-29, Codex diagnosed repeated Gemini fallback in the user's normal Docker stack using sanitized container configuration, persisted execution traces and synthetic live probes. It changed Gemini's default to server-planned single-call generation, retained native two-pass tool planning behind an explicit experimental flag, pinned the locally verified model default, added a configuration regression test and safe HTTP-status logging, and synchronized runtime/API/architecture documentation. No key, raw prompt or provider response was logged. Backend, frontend, Compose and three free-text gateway smokes were run; the exact outcomes and a transient provider rejection are recorded in the verification report rather than hidden.
