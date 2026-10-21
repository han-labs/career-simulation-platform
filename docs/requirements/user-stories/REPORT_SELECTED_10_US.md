# Report-selected User Stories — 2026-10-22

> **CURRENT selected report catalogue:** 10 US. This is NOT the frozen Baseline v1 15-US catalogue and is NOT the superseded 13-US report catalogue.
> **Source:** `MTSE-FinalProjectReport (1).docx`, §1.3. Original English requirement wording and acceptance criteria preserved.

## Mapping to selected Use Cases

| Story | Name | Selected UC | Assigned person |
|---|---|---|---|
| US-01 | Register account | UC-01 | Người 1 |
| US-02 | Log in | UC-02 | Người 1 |
| US-03 | Complete career quiz | UC-03 | Người 1 |
| US-04 | Explore simulations | UC-04 | Người 2 |
| US-05 | Complete simulation | UC-05 | Người 2 |
| US-06 | View simulation results | UC-05 | Người 2 |
| US-07 | Consult career agent | UC-06 | Người 3 |
| US-08 | Manage knowledge base | UC-07 | Người 3 |
| US-09 | Create AI agent | UC-08 | Người 3 |
| US-10 | View AI usage dashboard | UC-09 | Người 2 |

## Detailed descriptions and acceptance criteria

### US-01 — Register account

As a Student, I want to create my CareerSim account so that I can access a personal exploration workspace.

**Acceptance criteria**

- Can I register using an eligible account or identity flow?
- Can I see which required registration fields are missing or invalid?
- Can I see a clear message when an account already exists?
- Can I cancel registration without creating an account?

**Story points: 3     Priority: High**
**Related UC:** UC-01

### US-02 — Log in

As a Student, Enterprise Representative, or Administrator, I want to sign in securely so that I can use the functions authorized for my role.

**Acceptance criteria**

- Can I sign in through the configured Google OAuth2 login?
- Can I access only pages and information allowed to my role and account?
- Can I receive a clear message if login fails or my account is disabled?
- Can I log out so that the current session can no longer access protected information?

**Story points: 5     Priority: High**
**Related UC:** UC-02

### US-03 — Complete career quiz

As a Student, I want to complete an optional career quiz so that I can understand my interests and explore relevant career directions.

**Acceptance criteria**

- Can I choose an available career assessment without being required to complete another assessment first?
- Can I see instructions and answer the questions required by the published assessment?
- Can I receive field-level guidance when I leave required answers incomplete?
- Can I submit valid responses and view the resulting assessment summary?
- Can I retain separate results when I take different assessments or versions?

**Story points: 5     Priority: High**
**Related UC:** UC-03

### US-04 — Explore simulations

As a Student, I want to search and compare published simulations so that I can choose a career activity that interests me.

**Acceptance criteria**

- Can I browse available simulations even if I have never completed a career quiz?
- Can I search, filter, sort, and navigate paginated catalogue results?
- Can I open a simulation to see its role, expected tasks, resources, and difficulty?
- Can I receive a useful empty-state message when no simulation matches my search?
- Can I browse without seeing locked model answers?

**Story points: 3     Priority: High**
**Related UC:** UC-04

### US-05 — Complete simulation

As a Student, I want to complete the ordered tasks of a simulation so that I can experience representative work activities at my own pace.

**Acceptance criteria**

- Can I start or resume an eligible published scenario?
- Can I save a draft and return before making a final submission?
- Can I finalize a valid task without a duplicate submission when retrying?
- Can I view a task model answer only after my valid final submission?
- Can I confirm self-review before progressing to the next task?
- Can I complete a quiz task with a score of zero without being blocked by its score?

**Story points: 8     Priority: High**
**Related UC:** UC-05

### US-06 — View simulation results

As a Student, I want to view my completed simulation and practiced skills so that I can reflect on what I experienced without confusing participation with certification.

**Acceptance criteria**

- Can I view my completed tasks and their recorded submission status?
- Can I see quiz scores only for tasks that support objective scoring?
- Can I see practiced skills linked to completed tasks rather than assigned by score thresholds?
- Can I access an eligible completion record or resume snippet that does not claim professional competence?
- Can I view only my own private submissions and results?

**Story points: 3     Priority: High**
**Related UC:** UC-05

### US-07 — Consult career agent

As a Student, I want to interact with a personalized career agent so that I can understand my exploration evidence, receive career guidance, and plan my next learning activities.

**Acceptance criteria**

- Can I ask career-related questions even if I have not completed an assessment?
- Can I receive guidance based on my available assessment results, simulation activities, and authorized knowledge sources?
- Can I receive cited answers when the agent uses Knowledge Base information?
- Can I continue previous conversations and delete my conversation history?
- Can I request a structured career insight report and view the saved result?
- Can I review, edit, and explicitly confirm an exploration plan before it is saved?
- Can I receive a clear explanation when evidence is insufficient or the AI service is unavailable?
- Can I be prevented from accessing locked model answers and other users' private information?

**Story points: 8     Priority: High**
**Related UC:** UC-06

### US-08 — Manage knowledge base

As an Administrator, I want to upload, manage, and test authorized Knowledge Base content so that I can publish reliable sources for assigned Student Agents.

**Acceptance criteria**

- Can I create and manage a Knowledge Base within its permitted scope?
- Can I upload supported TXT, PDF, DOCX, MD, or XLSX documents and see ingestion status?
- Can I review publication status and source provenance before allowing retrieval?
- Can I test passage retrieval separately from an AI-generated answer?
- Can I see a clear error when an upload is invalid or retrieval has no matching authorized source?
- Can I keep existing published material available when a replacement ingestion fails?

**Story points: 8     Priority: High**
**Related UC:** UC-07

### US-09 — Create AI agent

As an Administrator, I want to configure, test, publish, and assign a Student Agent so that I can manage a dependable assistant without changing application code.

**Acceptance criteria**

- Can I create an Agent and configure its model, system prompt, policies, KB grants, and permitted tools?
- Can I save a version without automatically publishing it to Students?
- Can I preview the saved configuration without creating a Student conversation?
- Can I publish an eligible immutable configuration version and explicitly assign the Agent to Students?
- Can I verify that unassigned Students cannot use that Agent or access its private KBs?
- Can I see validation feedback for missing required settings or conflicting updates?

**Story points: 8     Priority: High**
**Related UC:** UC-08

### US-10 — View AI usage dashboard

As an Administrator, I want to view AI usage and service outcomes so that I can monitor AI reliability, latency, and operating cost.

**Acceptance criteria**

- Can I view request counts for a selected reporting interval?
- Can I see average latency and recorded or estimated cost without mistaking missing measurements for zero?
- Can I distinguish failed and refused requests from successful ones?
- Can I filter the report without viewing Students’ private chat content by default?
- Can I see an appropriate message when usage data is unavailable?

**Story points: 5     Priority: High**
**Related UC:** UC-09

## Important business boundaries

- Assessment is optional; simulation access does not depend on prior assessment.
- Valid finalized quiz task may have score 0; score does not gate completion.
- Skills are **practiced**, not certified.
- Model answer only after valid final submission, with self-review gating progress to next task.
- Agent access and KB are authorized; plans only persist after explicit Student confirmation.
- Preserve frozen Baseline v1 and DR1–DR2 unchanged.

## Repository alignment note (2026-10-22)

Selected story text/ACs above are preserved from the supplied report extract. **US-07 edit-before-plan-confirm is a known source/code gap**, not acceptance evidence or permission to weaken source/hash checks. UC-05 completion wording also needs explicit reconciliation. See [alignment register](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [traceability](../traceability-matrix.md) and [official assignments](../../TEAM_ASSIGNMENTS.md).
