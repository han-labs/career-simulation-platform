# US-01 Career Tests Sequence-to-Code Trace

Source: user-authorized four-assessment assignment; `docs/requirements/user-stories.md` US-01.  
Sequence artifact: this implementation-level trace.  
Implementation commit: current working tree, uncommitted.

| Step/message | Actor/lifeline | Endpoint or frontend action | Method owner | Layer/module | Data/provider interaction | Status | Notes/deviation |
|---|---|---|---|---|---|---|---|
| Open Career Tests | Student | `/assessment` | AssessmentPage / AssessmentIntro | React assessment feature | Assessment catalog and existing RIASEC local state | MATCH | Exactly four active cards |
| Select RIASEC | Student | `/assessment/riasec` | RiasecAssessmentFlow | React assessment feature | Existing `GET /assessments/questions`; existing attempt/submit/result routes | ADAPTED | Existing RIASEC service and persistence preserved |
| Select quiz type | Student | `/assessment/interests` or `/assessment/abilities` | CareerQuizPage / useCareerQuiz | React assessment feature | `GET /assessments/quizzes/{type}/questions` | MATCH | Quiz-specific local storage key |
| Select Work Activities | Student | `/assessment/work-activities` | CareerQuizPage / useCareerQuiz | React assessment feature | `GET /assessments/quizzes/WORK_ACTIVITIES/questions` | MATCH | Own local storage key; nine original preference questions |
| Start quiz | Student -> controller | `POST /assessments/quizzes/{type}/attempts` | AssessmentService.startQuizAttempt | Controller -> facade -> service | Validates type and active per-type question/category set; persists typed student attempt | MATCH | Uses current demo student header convention |
| Display questions | Service -> browser | Quiz question DTO list | AssessmentService.getQuizQuestions | Service -> question DAO -> mapper | Prompt/order only; category mapping remains private | MATCH | Question content is original CareerSim text |
| Answer and validate client-side | Student | Shared AssessmentForm | AssessmentForm | React view | Stores ratings under quiz-specific key | MATCH | Blocks incomplete submission; not score authority |
| Submit quiz | Browser -> controller | `POST /assessments/quizzes/attempts/{id}/submit` | AssessmentService.submitQuizAnswers | Controller -> facade -> service/domain | Ownership/type, category configuration, unique IDs, completeness, 1–5 validation | MATCH | Invalid payload fails before persistence |
| Save answers and scores | Service -> database | JPA repositories | AssessmentServiceImpl and scoring policy | Assessment -> DAO -> PostgreSQL | Saves answers, category sums, completion in one transaction | MATCH | Three ratings per category (3–15), all tests |
| Show result | Browser -> service | Submit response or `GET /assessments/quizzes/attempts/{id}/result` after reload | CareerQuizResult / AssessmentService | React -> API -> service -> score DAO | Backend scores and tied leaders; owner/type checked | MATCH | UI explains scale and non-validated nature |
| Retake or return | Student | New quiz attempt or `/assessment` link | CareerQuizPage / AssessmentIntro | React assessment feature | New attempt; per-test state remains isolated | MATCH | Does not require taking other assessments |
| Wrong owner/type, bad ID/scale, duplicate/incomplete | Browser -> service | Quiz submit/result routes | CareerQuizSubmissionPolicy / AssessmentServiceImpl | Service/domain | No answers or results returned across attempts/types | MATCH | API uses the existing temporary demo identity boundary |
| API failure | Browser | Alert and retry/retake navigation | useCareerQuiz | React feature state | Existing local draft remains available | ADAPTED | Manual browser failure path not executed |

Verification: `backend/.\mvnw.cmd verify` passed (71 tests, 0 failures, 0 errors, 6 skipped); `frontend/npm run check` passed (30 tests); `docker compose config --quiet` passed. Browser E2E was unavailable because local Vite navigation timed out in the in-app browser.

Work Activities adds three independent categories (Information, People, Things) with
three questions each and preference ratings. Backend type/configuration policy and
frontend result presentation use the same quiz flow described above. V11 extends the
assessment-type checks and seeds its questions; existing Interests and Abilities
question counts and score rules are unchanged.
