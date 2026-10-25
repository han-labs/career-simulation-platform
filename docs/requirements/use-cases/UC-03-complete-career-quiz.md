# UC-03 — Complete career quiz

> **Status:** SELECTED / present in current report
> **Actor:** Student
> **Report-selected User Story mapping:** US-03
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 1

## Trigger
The actor wants to explore vocational interests using an available career quiz.

## Description
Supports optional, versioned career assessments, including RIASEC and other configured assessment types; not the quiz task within a simulation.

## Preconditions
The actor is authenticated and an eligible assessment version is published.

## Postconditions
A valid attempt and assessment-specific result are saved; the actor may still browse simulations independently.

## Main flow
- 1. The actor selects an available career quiz, reads its instructions, answers the required questions, and selects Submit.
- 2. The system validates the answers against the published assessment version, computes the configured result, and displays the stored summary.

## Alternative flows
- 1a. The actor postpones the assessment. The system exits without creating an assessment result and returns the actor to assessment selection; simulation access remains available.
- 2a. The actor has previous results from other assessment types or versions. The system retains those records separately and displays the newly completed attempt.

## Exception flows
- 2.1. If a required response is missing, the system highlights the unanswered question and displays “Please answer all required questions before submitting your career quiz.” The actor remains on the quiz page and resumes Main Flow Step 1.
- 2.2. If the published version is no longer available, the system displays “This career quiz is temporarily unavailable. Please select another quiz.” No unsupported result is created; the actor returns to assessment selection.
- 2.3. If saving fails, the system displays “Your career quiz result could not be saved. Please try again.” It does not falsely claim completion.

## Source alignment / implementation notes
- Preserve the source specification; verify exact API/UI behavior and automated tests against current `main` before marking as accepted.

## Verification tracking
- API/component: [VersionedAssessmentController.java](../../../backend/src/main/java/edu/hcmute/careersim/assessment/controller/VersionedAssessmentController.java); [VersionedAssessmentPage.jsx](../../../frontend/src/features/assessment/pages/VersionedAssessmentPage.jsx).
- Automated tests: [CorePackageAIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/system/migration/CorePackageAIntegrationTest.java), [CareerQuizPersistenceIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/assessment/service/CareerQuizPersistenceIntegrationTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

Scoped follow-up (2026-10-23): [API contract](../../api/assessment-uc03.md),
[five-AC/flow plan](../../../.agent/plans/US-03-career-quiz-alignment.md) and
[executed verification/limitations](../../../.agent/reports/us-03-career-quiz-alignment.vi.md).
Source flow wording above is unchanged. Published-content review and human sign-off
remain separate from synthetic tests.

Subsequent authorized local-demo execution: four published quizzes, real browser
completion/refresh, real HTTP negative/ownership checks and PostgreSQL restart
persistence are recorded in [local acceptance evidence](../../../.agent/reports/us-03-local-acceptance-2026-10-23.vi.md).
[Setup/UAT steps](../../testing/us03-local-demo.vi.md) use an opt-in local fixture;
no production auth bypass or automatic production publication is introduced.

UI/scroll follow-up (2026-10-23): [four-quiz UI, EN/VI, responsive matrix and
SCROLL-01–10 evidence](../../../.agent/reports/us-03-ui-scroll.vi.md).
Business flow and scoring/persistence contracts remain unchanged; human sign-off
is still pending.

Independent versioned assessment attempts/results are implemented; this is not a simulation quiz task. Published content/human UAT are separate from synthetic regression.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
