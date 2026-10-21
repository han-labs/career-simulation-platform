# Current requirements index

Documentation synchronized 2026-10-22 against `origin/main@5486f9f4a3a56519c396bcebffd49fe0ff4776e2`. The local checkout remains `6a3e989`; no checkout/reset, code, migration or database change was performed.

## Authority by purpose

1. Direct project-lead decisions, including the official Person 1/2/3 allocation.
2. **Current catalogue:** latest `MTSE-FinalProjectReport (1).docx` and its CORRECTED Markdown handoff: [10 selected US](user-stories/REPORT_SELECTED_10_US.md), [16 UC index](use-cases/README.md), [TEAM_ASSIGNMENTS](../TEAM_ASSIGNMENTS.md). Only UC-01–09 are currently assigned; UC-10–16 are supplementary, with UC-14 deferred.
3. **Approved business invariants:** [frozen Requirements Baseline v1 + DR1–DR2](CareerSim_Requirements_Baseline_v1_Chat2_Handoff.md). Its 15 US IDs remain unchanged and distinct. Report wording does not automatically override those rules.
4. **Design and implementation:** [Target v1.2 notes](../architecture/data-model/CareerSim_v1_2_Design_Notes.md), [Core](../architecture/data-model/CareerSim_Core_Domain_Model_ReqBaseline_v1_2.mmd), [Agent](../architecture/data-model/CareerSim_AI_Agent_Domain_Model_ReqBaseline_v1_2.mmd), [logical ERD](../architecture/data-model/CareerSim_Target_Logical_ERD_ReqBaseline_v1_2.mmd); [as-built architecture](../architecture/README.md), [API](../api/README.md), production source/tests and dated evidence establish what P1–P6/V22 actually implement. Logical classes are not a physical-schema rewrite instruction.
5. **Historical sources:** [legacy 3-US stories](user-stories.md), original proposals, prior 13-US report and the 2026-10-21 supplementary report. Preserve their content and qualify their IDs.

## Read for a current task

- Open the assigned UC, its selected-US acceptance criteria, and [report/code alignment](REPORT_CODE_ALIGNMENT_2026-10-22.md).
- Use [traceability](traceability-matrix.md) for functional Baseline coverage, not ID equality. The exact old-report UC remap is in the UC index.
- Follow [TASKS](../../.agent/TASKS.md) for progress; [TEAM_ASSIGNMENTS](../TEAM_ASSIGNMENTS.md) is the single official ownership allocation. Names/Git aliases and the proposed reviewer rotation require separate confirmation.
- Use existing architecture sequence diagrams and relevant APIs/tests before planning any missing change; do not implement P1–P6 again or treat supplementary specs as new scope approval.

## Verification and unresolved decisions

UC-05 overall completion versus final self-review, asynchronous evaluation/new attempt, UC-04 highlighted recommendations and UC-06 editable plan require explicit reconciliation. UC-08 catalogue name is **Create AI agent**, with **Build** retained as source wording only. See the alignment register for exact source/test paths and report provenance.

No new UC/NFR IDs or NFR thresholds were invented. Assignment section 7 and existing safety/test rules supply nonfunctional constraints; the old report's NFR-01–10 remain source references without a complete standalone definition in this repository. A new NFR catalogue is not part of this synchronization. Final evaluation requires at least 20 cases and honest separation of deterministic/mock/wire evidence from live-model/human acceptance.

This update does not edit frozen Baseline, historical reports or Flyway V1–V22. It does not grant permission for production migration, paid calls, new features, commits or deployment.
