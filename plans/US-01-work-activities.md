# US-01 Work Activities Test Extension Plan

Status: Approved / Implemented  
Owner: User-directed CareerSim assessment extension; retain existing US-01 frontend owner boundary  
Date: 2026-10-13

## 1 Scope

- User story: user-authorized extension of the existing Career Assessments feature with a fourth independent assessment.
- Actor: Student.
- Source: user-provided Work Activities Test task; Job Bank Canada career quiz overview as purpose guidance: https://www.jobbank.gc.ca/career-planning/quizzes.
- Acceptance criteria: Exactly four active independently usable cards (RIASEC, Interests, Abilities, Work Activities); Work Activities loads original questions, accepts preference ratings, validates and scores on the backend, persists and reloads its result, supports retake and return navigation, remains isolated, and explains its exploratory limits in English.
- Explicit exclusions: occupational matching, external job-data lookup, validated psychometric claims, changes to existing assessment scoring, AI scoring, auth redesign, unrelated UI redesign.
- Owner boundary: extend the currently uncommitted, user-authorized three-test slice. Keep the addition confined to assessment UI/backend/schema/docs/tests; do not commit or discard existing changes.

## 2 Flow inventory

### Main success

1. Student opens `/assessment` and selects Work Activities Test.
2. Frontend fetches nine active, original prompts: three each for Information, People, and Things.
3. Backend creates a WORK_ACTIVITIES attempt; answers stay in the Work Activities local-state key.
4. Student rates each work-activity preference on a five-point scale and submits all answers.
5. Backend checks owner, attempt type, active question membership, completeness, uniqueness, and range; sums the three ratings per server-defined category and persists answers/scores/completion transactionally.
6. Frontend shows backend scores/tied leaders, category meanings, limitations, retake, and return navigation. A reload retrieves the persisted result.

### Alternate, exception, fallback

- Incomplete, duplicate, foreign question, out-of-range, wrong-owner/type, and incomplete question configuration are rejected without persisting partial completion.
- API errors leave the user's Work Activities answers available for retry; a completed result can be fetched again after reload.
- Retake creates a new attempt and its own local-state key; RIASEC, Interests, and Abilities state and scoring stay unchanged.

## 3 Responsibility design

| Responsibility | Expert/controller | Component | Reason |
|---|---|---|---|
| HTTP request/response | Controller | AssessmentController | Existing assessment API boundary |
| Type, owner, lifecycle, transaction | Application service | AssessmentServiceImpl | Cross-record attempt policy and persistence |
| Type-specific category/count validation | Domain policy | CareerQuizSubmissionPolicy | Keeps valid category configuration and response invariants server-side |
| Deterministic sum by question category | Domain policy | CareerQuizScoringPolicy | Questions carry authoritative category mapping |
| Quiz question/score persistence | DAO | Existing quiz repositories/tables | Reuses existing schema and workflow |
| Route, answer state, display | Frontend feature | AssessmentPage, CareerQuizPage, useCareerQuiz, CareerQuizResult | Reuses current assessment flow |

## 4 Change surface

- Frontend: catalog, assessment route switch, shared quiz page/result category copy and scale, tests.
- Backend: supported assessment type validation and category configuration; existing controller/service/DTO/API reused.
- Persistence: append-only V11 migration extends type constraints and seeds nine original Work Activities prompts in existing quiz tables; no new table.
- Documentation/evidence: plan, sequence trace, assessment/database docs, implementation/evidence report, AI assistance log.

## 5 API contract

| Method | Path | Request | Success | Error codes |
|---|---|---|---|---|
| GET | `/v1/assessments/quizzes/WORK_ACTIVITIES/questions` | none | Existing `CareerQuizQuestionDto[]` | Existing unsupported/not available error |
| POST | `/v1/assessments/quizzes/WORK_ACTIVITIES/attempts` | student context header per current project contract | Existing `AssessmentAttemptDto` | Existing API errors |
| POST | `/v1/assessments/quizzes/attempts/{id}/submit` | Existing `SubmitAnswersRequest` | Existing backend-calculated `CareerQuizResultDto` | Existing validation/ownership errors |
| GET | `/v1/assessments/quizzes/attempts/{id}/result` | student context header per current project contract | Existing persisted result DTO | Existing validation/ownership errors |

## 6 Security privacy and AI

- Keep attempt ownership and type validation in the assessment service.
- Keep question categories on the backend; public prompts omit answer/scoring keys.
- No new personal data, external provider calls, or AI scoring.
- Existing demo `X-Student-Id` contract remains a known non-production identity limitation.
- User-provided Work Activities task authorizes frontend integration and follows the previously approved backend-ownership decision for this assessment slice.

## 7 Tests and evidence

- Domain tests: three-category configuration, category cardinalities, complete/duplicate/foreign/range-invalid answers, deterministic separated category sums, ties.
- Frontend tests: four active cards, route, scale and result labels, invalid result handling, retake/selection navigation where existing test harness permits.
- Existing `backend verify`, `frontend npm run check`, Compose config, and Nginx/PostgreSQL smoke if environment supports.

## 8 Risks and decisions

| Question/risk | Impact | Decision |
|---|---|---|
| Job Bank concept is broad preference, unlike interests/abilities categories | Must avoid duplicate framing | Ask where users prefer to spend effort across Information, People, Things; use original activity prompts, not ability claims or topic interests |
| Existing persistence constrains four category quiz scores to 3–15 | Reuse schema only if each category retains 3 items | Use 9 questions total, 3 per category, preserving the existing score constraint and shared result contract |
| Result could sound predictive | Misleading career guidance | Present as self-reported preference signals, no validated-test or guaranteed-fit claims |

## 9 Scoring and sequence mapping

- Response options are preference ratings from 1 (Strongly prefer to avoid) to 5 (Strongly prefer).
- Three prompt responses are summed for each of INFORMATION, PEOPLE, THINGS (3–15). Highest values are tied preference signals; no occupational match is inferred.
- Job Bank describes Work activities as preferred work with data/information, people, and things, and as a short exploratory quiz. CareerSim uses that high-level purpose only; all question wording is original.
- Existing quiz question DTO/API, attempt lifecycle, service transaction, persistence entities, and result contract are reused. V11 is additive.
