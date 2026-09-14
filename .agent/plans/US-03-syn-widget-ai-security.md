# US-03 Syn Widget, AI Provider, and Security Plan

Status: Implemented / Local demo API flow verified  
Owner: Huỳnh Gia Hân  
Date: 2026-09-15

## 1 Scope

- User story: US-03 Explore evidence with Syn, plus focused platform hardening.
- Actor: authenticated student; unauthenticated visitors may see and open the widget but receive preview/Standard guidance only.
- Source: `docs/requirements/user-stories.md`, `.agent/PROJECT_CONTEXT.md`, and the project lead's 2026-09-15 request.
- Acceptance criteria: Syn is available on every route in collapsed, compact, and expanded modes; quick actions and objective result cards remain usable; open-ended requests may use a consent-gated provider; all provider failures preserve Standard guidance; common web attack surface is reduced at Spring and Nginx boundaries.
- Explicit exclusions: AI scoring, career prediction, unrestricted tools/RAG, long-term chat memory, a new authentication system, and enterprise-management changes.
- Related owner boundaries: the widget is mounted in shared layout but does not change US-01/US-02 business behavior.
- Follow-up scope (2026-09-15): preserve the yellow/blue context tags when the chat grows, and support Gemini as a second selectable provider without changing the API contract.
- Follow-up scope (2026-09-15): allow an explicitly enabled localhost-only demo identity to exercise the protected Syn flow before the full login UI is implemented. Production authorization remains unchanged by default.

## 2 Flow inventory

### Main success flow

1. Syn starts as a circular launcher on every page.
2. The student opens compact mode and may expand it without leaving the current page.
3. Syn loads the authorized Dashboard context, accepts a quick action or short free-text message, and displays a structured response with provenance.
4. Explicit quick actions use reviewed deterministic guidance; eligible open-ended requests use the configured provider only after consent.
5. A plan draft is persisted only after explicit confirmation.

### Alternate flows

1. With no authenticated live context, the widget remains useful with clearly labelled preview/Standard guidance.
2. The student can collapse or resize Syn while keeping the current session conversation during route navigation.
3. A Dashboard result action opens the global widget with the relevant request.
4. During local integration testing only, `LOCAL_DEMO_MODE=true` supplies the seeded `student@demo.com` principal for localhost guidance requests; the demo profile still requires recorded AI consent.

### Exception and fallback flows

1. Missing key, disabled provider, missing consent, prompt-injection pattern, timeout, provider error, refusal, incomplete output, or malformed schema returns Standard guidance.
2. Rate-limited requests receive HTTP 429 at the gateway; oversized bodies are rejected before Spring.
3. Provider errors, prompts, keys, and raw private responses are not returned or logged.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Three-mode global UI and session chat | React shared feature component | `SynWidget` | One mounted instance persists across routes without duplicating page logic |
| Concise Dashboard | US-03 page | `GuidancePage` | Keeps objective evidence separate from the floating assistant |
| Deterministic reply and unsafe-input boundary | Domain policy | `StandardGuidancePolicy` | Testable, provider-independent rules |
| AI variation boundary | Adapter port | `SynGuidanceProvider` | Prevents provider details leaking into the service/controller |
| OpenAI Responses integration | Infrastructure adapter | `OpenAiSynGuidanceProvider` | Owns HTTP, timeout, minimized context, structured output, and parsing |
| Gemini GenerateContent integration | Infrastructure adapter | `GeminiSynGuidanceProvider` | Uses the same port, minimized context, strict JSON response, timeout, and fallback contract |
| Consent/fallback/audit orchestration | Service implementation | `GuidanceServiceImpl` | Owns authorization, provider selection, and transaction decisions |
| Edge hardening | Nginx/Spring configuration | gateway config and `SecurityConfig` | Centralized headers, limits, and route policy |

## 4 Change surface

- Frontend: `AppShell`, `GuidancePage`, one `SynWidget` component, existing guidance API adapter, and existing stylesheet.
- Backend: existing guidance service/policy/DAO, one provider port, and selectable OpenAI/Gemini adapters.
- API: existing US-03 routes and response DTO remain compatible.
- Database: existing V6 plan migration; guidance audit rows gain correct AI/fallback metadata through existing columns, with no new table.
- Configuration: `AI_PROVIDER=openai|gemini|disabled`, backend-only provider keys/models, shared timeout/output cap, Nginx limits and security headers.
- Documentation/report: API setup, security decisions, tests, and AI assistance evidence.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| GET | `/api/v1/guidance/dashboard` | none | existing `DashboardResponse` | 401, 403, 404 |
| POST | `/api/v1/guidance/syn/messages` | existing validated message/action/context | existing `SynMessageResponse` with `AI` or `STANDARD` | 400, 401, 403, 404, 429 |
| POST | `/api/v1/guidance/plans` | existing validated plan | existing `PlanResponse` | 400, 401, 403, 429 |

## 6 Security privacy and AI

- Authentication/role: US-03 remains protected and revalidates STUDENT/ACTIVE server-side; runtime identity remains a separately tracked foundation gap.
- Local test boundary: demo authentication is opt-in, limited to guidance requests received with a loopback hostname, uses only the seeded synthetic student, and defaults to disabled. It is not a replacement for production login.
- Ownership: an attempt ID must occur in the current student's evaluated evidence.
- Sensitive data: no raw answers, evaluation rules, email, display name, credentials, or unrelated history are sent to the provider.
- AI context: at most three interest signals, three skill summaries, three recent objective results, and the current plan summary.
- Output: strict JSON schema plus local field/count/length validation.
- Cost controls: only open-ended eligible messages call AI; quick actions stay local; no tools/history; stable prompt prefix; bounded input and output; `store=false`.
- Timeout/fallback: bounded HTTP timeouts and classified internal failures always fall back deterministically.
- Logging/redaction: record provenance/model/category timing only; never key, full prompt, raw provider response, or private answer payload.

## 7 Tests and evidence

- Domain: prompt injection and deterministic action coverage.
- Service: consent, provider success, disabled/no-consent, provider failure, objective-score stability, and audit metadata.
- Provider: successful structured response, malformed output, non-2xx, and timeout with a mock HTTP boundary.
- Regression: a growing conversation must not shrink the Syn context-tag row.
- Controller: authentication, validation, and unchanged representation.
- Frontend: API adapter tests plus lint/build and manual three-mode/route checks.
- Security: headers, request-size/rate-limit configuration validation, secret scan, and safe error serialization review.
- Commands: backend `verify`, frontend `check`, `docker compose config --quiet`, Compose build/smoke, and browser viewport checks.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Runtime authentication is not implemented | Live US-03 calls remain 401 outside test security contexts | Keep protected; do not weaken authorization. Track identity as the next integration prerequisite | Required by security rules |
| The team needs browser-level AI testing before login exists | A blanket `permitAll` would expose paid provider usage | Add an explicit localhost-only demo principal behind `LOCAL_DEMO_MODE=false` by default; keep service account/consent checks | Approved by project lead for local internal testing |
| Provider cost/availability varies | Live test may depend on account access | Default to configurable cost-sensitive model and Standard fallback | Approved by request |
| Long chat history increases cost/privacy risk | More tokens and sensitive retention | Keep browser-session messages for UX but send only current message + minimized evidence in MVP | Approved by scope constraint |
