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

`V9__add_syn_agent_context.sql` adds the bounded agent metadata. `syn_sessions`
stores a short structured summary, last intent, and last compared path codes for 30
days; it does not store raw messages. Every session is student-owned and may be
deleted from the widget. `career_path_profiles` and `learning_resources` contain
reviewed server-owned reference data, while `simulation_task_skills` maps original
course-demo tasks to skills. These tables let the same deterministic tools operate
when Gemini is disabled or unavailable. The LLM receives observations prepared by
the service and never receives database access.

## US-01 career quizzes

`V10__add_interests_and_abilities_quizzes.sql` adds a type discriminator to
`assessment_attempts` while keeping existing RIASEC attempts and scoring intact.
Original Interests and Abilities prompts are stored in `career_quiz_questions`;
submitted responses and deterministic category totals are stored separately in
`career_quiz_answers` and `career_quiz_scores`. Each quiz has 12 active questions,
three per category, rated on a required 1–5 self-reflection scale. Attempt type,
student ownership, question membership, response completeness, and category mapping
are validated by the assessment service. Results are exploratory and do not claim
validated aptitude or psychological measurement.

`V11__add_work_activities_quiz.sql` extends the existing assessment-type constraints
and seeds nine original Work Activities prompts into the same question table. Three
items each measure a student's stated preference for Information, People, and Things;
their responses and 3–15 category sums use the existing answer and score tables.
This preference reflection complements the Interests and Abilities quizzes and does
not match students to occupations or claim validated measurement.

## Syn persistence migrations

The persistent development database already records the assessment schema changes
at V10 and V11. To keep those applied migration names and checksums intact, the
Syn lifecycle, vector knowledge, and trace migrations use V12, V13, and V14,
respectively. V13 requires the pgvector extension. The SQL contents of the Syn
migrations are unchanged by this version reassignment.
