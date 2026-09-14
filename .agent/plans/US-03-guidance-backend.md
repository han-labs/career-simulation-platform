# US-03 Guidance Backend Feature Plan

Status: Implemented; external AI and runtime authentication integration remain deferred  
Owner: Huynh Gia Han with backend implementation explicitly authorized by the project lead  
Date: 2026-09-13

## 1 Scope

- User story: US-03 Explore evidence with Syn.
- Actor: Authenticated active student.
- Source: `docs/requirements/user-stories.md` US-03 and the approved Dashboard/Syn frontend contract.
- Acceptance criteria: concise evidence Dashboard; evidence-grounded Syn replies and inline result cards; option-based next steps; explicit plan save; objective scores remain unchanged; visible provenance and deterministic fallback.
- Explicit exclusions: authentication/JWT implementation, assessment scoring, simulation evaluation, external provider-specific API integration, chat-history persistence, career prediction/certification, and a separate result-report page.
- Related boundary: assessment, simulation, and identity data are read through focused access interfaces owned by those modules.

## 2 Flow inventory

### Main success flow

1. An authenticated active student loads a Dashboard assembled from completed assessment scores, evaluated simulations, and the current saved plan.
2. The student sends a supported quick action or free-text question to Syn.
3. The service minimizes owned evidence, produces reviewed Standard guidance, optionally embeds the relevant result card or plan draft, records provenance when evidence exists, and returns a structured reply.
4. The student explicitly posts a reviewed plan draft; the service validates and upserts the current plan.

### Alternate flows

1. Missing assessment, simulation, or plan data produces an empty/partial Dashboard rather than an error.
2. A free-text request is classified into a supported explanation, review, evidence-gap, exploration, or plan action.
3. A configured AI provider can later replace Standard generation behind the internal provider boundary without changing the HTTP contract.

### Exception and fallback flows

1. Unauthenticated callers receive `401`; wrong-role, inactive, or locked accounts are rejected.
2. A requested attempt outside the student's completed evidence is reported as not found.
3. Invalid messages or plan fields receive a validation response.
4. AI is disabled in this slice; deterministic Standard guidance remains fully usable and objective evidence is unchanged.

## 3 Responsibility design

| Responsibility | Owner | Class | Reason |
|---|---|---|---|
| HTTP and authentication-name extraction | Controller | `GuidanceController` | Keeps transport separate from decisions. |
| Use-case boundary | Facade | `GuidanceService` | One stable US-03 API. |
| Authorization/orchestration/transactions | Service expert | `GuidanceServiceImpl` | Owns cross-record policy and persistence order. |
| Reviewed deterministic response | Domain policy | `StandardGuidancePolicy` | Keeps fallback wording and classification testable. |
| Student/account lookup | Identity access | `StudentAccountAccess` | Prevents guidance from querying identity tables directly. |
| Completed assessment evidence | Assessment access | `AssessmentEvidenceAccess` | Preserves module ownership and completion filtering. |
| Evaluated simulation evidence | Simulation access | `SimulationEvidenceAccess` | Preserves ownership and hides evaluation rules. |
| Plan/audit persistence | DAO | `GuidanceDao` | Isolates PostgreSQL queries and JSON persistence. |

## 4 Change surface

- Frontend: existing guidance API adapter contract remains unchanged.
- Backend: guidance controller, DTOs, service, Standard policy, focused evidence access interfaces/implementations, and DAO.
- Migration: additive `V3` for one current exploration plan per student.
- Security: `/v1/guidance/**` requires authentication; service rechecks role/status/ownership.
- Documentation: sequence trace, API/database docs, AI-assistance log, verification report.

## 5 API contract

| Method | Path | Request | Success | Errors |
|---|---|---|---|---|
| GET | `/v1/guidance/dashboard` | authenticated principal | Dashboard response | 401, 403, 404 |
| POST | `/v1/guidance/syn/messages` | message, actionType, optional attempt context | structured Syn reply | 400, 401, 403, 404 |
| POST | `/v1/guidance/plans` | title and 1-5 steps | saved current plan | 400, 401, 403 |

## 6 Security privacy and AI

- Authentication/role: Spring Security authentication plus server-side `STUDENT`/`ACTIVE` checks.
- Ownership: queries require the resolved database student ID; arbitrary student IDs are never accepted.
- Sensitive data: raw answers, answer keys, evaluation rules, credentials, and unrelated attempts are excluded.
- AI context: only completed dimension scores and evaluated task outcomes; external AI is disabled for this slice.
- Validation: bounded message/title/steps and an allowlisted action type.
- Fallback: reviewed deterministic Standard guidance with API provenance `STANDARD` and persisted source `FALLBACK`.
- Logging: no raw prompt, answer payload, or provider response logging.

## 7 Tests and evidence

- Policy tests: supported actions, empty/conflicting evidence, free-text classification, prompt injection/out-of-domain safe behavior, and score stability.
- Service tests: Dashboard composition, role/status, ownership, persistence, and explicit plan save.
- Controller tests: authentication, validation, and response shape.
- Migration test: running Docker/PostgreSQL stack and Flyway V3.
- Gates: backend verify, frontend check, Compose validation, and gateway/API smoke where authentication permits.

## 8 Decisions

| Decision | Result |
|---|---|
| External provider/model not selected | Do not invent a provider; provide a replaceable boundary and complete Standard mode. |
| Authentication backend is not implemented | Keep protected contract and test with authenticated principals; do not hard-code a demo identity. |
| US-01/US-02 backend is incomplete | Return partial/empty live data until their completed records exist. |
| Chat persistence | Defer; persist only evidence-grounded guidance audit and confirmed current plan. |
