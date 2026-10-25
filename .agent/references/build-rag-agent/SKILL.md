---
name: build-rag-agent
description: Build, integrate, review, or repair configurable document-grounded AI agents with independently testable Knowledge Bases, RAG, backend-controlled tool execution, scoped conversation memory, citations, and bounded native function-calling loops. Use for end-to-end agent features or specific ingestion, retrieval, runtime, configuration, and acceptance work; not for an ordinary chat UI with no retrieval or tools.
---

# Build a Configurable RAG Agent

## Purpose and portability

Deliver a working, explainable document-grounded agent, either as a standalone studio or as a feature inside an existing application. Adapt to the owner's language, stack, domain, permissions, budget, and deployment constraints. Do not impose the reference project's framework, screen names, model IDs, or numerical settings on another system.

This skill distills a React/Python/PostgreSQL project that evolved through KB ingestion, retrieval, a genuine single-agent runtime, configurable tools, persisted conversations, and manual acceptance repairs. Its transferable value is the contracts, invariants, delivery sequence, debugging method, and failure lessons—not a promise of accuracy or universal provider availability.

The owner reported that the latest project was working and tested. That report is not an independent live verification performed while authoring this skill. Cross-chat long-term memory and advanced integrations described here are extension guidance, not claims that the reference project implements them.

## Read only the material needed

- For implementation, integration, configuration, UI, tests, or extensions, read the relevant sections of [the implementation guide](references/implementation-guide.md).
- Before choosing ingestion/model/context defaults, or when a workflow fails, read the relevant entries in [lessons learned](references/lessons-learned.md).
- For a narrow repair, inspect the actual affected production code and tests first; do not restart the whole build sequence.
- For a new system, read the guide's scope, boundaries, contracts, and delivery sections before editing. Read its extension section only when an extension is requested.
- To reproduce the two studio features, use the guide's **sections 15–22: implementation-ready feature blueprint**. They specify KB and Agent use cases, payloads, persistence constraints, fixtures, acceptance gates and a Java/React/PostgreSQL integration route. This blueprint is a reference target to adapt explicitly to the host repository, not evidence that a Java port already exists.

Both references belong to this skill package. Copy the entire folder when sharing it with another AI or installing it elsewhere; no access to the original project is required.

For handoff to a coding AI, supply all three Markdown files plus access to the **target** application's source, build files, API/security conventions, migration head and test commands. Start with an integration audit, then implement the next approved complete use case. The guide's section 22 includes a ready-to-use handoff prompt and what not to copy from the original Python project.

## Essential distinctions

| Concept | Meaning | Do not confuse it with |
|---|---|---|
| Knowledge Base | Versioned external documents plus searchable chunks and provenance | Conversation history or fine-tuning |
| RAG | Retrieval supplies evidence for generation | A tool-using agent by itself |
| Agent | Model selects an action; backend validates and executes; model observes and reconsiders | Always-search-then-answer routing |
| Same-chat memory | Bounded prior messages in one conversation | Cross-chat memory or current evidence |
| Long-term memory | Optional consented facts/preferences across scoped chats | Automatically retained transcripts |
| Live data | Current authorized data obtained through a tool | Static indexed document knowledge |
| Citation | A validated reference to evidence actually delivered to the model | Proof that every statement is semantically correct |
| Indexing | Extraction, chunking, embedding, publication | Training the LLM's weights |

## Non-negotiable correctness boundaries

1. The backend owns authority. Model proposals, documents, memory, and tool results are untrusted inputs, not permission grants.
2. Keep KB management/search independent of agent execution. A failed answer must not hide successful retrieval.
3. `kb.search` retrieves evidence only. Do not nest another answer-generating LLM inside it.
4. Canonical domain tool names remain stable. Provider-specific identifiers and message formats belong inside adapters.
5. Tools require typed arguments, explicit enablement, current scope checks, timeouts, and bounded observations. Attaching a KB does not automatically enable its search tool.
6. New configurations receive no implicit KB or tool grants. Use stable IDs, not display names. Respect the approved one-KB or multi-KB contract; never silently search another KB.
7. Stage document versions; publish complete validated chunk/vector sets atomically. Failed replacement must not destroy the previous active version or expose partial indexing.
8. Record compatible embedding and parser/chunking profiles. An LLM switch and an embedding-space change are different operations.
9. Knowledge-dependent answers need supporting evidence from the current run. A social greeting needs no invented citation; missing information needs clarification or honest abstention.
10. Validate citation membership, delivered text, exact quotes or explicitly defined source spans, and current source authorization. Never fabricate a locator or weaken validation to conceal a bad answer.
11. Persist conversations in isolated scopes. Reset decisions, tool counters, deadlines, and reservations for every new user run; history does not spend a later run's execution counters.
12. Bound context and execution. A context window is not a conversation-message cap. Do not label bytes, heuristics, or reservations as measured provider tokens.
13. Cancellation and source/permission revocation remain effective after in-flight calls return. Late results cannot resurrect a cancelled or deleted run.
14. Secrets stay server-side. Public traces contain observable events and safe metadata, never API keys, protected protocol fields, or private chain-of-thought.
15. Deterministic doubles prove application behavior, not a live model's native tool capability, semantic accuracy, account access, or quota.

## Operating procedure

### 1. Inspect and choose scope

Read the repository's instructions, current status, relevant requirements, affected implementation, tests, migration head, and safe configuration values. Existing code is evidence of behavior; an older plan is not evidence that a feature is absent or complete.

Determine whether the request is **build**, **integrate**, **investigate**, **repair**, or **explain**. Read-only requests do not authorize implementation. Creating this feature does not authorize production migrations, reindexing, external writes, paid APIs, or public deployment.

Resolve only consequential questions: corpus formats/languages, expected size, source-only versus general-answer policy, selected KB scope, tool permissions, acceptable latency, privacy, model availability, and how the feature fits the host application. Mark unknowns rather than inventing values.

### 2. Establish contracts before layers

Use the host's existing API, persistence, error, component, and authorization patterns. Define only the data and functions needed for the next complete use case:

- A document/version/chunk/evidence contract with provenance and profile identity.
- An immutable saved agent configuration and its explicit grants.
- A conversation, message, and per-message run contract.
- A native decision adapter and backend-owned tool executor.
- A validated final answer and safe diagnostic contract.

These are responsibilities, not a mandate to create one class, file, service, or repository layer for each item. Do not add an agent framework, queue cluster, memory service, or evaluation UI without a demonstrated requirement.

### 3. Deliver complete vertical slices

Use these checkpoints, adapting them to existing implementation rather than rebuilding completed work:

| Checkpoint | Deliverable | Evidence required before continuing |
|---|---|---|
| KB foundation | Create KB, ingest a small file, inspect exact text/chunks | Real database persistence, publication and failure tests |
| Retrieval | Query embedding, scoped search, ranked passages/source inspector | Known and unsupported questions; stale/deleted-source tests |
| Configuration | Agent CRUD, saved versions, Brain, explicit KB/tool permissions, preview shell | Save/read makes zero model calls; conflict and rollback tests |
| First real runtime | Direct reply or model-requested `kb.search`, observation round trip, cited final | Direct, one-tool, sequential two-tool, denial, failure, budget and cancellation gates |
| Conversation usability | New/resume/clear/delete, bounded same-chat context, settings with effects | Isolation, reload, revocation and repeated-turn tests |
| Small approved tools | Deterministic time and arithmetic, if requested | Same registry/controller, typed inputs, no fabricated KB citations |
| Release hardening | Actual target corpus, unknown/override/derived-fact cases, run/demo instructions | Layer-specific results, real UI checks and authorized native-provider evidence |

Configuration-only previews must honestly state that runtime is unavailable. Never ship fake chat, fake vectors, or fake traces as production functionality.

### 4. Use a genuine bounded loop

```text
User message + pinned saved configuration + bounded same-chat context
    -> model chooses final / clarification / permitted tool request
    -> backend validates name, arguments, permissions, scope and budgets
    -> backend executes the actual tool
    -> sanitized observation correlated to the exact native request
    -> model reconsiders
    -> continue or validated final
    -> persist answer, citations, safe events and terminal run state
```

Disable SDK automatic tool execution when it would bypass application validation. Preserve required native call IDs, names, ordering, and opaque signatures in protected provider protocol; do not expose them as user reasoning.

Keep the current run's call/response protocol intact. For prior turns, prefer bounded user/assistant text pairs, not repeated historical retrieval passages or obsolete tool protocol. Historical answer text is conversational context, not a new citation ledger.

Before budgets are exhausted, leave an opportunity to conclude from collected evidence or abstain. Do not fix loops with unbounded retries, massive cap increases, hard-coded domain answers, or production keyword routing disguised as model choice.

### 5. Debug the layer that actually failed

| Observation | Investigate first |
|---|---|
| File never becomes Ready | Storage/extraction/normalization/embedding/publication stage and exact error |
| Expected fact absent from retrieved evidence | Extraction, chunk context, profile/index scope, retrieval and Top K |
| Correct evidence, incorrect answer | Grounding instructions, controller observations and generation |
| Correct answer, invalid source | Current-run evidence mapping, quote fidelity, provenance and revocation |
| Failure after several turns | Effective pinned settings, repeated searches, context size, reservation and fresh counters |
| Provider rejects a request | Exact HTTP/status/operation; schema, access, rate limit, timeout or transport—not guessed quota |
| Chat Stop/source/switch is wrong | Accepted run ID, pending state, scoped UI cache, lifecycle and API boundaries |

Reproduce with permission-cleared fixtures. Record failed-stage evidence before changing settings. Optimize only the measured bottleneck and compare the same fixtures before/after. See the lessons reference for the concrete incidents behind this method.

### 6. Test and report honestly

Run targeted tests after each functional change; then relevant regression, formatting/lint, typecheck/build, and real browser interaction for changed workflows. Use disposable databases/schemas/storage for destructive lifecycle or migration tests. Never test deletion or reindexing against owner data without authorization.

Separate three kinds of evidence:

- **Deterministic engineering tests:** application invariants, state machines, persistence and error paths.
- **Authorized live provider tests:** actual native calls, request compatibility, observation correlation and model output on recorded fixtures.
- **Human acceptance:** the owner follows the real workflow and judges domain correctness/usability.

A skipped live test is skipped. A successful model response on one fixture is not universal readiness. Obey stop instructions and quota/cost limits; do not repeatedly call a provider merely to accumulate passes.

For handoff, report exact changed files, contract/schema changes, commands actually executed, failed/skipped checks, observed evidence, owner-data safety, and remaining acceptance gates. Use **VERIFIED**, **REPORTED**, **PROPOSED**, **DEFERRED**, **UNKNOWN**, or **BLOCKED** with their appropriate meaning. Update actual code-to-component paths only after the code exists.

## Completion standard

The requested slice is complete when its approved behavior is demonstrated, relevant failure/permission/lifecycle tests pass, actual UI interaction is checked where affected, sources remain inspectable, and remaining live/human gaps are disclosed. Stop at the requested slice; optional extensions in this package do not authorize further development.
