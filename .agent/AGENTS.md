# AI Agent Working Rules for Career Simulation Platform

## 1 Priority and authority

This is an OOSE project. Correctness against approved requirements and demonstrable evidence is more important than adding features or making the code appear sophisticated.

Required traceability chain:

```text
Proposal/SRS -> User story -> Acceptance criteria -> Sequence/domain design
-> Code -> Tests -> Evidence -> Report update
```

Source priority:

1. Explicit instruction from the project lead.
2. Approved proposal/SRS and approved change decisions.
3. Approved user-story/use-case specifications.
4. Current sequence, class/domain, database, and architecture artifacts.
5. Existing code and tests.
6. Suggestions or assumptions from an AI agent.

When sources disagree, do not silently choose one. Record the mismatch, its impact, and the decision required.

## 2 Mandatory reading and pre-coding plan

Before editing a feature, read `PROJECT_CONTEXT.md`, `TASKS.md`, `DESIGN.md`, relevant rules, existing code, and the approved source artifacts. Copy `templates/feature-plan.md` into `plans/US-XX-short-name.md` and complete it.

The plan must identify:

- actor and owner;
- source requirements and exact acceptance criteria;
- main, alternate, exception, and fallback flows;
- affected frontend feature, backend module, API, database, tests, evidence, and report sections;
- sequence-to-code mapping;
- security/privacy impact;
- AI role and deterministic fallback, if any;
- open decisions and scope boundaries.

Do not implement unresolved behavior that materially changes requirements, data ownership, scoring, security, privacy, or another member's assigned frontend scope.

## 3 Hard scope rules

- Do not invent features outside the approved MVP.
- Do not turn exploration guidance into career prediction or a final decision.
- Do not let AI calculate, alter, or override RIASEC or simulation scores.
- Do not add recruitment/job matching, VR/AR, multiplayer, mobile app, proprietary model training, or full offline operation without a scope change.
- Do not modify another member's frontend user-story scope without explicit coordination.
- Do not infer backend ownership; it is currently unassigned.
- Do not expose entities, answer keys, evaluation rules, internal prompts, credentials, or unrestricted model output through public APIs.
- Do not put business rules in controllers, React components, mappers, DAOs, or exception handlers.
- Do not hard-code user IDs, role assumptions, provider keys, or environment-specific URLs in production logic.
- Do not commit secrets, private student data, production exports, or unlicensed third-party content.

## 4 Architecture and package rules

The MVP is a modular monolith. The required backend flow is:

```text
Controller -> Service interface / subsystem facade -> ServiceImpl
-> DAO and domain experts -> PostgreSQL or infrastructure adapter
```

- Controllers handle HTTP, request validation, authentication context extraction, status codes, and DTOs.
- Service facades express use cases and hide transactions and internal collaborators.
- Service implementations orchestrate cross-record rules, authorization, lifecycle, and failure handling.
- Entities own state-local invariants when doing so does not introduce framework coupling.
- DAOs perform persistence queries only.
- Mappers convert representation; they do not query data or make business decisions.
- Flyway migrations are append-only after sharing.
- External AI providers are infrastructure adapters behind an internal guidance interface.

Use domain-based packages under `edu.hcmute.careersim`: `identity`, `assessment`, `catalog`, `simulation`, `guidance`, `enterprise`, `common`, and `system`.

## 5 AI product behavior rules

- The backend selects and minimizes authorized context; the LLM never queries PostgreSQL.
- Validate model output against an explicit schema before persistence or display.
- Apply timeouts and classify provider, transport, parse, safety, and empty-evidence failures.
- On any unusable AI result, return reviewed deterministic guidance without losing objective results.
- Persist provenance: `AI` or `FALLBACK`, model name when applicable, status, and generation time.
- Guidance must state limitations and offer possible exploration steps, not certainty.
- Prompt injection or out-of-domain user input must not override system rules or disclose data.

## 6 Security and privacy rules

- Roles are `STUDENT`, `ENTERPRISE`, and `ADMINISTRATOR` unless requirements approve a change.
- Students may access only their own private profile, attempts, results, and reports.
- Enterprise users may manage only simulations they own or are explicitly allowed to manage.
- Inactive or locked accounts cannot use protected functions.
- Consent and data-minimization decisions must be enforced before sending personal evidence to an external AI provider.
- Public catalog APIs contain only published simulation metadata.
- Evaluation keys remain server-side.

## 7 Testing and quality gates

Before reporting completion, run:

```powershell
cd backend
.\mvnw.cmd verify

cd ..\frontend
npm run check

cd ..
docker compose config --quiet
```

For an integrated slice, also build/run the stack and verify `/api/health` and the affected API/UI flow.

Minimum test classes for each feature:

- main success flow;
- every approved alternative/exception/fallback flow;
- invalid boundary values;
- wrong actor, ownership, or status lock;
- persistence uniqueness/transaction behavior when relevant;
- AI timeout, malformed output, and fallback when AI is involved;
- controller contract separately from service decisions;
- frontend state for loading, success, empty, validation, and failure.

Do not weaken or delete a failing test merely to make CI green.

## 8 Evidence and report discipline

- Report only commands actually run and outcomes actually observed.
- Never fabricate model latency, evaluation quality, screenshots, test counts, deployment results, or URLs.
- Record the exact commit SHA used for final evidence.
- Keep architecture and sequence diagrams synchronized with implementation.
- Update `reports/` and the course report when behavior, interfaces, schema, architecture, or AI evaluation changes.
- Log substantive AI assistance using `templates/ai-assistance-log.md`, including human review and rejected suggestions.

## 9 Git workflow

- Use one feature branch per user story or foundation item.
- Suggested names: `feature/us01-riasec-assessment`, `feature/us02-simulation-workflow`, `feature/us03-guidance`, `chore/project-foundation`.
- Do not push directly to `main` unless the team explicitly allows it.
- Run all relevant gates before commit and again before PR.
- Keep commits focused; do not mix unrelated refactors with a feature.
- PR description must include the trace source, implemented flows, changed layers, migrations, tests, evidence, report impact, and known gaps.
