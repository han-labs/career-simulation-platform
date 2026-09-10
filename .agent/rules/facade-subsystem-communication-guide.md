# Facade and Subsystem Communication Guide

A service interface is the public application facade for its module. It hides internal DAOs, entities, mappers, transaction boundaries, evaluator strategies, provider adapters, and fallback policies.

Expected examples:

- `AssessmentService` for US-01.
- `SimulationCatalogService` for catalog discovery.
- `SimulationService` for US-02 attempt/task workflow.
- `GuidanceService` for US-03 Dashboard summaries, Syn guidance, and plan history.
- `SimulationManagementService` for enterprise content lifecycle.
- `AuthService` or `IdentityService` for authentication/profile policy.

Rules:

1. Controllers inject service interfaces only.
2. Controllers never inject DAOs or `ServiceImpl` classes.
3. One subsystem never imports another subsystem's implementation class.
4. Cross-subsystem business policy goes through the owning facade.
5. A focused read/access interface is allowed when only one fact is needed and no lifecycle/policy is bypassed.
6. Public contracts use IDs/value DTOs rather than JPA entities.
7. Do not create decorative `*Facade` classes when the service interface already plays the role.
8. Do not create a global facade that coordinates unrelated identity, assessment, simulation, guidance, and enterprise operations.
9. Document intentional cross-subsystem access in the feature trace.

For US-03, `GuidanceService` may request focused, authorized evidence from assessment/simulation access interfaces. It must not query their tables through unrelated DAOs if doing so bypasses ownership, completion, or visibility policy.
