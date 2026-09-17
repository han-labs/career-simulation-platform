CREATE TABLE syn_sessions (
    id UUID PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    summary VARCHAR(1200) NOT NULL DEFAULT '',
    last_intent VARCHAR(40),
    last_paths JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days')
);

CREATE INDEX idx_syn_sessions_student_updated
    ON syn_sessions(student_id, updated_at DESC);

CREATE TABLE career_path_profiles (
    code VARCHAR(60) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    riasec_codes VARCHAR(12) NOT NULL,
    typical_activities JSONB NOT NULL,
    skill_codes JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL UNIQUE,
    skill_code VARCHAR(80) NOT NULL,
    career_track VARCHAR(60) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    estimated_minutes INTEGER NOT NULL CHECK (estimated_minutes > 0),
    resource_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('REVIEWED', 'ARCHIVED')),
    reviewed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE simulation_task_skills (
    task_id BIGINT NOT NULL REFERENCES simulation_tasks(id) ON DELETE CASCADE,
    skill_code VARCHAR(80) NOT NULL,
    skill_name VARCHAR(120) NOT NULL,
    PRIMARY KEY (task_id, skill_code)
);

INSERT INTO career_path_profiles
    (code, title, summary, riasec_codes, typical_activities, skill_codes, status)
VALUES
    ('BACKEND_DEVELOPMENT', 'Backend development',
     'Build and diagnose server-side APIs, data access, and application rules.', 'IRC',
     '["API design","Database work","Debugging server behavior"]'::jsonb,
     '["api_reasoning","sql","backend_debugging"]'::jsonb, 'ACTIVE'),
    ('FRONTEND_DEVELOPMENT', 'Frontend development',
     'Create usable interfaces and translate product behavior into accessible browser experiences.', 'AIC',
     '["Interface implementation","Accessibility review","Browser debugging"]'::jsonb,
     '["semantic_html","accessibility","ui_feedback"]'::jsonb, 'ACTIVE'),
    ('DATA_ANALYSIS', 'Data analysis',
     'Investigate datasets, improve data quality, and communicate reproducible findings.', 'ICR',
     '["Data cleaning","Quality validation","Pattern investigation"]'::jsonb,
     '["data_quality","deduplication","data_validation"]'::jsonb, 'ACTIVE'),
    ('SOFTWARE_TESTING', 'Software testing',
     'Explore product risk through focused test design, evidence, and defect diagnosis.', 'CIR',
     '["Boundary analysis","Regression planning","Failure diagnosis"]'::jsonb,
     '["boundary_testing","risk_testing","test_diagnosis"]'::jsonb, 'ACTIVE'),
    ('CYBERSECURITY', 'Cybersecurity',
     'Recognize threats, preserve evidence, and choose proportionate defensive actions.', 'IRC',
     '["Incident triage","Evidence preservation","Credential response"]'::jsonb,
     '["incident_triage","evidence_handling","credential_security"]'::jsonb, 'ACTIVE');

INSERT INTO learning_resources
    (title, provider, url, skill_code, career_track, difficulty, estimated_minutes,
     resource_type, status, reviewed_at)
VALUES
    ('Building a RESTful Web Service', 'Spring',
     'https://spring.io/guides/gs/rest-service', 'api_reasoning', 'BACKEND_DEVELOPMENT',
     'INTRODUCTORY', 45, 'GUIDE', 'REVIEWED', CURRENT_TIMESTAMP),
    ('PostgreSQL Tutorial', 'PostgreSQL',
     'https://www.postgresql.org/docs/current/tutorial.html', 'sql', 'BACKEND_DEVELOPMENT',
     'INTRODUCTORY', 60, 'DOCUMENTATION', 'REVIEWED', CURRENT_TIMESTAMP),
    ('HTML: A good basis for accessibility', 'MDN',
     'https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Accessibility/HTML',
     'accessibility', 'FRONTEND_DEVELOPMENT', 'INTRODUCTORY', 35, 'GUIDE', 'REVIEWED', CURRENT_TIMESTAMP),
    ('WAI ARIA Authoring Practices Guide', 'W3C WAI',
     'https://www.w3.org/WAI/ARIA/apg/', 'semantic_html', 'FRONTEND_DEVELOPMENT',
     'INTERMEDIATE', 45, 'REFERENCE', 'REVIEWED', CURRENT_TIMESTAMP),
    ('Intro to data structures', 'pandas',
     'https://pandas.pydata.org/docs/user_guide/dsintro.html', 'data_quality', 'DATA_ANALYSIS',
     'INTRODUCTORY', 45, 'DOCUMENTATION', 'REVIEWED', CURRENT_TIMESTAMP),
    ('JUnit 5 User Guide', 'JUnit',
     'https://junit.org/junit5/docs/current/user-guide/', 'boundary_testing', 'SOFTWARE_TESTING',
     'INTRODUCTORY', 45, 'DOCUMENTATION', 'REVIEWED', CURRENT_TIMESTAMP),
    ('Writing tests', 'Playwright',
     'https://playwright.dev/docs/writing-tests', 'risk_testing', 'SOFTWARE_TESTING',
     'INTRODUCTORY', 30, 'GUIDE', 'REVIEWED', CURRENT_TIMESTAMP),
    ('OWASP Top 10', 'OWASP',
     'https://owasp.org/www-project-top-ten/', 'incident_triage', 'CYBERSECURITY',
     'INTRODUCTORY', 45, 'REFERENCE', 'REVIEWED', CURRENT_TIMESTAMP);

WITH mapping(simulation_slug, display_order, skill_code, skill_name) AS (
    VALUES
        ('backend-developer', 1, 'api_reasoning', 'API response analysis'),
        ('backend-developer', 2, 'sql', 'SQL querying'),
        ('backend-developer', 3, 'backend_debugging', 'Backend error diagnosis'),
        ('frontend-accessibility-review', 1, 'semantic_html', 'Semantic HTML'),
        ('frontend-accessibility-review', 2, 'accessibility', 'Keyboard accessibility'),
        ('frontend-accessibility-review', 3, 'ui_feedback', 'Accessible feedback'),
        ('data-quality-investigation', 1, 'data_quality', 'Missing-value analysis'),
        ('data-quality-investigation', 2, 'deduplication', 'Duplicate handling'),
        ('data-quality-investigation', 3, 'data_validation', 'Data validation'),
        ('software-test-design', 1, 'boundary_testing', 'Boundary testing'),
        ('software-test-design', 2, 'risk_testing', 'Risk-based testing'),
        ('software-test-design', 3, 'test_diagnosis', 'Test failure diagnosis'),
        ('security-incident-first-response', 1, 'incident_triage', 'Incident triage'),
        ('security-incident-first-response', 2, 'evidence_handling', 'Evidence preservation'),
        ('security-incident-first-response', 3, 'credential_security', 'Credential response')
)
INSERT INTO simulation_task_skills (task_id, skill_code, skill_name)
SELECT task.id, mapping.skill_code, mapping.skill_name
FROM mapping
JOIN career_simulations simulation ON simulation.slug = mapping.simulation_slug
JOIN simulation_tasks task
  ON task.simulation_id = simulation.id AND task.display_order = mapping.display_order;
