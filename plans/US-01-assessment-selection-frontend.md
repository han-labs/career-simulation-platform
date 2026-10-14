# US-01 Career Assessment Selection Frontend Plan

Status: Implemented (manual student checklist pending)
Owner: Frontend implementation (AI-assisted; human approval recorded in chat)
Date: 2026-10-13

## 1 Scope

- User story or foundation item: US-01 frontend assessment discovery and RIASEC flow
- Actor: Student
- Source requirements: `docs/requirements/user-stories.md` US-01; pasted Career Assessment and Quiz Selection System assignment; `.agent/TASKS.md` US-01 frontend scope.
- Acceptance criteria: present a general English assessment heading and supporting description; make the existing RIASEC assessment discoverable as the only available scored assessment; clearly mark additional researched categories as planned and prevent starting them; preserve the existing API/scoring/result integration; show accessible progress, answer validation, and exploratory result interpretation.
- Explicit exclusions: no backend, database, API contract, or scoring changes; no newly active assessment; no copied Job Bank questions; no claims that Job Bank assessments are validated for CareerSim.
- Related owner boundaries: US-01 assessment/landing frontend assigned to Võ Nguyễn Ngọc Bích. This work stays within the approved supplied assignment's frontend boundary; coordinate with the named owner before any follow-up scope expansion.

## 2 Scoring and result model

- Question structure: 42 backend-provided activities, with seven assigned to each RIASEC dimension (R, I, A, S, E, C).
- Answer values: required integer selection from 1 to 5, from strongly dislike to strongly like.
- Mapping: each submitted question ID maps to its existing backend-owned RIASEC dimension; the frontend does not infer or change this mapping.
- Aggregation: backend sums the seven answer values per dimension. No normalization or rounding is applied; each dimension ranges from 7 through 35.
- Ties: every dimension equal to the maximum returned score is presented as a shared strongest signal. Directions from tied dimensions are combined.
- Interpretation: dimension descriptions and IT exploration directions remain descriptive possibilities, not predictions, diagnoses, or suitability judgments.
- Display conditions: show scores only when the completed API result contains exactly one valid score for each of the six dimensions. Planned assessments have no question, scoring, or result model in this scope.

## 3 Flow inventory

### Main success flow

1. Student opens `/assessment` and sees the available RIASEC choice plus clearly disabled planned choices.
2. Student starts or resumes the existing RIASEC assessment.
3. Student answers all backend-provided questions on the existing 1–5 scale; progress and selection state remain visible.
4. Frontend submits the original answer DTO to the existing backend endpoint.
5. Backend validates, scores, persists, and returns the six dimensions and top signals.
6. Frontend presents scores, tied strongest dimensions when applicable, exploratory directions, and limitations.

### Alternate flows

1. A student with browser-local in-progress state can resume the same attempt and answers.
2. A completed student sees the existing result and can restart through the existing start endpoint.
3. Planned cards communicate their topic and planned status but provide no active start action.

### Exception and fallback flows

1. Question-fetch/start/submit API failures are shown; existing recoverable answers remain in local storage.
2. Incomplete submissions are blocked with an error summary and unanswered-item indication.
3. Missing/invalid result data renders a safe empty-result state rather than dereferencing malformed data.
4. Repeated submit clicks are blocked while the request is in progress.

## 4 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Assessment discovery and availability state | Feature view | AssessmentIntro | Keeps available and planned options clear without changing APIs |
| Question and attempt state/API calls | Feature hook | useAssessment | Preserves current frontend API adapter/state ownership |
| Required answer/progress feedback | Feature form/components | AssessmentForm, AssessmentProgressBar, AssessmentQuestionCard | Existing form owns UI validation; API remains scoring authority |
| RIASEC interpretation display | Feature view | AssessmentResult | Presents returned scores without calculating or changing them |

## 5 Change surface

- Frontend routes/pages/components/hooks/API adapters: assessment page, intro/selection, result, form/progress/question presentation, shell labels, landing copy, CSS, and Vitest tests.
- Backend controller/request/response DTOs: none.
- Service facade/domain implementation: none.
- DAO/entities/migrations: none.
- Configuration/environment: none.
- Documentation/diagram/report: this plan, implementation/evidence report, and AI assistance log.

## 6 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| GET | `/api/v1/assessments/questions` | None | Existing question DTO list | Existing HTTP/API errors |
| POST | `/api/v1/assessments/attempts` | Existing student header | Existing attempt DTO | Existing HTTP/API errors |
| POST | `/api/v1/assessments/attempts/{id}/submit` | Existing answer list, values 1–5 | Existing six-score result DTO | Validation/not found/server errors |
| GET | `/api/v1/assessments/attempts/{id}/result` | Existing student header | Existing result DTO | Existing not-found/bad-request errors |

## 7 Security privacy and AI

- Authentication/role: unchanged existing temporary local demo header contract.
- Ownership/visibility: backend remains responsible for attempt ownership.
- Sensitive data: no new data fields or external transmission.
- AI context fields: none; AI does not score this assessment.
- Output schema/validation: unchanged backend DTO contract.
- Timeout/fallback: existing API error behavior; no client-computed scoring fallback.
- Logging/redaction: no answer payload or credentials added to logs.

## 8 Tests and evidence

- Domain/policy tests: no backend domain changes.
- Service/controller/repository tests: no backend changes.
- Frontend tests: Vitest SSR/component markup tests for selection availability, accessible disabled states, result score ordering/ties/empty state, and non-diagnostic framing.
- End-to-end/manual evidence: project has no configured browser E2E runner; provide a student-executable manual checklist after implementation.
- Commands to run: `cd frontend; npm run check`.

## 9 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Additional tests lack backend questions/scoring/API contracts | Frontend-only scoring would violate project authority and break Syn's RIASEC evidence contract | Mark researched alternatives as planned/disabled until a separate approved vertical slice exists | Approved by user 2026-10-13 |
| Existing project assigns US-01 frontend to a named owner | Uncoordinated edits may overlap | Keep this work within US-01 frontend scope and disclose changed files for coordination | Approved plan accepted by user |
| Existing localStorage saves draft answers while backend only persists submitted answers | Resume does not synchronize across devices | Preserve and accurately describe browser-local resume | Accepted constraint |

## Superseding scope decision — 2026-10-13

The original frontend-only scope and its planned/disabled quiz choices were superseded by the user's later explicit authorization to assign backend ownership and require all three tests to be active. See [`US-01-career-tests.md`](US-01-career-tests.md) for the approved vertical-slice plan, backend APIs, scoring models, persistence, and verification scope. Do not use the original disabled-card acceptance criteria for final review.

