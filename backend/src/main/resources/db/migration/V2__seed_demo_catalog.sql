INSERT INTO career_simulations
    (slug, title, career_track, summary, difficulty, estimated_minutes, status)
VALUES
    ('backend-api-triage', 'Backend API Triage', 'BACKEND_DEVELOPMENT',
     'Inspect an API response, choose a safe database query, and identify the likely source of a server error.',
     'INTRODUCTORY', 35, 'PUBLISHED'),
    ('frontend-accessibility-review', 'Frontend Accessibility Review', 'FRONTEND_DEVELOPMENT',
     'Review a small interface brief and make evidence-based decisions about semantics, keyboard use, and feedback states.',
     'INTRODUCTORY', 30, 'PUBLISHED'),
    ('data-quality-investigation', 'Data Quality Investigation', 'DATA_ANALYSIS',
     'Find inconsistencies in a small product dataset and select reproducible cleaning and validation steps.',
     'INTERMEDIATE', 45, 'PUBLISHED'),
    ('software-test-design', 'Software Test Design', 'SOFTWARE_TESTING',
     'Turn a user story into boundary cases, prioritize risk, and diagnose a failing acceptance scenario.',
     'INTRODUCTORY', 40, 'PUBLISHED'),
    ('security-incident-first-response', 'Security Incident First Response', 'CYBERSECURITY',
     'Classify suspicious activity, preserve evidence, and choose an appropriate first-response action.',
     'INTERMEDIATE', 50, 'PUBLISHED');

INSERT INTO simulation_tasks
    (simulation_id, title, instructions, task_type, display_order, max_score, evaluation_rule)
SELECT id, 'Interpret an API response',
       'Read the supplied response and identify the most defensible client action.',
       'MULTIPLE_CHOICE', 1, 10,
       '{"strategy":"exact_option","correctOption":"retry-after-validation"}'::jsonb
FROM career_simulations WHERE slug = 'backend-api-triage';

INSERT INTO simulation_tasks
    (simulation_id, title, instructions, task_type, display_order, max_score, evaluation_rule)
SELECT id, 'Choose a safe lookup query',
       'Select the parameterized query that returns one active account without exposing unrelated data.',
       'MULTIPLE_CHOICE', 2, 10,
       '{"strategy":"exact_option","correctOption":"parameterized-active-user"}'::jsonb
FROM career_simulations WHERE slug = 'backend-api-triage';

INSERT INTO simulation_tasks
    (simulation_id, title, instructions, task_type, display_order, max_score, evaluation_rule)
SELECT id, 'Locate the failure boundary',
       'Use the trace evidence to identify whether the failure belongs to validation, business logic, or persistence.',
       'MULTIPLE_CHOICE', 3, 10,
       '{"strategy":"exact_option","correctOption":"persistence-timeout"}'::jsonb
FROM career_simulations WHERE slug = 'backend-api-triage';
