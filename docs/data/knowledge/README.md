# Knowledge corpus and retrieval evaluation

The JSON files here are original AI-assisted **drafts**, not reviewed publications.

- `corpus.draft.json`: 20 documents, EN/VI, five IT tracks. All reviewed=false; reviewer is empty.
- `development.json`: three development examples for tuning; never include them in held-out scores.
- `evaluation.candidate.json`: 60 candidate cases: 40 answerable, 10 no-answer, 10 adversarial. Expected source labels are provisional, not human gold chunks.

A human must review factual correctness, provenance/license, section boundaries and gold supporting passages. Edit a separate approved manifest; record reviewer and reviewed=true only after actual review. Independently paraphrase and freeze a held-out set before tuning. Some candidate queries intentionally overlap across languages; report each group separately. The retrieval runner measures source-level Recall@5, not claim-level citation support or adversarial agent safety.

## Publish

Use a pgvector database with migrations through V12 and backend-only GEMINI_API_KEY. Set KNOWLEDGE_EMBEDDING_ENABLED=true, then start the backend with:
```text
--app.knowledge.ingest-file=/absolute/path/corpus.approved.json
```

Do not use the draft manifest as a production corpus. Ingestion rejects it. Use server-local trusted files; the command is not exposed through REST or to the model. The app keeps running after ingestion; remove the flag before the next normal start. Re-running a document is DB-idempotent but currently can spend embedding calls again.

To revoke: invoke `KnowledgeIngestionService.revoke(sourceKey)` from an authorized internal operation, or the documented restricted operator SQL below. No public management endpoint was added:
```sql
UPDATE knowledge_documents SET status='REVOKED' WHERE source_key = 'reviewed-source-key';
```
Revoke immediately removes the source from retrieval and causes later finalization/replay referencing it to fail validation.

## Evaluate

After publishing an approved corpus, run:
```text
--app.knowledge.eval-file=/absolute/path/evaluation.approved.json
--app.knowledge.eval-output=/absolute/path/retrieval-results.json
```

This opt-in command calls query embeddings and PostgreSQL, produces lexical/vector/hybrid rows on the same corpus/profile, and requires reviewed=true for every case. It does not call generation. It fails when embedding is unavailable rather than pretending lexical results are a vector benchmark. Run in an isolated evaluation database.

The report records source keys, chunk IDs, version IDs, query group, Recall@5 for answerable cases, empty retrieval and combined timing. Combined time includes embedding plus all three modes; do not report it as separate per-mode latency. No-answer empty retrieval is diagnostic, not proof that the final agent abstains correctly.

Then run agent answers separately and have a reviewer label citation support, unsupported claims, language adequacy and correct refusals. Keep raw samples synthetic or explicitly consented. Record model/profile/corpus hashes and numerator/denominator; do not mark the P2 quality gates passed from mock embeddings or these draft labels.

