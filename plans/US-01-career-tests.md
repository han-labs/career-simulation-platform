# US-01 Career Tests Vertical Slice Plan

> Scope note: this plan records the earlier three-assessment baseline. Work Activities Test is added by the approved extension in `plans/US-01-work-activities.md`.

Status: Implemented (automated verification passed; browser visual check blocked)  
Owner: Backend and frontend implementation (user-authorized scope expansion)  
Date: 2026-10-13

## 1 Scope

- User story: US-01 assessment discovery and career self-exploration assessments.
- Actor: Student.
- Sources: `docs/requirements/user-stories.md` US-01; user's Career Tests expansion request and explicit backend authorization; `.agent/TASKS.md`; Job Bank career quiz category descriptions.
- Acceptance: exactly three active cards; independent RIASEC, Interests, and Abilities flows; required answer validation; backend deterministic scoring; persisted answers/results; owned result retrieval; retake and return to selection; English UI; no claims of validation or official Job Bank reproduction.
- Exclusions: user accounts/authentication redesign, changing RIASEC scoring, Syn score integration for the new quizzes, copying Job Bank question text, career prediction, psychometric/ability certification.
- Ownership boundary: backend work is explicitly authorized in the user's pasted approval. Existing uncommitted frontend work is pre-existing and must be preserved.

## 2 Scoring and result model

- RIASEC remains seven 1–5 responses per each of six backend-owned dimensions; its current API and scoring are preserved.
- Interests uses 12 original work-preference statements, three for each of Analytical, Creative, Collaborative, and Organizing. Required 1–5 interest ratings are summed per category (3–15); ties are reported as shared signals.
- Abilities uses 12 original work-task confidence statements, three for each of Problem solving, Communication, Organization, and Technical learning. Required 1–5 self-ratings are summed per category (3–15); ties are reported as shared strengths to explore.
- These are exploratory self-reflection summaries, not validated aptitude or psychological measurements. Backend question/category mapping is authoritative.

## 3 Flow inventory

### Main success

1. Student opens `/assessment`, sees exactly three active choices.
2. Student selects one and loads its backend-served questions.
3. Backend starts an attempt associated with the selected assessment type and student.
4. Student answers every required question and submits question IDs and 1–5 responses.
5. Backend validates attempt ownership, type, question membership, completeness, and scale; persists answers, category scores, and completion transactionally.
6. Frontend displays backend result with category scores, interpretation, limitations, retake, and selection navigation.

### Alternate and exception

- Each test has separate browser-local draft state and attempt ID; changing routes does not mix answers.
- Refresh resumes the selected assessment; completed attempts are retrieved by attempt ID; retaking starts a new attempt.
- Missing, duplicate, foreign, invalid-scale, wrong-type, incomplete, or wrong-owner requests fail safely; objective previous results remain unchanged.
- API unavailable/error states retain draft answers for retry.

## 4 Responsibility design

| Responsibility | Owner | Component |
|---|---|---|
| HTTP, validation, DTO/status | Controller | AssessmentController |
| Attempt ownership, lifecycle, answer validation, transaction | Application service | AssessmentServiceImpl |
| Quiz question and persisted result reads | DAO | CareerQuizQuestionRepository, CareerQuizAnswerRepository, CareerQuizScoreRepository |
| Category mapping and score construction | Domain policy | CareerQuizScoringPolicy |
| DTO conversion | Mapper | AssessmentMapper / quiz DTO records |
| UI routing and presentation | Frontend | AssessmentPage, AssessmentIntro, AssessmentForm, quiz result component |

## 5 Change surface and API

- Existing RIASEC routes remain unchanged.
- `GET /v1/assessments/quizzes/{type}/questions`
- `POST /v1/assessments/quizzes/{type}/attempts`
- `POST /v1/assessments/quizzes/attempts/{id}/submit`
- `GET /v1/assessments/quizzes/attempts/{id}/result`
- New append-only Flyway migration adds quiz type discriminator and original quiz question/answer/score tables and seeds approved original questions.
- No AI or new external data processing.

## 6 Security and privacy

- Reuse the current demo student header contract; service queries require both attempt ID and student ID.
- Do not expose scoring mappings in question DTOs; answer/question membership and score mapping remain backend-owned.
- No new sensitive fields or AI-provider context. Existing assessment authorization limitation (temporary demo header) remains and is not expanded into an identity redesign.

## 7 Verification

- Backend domain/service/controller tests cover deterministic results, invalid/incomplete submissions, ownership, attempt type, result retrieval, and idempotent completed submission.
- Frontend component/route tests cover exactly three active cards, type routing, per-test questions/results, validation and navigation.
- Run backend `mvnw verify`, frontend `npm run check`, Compose config, and applicable local API/UI smoke if the environment supports it.

## 8 Risks and decisions

| Risk | Decision |
|---|---|
| Existing user changes overlap frontend files | Preserve and extend current changes; do not restore or commit unrelated changes. |
| Results could be mistaken for validated measures | Label as self-reflection; describe explicit 3–15 category sums and limitations. |
| Existing RIASEC rows have no assessment discriminator | Existing routes and score logic remain unchanged; new quiz tables are separate and linked through quiz attempt type. |

