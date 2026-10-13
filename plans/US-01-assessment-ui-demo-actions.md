# US-01 Assessment UI Consistency and Demo Actions Plan

Status: Implemented
Owner: User-directed assessment UI refinement
Date: 2026-10-14

## 1 Scope

- User story: refine Career Assessments selection layout and add quick answer presets to the three career quizzes.
- Actor: Student / presenter.
- Source: user-provided task `Refine Career Assessments UI Consistency and Add Quick Demo Actions`.
- Acceptance criteria: exactly four assessment cards remain equally presented; selection layout uses the shared page width and responsive grid conventions; all four assessment forms expose assessment-appropriate demo answer presets; presets populate answers without auto-submitting or changing backend scoring; normal assessment routes and submission remain unchanged.
- Explicit exclusions: backend/API/schema/scoring changes, separate demo routes/results, unrelated page redesign.
- Related owner boundaries: keep changes within the existing assessment frontend feature and its styles/tests.

## 2 Flow inventory

### Main success flow

1. Student opens Career Assessments and sees four equal cards in the shared responsive layout.
2. Student starts one assessment and enters its existing backend-backed attempt flow.
3. Student chooses a named demo preset; the existing answer state is populated for that assessment.
4. Student reviews/changes answers and submits through the existing backend scoring endpoint.

### Alternate flows

1. Student skips the demo action and answers normally.
2. Student selects a different preset or changes individual answers before submission.

### Exception and fallback flows

1. Existing loading, question-unavailable, validation, and submission error states remain unchanged.
2. Demo presets do not submit answers and do not claim a result before backend response.

## 3 Responsibility design

| Responsibility | Expert/controller | Proposed component | Reason |
|---|---|---|---|
| Assessment card layout | Assessment feature | `AssessmentIntro` and assessment CSS | Existing card catalog remains the source of assessment metadata |
| Demo answer generation | Frontend presentation helper | `AssessmentDemoControls` / `AssessmentForm` | Reuses RIASEC preset interaction; backend remains score authority |
| Per-quiz local answer persistence | Existing feature state hook | `useAssessment`, `useCareerQuiz` | Maintains separate existing storage keys and API flows |

## 4 Change surface

- Frontend: `AssessmentIntro`, `AssessmentForm`, `AssessmentDemoControls`, `CareerQuizPage`, `useCareerQuiz`, relevant tests, assessment CSS.
- Backend/API/database: no change.
- Documentation: this plan.

## 5 API contract

No API changes. Demo-filled answers use the existing attempt submit endpoint only after the user submits the form.

## 6 Security privacy and AI

- Demo answers are local form values in the active assessment state.
- No fabricated results, additional API calls, or distinct demo records are introduced.
- Backend validation, ownership, scoring, persistence, and result response remain authoritative.

## 7 Tests and evidence

- Frontend component assertions for four card actions, card grid structure, quiz demo controls, and demo answer mapping.
- Run `npm run check` from `frontend`.
- Inspect final diff and responsive CSS breakpoints.

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Demo presets are synthetic examples | Must not imply measured result | Label actions as demo profiles; require normal backend submission for results | User task authorizes demo actions |
| Other quiz categories differ from RIASEC | Presets must not cross-populate unrelated content | Supply quiz-specific preset values by question order and retain each quiz's isolated state | Resolved in implementation |
