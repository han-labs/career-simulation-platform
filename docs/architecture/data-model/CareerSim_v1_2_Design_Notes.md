# CareerSim Target Design v1.2 — Patch Notes

**Status:** Target design for requirements alignment; **NOT as-built database/code**. Core .mmd/.svg byte-identical to v1.1. No changes to DR1–DR2, no Package B implementation.

## Exactly four design deltas

1. **Standalone Admin preview / KB test:** `AgentExecution.inputSnapshot: JsonNode` always records test request; `sessionId` and `inputInteractionId` can be absent. `configurationId` is required for `ADMIN_PREVIEW` but optional for `KB_TEST`; `knowledgeBaseId` is required for `KB_TEST`. No synthetic Student session/interaction is created.
2. **Pinned chat config:** `AgentSession.configurationId` points to the chosen immutable published `AgentConfiguration` version. Subsequent publishes do not alter existing chats. Each `STUDENT_CHAT` execution must use the session's pinned configuration.
3. **KB scope:** `KnowledgeBase.scopeType` distinguishes `PLATFORM` vs `ENTERPRISE`; `enterpriseId` is absent for platform KB and required for enterprise KB. Enterprise KB is schema-capable, **not an MVP workflow**; explicit KB grants/tenant authorization still apply.
4. **Auditable test usage:** `AgentExecution.executionMode` distinguishes `STUDENT_CHAT`, `ADMIN_PREVIEW`, `KB_TEST`; keeps `initiatedByUserId`, input, status, evidence/tools, tokens, latency, estimated cost and output. Token/cost fields must allow unknown/not applicable for retrieval-only KB Test; never fabricate consumption. Admin dashboard can aggregate persisted executions by mode/status.

## Retained rules

- Quiz score does not gate completion; free-form tasks have no required auto-grade; model answers unlock only after valid final submission; practiced skills are not proficiency certificates.
- Admin config → version → preview → publish → Student assignment.
- Report only on explicit request, plan only after Student confirmation; Student private uploads, multi-agent critique and Enterprise Agent remain deferred.
- Summaries maintain same-owner source lineage and invalidation on deletion; backup/audit retention policy remains OPEN.
- Enterprise Core scenario authoring/publication preserved.

## Diagram convention

`.mmd` is the **structural authority** for Codex; `.svg` is a visual overview. SVG diagrams are clean Graphviz layouts with no relationship-word labels; some UML/ER multiplicities and edge semantics are only fully specified in Mermaid source. Do not treat SVG as authoritative for cardinality.

## Before code

Use read-only audit. Do not modify Flyway V1–V18, invent deployment verification, or assume 1 UML class = 1 SQL table.
