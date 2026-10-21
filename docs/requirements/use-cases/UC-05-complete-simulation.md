# UC-05 — Complete simulation

> **Status:** SELECTED / present in current report
> **Actor:** Student
> **Report-selected User Story mapping:** US-05, US-06
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 2

## Trigger
The Actor selects Start or Resume on an eligible simulation.

## Description
This use case allows the Actor to complete ordered simulation tasks, review model answers, and view completion results so that the Actor can experience representative workplace activities and reflect on practiced skills without implying professional competence.

## Preconditions
The Actor is authenticated. The selected simulation is published, valid, and accessible to the Actor.

## Postconditions
Required task submissions and self-review confirmations are recorded, and the simulation is marked as completed. The Actor can view the completed tasks, applicable quiz scores, practiced skills, and eligible completion evidence.

## Main flow
- 1. Actor selects Start or Resume for an eligible published simulation.
- 2. System displays the simulation information, saved progress, and the current available task with instructions, resources, and submission requirements.
- Actor completes the current task and submits the required response.
- System validates and records the final submission, marks the task as submitted, and reveals its model answer.
- Actor compares the submitted work with the model answer and confirms the self-review.
- System records the self-review confirmation and makes the next ordered task available.
- Actor repeats Steps 3–5 for each remaining required task, with the System performing Step 6 whenever another task remains.
- System marks the simulation as completed after all required tasks and self-reviews are completed.
- System displays the simulation results, including completed tasks, recorded submission statuses, quiz scores where objective scoring applies, and practiced skills linked to completed tasks rather than score thresholds.
- System provides eligible completion evidence or a resume snippet describing the simulated experience without claiming professional competence.
- Actor reviews the results and available completion evidence.

## Alternative flows
- 1a. The actor has an unfinished attempt; the system offers “Resume” or “Start a New Attempt” according to the scenario policy; the actor selects an option and continues at Main Flow Step 2.
- 2a. The actor chooses to review available resources before continuing a task; the system keeps the attempt open and returns to Main Flow Step 2.
- 3a. The actor saves progress instead of submitting; the system preserves the current attempt state and allows the actor to continue later.
- 4a. Required items are incomplete; the system identifies each incomplete task or deliverable, displays the corresponding message, and returns to Main Flow Step 3.
- 5a. A task requires asynchronous evaluation; the system marks that task as Pending, evaluates the tasks that can be completed immediately, and keeps the attempt in an appropriate evaluation state.
- 5b. Artifact evaluation is unavailable; the system stores the available objective evidence, marks the affected rubric component as “Unavailable”, and continues with the remaining evaluation.

## Exception flows
- 1.1. The selected simulation is unavailable, invalid, or inaccessible; the system displays “This simulation cannot be started because the scenario is unavailable or you do not have permission to access it.” and returns to the simulation catalogue or previous valid entry point.
- 2.1. Required simulation resources cannot be loaded; the system displays “Required simulation resources could not be loaded. Please try again later.” and returns to the simulation details page.
- 4.1. Submission fails because of a network or persistence error; the system displays “Your submission could not be completed. Please check your connection and try again.”, keeps the attempt open, and returns to Main Flow Step 3.
- 5.1. The evaluation configuration is missing or invalid; the system preserves the submitted attempt, marks the evaluation status as Evaluation Error, and displays “This submission could not be fully evaluated because the scenario evaluation configuration is incomplete. Please contact the administrator.”
- 5.2. Evaluation cannot start after the submission has been stored; the system displays “Submission saved. Evaluation is temporarily pending.”, preserves the submission, and makes its status available through the dashboard or simulation history.

## Source alignment / implementation notes
- **Review point — completion semantics:** current UC-05 main/post conditions may imply final self-review is required before *overall completion*. Frozen DR2/BR-06 allows completion after all required final submissions; self-review gates **next-task progression**, with final review tracked separately. Resolve wording with Chat 3; do not silently change the approved rule.
- **Review point — evaluation:** UC-05 alternative flow mentions asynchronous rubric/artifact evaluation. Check whether this is approved/currently implemented; do not invent automatic grading of open/practical tasks.

## Verification tracking
- API/component: [EnrollmentController.java](../../../backend/src/main/java/edu/hcmute/careersim/simulation/controller/EnrollmentController.java); [CoreSimulationPage.jsx](../../../frontend/src/features/simulations/pages/CoreSimulationPage.jsx).
- Automated tests: [CorePackageAIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/system/migration/CorePackageAIntegrationTest.java), [TaskProgressionPolicyTest.java](../../../backend/src/test/java/edu/hcmute/careersim/simulation/domain/TaskProgressionPolicyTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

OPEN alignment D01/D02: last required FINAL may create completion before final self-review; explicit review gates the next task. No general same-version new attempt or asynchronous practical grader is established by the existing flow. Keep source prose, approved version policy and quiz-zero semantics unchanged until reconciliation.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
