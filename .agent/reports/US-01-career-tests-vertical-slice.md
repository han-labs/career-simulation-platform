# US-01 Career Tests Vertical Slice — Implementation and Evidence

> Historical scope note: this report documents the earlier three-assessment checkpoint. The current four-assessment scope and Work Activities verification are recorded in `.agent/reports/US-01-work-activities.md`.

Date: 2026-10-13  
Owner: Backend and frontend implementation (user-authorized scope expansion)  
Branch/commit: Current working tree; no commit created  
Verdict: READY WITH CAVEAT

## 1 Scope and source

Implements the user-authorized expansion to three active assessments: the existing RIASEC assessment, an original Interests Quiz, and an original Abilities Quiz. The source assignment is the user's pasted Career Tests request and subsequent explicit approval of backend ownership, together with US-01 and its frontend assignment boundary. Research used the Job Bank career quiz overview and its Interests and Abilities categories as inspiration. No Job Bank questions were copied and no official endorsement or validation is claimed.

The previous frontend-only checkpoint report remains as history and is superseded by this vertical-slice report. The follow-up scope decision is recorded in `plans/US-01-career-tests.md`.

## 2 Implemented flows

- The Career Tests selection page renders exactly three active cards: **RIASEC Interest Assessment**, **Interests Quiz**, and **Abilities Quiz**. Each navigates to its own route.
- RIASEC retains its existing question source, answer scale, API, deterministic backend scoring, and persisted result. Its flow now has a dedicated route and a return link to Career Tests.
- Interests serves 12 original work-preference questions with 1–5 interest ratings across Analytical, Creative, Collaborative, and Organizing categories.
- Abilities serves 12 original confidence-reflection questions with 1–5 ratings across Problem Solving, Communication, Organization, and Technical Learning categories.
- New quizzes require all 12 distinct valid answers. The backend verifies active question configuration, assessment type, attempt ownership, question membership, duplicates, and response scale before saving.
- The backend sums three ratings per category (3–15), persists answers and category totals with the completed attempt, and returns all scores and tied highest categories. Completed submissions are idempotent. Result retrieval checks ownership and quiz type.
- Each quiz has a separate browser-local progress key. Submitted answers and results are stored on the backend; the frontend only displays returned category scores. Retake creates a new attempt; all quiz routes link back to Career Tests.
- Results explain the category scale and frame outcomes as self-reflection rather than validated aptitude, diagnosis, certification, or career prediction.

## 3 Design to code mapping

The sequence-to-code trace is in `.agent/reports/US-01-career-tests-sequence-to-code.md`.

- Selection and routes: `frontend/src/features/assessment/components/AssessmentIntro.jsx`, `frontend/src/features/assessment/data/assessmentCatalog.js`, `frontend/src/features/assessment/pages/AssessmentPage.jsx`, and `frontend/src/app/App.jsx`.
- RIASEC integration: `RiasecAssessmentFlow.jsx` and the existing `useAssessment` API/state path.
- Interests/Abilities UI state and presentation: `useCareerQuiz.js`, `CareerQuizPage.jsx`, `CareerQuizResult.jsx`, and shared `AssessmentForm.jsx`.
- REST adapter and business behavior: `AssessmentController`, `AssessmentService`, and `AssessmentServiceImpl` in the assessment module.
- Validation/scoring: `CareerQuizSubmissionPolicy` checks question sets and answers; `CareerQuizScoringPolicy` sums backend question categories.
- Persistence and representation: `CareerQuizQuestion`, `CareerQuizAnswer`, `CareerQuizScore`, their repositories, and quiz DTOs.

## 4 API and database impact

Existing RIASEC endpoints are unchanged. Added endpoints:

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/assessments/quizzes/{type}/questions` | Return active quiz prompts without category scoring mappings |
| POST | `/api/v1/assessments/quizzes/{type}/attempts` | Start an owned `INTERESTS` or `ABILITIES` attempt |
| POST | `/api/v1/assessments/quizzes/attempts/{id}/submit` | Validate and persist answers, scores, and completion |
| GET | `/api/v1/assessments/quizzes/attempts/{id}/result` | Return persisted category scores for the owning student |

Append-only Flyway migration `V10__add_interests_and_abilities_quizzes.sql` adds `assessment_type` to attempts (existing rows default to `RIASEC`) and creates `career_quiz_questions`, `career_quiz_answers`, and `career_quiz_scores`, with response/category score bounds, primary keys, foreign keys, and seeded original questions. `docs/database/README.md` documents the new schema. No simulation or guidance schema changed.

## 5 Security, privacy, and AI behavior

Question IDs and category mappings are authoritative on the backend. Submission and result reads query the attempt by both ID and student ID and verify assessment type. No new personal data or AI provider context was added. The API follows the repository's existing temporary `X-Student-Id` demo contract and public assessment route configuration; runtime login/authentication is still pending, so this must not be treated as production identity protection. No AI calculates or alters scores.

## 6 Verification evidence

| Check | Command/scenario | Observed result | Evidence |
|---|---|---|---|
| Backend full gate | `cd backend; .\mvnw.cmd verify` (with local loopback access) | Passed: 71 tests, 0 failures/errors, 6 skipped; package, Spotless, JaCoCo report passed | Command output, 2026-10-13 22:33 +07:00 |
| Focused policy tests | `cd backend; .\mvnw.cmd '-Dtest=CareerQuizScoringPolicyTest,CareerQuizSubmissionPolicyTest' test` | Passed: 4 tests | Command output, 2026-10-13 22:29 +07:00 |
| Frontend gate | `cd frontend; npm run check` | Passed: ESLint; 8 test files / 30 tests; Vite production build | Command output, 2026-10-13 22:14 +07:00 |
| Compose configuration | `docker compose config --quiet` | Passed, no output | Command output, 2026-10-13 22:29 +07:00 |
| Diff whitespace | `git diff --check` | Passed, no output | Command output, 2026-10-13 |
| Browser visual / integrated flow | Tried opening the local Vite page in the in-app browser; browser returned `net::ERR_CONNECTION_TIMED_OUT`. Backend was not running for a full question/submit UI flow. | Not completed; no screenshot or live integrated result claimed | Computer Use output, 2026-10-13 |

The six skipped backend tests include Docker/Testcontainers-dependent and provider-live cases; Docker was unavailable. The first non-escalated full backend run could not create a loopback client; the final authorized local-loopback run passed.

## 7 Architecture and report reconciliation

The assessment module remains inside the existing modular monolith. The deployed topology and architecture diagram are unchanged; the assessment module responsibility is updated in `docs/architecture/README.md`. Database ownership and the additive V10 migration are documented in `docs/database/README.md`. No existing sequence artifact covered these new quiz endpoints; the new trace file maps each flow and exception.

## 8 Deviations and known gaps

- No browser screenshot or manual desktop/mobile walkthrough was possible because the in-app browser could not reach the local Vite server. The responsive card/result styles and frontend production build passed, but actual viewport behavior remains for a human browser check.
- No end-to-end request was sent through Nginx with PostgreSQL. Backend service policy tests and the complete existing backend suite passed; live PostgreSQL migration application was not run because Docker is unavailable.
- Runtime authentication is not implemented in the project. The existing demo identity/header remains a known non-production limitation.
- The project does not configure the new Interests/Abilities evidence for Syn. Their results remain available in their own test flows and do not change RIASEC/Syn evidence.
- The named US-01 frontend owner should review the expanded scope during team coordination.

## 9 Changed artifacts

- Application code: assessment backend controller/service/domain/DTO/entity/repository, V10 migration, frontend assessment routes/components/hooks/data/styles, and existing landing/navigation changes already present in the working tree.
- Tests: backend quiz scoring/submission policy; frontend selection, result, and existing landing/RIASEC tests.
- Documentation/evidence: approved vertical-slice plan, sequence trace, this report, AI assistance log, database README, architecture module table, and superseding notes in the earlier plan/report/log.

