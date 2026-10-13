# US-03 Syn coach A/B/C

Status: Approved for implementation by project lead in the current conversation, 2026-09-29.
Checkpoint: technical changes and basic tests delivered; content publication/held-out evaluation and live web success remain open. See `.agent/reports/us-03-syn-coach-abc-verification.md` for actual results and provider quota limits.
Actor: student. Frontend scope: US-03 (Huynh Gia Han); no inferred backend owner.

## Scope and acceptance
Extends docs/requirements/user-stories.md US-03 under the explicitly approved A/B/C increment. Preserve all existing uncommitted P1/P2 work. No voice, scoring changes, answer-key retrieval, job matching or autonomous writes.

- Mixed EN/VI resource/simulation/plan requests preserve each requested capability.
- Follow-up reuses ordered paths and selected result/draft; missing exact resource skill falls back within the requested path.
- Reviewed resources survive provider failures. Additional sources require consent and a bounded web search with server-owned URLs, timestamps and explicit live/unreviewed attribution.
- RAG retains reviewed-only publication, hybrid retrieval, citations and version/revocation checks. Human review of draft corpus remains an actual requirement, not inferred from approval to implement code.
- One bounded retry on Gemini 429/503, respecting Retry-After, cancellation/consent and overall deadline. Output checks reject broad competence/fit claims; no claim of exhaustive semantic verification.

## Flow inventory
Main: request -> ownership/snapshot/turn -> memory -> multi-capability policy -> local tools -> knowledge/web if justified -> minimized context -> generation -> validation -> persisted turn -> cards/sources.
Alternates: quick actions remain deterministic; ordinal path follow-up; published-only lexical retrieval when embedding fails; no relevant knowledge retains useful local evidence with explicit missing-source status.
Exceptions: no consent prevents external requests; unsupported topics never searched; web disabled/empty/provider failure preserves local results; unknown citations rejected; retry bounded; lifecycle ownership/clear/expiry protections retained.

## Responsibility and change surface
Policy/domain: capability composition, ordered references, resource relevance and output claim checks.
Toolbox/service: deterministic execution, resource fallback, RAG and web result assembly; request-scope cache prevents repeating search on fallback.
Infrastructure: Gemini generation/retry; Google Search grounding adapter (existing backend key), only provider grounding metadata becomes source links; no arbitrary URL fetch.
DAO: published resource/knowledge queries, no scoring changes. Existing RAG schema reused.
Frontend: Syn source panel renders live attribution and required provider search suggestions in isolated sandbox; no direct frontend-provider calls.
Docs: architecture, configuration, verification report, AI log and Vietnamese manual test guide.

## API and privacy
Existing POST /api/v1/guidance/syn/messages. Additive optional webSearch object to response (status, sources, retrieval time, provider search-suggestion HTML). HTML only in sandboxed iframe, never React HTML injection. Generic career/skill search query assembled from controlled vocabulary, never raw student message, score, email, ID or transcript. Grounded summaries are external untrusted data. Citations and local result cards remain backend-owned. Plan save still requires user confirmation.

## Verification
Targeted regression tests using the supplied Vietnamese turns; multi-intent, ordinal context, local resource fallback, no-consent/empty/error search, provider retry limits, false/missing citations and overclaim checks. Existing controller/lifecycle/ownership tests retained. Required gates: backend mvnw verify, frontend npm run check, docker compose config --quiet, build and gateway health/API smoke. Real provider availability is reported separately from mocked correctness. Corpus publication/evaluation results only reported if actually performed after human review.
