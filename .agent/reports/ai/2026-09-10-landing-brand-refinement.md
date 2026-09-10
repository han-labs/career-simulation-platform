# AI Assistance Log for CareerSim Landing and Brand Refinement

Date: 2026-09-10  
Human owner/reviewer: Huynh Gia Han or assigned project lead; final human review pending  
User story/foundation item: Catalog foundation and US-01 landing-page refinement

## Source material read

- Requirements: proposal DOCX, `docs/requirements/user-stories.md`, traceability matrix, and explicit project-lead request.
- Design/sequence/domain artifacts: `.agent/AGENTS.md`, `PROJECT_CONTEXT.md`, `TASKS.md`, `DESIGN.md`, every file under `.agent/rules/`, and the foundation/feature-plan templates.
- Existing code/tests: landing feature, shared brand/app shell, global CSS, router, catalog adapter/test, Docker Compose, backend catalog controller, and project README.

## Assistance used

- Tool/model if course disclosure requires it: OpenAI Codex coding agent; exact backend model identifier was not exposed to the repository workflow.
- Prompt/task summary without secrets or private data: Review project rules, govern academic external-source metadata collection, rename the public brand to CareerSim, improve landing-page responsive placement and visual style, and verify the full stack.
- Files/decisions influenced: documentation policy and traceability, brand strings, HTML metadata, landing eyebrow text, responsive CSS, focus treatment, visual tokens, verification evidence, and run/push guidance.

## Human review

- Behavior manually inspected: AI-assisted browser checks covered desktop, mobile, 320 px overflow, mobile navigation, and console errors; the responsible human should still approve the visual direction.
- Security/privacy/AI-boundary review: No product-AI or scoring behavior changed. External collection is limited to allowed public factual metadata and excludes personal/authenticated/protected content.
- Architectural responsibility review: Changes remain in shared presentation, landing feature, and governance documents; no backend responsibility boundary changed.
- Suggestions changed or rejected and why: Rejected the unconditional claim that non-commercial coursework alone grants crawling/republication rights. Replaced it with an approved academic purpose plus live terms/robots checks, field allowlisting, provenance, rate limiting, human review, and fail-closed behavior.

## Verification actually run

| Command/check | Result | Evidence location |
|---|---|---|
| `backend\\mvnw.cmd verify` | PASS; 7 tests | `.agent/reports/landing-brand-refinement-verification.md` |
| `frontend\\npm run check` | PASS; lint, 1 test, production build | `.agent/reports/landing-brand-refinement-verification.md` |
| `docker compose config --quiet` | PASS | `.agent/reports/landing-brand-refinement-verification.md` |
| `docker compose up -d --build` and gateway HTTP smoke | PASS; four services running, health/catalog/landing available | `.agent/reports/landing-brand-refinement-verification.md` |
| Responsive browser checks | PASS at requested desktop and tested mobile/narrow targets; no console errors | `.agent/reports/landing-brand-refinement-verification.md` |

## Remaining limitations

- No external-data collector, provenance persistence, or content-review UI was implemented.
- Assessment and simulation routes remain foundation placeholders.
- Human review and the exact commit SHA are pending before push or pull request.
