# AI Assistance Log

Date: 2026-09-15  
Human owner/reviewer: Huỳnh Gia Hân  
User story/foundation item: US-03 Syn widget, optional AI adapter, and security hardening

## Source material read

- Requirements: `docs/requirements/user-stories.md`, `.agent/PROJECT_CONTEXT.md`,
  `.agent/TASKS.md`, and the project-lead conversation.
- Design/sequence/domain artifacts: `.agent/DESIGN.md`, applicable architecture,
  security, testing, reporting, and AI rules, plus the US-03 plans.
- Existing code/tests: React routing/layout/guidance adapter, Spring guidance
  service/DAO/policy, migrations, Compose/Nginx, and all relevant tests.
- External primary documentation: official OpenAI Responses API and official
  Gemini model, API-key, GenerateContent, models-list, structured-output, and
  billing documentation. No private inputs or credentials were supplied to
  research tools.

## Assistance used

- Tool/model if course disclosure requires it: OpenAI Codex coding assistant;
  browser computer-use was used for local visual QA.
- Prompt/task summary without secrets or private data: make Syn a global
  three-mode assistant, add a bounded cost-conscious provider integration,
  harden common web boundaries, document configuration, and verify the stack.
- Files/decisions influenced: global React mount, custom Dashboard-to-widget
  event, provider port/adapters, consent/free-text gate, minimized context,
  strict schema, fallback audit, gateway limits/headers, localhost-only demo
  authentication, tests, and evidence.

## Human review

- Behavior manually inspected: all three modes, mobile/desktop layout, route
  persistence, Dashboard result action, objective score stability, and browser
  console.
- Security/privacy/AI-boundary review: key remains server-side; no identifying
  profile/raw answers/evaluation rules go to the provider; no raw provider
  failures are exposed; guidance remains authenticated.
- Architectural responsibility review: provider details remain in
  infrastructure; policy remains deterministic; service owns authorization,
  consent, fallback, and audit; frontend uses the existing API adapter.
- Suggestions changed or rejected and why: no autonomous tools/RAG/history were
  added because they increase cost, privacy, and explanation surface without
  improving the current story. Guidance was not made globally public just to
  enable a browser demo; an explicit loopback-only demo principal was used
  instead. HSTS was not added to an HTTP-only local baseline.

## Verification actually run

| Command/check | Result | Evidence location |
|---|---|---|
| backend `mvnw.cmd verify` | 47 tests passed, 2 opt-in live tests skipped; build/Spotless/architecture passed | backend test and JaCoCo output |
| `frontend/npm run check` | lint passed, 9 tests passed, production build passed | frontend console/dist |
| clean Compose build/start and health | images built; V1-V7 applied; health 200 | Compose logs/status |
| 15 rapid Syn requests and 17 KiB body | 429 throttling and 413 size rejection observed | gateway responses |
| browser regression check | after four chat actions at 390x650, both context tags remained visible and the fixed-height row did not shrink | local in-app browser inspection |
| secret-pattern and Git diff checks | no credential-shaped value or whitespace error found | console |
| localhost demo identity tests | 8 focused filter/controller tests passed | backend test output |
| local gateway Gemini smoke | health returned UP; Dashboard returned LIVE; safe synthetic free-text returned provenance AI | `careersimfresh` responses |

## Remaining limitations

- Production authentication/login and end-to-end consent capture are not yet
  wired. Browser AI use is available only through an explicitly enabled
  localhost demo principal backed by the synthetic student.
- A real Gemini smoke succeeded using minimized synthetic evidence. OpenAI was
  not called, and normal tests remain provider-mocked.
- Original local database volume needs an owner-approved reset or migration
  decision due an earlier development-only V3 checksum collision.

No credentials, private student inputs, unrestricted prompts, or raw model
responses are recorded here.
