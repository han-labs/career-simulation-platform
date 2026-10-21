# UC-08 — Create AI agent

> **Status:** SELECTED / present in current report
> **Actor:** Administrator
> **Report-selected User Story mapping:** US-09
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 3

## Trigger
The actor opens agent management to create, configure, test, publish, or assign a student career agent.

## Description
The system supports configurable student agents with versioned model settings, instructions, policies, authorized knowledge bases, and tools. The actor can test saved configurations before publishing and explicitly assigning agents to eligible students.

## Preconditions
The actor is authenticated with Administrator privileges. Knowledge bases and tools selected for configuration must be available and permitted within the actor's administrative scope.

## Postconditions
A valid configuration is stored as an immutable version. Publication and student assignment take effect only after their corresponding explicit actions. Previous saved or published versions remain unchanged by unsuccessful updates.

## Main flow
- 1. The actor opens agent management and creates or selects an agent. The system displays the configuration editor and current saved version, if available.
- 2. The actor configures the agent's model, instructions, policies, conversation settings, authorized knowledge bases, and permitted tools.
- 3. The actor selects Save. The system validates the configuration and stores a new immutable version without automatically publishing it, assigning students, or invoking the AI provider.
- 4. The actor opens Preview and tests the saved configuration. The system executes the preview within the actor's authorized scope, applies the configured tools and knowledge permissions, and displays the response and safe execution details. Preview does not create a student conversation.
- 5. The actor reviews the configuration and explicitly requests publication. The system verifies publication readiness and publishes the selected valid version.
- 6. The actor selects eligible students and confirms assignment. The system validates the assignments and grants access only to the approved students.
- 7. The system displays the published version and assignment status.

## Alternative flows
- 1a. Edit an existing agent: The actor selects an existing agent. The system loads its saved configuration and version information, then continues to Main Flow Step 2.
- 2a. Discard changes: The actor cancels unsaved edits. The system discards the draft changes and restores the last saved configuration.
- 3a. Save without publishing: The actor saves a valid configuration and leaves the editor. The system retains the immutable version as unpublished and creates no student assignments. The use case ends.
- 4a. Preview without publishing: The actor tests the saved configuration but chooses not to publish it. The system retains the saved version without changing publication or student access, then returns to Main Flow Step 2 or ends the use case.
- 5a. Revise after preview: The actor modifies the configuration after testing. The system requires a new Save, creating another immutable version before the revised configuration can be previewed or published. The flow returns to Main Flow Step 3.
- 6a. Publish without new assignments: The actor publishes a valid version but does not assign additional students. The system retains the published version without implicitly granting access.

## Exception flows
- 3.1. Invalid configuration: If required model, policy, or configuration fields are missing or invalid, the system identifies the affected fields and displays Proposed message: "Complete the required agent settings before saving." No invalid version is stored. The actor returns to Main Flow Step 2.
- 3.2. Version conflict: If another update has changed the saved configuration, the system displays Proposed message: "This agent configuration has changed. Reload the latest version before saving." The actor's unsaved edits are preserved for review; the existing saved version remains unchanged.
- 3.3. Unauthorized knowledge base or tool: If the configuration includes a knowledge base or tool outside the authorized scope, the system rejects the grant and displays Proposed message: "This knowledge source or tool is not permitted for this agent." No implicit permission is created. The actor returns to Main Flow Step 2.
- 4.1. Preview failure: If the model is unavailable, a tool fails, or the generated output is invalid, the system displays Proposed message: "Agent preview could not be completed. Review the configuration or try again." The saved configuration remains unchanged, no student conversation is created, and the actor may retry Main Flow Step 4.
- 5.1. Publication failure: If the selected version fails readiness checks or cannot be published, the system displays Proposed message: "The agent could not be published. Review the configuration and try again." The previous published version remains effective, and the actor returns to Main Flow Step 2 or Step 5.
- 6.1. Assignment failure: If a selected student is ineligible or assignment validation fails, the system identifies the affected selection and displays Proposed message: "The selected student assignment could not be completed." No unauthorized assignment is created. The actor returns to Main Flow Step 6.

## Source alignment / implementation notes
- **Source naming:** current report catalogue says **Create AI agent**, while internal specification cell says **Build AI agent**. This file uses catalogue name and retains historical wording in source field.

## Verification tracking
- API/component: [AgentAdminController.java](../../../backend/src/main/java/edu/hcmute/careersim/guidance/agent/controller/AgentAdminController.java); [AgentBuilderPage.jsx](../../../frontend/src/features/admin/agents/AgentBuilderPage.jsx).
- Automated tests: [AgentPlatformIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentPlatformIntegrationTest.java), [AgentConfigurationPolicyTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentConfigurationPolicyTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

Catalogue name is Create AI agent (D05); Build is retained only as source wording. Existing save/exact-preview/publish/assignment lifecycle and immutable session pins are reused, not renamed or rebuilt.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
