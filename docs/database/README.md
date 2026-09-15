# PostgreSQL and migration rules

- PostgreSQL is the system of record.
- Flyway owns schema changes. After a migration is shared, never rewrite it; add the next version.
- Hibernate uses `ddl-auto=validate` outside tests.
- Foreign keys and check constraints protect invariants that can be enforced safely at the database boundary.
- Business decisions still belong to domain/service code and must have tests.
- Persist objective evaluation separately from AI or fallback guidance so AI can never overwrite the score.
- Store guidance source and model provenance with every report.
- Do not place API keys, prompts containing private data, or unrestricted raw model logs in the database.

Local connection defaults:

```text
host: localhost
port: 5433
database: career_simulation
username: career_sim
password: career_sim_dev
```

These defaults are development-only. Deployment credentials must come from the environment or an approved secret manager.

## US-03 persistence

Migration `V6__add_exploration_plans.sql` adds one current plan row per student. The
plan title and bounded ordered steps are persisted only after the student calls the
explicit save endpoint. Dashboard data is derived from existing completed assessment
scores, evaluated simulation results, and this plan; there is intentionally no
duplicated `dashboard` table.

The existing `guidance_reports` table is reused as an internal provenance/audit record.
V6 adds `action_type` and measured `generation_ms`. `V7__sync_seed_sequences.sql`
repairs the `app_users` sequence after the explicit demo ID introduced by the pulled
US-01 seed. `V8__seed_complete_local_demo.sql` completes the synthetic localhost
journey with 42 assessment answers and scores, one evaluated Backend Developer
attempt, demo-only AI consent, and three original tasks for every published
simulation. It does not seed a plan so that plan drafting, review, and explicit save
remain demonstrable interactions. Standard deterministic replies use database source `FALLBACK` and API
label `STANDARD`; successful provider replies use `AI` plus the configured model.
Provider failures use `REPLACED_BY_FALLBACK`. No raw chat history, key, or prompt is
stored. A record is created only when completed evidence grounds the response.
