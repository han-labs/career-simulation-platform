# US-03 Syn Widget, AI, and Security Evidence Report

Date: 2026-09-15  
Owner: Huỳnh Gia Hân  
Branch/commit base: `main` / `b43f15a` with reviewed working-tree changes  
Verdict: READY FOR LOCAL DEMO WITH CAVEAT

## 1 Scope and source

The approved scope is US-03 evidence exploration plus focused platform
hardening: one global three-mode Syn popup, a bounded and token-conscious AI
adapter for open questions, deterministic fallback, plan confirmation, and
defences against common web abuse. Authentication implementation, AI scoring,
career decisions, certification, RAG, autonomous tools, and long-term chat
memory remain excluded.

Source artifacts: `docs/requirements/user-stories.md`, `.agent/PROJECT_CONTEXT.md`,
`.agent/DESIGN.md`, the applicable `.agent/rules/`, and
`.agent/plans/US-03-syn-widget-ai-security.md`.

## 2 Implemented flows

- Syn mounts beside `AppShell`, so the circular launcher appears on every
  current route and the browser-session conversation survives navigation.
- The launcher opens a compact chat; maximise toggles the expanded window;
  minus or Escape returns to the circle.
- Dashboard actions open the same global widget through one scoped custom
  event, avoiding a duplicate chat and avoiding a new state-management layer.
- Reviewed quick actions use deterministic Standard guidance and spend no
  provider tokens.
- Only safe `FREE_TEXT` from an ACTIVE STUDENT with recorded consent and an
  available configured provider may call Gemini or OpenAI. Success is labelled
  `AI-assisted`.
- Missing consent/key, disabled provider, unsafe input, timeout, HTTP failure,
  incomplete/malformed output, and invalid frontend response all preserve a
  labelled Standard fallback.
- Result cards reuse deterministic scores/outcomes. Plan drafts are written
  only after explicit confirmation.
- `LOCAL_DEMO_MODE=true` can supply the seeded synthetic student for
  loopback-host guidance requests while login is unfinished. The service still
  revalidates the account and recorded consent; the flag defaults to false.

## 3 Design-to-code mapping

The sequence is implemented as controller -> service interface -> service
implementation -> access/DAO/domain/provider ports -> infrastructure adapter.
`StandardGuidancePolicy` owns deterministic and safety decisions;
`GuidanceServiceImpl` owns identity, ownership, consent, provider selection,
orchestration, and audit; the Gemini and OpenAI adapters each own their provider
HTTP/schema/timeout details behind the same `SynGuidanceProvider` port.

Frontend transport stays in `guidanceApi.js`; the Dashboard remains concise in
`GuidancePage.jsx`; all popup behavior stays in the single `SynWidget.jsx`.
The full presentation trace is in `docs/evidence/us-03-frontend-walkthrough.md`.

## 4 API and database impact

- Existing contracts remain: `GET /v1/guidance/dashboard`,
  `POST /v1/guidance/syn/messages`, and `POST /v1/guidance/plans`.
- `SynMessageResponse` continues to expose structured result/plan/suggestion
  fields and explicit `AI` or `STANDARD` provenance.
- V6 adds exploration plans and the US-03 audit/persistence support.
- V7 advances sequences after explicit-ID team seed migrations, preventing a
  duplicate primary key on later inserts.
- Guidance audit rows now distinguish `AI` and `FALLBACK`, record the model
  only for AI, and mark provider replacement as `REPLACED_BY_FALLBACK`.

## 5 Security, privacy, and AI behavior

- Provider credentials are backend environment values and are never included
  in the React build.
- Provider context excludes email, display name, identifiers, raw answers, and
  evaluation rules. It contains only the current message and bounded evidence.
- Provider requests cap output tokens and require a JSON schema; the OpenAI
  request also sets `store=false`. Local parsing repeats length/count checks.
- The stable instruction treats both user text and evidence strings as
  untrusted data. Safety policy blocks common prompt/secret/answer-key and
  decision/guarantee requests before an external call.
- Provider errors are classified internally; raw errors, prompts, responses,
  and keys are neither returned nor logged.
- Spring keeps guidance authenticated, checks ACTIVE STUDENT and ownership,
  denies unspecified routes, restricts CORS, and emits security headers.
- Nginx applies a stricter Syn rate limit, 16 KiB Syn body limit, proxy timeout,
  version hiding, CSP, frame denial, MIME-sniffing, referrer, and permission
  headers.

These are layered risk reductions, not a guarantee that the application cannot
be attacked.

## 6 Verification evidence

| Check | Command/scenario | Observed result | Evidence |
|---|---|---|---|
| Backend | backend `mvnw.cmd verify` | BUILD SUCCESS; 47 tests, 0 failures/errors, 2 opt-in live tests skipped; Spotless and architecture checks passed | console; `backend/target/surefire-reports`, `backend/target/site/jacoco` |
| Frontend | `frontend/npm run check` | ESLint passed; 3 files/9 tests passed; production Vite build passed | console and `frontend/dist` |
| Compose | `docker compose config --quiet`; clean project `docker compose -p careersimverify up -d --build` | config/build passed; PostgreSQL healthy; backend applied V1-V7 and started | Compose status/logs |
| Integrated | `GET /api/health` | 200 with service status UP | gateway response |
| Access | unauthenticated `GET /api/v1/guidance/dashboard` | 401; endpoint was not weakened for demo | gateway response |
| Local demo access | localhost `GET /api/v1/guidance/dashboard` with explicit demo mode | 200, source `LIVE`, principal resolved to active `Demo Student` | `careersimfresh` gateway response |
| Local Gemini flow | consented synthetic demo profile; safe free-text POST through gateway | 200 with structured response, provenance `AI`, and three suggestions | `careersimfresh` gateway response |
| Edge abuse | 15 rapid Syn POSTs; 17 KiB Syn body | first requests reached auth, later requests returned 429; oversized body returned 413 | gateway responses |
| Headers | landing, health, and guidance responses | CSP, nosniff, DENY framing, referrer, and permissions headers present | response headers |
| Browser | mobile 390x650 after four quick-action replies | yellow Preview and blue result tags remained visible at 25/33 px; row stayed 43 px with `flex-shrink: 0` | manual computer-use inspection |
| AI adapters | mocked Responses and GenerateContent API tests | provider auth, schema, minimized context, output cap, malformed/blocked response fallback all passed | `OpenAiSynGuidanceProviderTest`, `GeminiSynGuidanceProviderTest` |
| Live AI | local gateway smoke with minimized synthetic evidence | Gemini returned a schema-valid response with provenance `AI`; automated live tests remain opt-in and were skipped by the default suite | gateway response; `GeminiSynGuidanceLiveTest` |
| Secret/diff | both Git diff checks plus repository key-pattern scan | no whitespace errors and no key-shaped secret found; only documented placeholders/references | console |

## 7 Architecture and report reconciliation

Architecture, API, database, deployment, frontend walkthrough, plan, task
status, and AI-assistance evidence now describe the same global-widget and
hybrid deterministic/AI design. Both providers remain behind a project-owned
port, so selecting a provider does not affect controller or UI contracts.

## 8 Deviations and known gaps

- Runtime authentication/login is not implemented. Normal browser guidance
  calls therefore receive 401 and intentionally show preview/Standard mode.
  Local internal testing may explicitly enable the loopback-only demo identity;
  production login remains the primary caveat and next foundation item.
- US-01/US-02 still contain temporary public/header-based development identity
  behavior. They must be moved to the same authenticated principal before real
  student use.
- CSRF is disabled only because the intended API is stateless bearer-auth. It
  must be re-reviewed if cookie/session authentication is chosen.
- The local Compose file exposes backend and frontend ports for debugging.
  Production should expose only a TLS gateway; HSTS is intentionally omitted
  from the current HTTP baseline.
- The original default local PostgreSQL volume was preserved after Flyway
  reported a checksum collision with the earlier untracked development V3.
  Verification used a separate clean `careersimverify` volume; no repair or
  deletion was performed.
- A real Gemini request using only the synthetic demo profile succeeded through
  the local gateway. Normal automated tests remain mocked and never spend tokens.

## 9 Changed artifacts

- Application: shared route layout, Dashboard, one Syn widget, guidance API
  adapter, guidance service/policy/DAO/provider, Spring and Nginx security.
- Tests: policy, service, controller/header, persistence, mocked provider, and
  opt-in synthetic live provider smoke.
- Migrations: V6 exploration plans and V7 seed-sequence synchronization.
- Documentation: root/API/architecture/database/deployment guides, current
  plan/status, frontend walkthrough, this evidence report, and AI log.
- Mechanical formatting: Spotless reformatted newly pulled simulation Java
  files without behavior changes so the repository quality gate passes.
