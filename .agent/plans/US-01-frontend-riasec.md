# US 01 Feature Plan

Status: Draft / Awaiting approval
Owner: Võ Nguyễn Ngọc Bích (Frontend)
Date: 2026-09-10

## 1 Scope

- User story or foundation item: US-01 Complete a RIASEC assessment (Frontend Only)
- Actor: Student
- Source proposal/SRS section: docs/requirements/user-stories.md
- Acceptance criteria:
  - Start, resume, submit assessment.
  - Answers use approved scale, mapped to 6 RIASEC dimensions.
  - Deterministic scoring.
  - Result shows 6 scores and strongest.
  - Career directions are exploration options.
- Explicit exclusions: Backend integration (currently mocking state on frontend), AI integration (belongs to US-03).
- Related owner boundaries: US-03 (Guidance) will later consume these results.

## 2 Flow inventory

### Main success flow

1. Student navigates to `/assessment`.
2. Student starts assessment.
3. Student answers a simplified mock set of RIASEC questions.
4. Student submits.
5. Frontend calculates deterministic score (R, I, A, S, E, C).
6. Frontend displays result screen with scores, highlighting strongest dimension.

### Alternate flows

1. Student reloads page: Progress is restored from localStorage (autosave).

### Exception and fallback flows

1. Student tries to submit without answering all: Form validation error displayed near the submit button.

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Question display | React component | `AssessmentForm.jsx` | Encapsulates form logic |
| Scoring logic | Frontend service | `riasecScorer.js` | Deterministic logic decoupled from UI |
| State persistence | Custom Hook | `useAssessment.js` | Abstracts localStorage saving |

## 4 Change surface

- Frontend routes/pages/components/hooks/API adapters:
  - `frontend/src/features/assessment/pages/AssessmentPage.jsx` (replaces placeholder)
  - `frontend/src/features/assessment/components/AssessmentForm.jsx`
  - `frontend/src/features/assessment/components/AssessmentResult.jsx`
  - `frontend/src/features/assessment/utils/riasecScorer.js`
  - `frontend/src/app/App.jsx` (update route)
- Backend controller/request/response DTOs: N/A
- Service facade methods: N/A
- Service/domain/evaluator/provider implementation: N/A
- DAO queries/entities: N/A
- Flyway migrations: N/A
- Configuration/environment: N/A
- Documentation/diagram/report: `.agent/plans/US-01-frontend-riasec.md`

## 5 API contract

(Mocked locally until backend is implemented)

## 6 Security privacy and AI

- Authentication/role: Local simulation for now.
- Ownership/visibility: Only the student sees their local result.
- Sensitive data: N/A
- AI context fields: N/A
- Output schema/validation: N/A
- Timeout/fallback: N/A
- Logging/redaction: N/A

## 7 Tests and evidence

- Frontend tests: Will manually verify component rendering and scoring logic.
- Commands to run: `npm run dev`, `npm run check`

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Backend is not ready | Cannot integrate | Build frontend with local state and mock data first, structured cleanly so it can be swapped with API calls later. | Yes |
