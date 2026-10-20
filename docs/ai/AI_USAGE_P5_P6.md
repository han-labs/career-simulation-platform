# P5/P6 AI usage and evaluation evidence

The product runtime reuses backend-owned Gemini/OpenAI adapters. No provider key,
private transcript, live paid call or Google OAuth authorization was used in this
implementation run. Local wire fixtures exercise actual native protocol/SSE and
mocked provider integration exercises actual application evidence, ledger and tools.
These are not evidence of a real model choosing three domain tools.

`backend/src/test/resources/guidance/agent-evaluation-cases.json` contains20 engineering
cases reviewed against the approved schema/evidence/safety rules, with explicit
human-review-pending status. `AgentEvaluationTest` executes the old P3 base validator
and current Student validator against the same synthetic objects. It writes actual
accept/reject outputs, canonical configuration hash and synthetic source refs to
`backend/target/agent-evaluation-results.json`, copied into the consolidated report
artifacts. Tokens/cost/model latency stay null because no model is invoked. This
before/after comparison measures validator behavior, not model-quality improvement.

Cases include grounded/refused chat, missing/forged evidence, unknown schema fields,
HTML/bidi injection, invented scores, career certainty, valid explicit reports,
missing/wrong disclaimers, fabricated practiced skills and draft-plan constraints.
Actual quiz score0 is separately verified from finalized Core evidence; Core task
completion never depends on passing score. Unsafe output is refused/failed, never
published as a valid report or confirmed business plan.

The usage dashboard's denominator is accepted executions grouped by mode/status and
UTC range. Model dispatch attempts, unknown model-attempt executions, native tool
attempts/success/denial, known token totals, unknown token executions, measured
finished-execution latency and known/partial/unknown estimates remain separate.
NOT_APPLICABLE denotes no generation model call, not a guessed zero token count.
Estimated costs are pinned pricing-snapshot estimates, not provider billing.
Embedding resource usage remains separate in the existing pricing snapshot.

Before course acceptance, the human team must review20 product cases, run approved
live models with a spending limit, record actual prompt/config/model/source hashes,
outputs/tool traces/usage and failure analysis, and demonstrate actual native choices
covering all3 domain tools plus grounded KB search. Do not overwrite missing live
results with planned/example measurements. Human review, live Google login and
production/Ubuntu delivery remain separate from local automated/browser evidence.
