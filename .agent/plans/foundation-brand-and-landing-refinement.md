# Foundation Brand and Landing Refinement Plan

Status: Implemented  
Owner: Shared frontend foundation with landing-page coordination authorized by the project lead  
Date: 2026-09-10

## 1 Scope

- User story or foundation item: Catalog foundation and US-01 landing-page refinement.
- Actor: Prospective student visiting the public landing page.
- Source proposal/SRS section: Proposal Part A sections 1-2; `docs/requirements/user-stories.md` US-01 and US-02; project-lead request dated 2026-09-10.
- Acceptance criteria:
  - The public product shorthand is `CareerSim` wherever the site brand is displayed.
  - At the supplied 1418 by 642 desktop viewport, the hero content begins higher and both `Start with RIASEC` and `Browse simulations` are visible without scrolling.
  - The landing page remains usable without horizontal scrolling at 320 px and preserves semantic controls, visible focus, and reduced-motion behavior.
  - The visual system feels calm, elegant, student-friendly, professional, and technology-oriented without copying The Forage branding or layout.
  - Academic external-data rules explicitly allow collection of permitted public factual metadata for IT simulation research while preserving source, terms, robots, rate-limit, copyright, privacy, and human-review controls.
- Explicit exclusions: No assessment workflow, simulation attempt workflow, backend contract, schema, scoring, AI behavior, or production crawler implementation.
- Related owner boundaries: The project lead explicitly requested this landing/shared-style change; no assessment or simulation-owned feature behavior changes.

## 2 Flow inventory

### Main success flow

1. A student opens the landing page and immediately sees the CareerSim identity, value proposition, and both exploration actions.
2. The student can start the RIASEC placeholder route or browse simulation placeholders.
3. The rest of the page explains the assess, simulate, reflect loop and shows catalog data or reviewed fallback metadata.

### Alternate flows

1. At tablet/mobile widths, navigation collapses and hero content becomes a single readable column.
2. If the catalog API is unavailable, original reviewed fallback cards remain visible with a status message.

### Exception and fallback flows

1. Reduced-motion preference suppresses non-essential transitions.
2. Very narrow viewports retain at least 14 px gutters and stack both actions at full width.
3. External-source terms, robots rules, or provenance cannot be verified: automated collection stops and synthetic/original content remains the publishing fallback.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Product shorthand | Shared presentation component | `BrandMark` and document metadata | Keeps visible branding consistent. |
| Hero content and actions | Feature view | `LandingPage` | Landing-specific presentation remains in its feature. |
| Responsive visual tokens/layout | Shared presentation styles | `styles/index.css` | Existing foundation owns reusable visual and breakpoint rules. |
| External-source governance | Project policy | `.agent/rules/external-data-and-research.md` and `docs/data/simulation-content-policy.md` | Keeps collection permission separate from runtime/catalog behavior. |

## 4 Change surface

- Frontend routes/pages/components/hooks/API adapters: `LandingPage`, `BrandMark`, `AppShell`, global styles, HTML metadata.
- Backend controller/request/response DTOs: None.
- Service facade methods: None.
- Service/domain/evaluator/provider implementation: None.
- DAO queries/entities: None.
- Flyway migrations: None.
- Configuration/environment: None.
- Documentation/diagram/report: external-data policy, project context, README, AI assistance log, verification report.

## 5 API contract

No API contract changes.

## 6 Security privacy and AI

- Authentication/role: Public landing behavior only; unchanged.
- Ownership/visibility: Public catalog metadata only; unchanged.
- Sensitive data: The external-data policy prohibits personal data collection.
- AI context fields: None.
- Output schema/validation: Not applicable.
- Timeout/fallback: Existing catalog fallback remains deterministic and original.
- Logging/redaction: A future collector must avoid response-body dumps and record only sanitized provenance/operational facts.

## 7 Tests and evidence

- Domain/policy tests: Not applicable.
- Service tests: Existing backend catalog tests must remain green.
- Controller/API tests: Existing backend catalog tests must remain green.
- Repository/migration tests: Existing verification suite; no migration change.
- Frontend tests: Existing catalog adapter tests, lint, and production build.
- End-to-end/manual evidence: Desktop 1418 by 642 and mobile 390 by 844 landing-page inspection; `/api/health` and landing/catalog smoke through the gateway when the stack is available.
- Commands to run: `backend\\mvnw.cmd verify`, `frontend\\npm run check`, `docker compose config --quiet`, Docker Compose build/run and HTTP smoke checks.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Does non-commercial coursework alone grant reuse rights? | Overbroad crawling or republication could violate source terms or copyright. | Treat academic use as the approved project purpose, collect only public factual metadata when current terms/robots permit it, and require separate written permission/license for protected content. | Approved scope from project-lead request, constrained by BR-16 and source controls. |
| Landing page belongs to the US-01 frontend owner. | Uncoordinated edits could cross team scope. | Record the project lead's explicit request as coordination authorization and avoid assessment behavior changes. | Approved by current request. |
| More decoration could reduce accessibility or copy another product. | Visual novelty could harm clarity or traceability. | Reuse the approved design tokens and create original CSS-native technology details with AA-oriented focus/contrast checks. | Approved by current request. |
