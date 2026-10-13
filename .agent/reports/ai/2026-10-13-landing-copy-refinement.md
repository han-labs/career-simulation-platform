# AI Assistance Log — landing copy refinement

Date: 2026-10-13
Human owner/reviewer: existing US-01 frontend owner; final visual review pending
User story/foundation item: US-01 landing and shared institutional shell, explicitly requested by project lead
Base: a69429088d793a8b9ec080b514339eac93c3d274 plus pre-existing uncommitted Syn/RAG work; no commit or push performed.

## Source material read
- Project lead's eight requested copy/typography corrections.
- .agent/AGENTS.md, PROJECT_CONTEXT.md, TASKS.md, DESIGN.md; testing and AI-assisted-development rules and templates.
- Existing landing components, AppShell, BrandMark, index.html, CSS and regression tests.

## Assistance used
- Codex applied scoped presentation patches, updated copy expectations and added consistent-CTA assertions.
- Technology and Engineering is used consistently despite item 5 repeating Education; assumption was announced before editing.
- CareerSim removed from shared visible branding and browser title, not from backend/product internals.
- Three hero highlights removed; descriptive copy shortened while retaining RIASEC methodology, objective scoring and student-choice limitations.
- Landing font weights reduced with scoped CSS; header/footer identity styles adjusted. Existing Syn, assessment logic, routes, APIs, database and dependencies left unchanged.

## Human review
- Agent inspected rendered updated copy, menu and responsive layout; responsible human review still pending.
- No new permissions, provider requests, data handling, scoring or persistence behavior introduced.
- Existing owner boundaries respected under explicit project-lead authorization.
- No security guarantees or scientific-validity claims were added. Existing absolute marketing claims were replaced with concise factual wording.

## Verification actually run
| Command/check | Result | Evidence location |
|---|---|---|
| frontend npm run check | Lint passed, 38 tests passed, production build passed | Tool output; frontend/dist (ignored) |
| backend mvnw.cmd verify | BUILD SUCCESS; 144 discovered, 140 executed, 4 opt-in skips, zero failures/errors | backend/target/surefire-reports (ignored) |
| docker compose config --quiet | Passed | Tool output |
| docker compose up -d --build --no-deps frontend; docker compose restart gateway | Frontend rebuilt/recreated; gateway restarted; existing database/backend preserved | Docker runtime careersimfresh |
| GET / and /api/health | New browser title/bundle; health UP | Tool output |
| Browser copy/DOM | Seven landing/shell assessment links use Career Assessment; removed labels and old school name absent; hero computed weight 500 | Tool output |
| Responsive checks | 320px and 1280px outer viewports: content width equals client width; mobile menu opens/closes; desktop hero two columns | Browser output; temporary viewport reset afterward |
| Screenshot | Default viewport after reset | .codex-tmp/landing-refined-2026-10-13.jpg (ignored) |
| git diff --check | Passed | Tool output |

## Remaining limitations
- Existing unrelated local work remains uncommitted; no attempt made to publish it.
- This increment does not redesign simulation detail/workspace or change the underlying RIASEC assessment.
- Full human content/accessibility review remains pending; the school-name change reflects the user's instruction, not independent institutional verification.
