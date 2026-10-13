# US-03 conversation quality correction — 2026-09-29

## Scope and design

User supplied a failing conversation and explicitly requested more useful LLM explanations, compact evidence, correct DevOps/AI comparison, better web status and RAG test guidance. Plan: `plans/US-03-syn-conversation-quality.md`. Base SHA `1c53253ee1cc1373a78557c3a790298e00613ea0` plus dirty A/B/C/P1/P2 tree. No commit/push. Scope is US-03 frontend (Huỳnh Gia Hân); backend owner remains unassigned. Voice, scoring and publication of unreviewed corpus excluded.

The old guidance-report evidence-first layout is superseded for chat by this explicit user request. Response schema, ownership/consent, read-only tools and confirmed save controls remain.

Sequence: SynWidget free text -> existing controller/facade/lifecycle -> SynAgentPolicy + memory -> toolbox SQL/knowledge/web -> provider generation -> schema/claim/citation validation -> at most one fresh output repair with the same tools/context and deadline -> response -> compact references and collapsed details. Quick actions remain Standard. Native optional planning keeps its two-pass ceiling without a third repair. Provider consent/transport failures still recover honestly.

## Changes and rationale

- Policy recognizes explicit DevOps and AI engineering topics in order, never substitutes old/backend/cyber paths for these requests; expands controlled web query vocabulary and preserves comparison purpose. These topics do not invent new catalog simulations or reviewed path profiles.
- Comparison free text omits unrequested simulation tool; matched private evidence must actually have the requested careerTrack. Empty matching results no longer cause unrelated personal results to be substituted.
- Generation receives requested paths and source identities, plus pending draft for follow-up. RAG miss no longer prevents model calls: general explanations are permitted with disclosure; missing document content and fresh web facts must not be invented.
- Prompt instructs natural comparison/tradeoffs, concrete weekly practice and output, no unnecessary evidence dump. Local schema limits are also in provider prompt/schema. STANDARD token allowance rises to 800, SHORT 400, subject to configured maximum and overall 1000 cap.
- Invalid generated output can be repaired once within remaining deadline >=4.5s, using a fresh constrained answer and cached tool data. No raw rejected answer logged or persisted. Existing score/citation/overclaim checks remain active. Native two-pass mode unchanged.
- Web HTTP429 maps to RATE_LIMITED, HTTP401/403 to ACCESS_DENIED. UI explains failure in reply language rather than suggesting an empty successful search. Prompt distinguishes local catalog resources from freshly searched sources.
- SynWidget folds comparison and objective results by default, preserves expanded results for explicit review, shrinks citation labels, puts title/passages behind disclosure, preserves paragraph line breaks and shows a calm fallback notice.
- Continuation 2026-10-02: general comparisons/concept questions omit unrequested private results, resources and simulation cards. The comparison payload no longer claims there are no completed results when evidence was deliberately omitted. `PERSONAL_EVIDENCE_SCOPE` distinguishes selected context from complete student history. EN/VI negated simulation requests remove the suggestion capability while retaining requested personal comparison.
- Transport budgets: connection allowance 2s -> 5s; request cap 10s -> configured value up to 20s, always limited by the remaining overall turn deadline. No transport retry loop added. Existing stalled-body/cancellation and oversized-response tests retained.

## Tests actually run

| Gate | Result |
|---|---|
| `backend/.\mvnw.cmd verify` (2026-10-02, final) | Passed after transport-budget adjustment: 144 discovered, 140 executed, 4 opt-in skips; 0 failures/errors. Includes PostgreSQL Testcontainers. |
| `frontend/npm run check` | Passed: lint, 26 tests in six files, production build. |
| `docker compose config --quiet` | Passed. |
| `git diff --check` | Passed. |

Regression tests use the user's DevOps/AI sentence, remembered unrelated paths, comparison with no unsolicited simulation, controlled search query purpose, missing knowledge still calling generation, bounded repair recovering from malformed output with no repeated retrieval, pending plan forwarded to model, quota-specific web response, collapsed results and compact citations, and FREE_TEXT submission. Existing privacy/lifecycle/fallback coverage retained.

Test adjustments reflect intentionally changed behavior: empty knowledge no longer skips generation; pending-plan free text uses a consented model fixture; comparison no longer expects SUGGEST_SIMULATIONS; selected personal evidence fixture now includes its actual track rather than relying on unrelated fallback. No tests disabled or validators removed.

## Runtime evidence

Automated passes alone are not evidence of live provider reliability or semantic quality. Existing last-24-hour trace aggregation before this deployment showed 11 AI_READY, 7 STANDARD_POLICY, 1 TIMEOUT_OR_TRANSPORT, 3 MALFORMED_OUTPUT and 5 PROVIDER_REJECTED; these mixed existing test/user turns are diagnostic counts, not an evaluation success rate.

Docker's multi-stage source build failed after Maven Central truncated commons-compress/zstd plugin artifact downloads. To avoid a redundant dependency download, the backend image uses the exact local JAR produced by successful `mvnw verify`, copied into the same non-root Java 17 runtime image/entrypoint. Temporary Dockerfile and logs are under ignored backend/target. Frontend `docker compose build frontend` succeeded. This is explicitly not a claim that the Docker Maven source stage passed. Normal repository Dockerfile is unchanged.

2026-10-02 first smoke, model `gemini-3.1-flash-lite`, gateway `localhost:8080`, image `38cb384613e29223d0398b24021705863583a8f6cf072124b381db5c4bb92c8e`:

| Prompt/case | Observed result |
|---|---|
| `so sánh 2 hướng frontend và backend cho tôi` | AI_READY, 8579ms client wall time, no private references/results/simulation cards; natural comparison, no false claim of absent history. Turn `3a0e61de-25f4-431c-85da-99c7dfaa2212`. |
| `Idempotency trong backend là gì? Giải thích theo tài liệu đã duyệt.` | AI_READY, 2822ms; QUERY_EMBEDDING executed, retrieval NO_RELEVANT_KNOWLEDGE, only SEARCH_KNOWLEDGE selected; model labels general knowledge, no K/A/S citations or simulation cards. Turn `4b3a2d9e-11b4-4d21-bf3c-cfb2a7e15c00`. |
| `Tìm nguồn học Spring Boot backend trên internet.` | AI_READY, 6096ms; WEB_SEARCH attempted once, RATE_LIMITED; one output repair, two generation calls. Text explicitly distinguishes local catalog from unsuccessful live search. Turn `2f5456bc-6f7c-4702-88af-5148a7d24778`. |
| Browser: personal comparison with `đừng đề xuất simulation` | First and single manual retry both TIMEOUT_OR_TRANSPORT (turns `56189aab-fa63-48a4-8b3f-507b3ac44d55`, `2dc985f3-b7df-43a3-aac3-226822efae45`). UI visibly showed honest fallback notice, compact A/S citations, collapsed comparison/results and no simulations. Triggered HTTP-budget correction; these failures remain recorded. |

Saved captures: ignored `backend/target/syn-completion-*.json`. Database read confirmed 0 PUBLISHED knowledge documents. These results do not establish successful live RAG or live web retrieval.

Final runtime after HTTP budget correction: image `sha256:ea2a9e8bd7bebd78be2455cf32a066b4a2176aef9949eca7b5c864c1589e8240`, built from the final verified JAR, backend recreated and gateway restarted. Frontend remains verified image `55993dfe9532`. Gateway health reported UP; frontend/dashboard data loaded successfully. No data volume deleted or score/plan modified.

Final browser retest of the same personal-comparison/no-simulations prompt succeeded: turn `2656ac9b-5cdd-4231-a7af-d3028b1784ee`, AI_READY, one generation call. Browser displayed natural Vietnamese comparison, A2/S2/S3 compact references, collapsed comparison/result details, relevant follow-up questions and AI-assisted; no simulation suggestions/cards. Capture `backend/target/syn-completion-personal.json`; screenshot `backend/target/syn-completion-ui.png`. This establishes the observed final smoke result, not that all network failures are eliminated or the timeout change alone caused recovery.

## Remaining limits

RAG corpus still awaits actual human review and publication. General model answers without K references are not RAG success. Hybrid quality needs reviewed corpus and evaluation; publication script is unchanged. Web search is subject to provider quota and grounded metadata, with no arbitrary scraper/key rotation bypass. Topic recognition is still controlled vocabulary, not a general semantic planner. Overclaim checks are heuristic. UI presentation and a few live prompts cannot prove conversational usefulness across all requests.

Manual test guide: `docs/testing/syn-coach-manual.vi.md`. Architecture/fallback map: `docs/architecture/syn-coach-abc.vi.md`. Human code/content/UX acceptance pending.
