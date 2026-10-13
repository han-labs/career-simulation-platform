# US-01 Landing copy and typography refinement

Status: Implemented; automated and browser checks passed; human visual review pending
Owner: Existing US-01 frontend owner; shared shell coordinated by explicit user request
Date: 2026-10-13

## 1 Scope
- Source: project lead's eight landing-page corrections in this chat.
- Actor: public visitors and students.
- Acceptance: use Technology and Engineering consistently; Career Assessment labels on landing/shell assessment actions; remove the three hero highlights and visible CareerSim branding; update institutional platform/tagline and hero badge; shorten copy and lighten landing typography while keeping routes and accessible controls.
- Exclusions: backend, scores, assessment internals, Syn, persistence, dependencies, other simulation screens.

## 2 Flow inventory
- Main: open landing, read concise content, follow unchanged assessment/simulation/dashboard routes.
- Alternate: completed assessment keeps its result CTA; mobile navigation remains usable.
- Fallback: preserve existing catalog sample notice and data fetching.

## 3 Responsibility design
| Responsibility | Component | Reason |
|---|---|---|
| Landing copy | Existing landing section components | Presentation only |
| Shared identity/navigation | AppShell, BrandMark, index.html | Existing shared presentation boundary |
| Typography | Scoped landing CSS and shell identity styles | Avoid touching assessment/Syn styles |

## 4 Change surface
- Frontend: landing sections, shared shell/brand, document metadata, CSS, copy regression tests.
- Backend/API/database/environment: no changes.
- Documentation: this plan and AI assistance evidence log; architecture unchanged.

## 5 API contract
No changes. Existing /assessment, /simulations and /dashboard routes retained.

## 6 Security privacy and AI
No changes to authorization, data, provider calls, schemas or fallback. Preserve focus indicators and content limitations. Do not publish or commit user work.

## 7 Tests and evidence
- Update copy expectations; add regressions for removed chips/branding and consistent assessment CTAs.
- Run frontend npm run check, backend mvnw.cmd verify and docker compose config --quiet.
- Rebuild frontend only, refresh gateway/browser; inspect normal/mobile rendering and route targets.

## 8 Risks and decisions
- Item 1 requests Engineering but item 5 repeats Education. Apply item 1 consistently; retain item 5's Career Simulation Platform suffix. Assumption announced before editing.
- Keep existing local Syn/RAG edits intact. No scoring or API behavior changes.
