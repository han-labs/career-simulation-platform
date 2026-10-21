# UC-06 — Consult career agent

> **Status:** SELECTED / present in current report
> **Actor:** Student
> **Report-selected User Story mapping:** US-07
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 3

## Trigger
The actor opens an assigned career agent to request guidance or continue a previous conversation.

## Description
The system provides personalized career guidance using authorized assessment information, simulation evidence, knowledge sources, and controlled AI tools. It also supports conversation history, structured insight reports, and exploration plans.

## Preconditions
The actor is authenticated and has access to a published, explicitly assigned career agent. External AI processing requires effective consent before protected information is transmitted.

## Postconditions
The response and permitted conversation records are saved within the actor's scope. Requested insight reports are stored only after successful validation. Exploration plans are saved only after explicit confirmation. No unauthorized data access or write occurs.

## Main flow
- 1. The actor opens an assigned career agent, enters a career-related question, and submits the request. An assessment result is not required to start a conversation.
- 2. The system verifies agent assignment, permissions, external AI consent, and available conversation context.
- 3. The system processes the request through the backend. The model may select authorized knowledge retrieval and application-data tools, including assessment results, published simulations, and the actor's simulation progress. The system validates tool arguments, permissions, and returned evidence before continuing generation.
- 4. The system validates the response, saves the permitted conversation records, and displays the answer with source references when applicable.
- 5. The actor reviews the response and may submit another question, returning to Main Flow Step 1.

## Alternative flows
- 1a. Resume conversation: The actor selects a previous conversation. The system loads only authorized active messages and bounded context, then returns to Main Flow Step 1.
- 1b. Delete conversation: The actor requests deletion and confirms the action. The system removes the conversation from active history, invalidates derived context summaries, and returns to the conversation list.
- 3a. Insufficient assessment evidence: The actor has not completed an assessment. The system uses available assessment descriptions, published simulation information, or authorized knowledge without inventing personal assessment results, then continues to Main Flow Step 4.
- 3b. Generate insight report: The actor requests a career insight report. The system generates structured JSON using authorized evidence, validates the required schema, saves a valid report within the actor's scope, and displays it before returning to Main Flow Step 5.
- 4a. Confirm exploration plan: The system presents a proposed exploration plan as an unsaved draft. The actor may review and edit the proposal. Only explicit confirmation causes the system to validate and save the plan. Cancellation preserves any previously saved plan.
- 4b. Insufficient knowledge: No relevant authorized evidence supports the requested answer. The system explains the limitation and invites the actor to clarify the question, returning to Main Flow Step 1.

## Exception flows
- 2.1. Agent unavailable: If the selected agent is unassigned, unpublished, or revoked, the system displays Proposed message: "This career agent is not available to your account." No restricted content is returned. The actor returns to agent selection.
- 2.2. Consent missing: If external processing consent is absent, the system displays Proposed message: "AI assistance requires your consent. Please review your privacy settings." No protected context is sent. The actor remains in the conversation interface and may retry after providing consent.
- 3.1. Unauthorized tool or source: If a requested tool or knowledge source is not permitted, the system rejects access and excludes its data from the response. It may provide guidance using other authorized evidence or explain the limitation.
- 3.2. Provider failure: If the provider times out or returns an invalid response, the system displays Proposed message: "AI assistance is temporarily unavailable. Please try again later." The submitted question remains available for retry, and non-AI functions remain accessible.
- 3.3. Invalid insight report: If structured output fails schema validation, the system does not save an invalid report. It displays Proposed message: "The insight report could not be generated. Please try again." The actor returns to the conversation.
- 4.1. Plan save failure: If validation or persistence fails after confirmation, the system displays Proposed message: "The exploration plan could not be saved." The previously saved plan remains unchanged, and the actor may review the draft and retry.

## Source alignment / implementation notes
- **Review point — plan editing:** confirm behavior for editing draft before confirmation against backend source hash/revision validation. Do not assume arbitrary edits are accepted.

## Verification tracking
- API/component: [StudentAgentController.java](../../../backend/src/main/java/edu/hcmute/careersim/guidance/agent/controller/StudentAgentController.java); [StudentAgentPage.jsx](../../../frontend/src/features/agents/StudentAgentPage.jsx).
- Automated tests: [StudentAgentIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/StudentAgentIntegrationTest.java), [NativeStudentStreamingTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/NativeStudentStreamingTest.java), [AgentStreamCredentialTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentStreamCredentialTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

OPEN alignment D04: exact source/hash confirmation is implemented; arbitrary suggestion edits are not. Current owner/assignment/consent/source and replay checks stay intact. AC-A09/full AC-A17 need actual live/human evidence.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
