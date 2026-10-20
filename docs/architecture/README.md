# System architecture

The MVP is a modular monolith deployed as separate frontend, backend, database, and reverse-proxy containers. The external LLM is an optional provider behind a backend-owned interface. PostgreSQL remains the only source of application data, and the LLM never receives direct database access.

Current approved Security/Auth and Core increment (2026-10-19):
[P1/P2 contracts, credential lifecycle, version compatibility and migration boundary](p1-p2-security-core.md).
It adds V19/V20 after immutable V1–V18. Authentication now uses short-lived JWT
cookies with CSRF; HttpSession holds only CSRF/OAuth handshake state. Historical
Package A/Syn session-authentication and future Package B numbering below describe
their earlier checkpoints and are not the current release contract.

P3/P4 adds scoped KB Studio and immutable Agent Builder on V21, behind an explicit
feature flag. See [P3/P4 boundaries and execution sequence](agent-platform-p3-p4.md)
and [API contract](../api/agent-platform-p3-p4.md).
P5/P6 now adds Student sessions/SSE, explicit reports, confirmed plan suggestions and
safe usage aggregates on additive V22. See [current P5/P6 runtime](student-agent-p5-p6.md)
and [Student/usage contracts](../api/student-agent-p5-p6.md); the P3/P4 document is its
historical checkpoint, not a statement that Student runtime is still absent.

```mermaid
flowchart TB
    Admin["Admin browser: KB Studio / Agent Builder"]
    Studio["guidance.knowledge: staged sources / scoped retrieval"]
    Platform["guidance.agent: registry / pinned Student sessions / shared native controller / ledger"]
    Student["Student browser"]
    Enterprise["Enterprise or content provider browser"]
    Gateway["Nginx reverse proxy"]
    Frontend["React and Vite frontend"]
    Backend["Spring Boot modular monolith"]
    Identity["Identity: JWT cookies / CSRF / current authority"]
    Google["Optional Google OIDC provider"]
    Agent["Syn bounded agent orchestrator"]
    Tools["Approved evidence and resource tools"]
    Database[("PostgreSQL")]
    Core["Versioned assessment / enrollment / final work / completion"]
    Assets[("Private asset volume")]
    AIAdapter["Backend AI adapter"]
    LLM["External LLM provider"]
    Fallback["Deterministic feedback templates"]

    Admin -->|HTTP local; HTTPS at production edge| Gateway
    Backend --> Studio
    Backend --> Platform
    Studio -->|reused chunks vector768 / immutable source versions| Database
    Studio --> Assets
    Platform -->|explicit KB grants| Studio
    Platform -->|ledger / assignments / sealed configurations| Database
    Platform -->|read-only Core facades; exact-source plan confirmation| Core
    Platform -->|history / lineage / reports / safe usage| Database
    Platform --> AIAdapter
    Platform -->|failed validation/provider unavailable| Fallback
    Student -->|HTTP local; HTTPS at production edge| Gateway
    Enterprise -->|HTTP local; HTTPS at production edge| Gateway
    Gateway --> Frontend
    Gateway -->|REST API| Backend
    Frontend -->|REST API through gateway| Backend
    Backend -->|JPA and JDBC| Database
    Backend --> Identity
    Identity -->|server-side authorization code / OIDC| Google
    Identity -->|identity / credential version / revocation| Database
    Backend --> Core
    Core -->|same datasource transaction| Database
    Backend -->|authorized upload/download| Assets
    Backend --> Agent
    Agent --> Tools
    Tools --> Database
    Agent --> AIAdapter
    AIAdapter -->|tool declarations and bounded context| LLM
    LLM -->|typed function calls / validated JSON; runtime-specific budgets| AIAdapter
    Agent -->|timeout invalid output unavailable tool or pass limit| Fallback
    Fallback --> Backend
    Database --> PGVolume[(Persistent PostgreSQL / pgvector volume)]
    subgraph LocalOrDeploymentHost["Local test host or deployment Docker host; no production deployment in P5/P6 verification"]
        Gateway
        Frontend
        Backend
        Database
        PGVolume
        Assets
    end
```

## Backend modules

| Module | Responsibility | Current state |
|---|---|---|
| `identity` | Authentication, roles, profiles, consent | Google OIDC / JWT cookies / current actor / purpose-specific consent (P1) |
| `assessment` | RIASEC, Interests, Abilities, and Work Activities assessments with deterministic scoring | Versioned multi-type assessments and preserved historical results (P1/P2) |
| `catalog` | Published simulation discovery | Working foundation slice |
| `simulation` | Tasks, attempts, submissions, deterministic evaluation | Pinned versions, ordered final/self-review policy with historical compatibility (P2) |
| `guidance` | Dashboard summaries, bounded session memory, typed read tools, bounded LLM tool calling, schema validation, and deterministic Standard fallback | Legacy Syn preserved; flag-gated P3–P6 KB/registry/Student runtime/reports/usage |
| `enterprise` | Simulation authoring, publishing, and monitoring | Owned draft/preview/publish and aggregate monitoring (P2) |
| `common` | Narrow cross-cutting API/configuration/error primitives | Working foundation |
| `system` | Health and operational endpoints | Working foundation |

Each module exposes a service interface as its application facade. Controllers depend on that interface, while implementation classes hide DAOs, entities, mappers, integrations, and transaction boundaries.

## Syn agent boundary

Syn is an orchestrated single-agent workflow, not an unrestricted autonomous agent.
For an eligible consented Gemini request, `SynAgentOrchestrator` permits exactly one
tool-selection pass and one final synthesis pass. Gemini may select only declared
`SynToolIdentifier` values with no arguments; `SynAgentToolbox` performs the
authenticated, read-only work and returns bounded observations. The synthesis pass
disables further function calls and validates the existing JSON reply schema.

`SynAgentPolicy` still computes the deterministic intent and static tool plan before
the provider exchange. It is the complete safety path when AI is disabled or Gemini
times out, rejects a request, returns malformed output, requests an unavailable tool,
or attempts a third step. Therefore Gemini cannot choose SQL, call an arbitrary URL,
change evidence, or persist a plan. Explicit plan confirmation remains a separate
service use case.

The policy also resolves `AUTO`/English/Vietnamese language, common Vietnamese
shorthand, exploration stage, and a bounded response depth. With no personal
evidence, the toolbox supplies reviewed catalog/path knowledge for honest onboarding
instead of pretending to personalize. Session memory stores compact codes rather
than raw dialogue. The frontend progressively reveals only the already validated
reply, anchors the submitted question, and leaves scrolling under student control;
this is deliberately not unvalidated provider-token streaming.

## Deployment correspondence

The root `docker-compose.yml` implements the architecture as four local services:

1. `gateway` routes browser traffic.
2. `frontend` serves the React build.
3. `backend` executes business rules and integrations.
4. `postgres` stores durable application data in a named volume.

Private bytes use a second named volume at `/app/private-assets`. Core routes and
the React core client default to disabled until a coordinated rollout. The runtime
database user and Flyway migrator can use separate credentials; deployment must
provision least-privilege roles before valuable-runtime startup. No production
cutover or default Compose startup was performed for Package A.

Package A adds V15–V18 only, after unchanged main/Syn V1–V14: 25 tables, 49
during coexistence (including the 24 retained tables after the main merge,
excluding Flyway history). It uses JDBC records/explicit SQL for new persistence
and retains existing legacy JPA mappings with validate. The configurable Agent
Platform now uses V21; P1/P2 already added V19/V20. Earlier Package B numbering
is historical and superseded by Target v1.2 P3–P6.

[Package A consolidated implementation/evidence report](../../.agent/reports/package-a-core-verification.vi.md)
records exact scope, tests, limits and the implemented request sequence. The
isolated gateway test uses an ephemeral Nginx/PG17 container and a host Spring Boot
server; it is distinct from deployment of the four-service Compose stack.

The source architecture image is retained in `.agent/references/system-architecture.png`. This Mermaid diagram must be updated when the deployed topology changes.


## P1/P2 Syn core and vector RAG increment

PostgreSQL now requires pgvector. V12 adds versioned turn/session lifecycle, V13 adds published knowledge document/version/chunk tables and `vector(768)`, V14 adds sanitized trace metadata. These versions follow the already-applied assessment V10 and V11 migrations. Business evidence remains relational and authoritative. No frontend-to-provider, model-to-SQL or model-to-plan-write path was added.

The message flow uses short snapshot/begin/finalization transactions around an external-call phase. The seventh read tool, SEARCH_KNOWLEDGE, combines exact vector retrieval and local full-text search. Publication is an internal reviewed workflow, independent of chat. Deadline/cancellation and shared schema validation protect the generation boundary. Package A supplies session authentication and explicit provider grants; the old consent timestamp is historical only. A single-instance per-user admission lock coordinates outbound dispatch with revoke, and finish/replay revalidate authorization. Horizontal scaling requires new admission coordination. Localhost demo is explicitly profile-gated and remains separate from production authentication.

[Complete Vietnamese component map, sequence, fallbacks and Q&A](syn-core-rag-implementation.vi.md).
# Syn A/B/C continuation

See [implementation and limits in Vietnamese](syn-coach-abc.vi.md) and [manual test guide](../testing/syn-coach-manual.vi.md). This increment composes capabilities, improves resource fallback and memory, adds bounded Gemini retry and optional grounded web resources. RAG corpus publication still requires actual human content review.
