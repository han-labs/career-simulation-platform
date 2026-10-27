# Agent model reliability — 2026-10-27

Scope: UC-06/08 real-model recovery on `fix/final-integrated-system`, based on
`15ab47949bdc89b5226798e49d7c7518a9d737d6` plus unstaged repairs. This record
does not replace the existing Agent/RAG, authorization or usage architecture.

## Diagnosis and measured selection

The old native catalog exposed only the environment-selected Gemini model. The
same catalog validated Save and Preview; choosing an alternative failed before
dispatch. Separately, the configured legacy endpoint returned HTTP503. Working
embeddings did not establish generation availability.

A one-shot diagnostic ran inside the existing backend container with its existing
credential. It printed only configured booleans and safe result metadata. Each
candidate received one metadata GET and one minimal text request; no Student data
was sent. These are observations for this project, not general model rankings.

| Model | Metadata | Minimal inference | Inference latency |
|---|---|---|---|
| gemini-3.8-flash | 200 | Transport timeout | 22,009ms |
| gemini-3.6-flash | 200 | 200, exact marker returned | 2,327ms |
| gemini-3.5-flash-lite | 200 | 200, exact marker returned | 5,334ms |
| gemini-3.1-flash-lite | 200 | HTTP503 | 1,480ms |

Initial minimal-probe recommendation was3.6→3.5-lite. Owner's later actual Agent
runs established3.5-lite cited RAG success, while3.6/3.8 experienced transient
unavailability/deadlines. The revised UAT order is primary
`gemini/gemini-3.5-flash-lite`, then `gemini/gemini-3.6-flash`.
The 3.8 endpoint remains a supported selection but
is excluded from this measured default chain. 3.1 remains legacy-compatible.
There is no observed reason to replace the credential. Metadata success alone
never changes an inference failure into PASS.

Official compatibility references: [3.8 model](https://ai.google.dev/gemini-api/docs/models/gemini-3.8-flash),
[3.6 GenerateContent changes](https://ai.google.dev/gemini-api/docs/generate-content/whats-new-gemini-3.6),
[changelog](https://ai.google.dev/gemini-api/docs/changelog),
[function calling](https://ai.google.dev/gemini-api/docs/generate-content/function-calling).
The existing GenerateContent adapter is retained. Modern supported model requests
omit removed sampling parameters, use low thinking and preserve native function
messages/signatures within a model. JSON final responses remain schema-validated
by CareerSim before persistence. No external pricing is imported.

## Immutable configuration and bounded execution

Optional `experienceConfig.fallbackModelKeys` contains at most three ordered,
unique same-provider allowlisted keys, distinct from the primary `modelKey`.
Its raw value/order participates in the existing configuration hash. Legacy sealed
v1/v2 JSON without the field is neither rewritten nor rehashed. No migration.
Availability in Admin options means selected provider plus configured credential;
it is not evidence of quota, billing or successful inference.

`AgentModelFailover` is per execution and shared across native rounds. It cannot
dispatch tools. At most six generation attempts cover the entire execution, with
at most one retry per model decision. Backoff starts at200ms plus0–50ms jitter,
bounded by the remaining execution deadline. Multi-model attempts are capped at
12s and40% of remaining time while another fallback remains; the last candidate
has at most18s. Single-model legacy calls retain their execution budget. Connect
remains independently bounded at5s. Body streaming now enforces the same absolute
attempt deadline as headers. A timed-out model is not repeated before switching.

Eligible transient HTTP statuses:408/429/500/502/503/504. A missing model switches
directly. Classified timeout/reset failures are eligible; DNS/TLS/connect failures
are not blindly retried. HTTP400/401/402/403 never switch to evade a bad request,
authentication, billing or project restriction. Provider safety refusals remain
refusals. One malformed/schema-invalid output repair is permitted within the same
global attempt/round/deadline budgets. Invalid reports are never saved.

Authorized tool observations survive generation retries. Switching models omits
foreign opaque function signatures and passes the exact observations as untrusted
data. The bounded loop owns tool execution; a generation retry cannot replay a
business write. All four currently registered model tools are read-only. Plan
confirmation remains the explicit authorized source/hash command with existing
transactional idempotency; the model does not receive a write capability.

For configured failover, Student answer deltas are buffered until validation so a
failed attempt cannot contaminate the visible final answer. Existing single-model
streaming and cancellation/consent/source fences remain in force. Total failure
returns the temporary-unavailable state, not an invented answer; core stays usable.

## Audit and evidence boundaries

Existing execution JSON contains safe `MODEL_ATTEMPT` rows with requested/effective
model, attempt number, fallback source/reason, status and duration. Existing usage
scopes count each actual generation attempt, including unsuccessful ones. UC-09
projects only allowlisted model/reason metadata; private inputs, raw requests,
responses, arguments and credentials are excluded. Unpriced models keep UNKNOWN
or PARTIAL coverage under the existing operator-pinned estimator.

The Owner's 24-case UAT pack is reconciled in
`backend/src/test/resources/guidance/careersim-uat/cases.json` and
`AgentUatEvaluationTest`. Generated `target/agent-uat-evaluation.json` labels its
before/after results as deterministic controller/schema fixtures, not live model
quality. The before strategy is the pre-repair single-model dispatcher on the same
fixtures. A separate runtime report must record real RAG, source viewing, Student
tools, structured artifacts and publication evidence; no unrun case is a live PASS.

## Owner UAT quality correction

The real3.5 comparison run reached generation twice and retrieved5 passages, then
failed local output validation (MALFORMED_OUTPUT), not transport. Its raw rejected
response was deliberately not retained, so the historical exact field mismatch
cannot be asserted. Future diagnostics expose only a safe schema/citation check
category. UI distinguishes rejected output from provider unavailability.

The tested v1 had only2 rounds: tool round plus final round left no repair or
alternative-source round. Its KB_ONLY policy also required a citation for greetings.
The tested3.8 v3 had an empty fallback list. No saved configuration is silently
rewritten. New Agent defaults are4rounds,4tool calls,60s,GENERAL_ALLOWED. KB Test
keeps its separate strict KB grounding. Existing drafts must be consciously changed,
saved and tested in a new pinned chat.

Platform prompt v2 clarifies handbook versus simulation-catalogue selection, balanced
comparison retrieval, exact citation/suggestion limits, and actor-specific context.
V1 remains byte-exact/readable by its original hash reference. New immutable Saves
use the current v2 reference. No old sealed hash/publication proof is recalculated.

One bounded real native-adapter diagnostic with v2 and the exact5 authorized
synthetic passages from the failed comparison returned a validated761-character
answer,3literal accepted citations, both roles named,2,647ms. This is a live adapter
and validator observation, not an Admin Preview execution/publication proof.

## Student follow-through: constrained artifacts and admission

Three real Student turns successfully used assessment.lookup, simulation.explore
and simulation.myEvidence. The combined turn failed local validation after tools
succeeded; no rejected provider text was retained. Student requests now project
the existing closed schema through GenerateContent responseJsonSchema, including
with native tools on supported Gemini3 models. Existing local validators still
check evidence/quotes, bounds, practiced-skill provenance and exact report disclaimer.
The report wire budget is bounded at4096 output tokens; normal chat remains2048.
No provider API migration, separate tool/event system or schema relaxation occurs.
See the official [GenerateContent structured-output contract](https://ai.google.dev/gemini-api/docs/generate-content/structured-output).

Owner explicitly changed the Student policy during real UAT: explicit requests no
longer need a separate consent grant. This changes STUDENT_CHAT only. No grant is
created and no authentication is bypassed. Privacy revision, own session/evidence,
role-active publication, account status and cancellation remain dispatch/replay
fences. Other consumers keep their own consent policies. Historical refusal audit
is not rewritten. Student chat reuses shared UI tokens/Button/FormField/FeedbackPanel
and scoped alternating bubbles; no retired Syn runtime or popup is introduced.

The first post-schema combined Student run still failed CITATION_QUOTE_MISMATCH
after four successful provider decisions and four successful read tools. This
proves that valid JSON alone does not guarantee literal evidence quotations.
The first hard-quote anyOf schema worked with a small synthetic control but the
actual authorized fixture's schema returned HTTP400. A bounded diagnostic reproduced
that rejection without native history. The same fixture with a flat reference enum
and plain quote string returned HTTP200 (1,965ms). The provider's exact lower-level
rejection reason was not retained, so it is not claimed as a billing/key failure or
a universal prohibition on anyOf.

Final Student wire schemas therefore bound current reference IDs, while literal
reference/quote examples derived from current-run authorized evidence are supplied
as currentRunCitationOptionsUntrustedData (max20 references, max3 short windows each).
These are data, never platform instructions or permissions. The candidate is never
silently rewritten: the unchanged host validator checks its exact reference/quote
pair again. No new evidence is introduced and no mismatch is accepted automatically.

Student execution reserves two final slots when at least3 rounds are configured:
one final answer and at most one output repair, within the same round/tool/deadline
caps. Other hosts keep their previous default. The central repair prompt is
agent-output-repair-v1.txt; its hash reference is recorded safely in OUTPUT_REPAIR.
Previously sealed platform v1/v2 prompt bytes/config hashes are not rewritten.
