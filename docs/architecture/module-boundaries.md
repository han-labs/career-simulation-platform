# Module boundary rules

## Dependency direction

```text
React page -> feature API adapter -> HTTP controller -> service interface
             -> ServiceImpl -> domain entity / DAO / infrastructure adapter
             -> PostgreSQL or an external provider
```

- A controller may inject only a service interface from the owning module.
- A service implementation may use its own DAO and focused cross-module query interfaces.
- Cross-module business policy must be accessed through the owning module's service/facade, not its implementation class.
- A DAO cannot call a service or controller.
- Entities cannot depend on DTO, HTTP, JSON response, JWT, or Spring Security types.
- Public APIs use request/response DTOs. Never serialize JPA entities.

## Design patterns used intentionally

- **Facade:** each service interface is the stable use-case entry point for a subsystem.
- **Repository/DAO:** persistence operations are isolated from business decisions.
- **Strategy:** deterministic task evaluators vary by task type without changing attempt orchestration.
- **Adapter:** hosted AI providers and a future local model implement an internal guidance-provider contract.
- **Circuit breaker/fallback policy:** timeouts, invalid output, and provider errors lead to deterministic guidance.
- **Mapper:** API representations are created independently of persistence entities.

Do not create a pattern class without a concrete variation, boundary, or failure mode that justifies it.

## US-03 focused evidence access

`GuidanceService` is the US-03 facade. It does not reach into assessment, simulation,
or identity DAOs. Each source module exposes one narrow read contract:

- `StudentAccountAccess` resolves the authenticated email and returns role/status.
- `AssessmentEvidenceAccess` returns only the latest completed dimension scores.
- `SimulationEvidenceAccess` returns only recent evaluated scores and safe task
  outcomes; answer payloads and evaluation rules are never selected.

`GuidanceDao` owns confirmed-plan persistence and guidance provenance.
`StandardGuidancePolicy` is deterministic. `SynGuidanceProvider` is the provider-neutral
port, while `OpenAiSynGuidanceProvider` and `GeminiSynGuidanceProvider` are selectable
infrastructure adapters. The service selects AI only for consented, safe free-text
input and otherwise uses the policy without changing controllers or objective
evidence storage.
