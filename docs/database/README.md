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
