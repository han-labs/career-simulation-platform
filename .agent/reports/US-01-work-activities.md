# US-01 Work Activities Test — Implementation and Evidence

Date: 2026-10-13  
Owner: User-directed assessment extension; existing team ownership remains documented in `.agent/TASKS.md`  
Branch/commit: Current shared working tree; no commit created  
Verdict: READY WITH CAVEAT

## 1 Scope and source

Adds Work Activities Test to the existing three assessments, resulting in exactly four active choices: RIASEC, Interests Quiz, Abilities Quiz, and Work Activities Test. The user-provided task explicitly requests end-to-end frontend/backend implementation and persistence. Job Bank's career quiz overview describes Work Activities as exploring preference for working with information/data, people, and things. CareerSim uses that high-level purpose only; all prompts, labels, and scoring presentation are original. The result is not a validated assessment or career prediction.

## 2 Implemented flows

- The catalog adds an active Work Activities Test card at `/assessment/work-activities`, with nine questions, an estimated three minutes, and a `Start Test` action.
- The route uses the existing quiz API/state lifecycle and its own `careersim_work_activities_quiz_v1` browser key.
- Nine original prompts cover Information (analyzing, comparing solutions, organizing), People (helping, explaining, coordinating), and Things (designing/prototyping, configuring tools, hands-on improvement).
- Students rate preference from 1 (Strongly prefer to avoid) to 5 (Strongly prefer this); all nine answers are required and may be selected or changed before submission.
- Backend verifies active quiz configuration, student ownership, attempt type, question membership, uniqueness, completeness, and rating bounds. The existing scoring policy sums the three backend-mapped answers per category (3–15), persists answers/scores/completion in one transaction, and returns top categories including ties.
- Completed result retrieval, retake, retry, and return to Career Assessments use existing flow components. Quiz type and browser key isolate Work Activities data.
- The UI calls scores preference signals, describes each category, and says this self-reflection is not validated or predictive.

## 3 Design to code mapping

Sequence details are recorded in `.agent/reports/US-01-career-tests-sequence-to-code.md`; approved implementation plan: `plans/US-01-work-activities.md`.

- Catalog and route: `assessmentCatalog.js`, `AssessmentPage.jsx`, `AssessmentIntro.jsx`.
- API lifecycle/local state: existing `useCareerQuiz.js` and `CareerQuizPage.jsx`.
- Scale/result: `CareerQuizPage.jsx`, `CareerQuizResult.jsx`, and shared `AssessmentForm.jsx`.
- Server behavior: `AssessmentController`, `AssessmentService`, `AssessmentServiceImpl`, `CareerQuizSubmissionPolicy`, and `CareerQuizScoringPolicy`.
- Persistence: existing quiz entities and repositories; append-only V11 migration.

## 4 API and database impact

No new endpoints, DTO shapes, or tables. The existing quiz endpoints accept `WORK_ACTIVITIES` using the existing `CareerQuizQuestionDto`, `SubmitAnswersRequest`, `CareerQuizResultDto`, and attempt DTO. `V11__add_work_activities_quiz.sql` expands the assessment-type checks and adds nine rows to `career_quiz_questions`; answers and scores reuse existing tables. V10 is unchanged.

## 5 Security, privacy, and AI

The backend remains the score authority; clients submit only question IDs and ratings. Category mapping is omitted from question DTOs. The service checks attempt ID and current project student identity before accessing results. No new private data or AI integration was added. Existing demo `X-Student-Id` identity remains a non-production limitation.

## 6 Verification evidence

| Check | Command/scenario | Observed result | Evidence |
|---|---|---|---|
| Backend | `cd backend; .\mvnw.cmd verify` | Passed: 76 tests, 0 failures/errors, 2 provider-live skips; PostgreSQL Testcontainers applied V11; Spotless and JaCoCo passed | Final command output, 2026-10-13 23:27 +07:00 |
| Focused assessment | `cd backend; .\mvnw.cmd '-Dtest=CareerQuizPersistenceIntegrationTest,CareerQuizScoringPolicyTest,CareerQuizSubmissionPolicyTest' test` | Passed: 9 tests, 0 failures/errors/skips; PostgreSQL persistence and migration verified | Command output, 2026-10-13 23:25 +07:00 |
| Frontend | `cd frontend; npm run check` | Passed: ESLint, 8 test files / 31 tests, Vite production build | Command output, 2026-10-13 23:23 +07:00 |
| Compose configuration | `docker compose config --quiet` | Passed, no output | Command output, 2026-10-13 |
| Integrated flow | Isolated Compose project on alternate ports; `/api/health`, Work Activities questions/start/submit/result, validation, reload, retake, and return via browser | Passed; four active options and nine questions visually observed; saved result showed three backend scores; browser check at mobile-width viewport | Nginx access log, browser DOM snapshot/screenshot, and PostgreSQL integration test, 2026-10-13 |
| Diff whitespace | `git diff --check` | Passed, no output | Command output, 2026-10-13 |

## 7 Architecture and report reconciliation

Assessment architecture table and database README are updated. The existing sequence trace now includes the fourth choice and per-type question configuration. No deployment topology changed.

## 8 Deviations and known gaps

- PostgreSQL migration and service persistence were verified with Testcontainers. An initial alternate-origin browser POST received 403 under the repo's default CORS allowlist; the isolated smoke backend was restarted with only `http://127.0.0.1:18080` allowed, after which the browser flow passed. This test-only override did not change project configuration.
- The Work Activities preference quiz is deliberately complementary to Interests and Abilities: it asks which broad work objects/interaction modes a student prefers, rather than what topics interest them or what skills they believe they have.
- Runtime authentication is still pending; the existing demo ID header must not be treated as production identity protection.

## 9 Changed artifacts

- Frontend catalog/route/quiz/result and tests.
- Backend shared quiz type/category policy and policy tests.
- Append-only V11 migration.
- Assessment architecture/database documentation, plan, sequence trace, this report, and AI assistance log.
