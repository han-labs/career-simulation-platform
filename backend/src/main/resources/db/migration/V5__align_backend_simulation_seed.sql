-- Align the backend simulation seed with the US-02 frontend mock.
UPDATE career_simulations
SET
    slug = 'backend-developer',
    title = 'Backend Developer',
    summary = 'Investigate API behavior, database queries, and common backend failures.'
WHERE slug = 'backend-api-triage';

UPDATE simulation_tasks
SET
    title = 'Interpreting an API response',
    instructions = 'An API returns HTTP 404. What does this usually mean?',
    max_score = 1,
    evaluation_rule = jsonb_build_object(
        'strategy', 'exact_option',
        'options', jsonb_build_array(
            jsonb_build_object(
                'id', 'a',
                'label', 'The server successfully created a resource.'
            ),
            jsonb_build_object(
                'id', 'b',
                'label', 'The requested resource was not found.'
            ),
            jsonb_build_object(
                'id', 'c',
                'label', 'The user is authenticated but forbidden.'
            ),
            jsonb_build_object(
                'id', 'd',
                'label', 'The server timed out.'
            )
        ),
        'correctOption', 'b',
        'explanation', 'HTTP 404 indicates that the requested resource could not be found.'
    )
WHERE simulation_id = (
    SELECT id
    FROM career_simulations
    WHERE slug = 'backend-developer'
)
AND display_order = 1;

UPDATE simulation_tasks
SET
    title = 'Selecting an appropriate SQL query',
    instructions = 'Which query returns every row from the users table?',
    max_score = 1,
    evaluation_rule = jsonb_build_object(
        'strategy', 'exact_option',
        'options', jsonb_build_array(
            jsonb_build_object(
                'id', 'a',
                'label', 'SELECT * FROM users;'
            ),
            jsonb_build_object(
                'id', 'b',
                'label', 'GET ALL users;'
            ),
            jsonb_build_object(
                'id', 'c',
                'label', 'FIND users;'
            ),
            jsonb_build_object(
                'id', 'd',
                'label', 'RETURN users;'
            )
        ),
        'correctOption', 'a',
        'explanation', 'SELECT * FROM users is the SQL statement that retrieves all rows and columns.'
    )
WHERE simulation_id = (
    SELECT id
    FROM career_simulations
    WHERE slug = 'backend-developer'
)
AND display_order = 2;

UPDATE simulation_tasks
SET
    title = 'Identifying a backend error',
    instructions = 'A request fails because required input is missing. Which HTTP status fits best?',
    max_score = 1,
    evaluation_rule = jsonb_build_object(
        'strategy', 'exact_option',
        'options', jsonb_build_array(
            jsonb_build_object(
                'id', 'a',
                'label', '200 OK'
            ),
            jsonb_build_object(
                'id', 'b',
                'label', '400 Bad Request'
            ),
            jsonb_build_object(
                'id', 'c',
                'label', '404 Not Found'
            ),
            jsonb_build_object(
                'id', 'd',
                'label', '500 Internal Server Error'
            )
        ),
        'correctOption', 'b',
        'explanation', 'HTTP 400 communicates that the submitted request is invalid or incomplete.'
    )
WHERE simulation_id = (
    SELECT id
    FROM career_simulations
    WHERE slug = 'backend-developer'
)
AND display_order = 3;
