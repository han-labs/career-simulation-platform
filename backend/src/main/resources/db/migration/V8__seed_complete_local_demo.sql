-- Complete the synthetic localhost demo without changing production authentication rules.
-- Content is original course-demo material and contains no real student data.

WITH task_seed(
    simulation_slug,
    display_order,
    title,
    instructions,
    correct_option,
    explanation,
    options
) AS (
    VALUES
        (
            'frontend-accessibility-review', 1,
            'Choose the page heading structure',
            'A page title is followed by two independent content sections. Which heading structure is the clearest?',
            'b',
            'One h1 for the page followed by h2 section headings creates a clear semantic outline.',
            '[{"id":"a","label":"Use an h1 for every heading"},{"id":"b","label":"Use one h1, then h2 headings for the sections"},{"id":"c","label":"Use only styled div elements"},{"id":"d","label":"Skip directly from h1 to h4"}]'::jsonb
        ),
        (
            'frontend-accessibility-review', 2,
            'Support keyboard interaction',
            'A custom clickable card must also work without a mouse. What is the best first choice?',
            'a',
            'A native button provides keyboard activation, focus behavior, and semantics by default.',
            '[{"id":"a","label":"Use a native button with a visible label"},{"id":"b","label":"Add only an onClick handler to a div"},{"id":"c","label":"Hide the control from the tab order"},{"id":"d","label":"Require double-click to activate it"}]'::jsonb
        ),
        (
            'frontend-accessibility-review', 3,
            'Communicate a validation error',
            'A required answer is missing after submit. Which feedback is most accessible?',
            'c',
            'Text linked to the field and an error summary communicate the problem without relying on color alone.',
            '[{"id":"a","label":"Change only the border color to red"},{"id":"b","label":"Clear every completed field"},{"id":"c","label":"Show linked error text and focus the error summary"},{"id":"d","label":"Wait for the user to discover the problem"}]'::jsonb
        ),
        (
            'data-quality-investigation', 1,
            'Identify a missing-value risk',
            'A required product category is blank in 8 percent of rows. What should you do first?',
            'b',
            'Measure and inspect the affected rows before choosing a documented cleaning rule.',
            '[{"id":"a","label":"Delete the entire dataset"},{"id":"b","label":"Inspect affected rows and quantify the pattern"},{"id":"c","label":"Replace every blank with a random value"},{"id":"d","label":"Ignore the issue because most rows are complete"}]'::jsonb
        ),
        (
            'data-quality-investigation', 2,
            'Handle duplicate records',
            'Two rows share the same stable product ID but have different update times. What is the most reproducible approach?',
            'd',
            'A documented rule using a stable key and timestamp can be reviewed and repeated.',
            '[{"id":"a","label":"Keep whichever row appears first"},{"id":"b","label":"Merge values manually without notes"},{"id":"c","label":"Remove both rows"},{"id":"d","label":"Keep the latest row using ID and update time, and document the rule"}]'::jsonb
        ),
        (
            'data-quality-investigation', 3,
            'Validate a cleaning result',
            'After cleaning, which check gives the strongest evidence that the transformation worked?',
            'a',
            'Re-running explicit quality checks makes the result measurable and reproducible.',
            '[{"id":"a","label":"Run the same completeness, uniqueness, and range checks again"},{"id":"b","label":"Judge the first ten rows by appearance"},{"id":"c","label":"Rename the output file"},{"id":"d","label":"Assume the script is correct because it finished"}]'::jsonb
        ),
        (
            'software-test-design', 1,
            'Find a boundary case',
            'A password accepts 8 to 64 characters. Which set best tests the length boundary?',
            'c',
            'Values immediately below, on, and above each boundary expose off-by-one errors.',
            '[{"id":"a","label":"Only 20 and 30 characters"},{"id":"b","label":"Only 8 and 64 characters"},{"id":"c","label":"7, 8, 9, 63, 64, and 65 characters"},{"id":"d","label":"One randomly selected length"}]'::jsonb
        ),
        (
            'software-test-design', 2,
            'Prioritize regression coverage',
            'A release changes payment validation but not profile avatars. What should receive the highest regression priority?',
            'b',
            'Changed, high-impact payment behavior carries more immediate product risk.',
            '[{"id":"a","label":"Avatar color selection"},{"id":"b","label":"Payment validation success and failure paths"},{"id":"c","label":"Footer link spacing"},{"id":"d","label":"An unrelated archived page"}]'::jsonb
        ),
        (
            'software-test-design', 3,
            'Diagnose a failing acceptance test',
            'A test expected HTTP 400 but received HTTP 500 for missing input. What is the best next step?',
            'a',
            'The server logs and validation boundary help distinguish a missing validation rule from an internal defect.',
            '[{"id":"a","label":"Inspect the validation path and sanitized server logs"},{"id":"b","label":"Change the test to expect 500"},{"id":"c","label":"Delete the failing test"},{"id":"d","label":"Retry until it passes"}]'::jsonb
        ),
        (
            'security-incident-first-response', 1,
            'Triage a suspicious login',
            'Several failed logins are followed by a successful login from a new country. What is the safest first response?',
            'c',
            'Containment plus evidence preservation reduces immediate risk without destroying investigation data.',
            '[{"id":"a","label":"Publish the user details in a public channel"},{"id":"b","label":"Delete all authentication logs"},{"id":"c","label":"Protect the account and preserve relevant logs"},{"id":"d","label":"Ignore it until another incident occurs"}]'::jsonb
        ),
        (
            'security-incident-first-response', 2,
            'Preserve investigation evidence',
            'Which action best preserves evidence from a suspicious server?',
            'b',
            'A controlled snapshot and documented chain of custody preserve evidence for later analysis.',
            '[{"id":"a","label":"Edit the original logs to make them shorter"},{"id":"b","label":"Create a controlled snapshot and record who handled it"},{"id":"c","label":"Post the full logs on social media"},{"id":"d","label":"Reinstall immediately before collecting evidence"}]'::jsonb
        ),
        (
            'security-incident-first-response', 3,
            'Limit credential exposure',
            'An API key may have been copied from a repository. What should the team do?',
            'd',
            'Revoking and rotating the key limits further abuse; history and logs still need investigation.',
            '[{"id":"a","label":"Rename the variable but keep the same key"},{"id":"b","label":"Move the key to another public file"},{"id":"c","label":"Wait for confirmed abuse before acting"},{"id":"d","label":"Revoke and rotate the key, then inspect history and access logs"}]'::jsonb
        )
)
INSERT INTO simulation_tasks
    (simulation_id, title, instructions, task_type, display_order, max_score, evaluation_rule)
SELECT
    simulation.id,
    seed.title,
    seed.instructions,
    'MULTIPLE_CHOICE',
    seed.display_order,
    1,
    jsonb_build_object(
        'strategy', 'exact_option',
        'options', seed.options,
        'correctOption', seed.correct_option,
        'explanation', seed.explanation
    )
FROM task_seed seed
JOIN career_simulations simulation ON simulation.slug = seed.simulation_slug
WHERE NOT EXISTS (
    SELECT 1
    FROM simulation_tasks existing
    WHERE existing.simulation_id = simulation.id
      AND existing.display_order = seed.display_order
);

DO $$
DECLARE
    demo_student_id BIGINT;
    demo_assessment_id BIGINT;
    demo_simulation_id BIGINT;
    demo_simulation_attempt_id BIGINT;
BEGIN
    SELECT id
    INTO demo_student_id
    FROM app_users
    WHERE email = 'student@demo.com'
      AND role = 'STUDENT'
      AND status = 'ACTIVE';

    IF demo_student_id IS NULL THEN
        RAISE EXCEPTION 'The synthetic demo student is missing';
    END IF;

    -- This consent belongs only to the synthetic demo identity. It enables an
    -- explicitly configured external provider during localhost demonstrations.
    UPDATE student_profiles
    SET consented_to_ai_at = COALESCE(consented_to_ai_at, TIMESTAMPTZ '2026-09-01 09:00:00+07'),
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = demo_student_id;

    IF NOT EXISTS (
        SELECT 1
        FROM assessment_attempts
        WHERE student_id = demo_student_id
          AND status = 'COMPLETED'
    ) THEN
        INSERT INTO assessment_attempts
            (student_id, status, started_at, completed_at)
        VALUES
            (demo_student_id, 'COMPLETED',
             TIMESTAMPTZ '2026-09-01 09:10:00+07',
             TIMESTAMPTZ '2026-09-01 09:20:00+07')
        RETURNING id INTO demo_assessment_id;

        WITH ranked_questions AS (
            SELECT
                id,
                dimension,
                ROW_NUMBER() OVER (PARTITION BY dimension ORDER BY display_order) AS dimension_order
            FROM riasec_questions
            WHERE active = TRUE
        )
        INSERT INTO assessment_answers (attempt_id, question_id, score)
        SELECT
            demo_assessment_id,
            id,
            CASE dimension
                WHEN 'I' THEN CASE WHEN dimension_order <= 4 THEN 5 ELSE 4 END
                WHEN 'C' THEN CASE WHEN dimension_order = 1 THEN 5 ELSE 4 END
                WHEN 'R' THEN CASE WHEN dimension_order <= 5 THEN 4 ELSE 3 END
                WHEN 'A' THEN CASE WHEN dimension_order <= 3 THEN 4 ELSE 3 END
                WHEN 'S' THEN CASE WHEN dimension_order <= 2 THEN 4 ELSE 3 END
                ELSE 3
            END
        FROM ranked_questions;

        INSERT INTO assessment_scores (attempt_id, dimension, score)
        SELECT demo_assessment_id, question.dimension, SUM(answer.score)
        FROM assessment_answers answer
        JOIN riasec_questions question ON question.id = answer.question_id
        WHERE answer.attempt_id = demo_assessment_id
        GROUP BY question.dimension;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM simulation_attempts
        WHERE student_id = demo_student_id
          AND status = 'EVALUATED'
    ) THEN
        SELECT id
        INTO demo_simulation_id
        FROM career_simulations
        WHERE slug = 'backend-developer'
          AND status = 'PUBLISHED';

        INSERT INTO simulation_attempts
            (simulation_id, student_id, status, started_at, submitted_at, evaluated_at)
        VALUES
            (demo_simulation_id, demo_student_id, 'EVALUATED',
             TIMESTAMPTZ '2026-09-02 14:00:00+07',
             TIMESTAMPTZ '2026-09-02 14:25:00+07',
             TIMESTAMPTZ '2026-09-02 14:25:00+07')
        RETURNING id INTO demo_simulation_attempt_id;

        INSERT INTO task_submissions (attempt_id, task_id, answer_payload, submitted_at)
        SELECT
            demo_simulation_attempt_id,
            task.id,
            jsonb_build_object(
                'selectedOption',
                CASE task.display_order WHEN 1 THEN 'b' WHEN 2 THEN 'a' ELSE 'd' END
            ),
            TIMESTAMPTZ '2026-09-02 14:25:00+07'
        FROM simulation_tasks task
        WHERE task.simulation_id = demo_simulation_id;

        INSERT INTO evaluation_results
            (attempt_id, score, max_score, task_outcomes, evaluated_at)
        SELECT
            demo_simulation_attempt_id,
            COUNT(*) FILTER (
                WHERE task.evaluation_rule ->> 'correctOption' =
                    CASE task.display_order WHEN 1 THEN 'b' WHEN 2 THEN 'a' ELSE 'd' END
            ),
            COUNT(*),
            jsonb_agg(
                jsonb_build_object(
                    'taskId', task.id,
                    'title', task.title,
                    'selectedOption',
                        CASE task.display_order WHEN 1 THEN 'b' WHEN 2 THEN 'a' ELSE 'd' END,
                    'isCorrect',
                        task.evaluation_rule ->> 'correctOption' =
                            CASE task.display_order WHEN 1 THEN 'b' WHEN 2 THEN 'a' ELSE 'd' END,
                    'explanation', task.evaluation_rule ->> 'explanation'
                )
                ORDER BY task.display_order
            ),
            TIMESTAMPTZ '2026-09-02 14:25:00+07'
        FROM simulation_tasks task
        WHERE task.simulation_id = demo_simulation_id;
    END IF;
END $$;

