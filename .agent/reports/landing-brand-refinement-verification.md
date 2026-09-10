# CareerSim Landing and Brand Refinement Verification

Date: 2026-09-10  
Scope: Public brand, landing-page responsive layout, visual-system refinement, and external-data governance documentation

## 1 Scope and source requirement

The project lead requested the public shorthand `CareerSim`, a more elegant and student-friendly technology visual style, a higher hero composition that exposes both main actions on initial desktop load, and explicit approval for academically scoped Forage data research.

The implementation follows `.agent/plans/foundation-brand-and-landing-refinement.md`, the proposal's explore-simulate-reflect intent, the US-01/US-02 acceptance baseline, BR-16, and the existing accessibility gate.

## 2 Implemented flows

- Main: a public visitor sees the CareerSim brand, value proposition, both exploration actions, evidence limitation, and exploration-map preview in the first desktop viewport.
- Responsive: the hero stacks on narrower layouts, both actions become full width on mobile, and the navigation opens and closes through its named button.
- Catalog fallback: existing original fallback metadata remains unchanged when the backend catalog is unavailable.
- External-data fallback: source ambiguity or denial stops automated collection and preserves original/synthetic content as the safe path.

## 3 Design-to-code mapping

| Decision | Implementation |
|---|---|
| Public shorthand | `frontend/src/shared/ui/BrandMark.jsx`, `AppShell.jsx`, and `frontend/index.html` |
| Above-fold actions | Responsive hero height, spacing, typography, and compact preview rules in `frontend/src/styles/index.css` |
| Student-friendly technology mood | Warm neutral surfaces with restrained teal/blue accents and original CSS-native components |
| Keyboard and motion access | Global `:focus-visible` outline and retained reduced-motion media query |
| External-source boundary | `.agent/rules/external-data-and-research.md` and `docs/data/simulation-content-policy.md` |

## 4 API and database impact

No endpoint, DTO, backend behavior, database schema, migration, score, or AI contract changed.

## 5 Security privacy and external-data decisions

The academic purpose permits only factual public IT-simulation metadata when the current source Terms of Use, `robots.txt`, and technical controls permit automated collection. Authenticated content, personal data, employer tasks/files, model answers, certificates, screenshots, branding, and automatic publication remain excluded. A future collector must use a field allowlist, conservative rate limit, provenance per record, human review, and fail-closed behavior.

Non-commercial status is recorded as project purpose rather than treated as an automatic license. No crawler or third-party content was added in this change.

## 6 Tests and exact commands run

| Command or check | Observed result |
|---|---|
| `cd backend; .\\mvnw.cmd verify` | PASS; 7 tests, 0 failures/errors/skips; JAR, Spotless, architecture tests, and JaCoCo report completed. |
| `cd frontend; npm ci` | PASS; 171 packages installed, 0 reported vulnerabilities. |
| `cd frontend; npm run check` | PASS; ESLint, 1 Vitest test, and Vite production build completed. |
| `docker compose config --quiet` | PASS. |
| `docker compose up -d --build` | PASS; frontend/backend images built and PostgreSQL, backend, frontend, and gateway started. |
| `git diff --check` | PASS after the final documentation update. |
| `Invoke-RestMethod http://localhost:8080/api/health` | PASS through the Nginx gateway; `success=true`, `status=UP`. |
| `Invoke-RestMethod http://localhost:8080/api/v1/simulations` | PASS through the Nginx gateway; 5 published records; first slug `backend-api-triage`. |
| `Invoke-WebRequest http://localhost:8080/` | PASS; HTTP 200 and CareerSim document title present. |
| `docker compose ps --status running --services` | PASS; `backend`, `frontend`, `gateway`, and `postgres` running. |

## 7 Manual and integration evidence

- Browser through `http://localhost:8080/`, target viewport 1418 by 642: CareerSim title/brand rendered; the action group measured from y=493 to y=539 and was fully visible; no horizontal overflow.
- Browser target viewport 390 by 844: both actions were visible in the initial viewport and the mobile navigation opened and closed successfully.
- Browser target viewport 320 by 720 after the narrow-layout fix: document `scrollWidth` equaled `clientWidth` at 305 CSS px after scrollbar allocation, horizontal overflow was false, and both actions ended at y=556 within the viewport.
- Browser console: no error entries observed during the final responsive interaction checks.
- Gateway health returned `UP`, the public catalog returned 5 records, and the landing document returned HTTP 200 with the CareerSim title.

## 8 Deviations gaps and next action

- This change defines collection policy but does not implement a crawler, review UI, provenance schema, scheduled job, or import endpoint.
- The official source terms must be re-checked at the time of every future collection run. If they do not permit the proposed automation, written permission or manual/synthetic sourcing is required.
- The landing page routes still lead to the existing assessment and simulation placeholders, consistent with the current foundation checkpoint.
- Human owner review of wording, visual preference, and external-source authorization remains required before commit/PR.
- Exact commit SHA remains pending because this handoff intentionally stops before staging, committing, or pushing.

## 9 Files and artifacts changed

See the final `git diff --stat` and `git status --short` output for the complete set. Architecture and database diagrams were not changed because deployment and runtime boundaries are unchanged.
