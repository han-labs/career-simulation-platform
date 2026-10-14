# CareerSim — UML Class Diagram v3: Lean modular views

## Purpose and status
- **Target design**, not verified against JPA classes, Flyway migrations, or current backend implementation.
- Two complementary views of **one modular monolith**, not two separate deployed systems: (1) Core & Learning, (2) Agent/RAG/Consent.
- Primary use-case rules remain from POSE requirements and user-approved ADRs. SVG files are editable vectors; `.mmd` files carry the same class/association model for Mermaid.
- Standard naming: PascalCase classes; camelCase attributes/methods; typed attributes; 1–2 representative operations; no text labels on relationship lines. Multiplicities are shown at line ends.

## Deliberate selection

### Core diagram
- Includes UserAccount, StudentProfile, Enterprise, EnterpriseRepresentative because enterprise is an organization, never a subtype of user.
- AssessmentType + AssessmentVersion + AssessmentAttempt + AssessmentResult: versioned optional tests, repeated attempts, result source preserved.
- Scenario + ScenarioVersion + SimulationTask + TaskDeliverableDefinition + SubmittedDeliverable + TaskSubmission + SimulationEnrollment + ModelAnswer + SimulationCompletion reflect approved business policies, including **one accepted submission per Enrollment–Task**, **many deliverables per Task**, **V1/V2 parallel enrollment**, **model answer unlock after accepted submission**, and **no automatic grading**.
- CareerSkill is shown as a shared vocabulary for *practiced skills*, not verified competence. Resume snippet and certificate metadata live in SimulationCompletion according to the simplified baseline; issuance/retry remains a separate application concern.
- AssessmentApplicationService and SimulationApplicationService are the only shown `<<control>>` classes: they coordinate use cases and transactions; they do not duplicate domain attributes.

### Agent diagram
- AgentConfiguration is versioned, and old AgentConversations stay pinned to a configuration. Many KnowledgeBases can be attached per configuration. Students and enterprise accounts use authorized agents, administered by Admin.
- Messages and runs record chat and execution. Tool selections can remain a validated set in AgentConfiguration; do not invent a `Tool` entity unless a tool registry has independent state/policy.
- AgentMemory is per-account/per-agent opt-in; `ConsentGrant` is scope-specific and revocable. Enterprise evidence, assessment sharing, recruitment, external AI and cross-chat memory must not accidentally share a single authorization switch.
- AgentActionProposal needs human approval and a **second authorization check at execution time**; `approve()` only changes proposal state, not the target domain.
- KnowledgeDocumentVersion/KnowledgeChunk preserve RAG source lineage. Upload progress, embedding indexes, background jobs and knowledge evaluation cases can remain implementation detail unless persistent business requirements demand dedicated tables/classes.

## What is intentionally absent on the master UML (but can remain in the Logical ERD)
- AssessmentQuestion, AssessmentResponse, TaskResource, TaskSkill, FileAsset, AgentConfigurationKb, provider configuration records, audit event rows, retrieval tracing rows and some storage/junction tables.
- These are not asserted to be unnecessary **tables**: they are omitted **from the high-level class diagrams** to prevent source-code cloning. In particular, physical ERD still needs AssessmentResponse for answer persistence, TaskSkill for M:N, and SubmittedDeliverable for multi-item submissions.
- No User subclasses; no AdminProfile by default; no grading Rubric or TaskOutcome class in the required Simulation workflow.
- No Controller/DTO/Repository/Mapper/provider adapter classes here; those belong in a separate implementation-level design diagram if the course requires it.

## Five focal GRASP principles
| GRASP principle | Design assignment |
|---|---|
| Information Expert | TaskSubmission manages finalization of its item set; AssessmentAttempt owns attempt progress; ScenarioVersion validates its content; ConsentGrant evaluates its grant state. |
| Creator | Aggregate owners manage contained objects: ScenarioVersion -> SimulationTask, TaskSubmission -> SubmittedDeliverable, Agent -> AgentConfiguration. |
| Controller | One assessment, one simulation, and one agent use-case application service coordinate transactions, auth and external calls. |
| Low Coupling | AgentApplicationService uses approved application use-cases and minimized evidence views, not direct arbitrary SQL modifications of learning records. |
| High Cohesion | Entity methods implement local invariants; cross-entity authorization and persistence transactions live in application services. |

## Must-retain database invariants (not all visible on UML)
- Unique case-normalized UserAccount email, one role per account, one Enterprise per Representative account, many Representatives per Enterprise.
- AssessmentVersion uniqueness per AssessmentType; allow multiple AssessmentAttempts per Student/Version; responses must reference questions from the Attempt's pinned version.
- Enrollment `UNIQUE(studentId, scenarioVersionId)`; no unique-by-Scenario preventing parallel versions; TaskSubmission `UNIQUE(enrollmentId, taskId)` includes draft state; composite FKs enforce version and TaskDeliverableDefinition consistency.
- Never overwrite submitted deliverables; ModelAnswer and model answer assets server-authorized only after valid submitted status.
- Completion after all required tasks; no grade or skill certification. Fixed resume snippet per Completion.
- Consent default-on *suggestion* is not a consent grant until student affirmatively accepts; detailed enterprise/assessment evidence access always checks tenant, purpose and scope.
- AgentConversation pins AgentConfiguration version, configurations reference many KBs, context-limited AgentMemory and action approvals are enforced at runtime.

## Cross-view consistency
- `UserAccount` appears as a minimal **reference projection** in the Agent diagram; it is the same domain class fully shown in Core diagram, not another database entity.
- `ConsentGrant` is maintained by Identity/Governance; Agent use cases query authorization through approved interfaces rather than importing identity repositories.
- `ExplorationPlan` belongs to a Student role and is persisted only after explicit confirmation; `AgentRun` may propose it.
- Relationship labels are absent. End multiplicities shown, e.g. `(1)` and `(0..*)`.

## UML abstraction notes
- Class associations describe object relationships, not foreign key columns. Absence of `questionId` or `scenarioVersionId` in a class does not imply such columns are absent in the ERD.
- Composition diamonds are **conceptual ownership** and must not be interpreted as hard-delete of published/versioned history. The persistence policy is history preserving.
- `JsonNode` is a Java-oriented representation candidate; the actual physical schema may use JSONB or normalized tables depending on invariant and retrieval requirements.
- The 5 GRASP principles are the focus requested here; GRASP comprises more principles in full.

## References
- Project SRS: `Updated_CareerSimulation_Report_Requirement_Revised.docx` (FR-S05–S17, FR-E01–E11, BR-02–BR-16, UC-05/07/08/09).
- Architecture tutorial: `how-to-design-class.pdf` (3-layer/MVC, responsibility assignment, GRASP/SOLID context).
- OMG UML 2.5.1 specification: https://www.omg.org/spec/UML/2.5.1/
