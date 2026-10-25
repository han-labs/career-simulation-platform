# Lessons Learned: Avoid Repeating Expensive RAG Agent Mistakes

## How to use these lessons

These lessons combine observed source behavior and historical project incidents. **OBSERVED** means code or retained project evidence supports the mechanism; it does not mean the incident was rerun while this skill was written. **RECOMMENDED** identifies a transferable prevention method. Extension ideas are not already-implemented capabilities.

The owner reports the latest application is now working and tested. Earlier failures remain valuable regression cases; they are not claims that the current application still fails. No live provider calls were made to author this package.

Read the relevant entry before changing a failing layer. Do not apply every possible repair at once.

For Java/React/PostgreSQL reproduction, also read implementation-guide.md sections 15–22. They turn the lessons into named use cases, concrete contracts and executable acceptance targets. Section 23 below lists **porting risks**, not additional bugs observed in an implemented Java version.

## 1. A schema rejection was called a quota problem

**OBSERVED:** a generated-question request returned `400 INVALID_ARGUMENT`, while simpler live provider requests worked. Generic "check quota/model" copy hid the request incompatibility. Historical evidence did not isolate every rejected schema keyword, so attributing it to one specific keyword would be guessing.

**RECOMMENDED:** retain safe HTTP/status/model/operation details, minimize a reproducer, compare accepted/rejected wire shapes, and test the supported adapter contract locally. Distinguish request/schema, auth, access, rate/quota, timeout and transport failures. Do not buy a key, add retries, or switch models to conceal an invalid request.

**Regression:** simulated 400 and 429 must have different categories; a malformed structured response must fail validation rather than become a fake tool call.

## 2. Successful embedding was followed by publication failure

**OBSERVED:** a PDF parser produced NUL/control artifacts. PostgreSQL rejected the text after costly embedding calls. "Processing failed" made the failure look like RAG/model trouble even though storage publication was the immediate cause.

**RECOMMENDED:** normalize and preflight persistable text/source spans before embedding, preserve original bytes, version normalization, disclose warnings, and classify the real failed stage. Removing a control artifact does not reconstruct a missing mathematical symbol or guarantee PDF reading order.

**Regression:** a synthetic PDF that extracts problematic controls; real PostgreSQL binding/publication; injected publication failure keeps the old active version.

## 3. One short document correctly produced one chunk

**OBSERVED:** a roughly 237-byte fixture yielded one 236-character chunk under a 1,200-code-point cap. This was suspected to be failed chunking, but the entire short text fit normally.

**RECOMMENDED:** explain actual text length, unit, profile, boundaries and indexed versus draft counts. Do not split a short file just to make the chunk count look impressive. One chunk containing the known fact can satisfy retrieval correctly.

**Regression:** small input is intact; the source span covers its original normalized content; Top K five may return one available candidate.

## 4. A small file still legitimately produced three chunks

**OBSERVED:** the supplied clinic guide was about 3.2 KB on disk and produced three chunks under the existing character-based profile. Boundaries, newlines and overlap influence count; file bytes and text code points are not interchangeable.

**RECOMMENDED:** show the exact indexed text sizes and source coverage. State that 1,200 Unicode code points is not 1,200 tokens. For an overlap O and cap C, approximate count formulas ignore boundary/group rules and should never replace the actual chunker output.

**Regression:** all facts near beginning/middle/end remain retrievable; size/overlap reports use the correct unit.

## 5. Retrieval output was mistaken for a generated answer

**OBSERVED:** the owner expected a concise answer while using a retrieval-only mode that correctly displayed the matching full passage. A one-candidate response looked inaccurate because search and generation were visually ambiguous.

**RECOMMENDED:** expose clear **Retrieve passages** and **Answer with sources** actions. Keep ranked evidence visible independently. Describe Top K as a maximum candidate count, not how many answer sentences should be extracted.

**Regression:** retrieval-only makes no answer-generation call; generation failure preserves the already retrieved passages and sources.

## 6. Text inspection overwhelmed the screen while chunk inspection hid useful details

**OBSERVED:** cards per extracted line and large KB statistic cards wasted space; chunk text alone did not expose enough size/profile/source context. KB names were nested in the wrong navigation area.

**RECOMMENDED:** use a continuous document-like preview, compact KB list/detail counts and a richer but restrained chunk inspector. Preserve detailed backend metadata without showing it as repeated visual clutter. Verify filled-blue hover/focus foregrounds across actual tabs/rows.

**Regression:** readable long documents, exact size/locator/profile inspection, correct KB workspace, no mobile overflow and usable keyboard focus.

## 7. Cross-language source highlights used different string units

**OBSERVED:** Python source offsets use Unicode code points, while JavaScript indexes strings in UTF-16 code units. An emoji can shift a naive frontend highlight even though backend spans are correct.

**RECOMMENDED:** declare an offset convention and convert at the UI boundary, or use an agreed mapping. Keep inserted separators distinct from source spans. Do not "fix" this by moving source offsets until the highlight looks right.

**Regression:** emoji, accented Vietnamese, long lines and cross-block spans reconstruct and highlight the exact intended normalized source.

## 8. Sequential embedding calls dominated ingestion

**OBSERVED:** a recorded long-TXT baseline spent approximately 98% of ingestion time in embedding. A larger PDF reached about 80 seconds and failed around its 25th embedding request. Chunking and PostgreSQL writes were not the measured bottleneck.

**RECOMMENDED:** profile stages before optimizing the parser/database. Compare provider batching, client reuse and transport costs with the same safe texts/profile. Put batching at the provider boundary; verify SDK grouping, ordering and response count. Batching is not asynchronous provider Batch API and is not concurrency.

The implemented small optimization used sequential synchronous batches of at most three chunks. Recorded paired samples improved long-TXT time and reduced calls; those samples do not prove every corpus/account has the same speedup. Check the current target's own measurements.

**Regression:** 1/3/4/multiple-batch inputs, exact vector ordering, malformed dimensions/values, failed later batch, cancellation, and unchanged atomic publication.

## 9. Transport delay was not model reasoning time

**OBSERVED:** the provider boundary includes a Windows-specific IPv4 transport choice to avoid waits on unreachable IPv6 routes. Network setup can therefore contribute to apparent model slowness.

**RECOMMENDED:** separate DNS/connect/read/SDK-retry timing from inference and application queue wait. Investigate transport only when error/timing evidence supports it. Do not force every host to IPv4 or blame this mechanism for all provider failures.

**Regression:** safe transport classification, timeout bounded by remaining deadline, no leaked exception URL/key.

## 10. Top K one starved a multiple-fact question

**OBSERVED:** an owner conversation pinned Top K one. A fee question needed a consultation price and a membership discount from different chunks. The model requested several searches and hit its internal tool cap; the conversation did not have a five-message limit.

**RECOMMENDED:** inspect effective saved/pinned settings, then measure evidence coverage. A modest higher K can reduce extra searches when facts are distributed, but must fit the observation/input bounds. Keep both premises available; do not hard-code the calculated answer or automatically enable another tool.

**Regression:** two-premise deduction with valid sources; fresh run counters; saved Top K applied unless an explicitly allowed request overrides it.

## 11. Context inflation was mislabeled as generic token exhaustion

**OBSERVED:** internal guards serialized instructions, tool schemas and protocol into a conservative UTF-8 byte bound. A recorded request attempted 8,448 against an 8,192 input bound; another attempted a 30,625 reservation against 24,000. Provider-reported token usage was much lower and these were not recorded provider 429 failures.

**RECOMMENDED:** separately label input bytes/upper-bound estimates, output allowances, cumulative reservations and reliable provider usage. Remove repeated history/tool passages and unnecessary metadata before raising limits. A repeated search with no new evidence still adds protocol framing and consumes provider/time/budget.

Do not assume paying the provider fixes an application guard. Do not infer model context capacity from a local bound. "Last dispatched input" can be smaller than the next blocked request; those diagnostics describe different requests.

**Regression:** distinct input/reservation/reported/output failures; no fabricated usage when SDK metadata is absent; blocked requests are not dispatched.

## 12. Memory should not replay every old tool observation

**OBSERVED:** replaying complete old native protocol groups grew context. The repair uses recent successful same-chat user/assistant text pairs and keeps native call/response protocol only in the active run. Local ten-turn tests passed with deterministic models.

**RECOMMENDED:** store history durably but send a bounded window. Preserve current native correlation; old answers are not new evidence. Use server-side scoped reads, not browser-transcript state as the source of truth. No cross-chat memory is required for same-chat follow-ups.

**Regression:** many consecutive turns, permitted prior text present, prior tool responses absent, another conversation/Agent isolated, and revoked-source turns handled under the approved policy. Ten scripted turns do not prove a live model always answers correctly.

## 13. The model spent its last opportunity requesting another tool

**OBSERVED:** the controller originally could reach the tool/decision cap before the model produced a final answer. A repair removes declarations at the tool cap or last allowed decision and asks for a supported final/abstention within existing bounds.

**RECOMMENDED:** reserve a conclusion opportunity, report remaining budgets, discourage equivalent searches without a new lead, and identify no-new-evidence observations accurately. Do not hard-code "search twice" or forbid legitimate new leads merely to reduce request counts. A model that still requests an exhausted tool must remain rejected.

**Regression:** final answer and abstention at the cap, genuine sequential two-tool behavior, bounded repeated searches, invalid requests still fail, fresh budgets per user run.

## 14. Defaults changed, but saved chats did not

**OBSERVED:** runtime source defaults, env overrides, saved Agent settings and conversation-pinned versions could disagree. Existing chats kept earlier Top K/tool/history settings. Locally edited server caps did not automatically rewrite saved configurations.

**RECOMMENDED:** inspect effective run limits/config version, not README alone. Document restart after backend/env edits and Save → New chat after Agent configuration changes. Do not silently modify old snapshots or owner settings.

**Regression:** old chat retains pinned settings, new chat uses saved version, revocations apply immediately, and actual server caps constrain saved limits. Test configurable invariants against the configured cap or explicitly pin the scenario's baseline—not a stale magic number.

## 15. Citations were real IDs but invalid quotations

**OBSERVED:** live generation occasionally paraphrased a source quote or attached a citation to abstention. Strict substring validation correctly rejected the answer. One cancellation failure's precise malformed quote was not captured, so its exact quote defect remained unknown.

**RECOMMENDED:** request one verbatim supporting substring, preserve table punctuation, classify unknown source versus quote mismatch, and retain safe evidence for diagnosis. Do not silently substitute a quote, attach an arbitrary source, or make validation weaker to get a green result.

**Regression:** correct source/wrong quote rejected; wrong source rejected; unknown/clarification without fabricated support; source opens the exact active normalized text. Mechanical validity still needs semantic review.

## 16. The answer omitted a fee despite finding the exception

**OBSERVED:** a live answer correctly denied a free telehealth follow-up but omitted the supported numeric fee. Local repairs improved generic instructions to include the applicable fee when available and tested exception plus fee evidence. Final live re-verification was stopped by the owner; it was not retroactively marked passed.

**RECOMMENDED:** if evidence is correct but the answer incomplete, fix generation guidance/acceptance, not chunking or embedding. Label every expected fact and relevant exception; do not inject domain-specific answer text into production prompts.

**Regression:** cancellation rule plus consequence; exception plus applicable numeric amount; all reasoning premises cited. A scripted correct final only proves validation/pipeline handling, not the provider's future semantic reliability.

## 17. Scoped updates required more than one plausible passage

**OBSERVED:** the exam corpus explicitly stated that its Updates section overrides earlier sections. Some updates affected only a service or branch. Returning an earlier similar passage alone was insufficient for a correct current answer.

**RECOMMENDED:** use retrieval labels that include the required override, and prompt for source-stated authority/scope. Test the changed scope and unaffected scope separately. Do not assume "last section wins" in every corpus or compensate for missing update evidence with memorized facts.

**Regression:** old versus current fee, branch-specific suspension versus unaffected branch, supported arithmetic, and unspecified discount stacking remains unknown.

## 18. Fake-model passes were mistaken for live readiness

**OBSERVED:** local controller/citation tests passed while live models still sometimes returned malformed final JSON, omitted facts, or hit 429. A later successful smoke did not erase earlier retained failures or skipped final cases.

**RECOMMENDED:** keep engineering, native-provider and human acceptance evidence separate. Record model/SDK/fixture/status and actual retrieved passages for failures. Honor deadlines and stop instructions. Never claim a final corpus suite passed because the fake model returned the expected string.

**Regression:** doubles test branch/state invariants; opt-in native tests verify actual calling/correlation; human questions assess semantics/unseen behavior. Report skipped, partial and rate-limited status precisely.

## 19. An LLM fallback was not an embedding fallback

**OBSERVED:** the manual Groq LLM adapter shared the existing controller and retrieval, but document/query embeddings still called Gemini. Some Groq native calls worked; later calls hit rate limits or invalid citations, so live status was partial rather than fully verified.

**RECOMMENDED:** draw the dependency graph before calling a fallback outage-proof. Switch answer models manually when that is sufficient; no re-embedding is needed for an LLM-only switch. An embedding-provider change needs its own compatible profile/index migration and quality tests.

**Regression:** same evidence and permissions across LLM adapters, native call correlation, distinct provider errors, no automatic paid switch, and honest failure when the embedding service is unavailable.

## 20. Chat state races targeted the wrong run

**OBSERVED:** immediately after Send, UI state briefly reused a previous completed run ID as pending. Stop could therefore address the wrong run. New-chat creation also needed composer protection to avoid submitting to an old selected chat.

**RECOMMENDED:** populate pending state from accepted server run/message IDs, disable unsafe transition actions, scope caches by Agent/conversation, and keep errors/citations attached to their own turn. Reload should read persisted state, not resend.

**Regression:** immediate Stop after acceptance, creation/switch race, reload during a run, late provider result stays cancelled, long wrapped messages, correct citation attachment and unavailable-source state.

## 21. Evaluation UI became a second product instead of a testing aid

**OBSERVED:** generated questions, review/golden suites, run history and cards added product complexity. The owner later chose one Test KB flow; existing backend/history/tests were preserved rather than destructively removed.

**RECOMMENDED:** separate product scope from engineering verification. Start with a small human-approved question matrix and retrieval/answer diagnostics. Defer UI explicitly without deleting requirements/history or forcing Agent implementation to depend on a test-management subsystem.

**Regression:** removed entry point really is hidden; retained engineering evaluation still works; no migration/data deletion disguises a scope simplification.

## 22. Broad repairs consumed time without identifying the failing layer

**OBSERVED:** suspected "bad RAG" could actually be storage controls, schema rejection, repeated tool protocol, weak Top K, invalid quotation, a UI race or provider rate limits. Changing chunking/model/architecture all at once would have obscured which fix worked.

**RECOMMENDED:** capture one minimal failing case, locate the immediate failure and contributing factors, patch one contract, run targeted tests, then relevant regression. Keep implementation phases small and use current code/status rather than restarting from an old plan. Independent review is valuable when authorized; otherwise disclose self-review.

**Regression:** preserve the original failing fixture/sequence and test the invariant, not merely the updated English error wording. Protect owner data with disposable schemas; verify cleanup targets before deleting anything.

## 23. Port behavior to Java; do not translate Python syntax mechanically

**PORTING RISKS / RECOMMENDED CHECKS, NOT OBSERVED JAVA FAILURES.** The target backend is Java; no Java application was compiled or live-tested while these documents were updated. Reuse the host's framework, build, auth, migrations and frontend conventions.

| Risk | Preventive implementation rule | Regression / blueprint reference |
|---|---|---|
| String.length counts UTF-16 units, unlike the chosen code-point profile | Explicit code-point offset/count conversion; UTF-8 bytes measured separately | Emoji/Vietnamese chunk and highlight round trip; K-T03/K-T05, section 19.3 |
| Assuming .env automatically loads in every Java run | Use and document the actual server environment/config loader | Configuration starts through documented command; no key in frontend; section 19.5 |
| Transaction annotation not invoked through the intended proxy boundary | Short real transaction boundaries; explicit rollback policy; no provider I/O under long DB locks | Inject final-publication/config-save failure and reload another transaction; K-T04/A-T02 |
| Treating @Async/common-pool futures as durable work/cancellation | Commit queued record, atomic claim, bounded dispatcher, persisted cancel/restart state | Accept then restart/Stop/late completion tests; A-T04/A-T08 |
| Calling SDK automatic tools bypasses backend permissions | Native declarations/requests only; backend validates/executors; correlated response | Disabled tool and out-of-scope KB never execute; A-T03/A-T05 |
| Using generic numeric arrays as if pgvector SQL/type mapping works automatically | Explicit supported mapping and parameterized cosine query in actual PostgreSQL | Known vectors/rank/filter/profile; K-T06 |
| JPA dirty tracking misses in-place JSON changes or exposes entity internals | Explicit DTO/persistence updates; protected protocol not in public serializer | Reload run/events/config from a fresh transaction and inspect API; A-T02/A-T05 |
| Java parser emits different text but reuses an old chunk/profile identity | Define Java extraction/normalization profile; test table/row/page structure before embedding | Exact normalized blocks/spans and explicit reindex; K-T03/K-T07 |
| New default/server limit is assumed to change a saved conversation | Expose/pin effective saved limits and provider/model version; Save then New chat | Old versus new conversation settings; A-T04/A-T07 |
| H2/Mockito-only tests are reported as PostgreSQL atomicity/vector proof | Use disposable PostgreSQL+pgvector for DB contracts, doubles for provider branches | K-T04/K-T06/X-T01, not invented H2 vector support |
| Original no-login local mode bypasses an existing host's ownership | Server-derived principal/tenant scope applies to all KB/Agent/chat/source routes | Host unauthorized-object/isolation tests plus A-T03/A-T07 |

Do not copy the original Python backend or Alembic migrations into the Java application. Implement the observable use cases and invariants, not a second backend. Dependency selection and provider builder signatures must be checked against the target's pinned versions; a plausible class name is not an integration test.

## Fast diagnosis card

| Symptom | First concrete evidence to collect | Do not do first |
|---|---|---|
| Upload Failed | Job stage/category, extracted controls, vector response shape, DB rollback | Change embedding model |
| Too few chunks | Actual normalized text length/unit/profile/coverage | Force artificial splits |
| Search appears wrong | Expected fact versus returned exact passages/scope | Assume generation failed |
| Correct passages, wrong answer | Model final, necessary facts, update/exception rules | Reindex the KB |
| Invalid citation | Delivered IDs and exact quote/span comparison | Weaken the validator |
| Fails on later turns | Effective pinned settings, active protocol bytes, fresh counters | Assume five-message cap or buy a key |
| Too many searches | Top K, new-versus-seen evidence, missing premises, remaining budgets | Add unlimited retries |
| Provider 400/429 | Actual safe HTTP/status/operation/model | Label both quota |
| Fallback still fails | LLM versus embedding dependencies, exact failing operation | Add automatic provider orchestration |
| Stop/resume/source wrong | Accepted run ID, scoped cache, current permissions/active source | Redesign the entire Builder |

## Final habit

For every repair, explain: **what failed, what evidence proves it, what changed, what remained unchanged, which tests actually ran, and what remains unknown**. That habit saves more time than adding a sophisticated RAG technique before understanding the existing pipeline.
