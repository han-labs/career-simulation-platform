ALTER TABLE assessment_attempts
    DROP CONSTRAINT chk_assessment_attempt_type;

ALTER TABLE assessment_attempts
    ADD CONSTRAINT chk_assessment_attempt_type
    CHECK (assessment_type IN ('RIASEC', 'INTERESTS', 'ABILITIES', 'WORK_ACTIVITIES'));

ALTER TABLE career_quiz_questions
    DROP CONSTRAINT career_quiz_questions_quiz_type_check;

ALTER TABLE career_quiz_questions
    ADD CONSTRAINT chk_career_quiz_questions_type
    CHECK (quiz_type IN ('INTERESTS', 'ABILITIES', 'WORK_ACTIVITIES'));

INSERT INTO career_quiz_questions (quiz_type, category, prompt, display_order) VALUES
('WORK_ACTIVITIES', 'INFORMATION', 'I prefer examining information to find patterns that can guide a decision.', 1),
('WORK_ACTIVITIES', 'INFORMATION', 'I prefer comparing possible solutions and weighing their trade-offs.', 2),
('WORK_ACTIVITIES', 'INFORMATION', 'I prefer organizing details into a clear sequence of steps.', 3),
('WORK_ACTIVITIES', 'PEOPLE', 'I prefer helping someone work through a task by understanding what they need.', 4),
('WORK_ACTIVITIES', 'PEOPLE', 'I prefer explaining information to someone who is new to a topic.', 5),
('WORK_ACTIVITIES', 'PEOPLE', 'I prefer coordinating with others to agree on how to move a project forward.', 6),
('WORK_ACTIVITIES', 'THINGS', 'I prefer turning an idea into a sketch, model, or working prototype.', 7),
('WORK_ACTIVITIES', 'THINGS', 'I prefer assembling or configuring tools and equipment to make something work.', 8),
('WORK_ACTIVITIES', 'THINGS', 'I prefer hands-on tasks that involve adjusting, repairing, or improving an object.', 9);
