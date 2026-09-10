# Architecture and Design Pattern Rules

## Target structure

The backend is a modular monolith. A module owns its API adapter, application facade, domain behavior, persistence, and infrastructure adapters.

```text
Controller -> Service interface -> ServiceImpl -> DAO/domain/infrastructure
```

React follows feature ownership:

```text
feature page -> feature component/hook -> feature API adapter -> shared HTTP client
```

## Layer rules

- Controller: HTTP only. No DAO, entity graph construction, scoring, ownership decision, prompt construction, or provider call.
- Service interface: one stable use-case/subsystem facade.
- ServiceImpl: transaction and orchestration across domain objects, DAOs, policies, and adapters.
- Entity/domain object: state and local invariants without HTTP/security/DTO coupling.
- DAO: query and persistence only.
- Mapper: conversion only; no query or policy.
- Infrastructure adapter: external AI, mail, storage, or telemetry implementation behind an internal interface.

## Approved patterns and triggers

| Pattern | Use when | Do not use when |
|---|---|---|
| Facade | A subsystem needs one stable use-case boundary | An identical wrapper only forwards calls |
| Strategy | Task types require different deterministic evaluators | One `if` expresses a stable trivial rule |
| Adapter | Providers expose different external APIs | Provider types leak into service/domain code |
| Repository/DAO | Persistence queries must be isolated | Business rules are being hidden in query classes |
| Mapper | API and entity models must remain separate | Mapping starts performing queries or decisions |
| Template Method | Evaluators share an invariant algorithm with controlled variation | It creates inheritance without a real invariant |
| Circuit breaker/fallback policy | External AI can timeout/fail | It is used to mask defects in core local code |

Patterns require an identified problem, expected variation, and test. Do not add factories, managers, handlers, or abstractions only to make the design look advanced.

## Architecture-test intent

The ArchUnit foundation forbids Controller -> DAO/entity/ServiceImpl, DAO -> service/controller, and entity -> web/security/DTO dependencies. Extend architecture tests when a new boundary becomes important; do not disable the test to permit a shortcut.
