# Syn usage and cost trace

`SynExecutionScope` records each external call's phase, duration and success. `tokenUsage` records provider, configured model, phase, input/output tokens and REPORTED/NOT_REPORTED. `SynUsagePricing` enriches the trace before `SynTurnLifecycleService` persists it in `syn_turns.trace`.

- PLANNING and GENERATION are separate calls; a two-pass model is not counted as one request.
- QUERY_EMBEDDING and DOCUMENT_EMBEDDING are separate phases. A cached retrieval in the same turn does not make another embedding request.
- Gemini embedding input usage is read from `usageMetadata.promptTokenCount`; an embedding produces no generated output tokens. Missing metadata remains unavailable. See [embedding response contract](https://ai.google.dev/api/embeddings#EmbeddingUsageMetadata).
- Gemini output usage includes response candidate tokens plus optional thinking tokens; OpenAI Responses uses its reported output total. See [Gemini generation usage contract](https://ai.google.dev/api/generate-content#UsageMetadata).
- A failed HTTP request remains a timed call even if no response/usage arrives. It is not treated as free.
- Invalid negative, fractional or non-finite token counts are unavailable.

## Pin rates in operator configuration

`SYN_COST_RATES_JSON` defaults to `[]`. Set a JSON array of records with these required fields:

| Field | Meaning |
|---|---|
| provider | `gemini` or `openai` |
| model | Exact configured model name, including `gemini-embedding-001` for embeddings |
| snapshot | Operator's price/source/tier/effective-date identifier, at most 240 characters |
| inputUsdPerMillion | Nonnegative decimal USD per million input tokens |
| outputUsdPerMillion | Nonnegative decimal USD per million output tokens; zero for embeddings |

Obtain rates for the deployed account/model/tier from the provider and record their effective date/source in `snapshot`. No live price is hardcoded or guessed. A model rename does not inherit another model's price. Configuration errors fail startup with a generic message without echoing configuration. Keep this backend-only.

The automated tests use explicitly synthetic prices. Do not copy them into production as provider prices.

## Interpret the result

For each fully reported, priced call:

```text
estimated USD = (inputTokens × inputUsdPerMillion
                 + outputTokens × outputUsdPerMillion) / 1,000,000
```

Calculation uses decimal arithmetic without rounding small embedding costs to zero. Trace stores the rate and snapshot used for each estimate so changing later configuration does not rewrite historical calculations.

| Cost status | Meaning |
|---|---|
| NO_EXTERNAL_CALLS | No call in this turn; total estimate is zero |
| ESTIMATED | Every observed call has valid usage and a matching rate |
| PARTIAL | Some calls are priced, others have unknown usage/rates |
| UNAVAILABLE | Calls occurred but none can be priced |

`estimatedSubtotalUsd` sums only known estimates. `totalEstimateUsd` is null when any call is unpriced/unreported. `callsWithoutUsage` counts calls with no usage record; per-line statuses additionally identify missing fields and unknown model prices. `callCounts` separates phases.

Method is `PINNED_UNDISCOUNTED_TOKEN_RATES`: it applies the configured input rate to all input tokens, including cached input. It does not claim an exact invoice, apply cache/free-tier/batch discounts, taxes, currency conversion or tool surcharges. If a provider changes its billing structure, update the pricing policy before interpreting the estimate as comparable. No billing query or extra token-count API call is made.

The persistent trace is per accepted chat turn. Standalone corpus ingestion/evaluation operates outside chat turn persistence; its operator execution is not a student turn or part of that turn's cost. A process crash or cancellation before finalization can leave provider billing that the accepted-turn trace cannot recover; reconcile against provider billing for financial totals.
