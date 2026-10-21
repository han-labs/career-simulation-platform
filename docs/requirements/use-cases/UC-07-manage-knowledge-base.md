# UC-07 — Manage knowledge base

> **Status:** SELECTED / present in current report
> **Actor:** Administrator
> **Report-selected User Story mapping:** US-08
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 3

## Trigger
The actor opens knowledge base management to create, update, inspect, or test authorized knowledge sources.

## Description
The system supports independent knowledge base management, document ingestion, versioned publication, source inspection, and testing of retrieval and grounded answers before knowledge is made available to authorized agents.

## Preconditions
The actor is authenticated with Administrator privileges and has permission to manage the selected knowledge base scope.

## Postconditions
Valid documents are indexed with versioned source provenance and become available within the authorized scope. Failed ingestion or replacement does not expose incomplete data or remove the previous published version.

## Main flow
- 1. The actor opens knowledge base management and creates or selects an authorized knowledge base.
- 2. The system validates the scope and displays the knowledge base, its documents, publication status, and available management actions.
- 3. The actor uploads supported TXT, PDF, DOCX, MD, or XLSX documents and initiates ingestion.
- 4. The system validates the files, extracts and normalizes text, creates source-linked chunks, generates embeddings, and records ingestion progress. Only a complete validated document version becomes active and retrievable.
- 5. The actor inspects the processing status, extracted text, chunks, and source metadata, then opens the independent knowledge base testing interface.
- 6. The actor enters a question and selects either passage retrieval or answer generation with sources. The system performs retrieval within the selected authorized knowledge base and displays ranked passages with source references. If answer generation is selected, the system separately generates and validates a grounded answer.

## Alternative flows
- 1a. Create an empty knowledge base: The actor creates a knowledge base without uploading documents. The system saves its metadata with no searchable content and returns to Main Flow Step 2.
- 3a. Replace or reindex a document: The actor explicitly requests replacement or reindexing. The system processes a new version while preserving the previous active version until successful publication, then returns to Main Flow Step 5.
- 5a. Inspect a document: The actor selects a document or version. The system displays available extracted text, chunks, provenance, and processing warnings before returning to Main Flow Step 5.
- 6a. Retrieval-only test: The actor selects passage retrieval. The system returns ranked passages and source details without invoking answer generation. The actor may submit another question at Main Flow Step 6.
- 6b. No matching evidence: The search returns no useful authorized evidence. The system displays Proposed message: "No matching authorized evidence was found." No unsupported answer is generated. The actor remains in the testing interface and may revise the question.

## Exception flows
- 3.1. Invalid document: If a file has an unsupported format, exceeds an allowed limit, or contains no processable content, the system displays Proposed message: "This document cannot be processed. Check the file format and content." The invalid file is not published. The actor remains on the document management page and may select another file.
- 4.1. Ingestion failure: If extraction, embedding, storage, or publication fails, the system records the failed stage and displays Proposed message: "Document ingestion failed. Previously published content remains available." The actor may inspect the failure and explicitly retry from Main Flow Step 3.
- 4.2. Version conflict: If the selected document changes during an update, the system rejects the conflicting publication and displays Proposed message: "This document has changed. Reload the latest version before continuing." Existing published content remains unchanged.
- 6.1. Answer generation failure: If the provider fails or the generated answer does not pass validation, the system displays Proposed message: "The AI answer could not be generated. Retrieved passages are still available." Valid retrieval results remain visible in the testing interface.
- 6.2. Source permission revoked: If a knowledge source is deleted or becomes unauthorized, the system prevents further retrieval and displays Proposed message: "This knowledge source is no longer available." The actor returns to the authorized knowledge base view.

## Source alignment / implementation notes
- Preserve the source specification; verify exact API/UI behavior and automated tests against current `main` before marking as accepted.

## Verification tracking
- API/component: [KnowledgeAdminController.java](../../../backend/src/main/java/edu/hcmute/careersim/guidance/knowledge/controller/KnowledgeAdminController.java); [KnowledgeStudioPage.jsx](../../../frontend/src/features/admin/knowledge/KnowledgeStudioPage.jsx).
- Automated tests: [AgentPlatformIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/AgentPlatformIntegrationTest.java), [KnowledgeParserContractTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/knowledge/KnowledgeParserContractTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

READY ingestion staging and explicit confirmed publication are separate (D07). Five-format/parser/scoped retrieval fixtures are not live embedding/model-quality acceptance.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
