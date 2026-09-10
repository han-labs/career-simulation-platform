# GRASP Responsibility Assignment Guide

## Expert

Assign behavior to the object with the information needed to answer it.

- RIASEC scoring belongs to a deterministic assessment score calculator/domain policy.
- Whether an attempt can accept answers belongs to attempt lifecycle/domain behavior.
- Cross-record ownership, uniqueness, authorization, and transaction decisions belong to the owning service implementation with focused DAO queries.
- AI schema validation belongs to the guidance adapter/validator, not the controller or React page.

Entities must never contain JWT, HTTP, DTO, JSON response, database query, or Spring Security behavior.

## Creator

- Service implementations may create aggregates after validating use-case input.
- Mappers create response DTOs.
- Evaluator strategies create task outcomes from server-side rules and submitted answers.
- Guidance factories may create fallback reports from reviewed templates and objective evidence.
- Controllers never create and persist domain graphs.

## High cohesion

- Assessment does not manage simulation attempt lifecycle.
- Simulation evaluation does not generate prose guidance.
- Guidance does not recalculate scores.
- Identity does not absorb profile-independent content management.
- `common` contains only genuinely shared primitives; it is not a miscellaneous bucket.

## Low coupling

- Depend on service/provider interfaces, not implementation classes.
- Pass focused IDs/value data rather than broad entity graphs across modules.
- Prefer focused queries or access interfaces over long getter chains.
- Keep provider SDK types inside infrastructure adapters.
- Keep React shared components generic; domain behavior stays in the feature.

## Controller

A controller receives and validates API representation, extracts identity, delegates to one primary facade, and returns DTOs/status. It must not calculate scores, decide publication/ownership, select fallback, construct prompts, query DAOs, or catch generic exceptions to invent a success response.

## Feature checklist

- [ ] Every flow step has a named responsibility owner.
- [ ] State-local rules have an explicit Expert.
- [ ] Cross-record/transaction rules are orchestrated by the owning service.
- [ ] Object creation protects a real invariant.
- [ ] Controller calls one primary service facade.
- [ ] DAOs contain only persistence work.
- [ ] API uses DTOs and hides secrets/evaluation rules.
- [ ] No long cross-module getter chain was introduced.
- [ ] Tests prove each responsibility boundary and failure decision.
