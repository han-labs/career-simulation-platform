# US-03 — Syn core and vector RAG implementation evidence

Date: 2026-09-28  
Scope: approved P1/P2 in `plans/US-03-syn-core-vector-rag-voice.md`; continuation of existing work.  
Branch: `codex/syn-core-vector-rag`  
Base SHA: `1c53253ee1cc1373a78557c3a790298e00613ea0` plus uncommitted working-tree changes. This is **not** evidence for that SHA alone. No commit or push requested.  
Verdict: implementation delivered for technical review; **not full P1/P2 acceptance or production release** until the open gates below are resolved.

## 1. Implemented behavior and traceability

| Plan | Implementation and evidence | Remaining limit |
|---|---|---|
| C01–C03, C16 | Shared strict output validator; duplicate/trailing JSON rejection; Gemini scans every part; OpenAI rejects unexpected output types; score/reference guards and server-owned plan confirmation. Provider/domain/orchestration tests. | Structural and numeric checks do not prove semantic truth. Live generation suite remains opt-in. |
| C04 | Monotonic 20-second external budget with 2-second finish reserve; HTTP deadline covers full body; async cancellation, 8-call cap, 256 KiB body cap; gateway 30 seconds. Stalled-body and oversized-body tests. | Not a whole-system SLA under DB/server overload. Uses bounded async HTTP rather than a separate worker executor. |
| C05–C06 | Short repeatable-read snapshot, separate begin/network/finalize; unique request ID/hash, one pending turn/session, version and owner locks. PostgreSQL concurrency, replay, rollback and SQL-failure tests. | External billing cannot be exactly-once after a process crash. |
| C07–C08 | Typed tool data, explicit routing, degraded-tool metadata, max four tools; required knowledge cannot silently disappear from the plan. Routing/provider contract tests. | Deterministic routing still needs broader language evaluation. |
| C09–C10 | Bounded session state, server draft plan and ordinal follow-up, selected result ID reauthorized by owner, clear/cancel/TTL/late-response protection. UI keeps transcript only in mounted tab. | State extends the existing bounded summary format; not a new general-purpose slot/dialogue engine. |
| C11 | `syn_turns` supports onboarding with no evidence FK, reason/provenance/model/timing, phase calls, reported usage, tool status and source versions; V10/V12 migrations. | Trace stores executionReason separately from final responseMode, including provider-error → reviewed-extract transitions. Per-call cost is estimated only with reported usage and an operator-pinned model price snapshot. |
| C12 | Backend-generated A/S attempt IDs for authorized personal evidence; K chunk IDs plus active version/section/excerpt and backend-owned URL for knowledge. UI source details; citation tests. | Personal attempts are completed immutable records, not versioned knowledge documents; no arbitrary personal-evidence URL is generated. |
| C13–C14 | Core failures surface as errors; explicit Preview only; owner/role/active/consent rechecks before HTTP/finalization; production demo identity disabled by default. | Production login/identity integration remains a separately assigned prerequisite. Local smoke uses opt-in synthetic demo identity. |
| C15 | Direct owner + EVALUATED lookup for requested/remembered results outside ten recent dashboard entries. | No access to another student's attempt. |
| R01–R02, R06–R07 | Reviewed manifest validation, version/hash/profile, atomic publish, exact 768-dimensional pgvector search, active/published filtering, revoke checks through finalize and replay. Real PostgreSQL tests and API revocation smoke. | Draft corpus has not been approved or published. |
| R03, R10 | Real Gemini document/query embeddings, exact cosine + normalized lexical search + RRF, typed SEARCH_KNOWLEDGE on both provider branches; local lexical fallback. Actual embedding and vector smoke passed. | Full held-out lexical/vector/hybrid quality benchmark is pending human gold labels. A one-source smoke is not Recall@5. |
| R04–R05, R08–R09 | Citation identity/version checked against retrieved sources; no-source skips synthesis; invalid citation becomes a source extract; minimization/consent before external query embedding; no score or write tools. | Claim support and bilingual/adversarial quality still require review. Query minimization is not comprehensive PII detection. |
| R11 | Separate phase-call counts/timings and provider/model token usage, embedding metadata, Gemini thinking tokens, pinned decimal price estimates, partial/unknown cost status. Four pricing tests plus embedding contract test. | Requires deployment-specific rate snapshots; an undiscounted estimate is not an invoice. Failed/cancelled process billing cannot be reconstructed from accepted turns alone. |
| R12 | pgvector 0.8.2 / PostgreSQL 17, migrations V10–V12, isolated stack, actual dump/restore and restored vector query. | Production backup/restore operations remain an owner responsibility. |

Current file/component map, sequence diagrams, fallback rules and viva questions: [Vietnamese implementation guide](../../docs/architecture/syn-core-rag-implementation.vi.md).

## 2. Commands and observed outcomes

| Check actually executed | Outcome | Local evidence |
|---|---|---|
| `backend/.\mvnw.cmd -q spotless:apply verify` | 124 tests discovered, 0 failures/errors, 4 opt-in skips; 117 executed. Includes 14 PostgreSQL integration tests. | `backend/target/syn-final-verify.log`, `backend/target/surefire-reports/` |
| `frontend/npm run check` | ESLint, 24 tests across 6 files, production build passed. JSX component tests included by Vite pattern. | `frontend/syn-check-output.log` |
| `docker compose config --quiet` | Passed. | Command result in task history |
| `git diff --check` | Passed; Git reports ordinary CRLF normalization warnings. | Command result in task history |
| `docker compose --env-file backend/target/syn-smoke.env -p careersim-syn-verification up --build -d` | Isolated stack built and ran; gateway/UI/API checked. | `backend/target/syn-stack-final.log`; final verified JAR overlay described below |
| `-Dtest=GeminiEmbeddingProviderTest test`, opt-in live embedding enabled | Live document/query requests returned finite normalized 768-dimensional embeddings; local contract also passed. | `backend/target/syn-embedding-live.log` |
| Actual Gemini embeddings inserted and queried in isolated pgvector DB | Exact cosine matched synthetic SQL source, similarity **0.755662702192745**. This proves the path executes, not retrieval quality. | `backend/target/syn-live-vector-smoke.json` |
| Gateway `/api/health`, dashboard and Syn API smoke | UP / LIVE; exact replay, payload conflict 409, request recovery, clear/cancel, no-source Standard fallback passed. | `backend/target/syn-api-smoke.json` |
| Published source revoked after accepted response | Source 200 before → 404 after; replay and request recovery both 409. Fixture remains revoked. | `backend/target/syn-revocation-smoke.json` |
| Browser at `http://localhost:18080/dashboard` | Submitted SQL question; observed Standard extract `[K2]`; expanded source showed section, version 2, excerpt and source link; visual inspection completed. | Observed accessibility tree/screenshot in task; not claimed as automated E2E |
| `pg_dump -Fc`, restore to separate `syn_restore_verification` DB | Restored vector extension 0.8.2, 12 successful Flyway migrations and vector dimension 768. | Actual Docker/psql command results in task history |

The four default skips are three live generation tests and the opt-in embedding test. The embedding test was separately executed successfully; live generation was not rerun. Do not describe the four skips as PostgreSQL tests: PostgreSQL/Testcontainers ran successfully.

Environment: separate Compose project/volume, localhost-only ports 18080/18081/13000/15433, synthetic student/content, generation disabled for deterministic gateway smoke. No existing application database was migrated by this smoke. Local `.env` was already modified/staged before this increment and was not edited or included in evidence. Secrets were not printed or committed.

An intermediate Docker rebuild failed during Maven network downloads; a later normal rebuild succeeded. An initial custom-port browser request failed CORS while direct API calls passed; Compose now passes `CORS_ALLOWED_ORIGINS`, and the browser flow was rechecked successfully. These failed attempts are not counted as passing evidence.

## 3. Reproducible data and open acceptance gates

- `docs/data/knowledge/corpus.draft.json`: 20 original AI-assisted draft documents, EN/VI, five tracks; all `reviewed=false`.
- `evaluation.candidate.json`: 60 provisional cases (40 answerable, 10 no-answer, 10 adversarial); not an independently reviewed held-out gold set.
- `development.json`: three separate development examples.
- Server-local ingestion and evaluation commands are documented in [knowledge README](../../docs/data/knowledge/README.md). Both reject unreviewed inputs; neither is an LLM tool or public upload endpoint.

Before declaring all plan exit gates met:

1. A human content reviewer must approve correctness, rights/provenance and supporting chunks; freeze an independent held-out set before tuning. No approval is inferred from permission to implement code.
2. Publish that approved corpus in an evaluation environment, then measure lexical/vector/hybrid Recall@5 by language group and review generated claim support, abstention and adversarial cases against plan §7.2. No numeric quality target is marked passed here.
3. Complete production authentication integration with its assigned owner. Current backend authorization checks and demo smoke do not implement login.
4. Configure and pin deployment-specific provider price snapshots before operational cost reporting. The accounting code is implemented; no provider prices or billed totals are fabricated. See [usage/cost configuration](../../docs/deployment/syn-usage-cost.md).

P3 turn-based voice and P4 realtime are intentionally not implemented: the user's implementation authorization was P1/P2. Their planned architecture and dependencies remain in the approved plan. No audio collection, STT/TTS call or realtime connection was added.

## 4. Review and delivery status

AI-assisted implementation and automated/browser verification completed by Codex. Human code/security/content review is pending; frontend owner remains Huỳnh Gia Hân and backend/auth/ops ownership is not invented. See [AI assistance log](ai/2026-09-28-syn-core-vector-rag-implementation.md).

Earlier foundation reports describe earlier code. Their long-transaction/no-vector statements are historical and are superseded for this increment by this report and the current implementation guide.


For the final audit/usage corrections after the full normal Docker build, the JAR produced by the passing local Maven verify was copied onto that same runtime image using a temporary Dockerfile under `backend/target`. Backend/gateway were recreated without downloading Maven dependencies again. This preserves the runtime image/user/entrypoint and avoids relabeling an older image as the final source. Final API smoke and artifact digest are recorded in `backend/target/syn-final-artifact-smoke.json`.

Final observed checks: health UP; personal references A1/S1 matched the authorized dashboard attempts; exact retry returned the same accepted response; PostgreSQL trace contained both `executionReason` and `responseMode`, plus cost status NO_EXTERNAL_CALLS and totalEstimateUsd=0 for the Standard-only turn. The regression test additionally verifies MALFORMED_OUTPUT → REVIEWED_EXTRACT preserves both values. Browser reload after this deployment rendered the live dashboard successfully.

- Verified JAR SHA-256: `5f30b1f6152ec00da3dda8f6623f3cac4a414054331cc98d36a1f7e80d936b35`.
- Running backend image: `sha256:ffa994f7154db8d7da2e5eeab9420d4e3ceb582db6741fd927a9d5d33c47b584`.
- Temporary evidence-query correction: the first diagnostic SQL used nonexistent `trace_metadata`; corrected to migration V12's `trace` column and observed the persisted result. This was a smoke-script error, not a passing query or an application schema change.


Final continuation also closes the explicit-plan-save error path: network/auth/server failures retain the draft and display retry; a saved notification requires `saved=true`; repeated clicks while saving are blocked. Preview mode does not POST a plan. Added two adapter tests and one component test, then reran frontend check (24 tests, lint/build passed) and rebuilt its Docker image. Usage/pricing validation additionally caught a nullable-token unboxing bug; it was corrected without weakening the test before the final 121-test backend verification passed.


Final browser plan smoke: Draft with Syn displayed DRAFT—NOT SAVED; an API read confirmed no persisted plan. Review before saving → Confirm and save displayed saved confirmation; a subsequent API read found the expected plan. RIASEC 32/29/26 and simulation score 67 remained unchanged. This modifies only the isolated synthetic verification student.

## 5. Gemini runtime reliability correction — 2026-09-29

The normal localhost stack repeatedly returned `STANDARD / PROVIDER_REJECTED` for free text while quick actions correctly returned `STANDARD_POLICY`. Diagnosis separated configuration, model availability and orchestration:

- the backend container had a valid Gemini key and `AI_PROVIDER=gemini`;
- direct `gemini-2.5-flash` text generation worked, but its native tool-planning response was malformed for Syn's contract;
- `gemini-3.5-flash-lite` structured generation exceeded the product deadline in observed live probes;
- `gemini-3.1-flash-lite` passed the one-pass strict structured synthetic live test in 4.572 seconds during diagnosis.

Runtime now defaults to backend-owned deterministic tool selection followed by one Gemini generation call. Native Gemini planning remains available only through `AI_GEMINI_TOOL_PLANNING_ENABLED=true`. This changes orchestration compatibility, not authorization, tool permissions, RAG, scoring, output validation or fallback.

Observed verification after rebuild on the normal `localhost:8080` stack:

| Check | Outcome |
|---|---|
| Focused provider/orchestrator tests | Passed |
| `backend/.\mvnw.cmd -q spotless:apply verify` | Passed; 122 discovered, 0 failures/errors, 4 opt-in skips |
| `frontend/npm run check` | Passed; lint, 24 tests in 6 files, production build |
| `docker compose config --quiet` | Passed |
| Runtime environment | `gemini-3.1-flash-lite`, tool planning `false`, provider `gemini` |
| Gateway health | `UP` at `http://localhost:8080/api/health` |
| Three consecutive free-text smoke turns after backend restart | `AI / AI_READY`; one `GENERATION` call each |
| Final source-built backend image smoke | Health `UP`; `AI / AI_READY`; model `gemini-3.1-flash-lite`; one `GENERATION` call in 3.678 seconds |

The three persisted traces reported input/output token usage and model `gemini-3.1-flash-lite`; observed end-to-end times were 3.359, 3.795 and 7.957 seconds. An immediately preceding smoke returned `PROVIDER_REJECTED`, so this evidence does not claim provider availability is guaranteed. The deterministic Standard response remains the required recovery path. The initial Docker build attempt also failed on a truncated Maven Central download and succeeded on retry; that was a dependency transport failure, not an application compile failure.

Final backend image: `sha256:24f904bc003290811bce5b020f60d5ba1544e8ea62476647158090c30f258588`.
