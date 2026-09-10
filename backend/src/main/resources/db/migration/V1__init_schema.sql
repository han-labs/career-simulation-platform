CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('STUDENT', 'ENTERPRISE', 'ADMINISTRATOR')),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE student_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE RESTRICT,
    school_name VARCHAR(255),
    education_level VARCHAR(80),
    graduation_year SMALLINT,
    consented_to_ai_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE riasec_questions (
    id BIGSERIAL PRIMARY KEY,
    dimension CHAR(1) NOT NULL CHECK (dimension IN ('R', 'I', 'A', 'S', 'E', 'C')),
    prompt VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL UNIQUE CHECK (display_order > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE assessment_attempts (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE TABLE assessment_answers (
    attempt_id BIGINT NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES riasec_questions(id) ON DELETE RESTRICT,
    score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    PRIMARY KEY (attempt_id, question_id)
);

CREATE TABLE assessment_scores (
    attempt_id BIGINT NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    dimension CHAR(1) NOT NULL CHECK (dimension IN ('R', 'I', 'A', 'S', 'E', 'C')),
    score INTEGER NOT NULL CHECK (score >= 0),
    PRIMARY KEY (attempt_id, dimension)
);

CREATE TABLE career_simulations (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    career_track VARCHAR(60) NOT NULL,
    summary VARCHAR(600) NOT NULL,
    difficulty VARCHAR(30) NOT NULL CHECK (difficulty IN ('INTRODUCTORY', 'INTERMEDIATE', 'ADVANCED')),
    estimated_minutes INTEGER NOT NULL CHECK (estimated_minutes > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    provider_id BIGINT REFERENCES app_users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE simulation_tasks (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES career_simulations(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    instructions TEXT NOT NULL,
    task_type VARCHAR(30) NOT NULL CHECK (task_type IN ('MULTIPLE_CHOICE', 'SHORT_TEXT', 'CODE', 'ORDERING')),
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    max_score NUMERIC(6, 2) NOT NULL CHECK (max_score > 0),
    evaluation_rule JSONB NOT NULL,
    UNIQUE (simulation_id, display_order)
);

CREATE TABLE simulation_attempts (
    id BIGSERIAL PRIMARY KEY,
    simulation_id BIGINT NOT NULL REFERENCES career_simulations(id) ON DELETE RESTRICT,
    student_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EVALUATED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,
    evaluated_at TIMESTAMPTZ
);

CREATE TABLE task_submissions (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES simulation_attempts(id) ON DELETE CASCADE,
    task_id BIGINT NOT NULL REFERENCES simulation_tasks(id) ON DELETE RESTRICT,
    answer_payload JSONB NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (attempt_id, task_id)
);

CREATE TABLE evaluation_results (
    attempt_id BIGINT PRIMARY KEY REFERENCES simulation_attempts(id) ON DELETE CASCADE,
    score NUMERIC(6, 2) NOT NULL CHECK (score >= 0),
    max_score NUMERIC(6, 2) NOT NULL CHECK (max_score > 0),
    task_outcomes JSONB NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guidance_reports (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    assessment_attempt_id BIGINT REFERENCES assessment_attempts(id) ON DELETE RESTRICT,
    simulation_attempt_id BIGINT REFERENCES simulation_attempts(id) ON DELETE RESTRICT,
    source VARCHAR(30) NOT NULL CHECK (source IN ('AI', 'FALLBACK')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('READY', 'FAILED', 'REPLACED_BY_FALLBACK')),
    model_name VARCHAR(120),
    content JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (assessment_attempt_id IS NOT NULL OR simulation_attempt_id IS NOT NULL)
);

CREATE INDEX idx_assessment_attempts_student ON assessment_attempts(student_id, started_at DESC);
CREATE INDEX idx_simulations_catalog ON career_simulations(status, career_track);
CREATE INDEX idx_simulation_attempts_student ON simulation_attempts(student_id, started_at DESC);
CREATE INDEX idx_guidance_reports_student ON guidance_reports(student_id, created_at DESC);
