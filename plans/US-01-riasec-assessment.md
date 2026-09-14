# US-01 RIASEC Assessment Feature Plan

Status: Approved / Implemented  
Owner: AI Agent
Date: 2026-09-14

## 1 Scope

- User story or foundation item: US-01 Complete a RIASEC assessment (Backend only)
- Actor: Student
- Source proposal/SRS section: docs/requirements/user-stories.md
- Acceptance criteria:
  - Start, resume, submit assessment.
  - Answers belong to six RIASEC dimensions, scale 1-5.
  - Scoring is deterministic.
- Explicit exclusions: AI interpretation, frontend implementation (assigned to another member).
- Related owner boundaries: Coordinate API contracts with Võ Nguyễn Ngọc Bích.

## 2 Flow inventory

### Main success flow

1. Student requests questions.
2. Student starts attempt.
3. Student submits answers.
4. System calculates deterministic scores, saves attempt, and returns results.

### Alternate flows

1. Student requests questions without an active attempt.
2. Student submits answers to an already COMPLETED attempt (returns existing result).

### Exception and fallback flows

1. Student submits invalid scores (< 1 or > 5) -> Validation Error.
2. Student submits missing dimensions -> Validation Error.
3. Student not found -> Not Found Error (mocked via header).

## 3 Responsibility design

| Responsibility | Expert/creator/controller | Proposed class/component | Reason |
|---|---|---|---|
| Receive HTTP, extract user ID | Controller | AssessmentController | Keeps HTTP concerns separated |
| Convert DTOs to Entities | Mapper | AssessmentMapper | Isolates representation |
| Implement scoring/transactions | ServiceImpl | AssessmentServiceImpl | Centralizes business logic |
| Query database | DAO | AssessmentRepositories | Persistence isolation |

## 4 Change surface

- Backend controller/request/response DTOs: AssessmentController, various DTOs.
- Service facade methods: AssessmentService interface.
- Service/domain implementation: AssessmentServiceImpl.
- DAO queries/entities: RiasecQuestion, AssessmentAttempt, AssessmentAnswer, AssessmentScore.
- Flyway migrations: V3__seed_demo_assessment.sql.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| GET | `/v1/assessments/questions` | None | `List<RiasecQuestionDto>` | 500 |
| POST | `/v1/assessments/attempts` | None | `AssessmentAttemptDto` | 500 |
| POST | `/v1/assessments/attempts/{id}/submit` | `SubmitAnswersRequest` | `AssessmentResultDto` | 400, 404 |
| GET | `/v1/assessments/attempts/{id}/result` | None | `AssessmentResultDto` | 404 |

## 6 Security privacy and AI

- Authentication/role: Simulated via `X-Student-Id` header (default 1L).
- Ownership/visibility: Students only see their own attempts.
- Sensitive data: Scores are private.
- AI context fields: None.
- Output schema/validation: Standard `ApiResponse`.
- Timeout/fallback: None.
- Logging/redaction: N/A.

## 7 Tests and evidence

- Commands to run: `mvnw verify`

## 8 Risks and decisions

| Question/risk | Impact | Recommendation | Approval |
|---|---|---|---|
| Identity is not implemented | Cannot track user attempts | Mock via header / ID 1L | Approved |
