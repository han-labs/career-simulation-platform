# Copy-ready Sol 6.1 handoff — use only after human approval

The following prompt is intended for the human to copy after approving the specification. Its presence in this repository does not authorize implementation.

---

Implement the approved CareerSim architecture in this repository. Read AGENTS.md and its required `.agent` files, then these architecture specifications under `docs/architecture/data-model/physical-design-draft/`:

1. `04_SOL_MASTER_IMPLEMENTATION_PLAN.md` — primary execution sequence, scope, tests and acceptance matrix.
2. `01_PHYSICAL_DATABASE_DESIGN.md` — exact physical schema, ownership, identifiers and invariants.
3. `02_LEGACY_MAPPING_AND_MIGRATION.md` — all legacy mappings, migration order and preservation/cutover policy.
4. `03_BACKEND_AND_AGENT_ARCHITECTURE.md` — modules, interfaces, authorization, Agent/KB execution and API compatibility.

These documents become the architecture specification through this approval. Execute Package A then Package B in dependency order, autonomously within one session where feasible; stages are internal checkpoints, not separate review cycles. If only one package is explicitly authorized in this message, implement that package completely and report the remaining package. Preserve existing staged/user changes and inspect the actual Git/Flyway baseline before editing. Follow the specified table names, version pinning, final-only submission, completion-only semantics, purpose-specific consent, tenant isolation, configuration provenance and approved typed draft/plan actions. Reuse sound existing infrastructure; do not rewrite the backend or rename Syn into a generic runtime.

Run focused tests at internal checkpoints, fix ordinary compile/test/formatting failures yourself, then run the consolidated backend/frontend/Compose/PostgreSQL/API checks. Use isolated development/test databases with synthetic fixtures. Do not treat skipped PostgreSQL tests, frontend mock results, code existence or prior reports as current runtime evidence. Keep a short execution log to resume after context compaction.

Never modify V1–V12, drop/reset valuable databases, delete historical evidence, infer new consent from legacy timestamps, fabricate content versions/completions, weaken authentication, silently break APIs, execute unapproved destructive migrations, or commit/push/deploy without explicit authorization. Real-data migration/cutover requires the specified runtime preflight and restore evidence. Do not guess enterprise ownership, profile compatibility or missing historical provenance. Minimum frontend changes require the coordination included in the approved scope; absent that coordination, finish backend/tests but mark release cutover blocked.

Stop affected work and report concrete evidence for irreversible data-loss risk, migration-history conflict, unsupported real-data assumptions, contradictory mandatory requirements, security/ownership ambiguity or major architecture contradiction. Continue unaffected safe work. Do not request architectural review for routine implementation defects.

Deliver one consolidated final report with actual files/interfaces/schema changes, requirement-to-test evidence, exact commands and outcomes, migration/backfill results, legacy/API preservation, failed/skipped/blocked checks and remaining human/runtime acceptance. Distinguish deterministic tests, live provider/runtime checks and human content review. Do not claim full completion while required work remains.
