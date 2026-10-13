ALTER TABLE assessment_attempts
    ADD COLUMN assessment_type VARCHAR(30) NOT NULL DEFAULT 'RIASEC';

ALTER TABLE assessment_attempts
    ADD CONSTRAINT chk_assessment_attempt_type
    CHECK (assessment_type IN ('RIASEC', 'INTERESTS', 'ABILITIES'));

CREATE TABLE career_quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_type VARCHAR(30) NOT NULL CHECK (quiz_type IN ('INTERESTS', 'ABILITIES')),
    category VARCHAR(40) NOT NULL,
    prompt VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (quiz_type, display_order)
);

CREATE TABLE career_quiz_answers (
    attempt_id BIGINT NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES career_quiz_questions(id) ON DELETE RESTRICT,
    score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    PRIMARY KEY (attempt_id, question_id)
);

CREATE TABLE career_quiz_scores (
    attempt_id BIGINT NOT NULL REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    category VARCHAR(40) NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 3 AND 15),
    PRIMARY KEY (attempt_id, category)
);

CREATE INDEX idx_career_quiz_questions_type ON career_quiz_questions(quiz_type, active, display_order);
CREATE INDEX idx_career_quiz_answers_attempt ON career_quiz_answers(attempt_id);

INSERT INTO career_quiz_questions (quiz_type, category, prompt, display_order) VALUES
('INTERESTS', 'ANALYTICAL', 'I enjoy looking for patterns in information before deciding what to do.', 1),
('INTERESTS', 'ANALYTICAL', 'I like investigating why a process or system behaves in an unexpected way.', 2),
('INTERESTS', 'ANALYTICAL', 'I enjoy comparing evidence from several sources to answer a question.', 3),
('INTERESTS', 'CREATIVE', 'I like shaping an original idea into a useful digital experience.', 4),
('INTERESTS', 'CREATIVE', 'I enjoy finding different ways to explain or present information.', 5),
('INTERESTS', 'CREATIVE', 'I am interested in experimenting with designs and improving them.', 6),
('INTERESTS', 'COLLABORATIVE', 'I enjoy helping someone understand a technical idea.', 7),
('INTERESTS', 'COLLABORATIVE', 'I like gathering feedback and using it to improve a shared project.', 8),
('INTERESTS', 'COLLABORATIVE', 'I enjoy working with others to solve a problem.', 9),
('INTERESTS', 'ORGANIZING', 'I like arranging information so that it is easy to find and maintain.', 10),
('INTERESTS', 'ORGANIZING', 'I enjoy planning steps and checking that each one is complete.', 11),
('INTERESTS', 'ORGANIZING', 'I like keeping records accurate and following a clear process.', 12),
('ABILITIES', 'PROBLEM_SOLVING', 'I can break an unfamiliar problem into smaller questions to investigate.', 1),
('ABILITIES', 'PROBLEM_SOLVING', 'I can compare possible solutions and explain the trade-offs I notice.', 2),
('ABILITIES', 'PROBLEM_SOLVING', 'I can use clues from an error or result to decide what to check next.', 3),
('ABILITIES', 'COMMUNICATION', 'I can explain a technical step in language suited to the person asking.', 4),
('ABILITIES', 'COMMUNICATION', 'I can ask clear questions when a request or instruction is unclear.', 5),
('ABILITIES', 'COMMUNICATION', 'I can summarize progress and identify what input I need from others.', 6),
('ABILITIES', 'ORGANIZATION', 'I can keep track of tasks, deadlines, and the next action for each one.', 7),
('ABILITIES', 'ORGANIZATION', 'I can follow a checklist carefully and notice when a step is missing.', 8),
('ABILITIES', 'ORGANIZATION', 'I can keep files or notes arranged so I can return to them later.', 9),
('ABILITIES', 'TECHNICAL_LEARNING', 'I can learn a new digital tool by trying it and checking the outcome.', 10),
('ABILITIES', 'TECHNICAL_LEARNING', 'I can use documentation or examples to learn an unfamiliar feature.', 11),
('ABILITIES', 'TECHNICAL_LEARNING', 'I can apply feedback to improve a technical task on another attempt.', 12);
