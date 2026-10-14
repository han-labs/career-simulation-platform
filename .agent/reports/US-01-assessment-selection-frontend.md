# US-01 Assessment Selection Frontend Implementation and Evidence

Date: 2026-10-13
Owner: Frontend implementation (AI-assisted; plan approved by user)
Branch/commit: `main` working tree; no commit created
Verdict: READY WITH CAVEAT

## 1 Scope and source

Implements the approved frontend-only assessment-selection plan based on US-01 in `docs/requirements/user-stories.md` and the pasted Career Assessment and Quiz Selection System assignment.

The assessment area now has the general English heading **Career exploration assessments** and its supporting description. The existing RIASEC assessment is the only available scored option. Two additional choices are clearly identified as planned and cannot be started. No new scoring authority, backend endpoint, API DTO, database schema, or third-party question text was added.

## 2 Implemented flows

- **Selection:** RIASEC displays six dimensions, 42 questions, an approximate duration, self-rating format, and available status. Planned abilities/strengths and work-activities cards disclose unknown duration/question counts, inspiration, and why their controls are disabled. All student-facing copy is in English.
- **Main assessment:** existing question retrieval/start/submit endpoints and backend scoring remain in place. The student sees progress, selected radio answers, required-answer errors, and can resume browser-local answers.
- **Result:** all six scores appear in stable RIASEC order with numeric values and accessible bars. Equal maximum scores appear as shared signals, with directions combined as exploration options.
- **Errors and recovery:** API errors are announced; failed starts do not enter the questionnaire; empty/incomplete result data is handled; submit is locked against repeated in-flight calls; restarting returns to selection.
- **Persistence boundary:** submitted attempts/results remain backend-persisted under the existing contract. Unsubmitted answers remain in this browser's local storage; they are not backend autosaved and do not sync across devices.
- **AI boundary:** AI does not calculate or alter scores.

## 3 Reference research and design-to-code mapping

Job Bank's career quiz page was inspected on 2026-10-13. It presents six quiz categories:

- Career possibilities: **Interests**, **Abilities**, and **Work activities**.
- Personality exploration: **Multiple intelligences**, **Learning style**, and **Work values**.

The inspected quiz descriptions present these as self-exploration aids. Additional quiz pages showed required-answer questionnaires and ranked results. CareerSim uses only the categories Abilities and Work activities as high-level inspiration for clearly marked future options; the implementation includes no copied questions and does not imply Job Bank endorsement.

Sources:

- [Job Bank Career Quizzes](https://www.jobbank.gc.ca/career-planning/quizzes)
- [Job Bank Abilities quiz](https://www.jobbank.gc.ca/abilities)
- [Job Bank Interests quiz](https://www.jobbank.gc.ca/workpreference)
- [Job Bank Multiple intelligences quiz](https://www.jobbank.gc.ca/intelligence)
- [Job Bank Learning style quiz](https://www.jobbank.gc.ca/seeheardo)

Trace mapping:

- Approved requirements and scope: `plans/US-01-assessment-selection-frontend.md`
- Selection metadata and availability: `frontend/src/features/assessment/data/assessmentCatalog.js`
- Selection UI: `frontend/src/features/assessment/components/AssessmentIntro.jsx`
- Existing question, attempt, and answer state/API: `frontend/src/features/assessment/hooks/useAssessment.js`
- Question flow and progress: `frontend/src/features/assessment/pages/AssessmentPage.jsx`, `AssessmentForm.jsx`, `AssessmentQuestionCard.jsx`, `AssessmentProgressBar.jsx`
- Results and interpretation: `frontend/src/features/assessment/components/AssessmentResult.jsx`
- General product-facing labels: `frontend/src/shared/layout/AppShell.jsx` and `frontend/src/features/landing/components/`

No backend or architecture diagram changes were needed.

## 4 API and database impact

No API, DTO, backend, persistence, or migration changes.

The frontend continues using:

- `GET /api/v1/assessments/questions`
- `POST /api/v1/assessments/attempts`
- `POST /api/v1/assessments/attempts/{id}/submit`

The backend remains responsible for answer validation, deterministic sums for the six RIASEC dimensions, result persistence, and top-dimension response. The frontend displays returned scores; it does not recalculate them.

## 5 Security, privacy, and AI behavior

No additional personal data is collected or sent to a provider. The existing temporary local demo identity/header contract is unchanged. Backend attempt ownership remains the authorization boundary. New results content describes exploratory self-reflection and explicitly disclaims diagnosis, measured ability, guaranteed suitability, and career prediction.

## 6 Verification evidence

| Check | Command/scenario | Observed result | Evidence |
|---|---|---|---|
| Frontend quality gate | `cd frontend; npm run check` | Passed: ESLint; 7 test files, 28 tests; production build | Command output in this task, 2026-10-13 |
| Patch whitespace | `git diff --check` | Passed with no output | Command output in this task |
| Browser E2E | No configured Playwright/Cypress/browser E2E runner | Not available in repository | `frontend/package.json`, Vitest config |
| Rendered visual inspection | Browser helper stopped before navigation because it could not determine the current browser URL with enough confidence | Not completed | Computer Use tool output in this task |

Backend and Compose checks were not run because this change does not touch those layers. No manual student run is claimed.

## 7 Architecture and report reconciliation

Frontend-only feature; backend architecture and diagrams are unchanged. The original US-01 feature plan describes the assessment backend and is left intact. This additive frontend plan/report documents the selection experience and its unchanged backend contract.

## 8 Deviations and known gaps

- Only RIASEC is currently active. Abilities and work activities are planned options pending approved content and compatible backend scoring/API contracts.
- Progress restoration is browser-local and depends on local storage. The backend stores answers only on submission.
- No browser E2E tool is configured; visual inspection could not be completed in this run.
- Manual cases below are independently executable. Their actual result and pass/fail fields remain not run until a human executes them.
- The repository assigns the US-01 frontend to a named teammate; this approved change remains within that feature boundary, but should be shared with that owner during team review.

## 9 Manual student test checklist

For every case, fill in **Actual result** and change **Status** only after executing it.

### MT-01 — Choose the available assessment

- **Purpose:** Verify assessment discovery and the active RIASEC choice.
- **Preconditions:** Application and backend are running; open a fresh browser or clear CareerSim assessment local storage.
- **Steps/input:** Open `/assessment`; review the heading, supporting description, RIASEC card, dimension list, question count, duration, format, and status.
- **Expected result:** The general English heading and description appear. RIASEC is available and shows 42 questions and the 1–5 format. Planned cards are visibly labelled as planned.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-02 — Planned options cannot start

- **Purpose:** Confirm future options are not presented as working tests.
- **Preconditions:** Assessment selection page is open.
- **Steps/input:** Inspect the abilities/strengths and work-activities cards; try keyboard tab navigation through their controls.
- **Expected result:** Both are labelled Planned; duration and question count are shown as unknown; start controls remain disabled and each card explains that it is not ready.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-03 — Complete RIASEC and view results

- **Purpose:** Exercise the full available assessment flow.
- **Preconditions:** Backend is running and has 42 active questions.
- **Steps/input:** Start RIASEC. Use each question's five-point scale and answer all 42 items. For a deterministic fixture, select 5 for each Investigative question and 1 for each other question. Submit once.
- **Expected result:** Progress reaches 42/42; the backend accepts one submission; the result shows all six dimensions in R-I-A-S-E-C order, each numeric score, strongest interest signal(s), exploratory directions, limitations, and a link to IT simulations.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-04 — Incomplete answers are blocked

- **Purpose:** Verify required-answer validation.
- **Preconditions:** Start a new RIASEC attempt.
- **Steps/input:** Answer the first two questions only, then select Submit answers.
- **Expected result:** Submission is blocked; the number of unanswered questions and first unanswered item are indicated; selected answers remain visible.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-05 — Resume and back navigation preserve answers

- **Purpose:** Verify browser-local progress restoration.
- **Preconditions:** Start RIASEC and answer the first three questions.
- **Steps/input:** Navigate back to the selection page using browser history or refresh the page, then choose Continue.
- **Expected result:** The same in-progress attempt resumes and the three previous selections remain selected in that browser.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-06 — Tied strongest scores are shown as shared signals

- **Purpose:** Verify tie-aware result presentation.
- **Preconditions:** Submit answers that give at least two RIASEC dimensions the same highest total, or use a prepared backend fixture if available.
- **Steps/input:** Open the completed result.
- **Expected result:** Every tied highest dimension is shown as a strongest signal; directions from those dimensions are combined. No single tied dimension is presented as uniquely strongest.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-07 — Repeated submit is prevented

- **Purpose:** Ensure fast repeated clicks do not create duplicate submissions.
- **Preconditions:** Complete all questions; use a network throttle if available so submission remains pending briefly.
- **Steps/input:** Click Submit answers repeatedly while the first request is pending.
- **Expected result:** Submit enters a loading/disabled state; only one in-flight submission is sent; the completed result appears once.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-08 — Mobile layout

- **Purpose:** Check selection, questions, and results at a narrow viewport.
- **Preconditions:** Open the app in a browser with responsive device mode.
- **Steps/input:** Set viewport to 320 CSS pixels wide; inspect selection, answer controls, progress, and a completed result; navigate by keyboard where practical.
- **Expected result:** Cards stack vertically; no horizontal page scrolling is needed; answer controls remain usable; score labels and numeric values remain visible; focus indicators are visible.
- **Actual result:** Not executed.
- **Status:** Not run.

### MT-09 — API failure recovery

- **Purpose:** Verify failure messages and recoverable state.
- **Preconditions:** Open selection and use browser dev tools to block the assessment API or stop the backend.
- **Steps/input:** Attempt to load questions, then restore the API and retry/reload. In a second attempt, answer several questions and simulate a submit failure.
- **Expected result:** A visible, announced error appears; the start action remains unavailable while questions cannot load; after recovery, the student can continue. A failed submission leaves chosen answers available for retry.
- **Actual result:** Not executed.
- **Status:** Not run.

## Superseding implementation note — 2026-10-13

This report documents the earlier frontend-only checkpoint and is superseded by `US-01-career-tests-vertical-slice.md`. After this report was created, the user explicitly authorized backend ownership and required RIASEC, Interests, and Abilities all to be active, scored by the backend, and persisted. The earlier limitations stating that Interests and Abilities are planned no longer describe the current authorized scope.

