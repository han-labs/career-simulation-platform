# Agent Platform P3/P4 — implementation boundary

Approved Baseline v1/DR1–DR2/Target v1.2, 2026-10-20. Core BIGINT identity and
normalized evidence remain unchanged. New UUID aggregates live under the existing
`guidance` modular-monolith boundary; no new service, framework or direct SQL tool.

```mermaid
sequenceDiagram
 participant A as Admin React / Nginx
 participant C as KB/Agent Controllers
 participant S as Service facade + ServiceImpl
 participant D as JDBC DAO / PostgreSQL
 participant P as existing Syn HTTP/embedding + native model adapter
 A->>C: JWT cookie + CSRF + explicit reviewed upload
 C->>S: current actor + typed request
 S->>D: private source / durable QUEUED job
 S->>P: bounded extraction + embeddings outside transaction
 S->>D: same transaction stage full chunks + READY job
 A->>S: inspect then explicitly publish (If-Match)
 S->>D: lock parent + atomic active-version switch
 A->>S: save configuration (no provider call)
 S->>D: immutable sealed payload + explicit KB grants
 A->>S: preview exact saved version / independent KB Test
 S->>D: ACCEPTED execution committed
 S-->>A: 202 execution ID
 S->>P: bounded native call / kb.search observation / final JSON
 S->>D: recheck current authority, sources, cancel; terminal result + usage
 A->>S: poll saved result (no regeneration)
 A->>S: publish exact successful preview then explicit assignment
 S->>D: published pointer / assignment history
```

Controllers only bind HTTP and extract active actor; service interfaces hide
transactions, locks and async workers. `KnowledgeParser` is infrastructure;
`KnowledgeSourcePolicy`, `KnowledgeRanking`, `AgentConfigurationPolicy` and
`AgentOutputValidator` own extraction-policy/ranking/config/output invariants.
DAOs contain persistence queries. `NativeAgentModelProvider` translates Gemini
and OpenAI native protocols behind `AgentModelProvider`, using Syn bounded HTTP,
execution scope, credentials, embedding profile and usage/pricing infrastructure.

KB Test has a KB/initiating Admin and nullable configuration, never a fake Student
session. Preview pins configuration/hash and Admin, never reads Core personal
ports. Only `kb.search` is implemented in P3/P4 and enabled explicitly. Source
policy is latest published per execution with immutable delivered chunk references,
not a frozen whole-KB release. Registry saving no tools/grants never enables them.

Execution commits acceptance before dispatch. Two bounded workers,16 queue slots,
mode-specific deadline, native-round/tool-call/context bounds. A short admission
transaction locks current actor/Agent/KB/source while starting the HTTP future;
locks are released before waiting for its response. Cancellation intent prevents
late success and preserves available actual usage. Revocation/hidden source drops
answer/source text at commit; redacted tool-event metadata remains. Replay checks
current source scope. Interrupted work is failed on read after90s, never silently
regenerated. There is no distributed queue or automatic crash recovery claim.

V21 creates12 platform tables, nullable source locators and integrity guards on
reused V13 storage. Published source/config payload and grants are immutable;
active pointers have same-parent foreign keys. Existing V1–V20 never change.
Session/interaction/summary tables are only dependency targets for P5; no Student
session API is present. `GET /v2/agents` exposes public assigned names/purposes.
Legacy Syn retrieval explicitly excludes scoped sources and cannot publish into
scoped assets; no old corpus adoption, reindex, re-embedding or implicit grants.

Feature flags defaultfalse. Core/Syn endpoints, consent checks and private evidence
ports retain their current contracts. No Enterprise KB/Agent authoring is exposed.
Raw files stay in the existing private volume; D5 retention and real content review
remain release gates. API details: [contracts](../api/agent-platform-p3-p4.md).
Verification and limitations: [Vietnamese report](../../.agent/reports/p3-p4-completion.vi.md).
