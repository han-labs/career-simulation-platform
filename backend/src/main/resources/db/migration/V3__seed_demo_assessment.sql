INSERT INTO app_users (id, email, password_hash, display_name, role, status)
VALUES (1, 'student@demo.com', '$2a$10$dummyhash', 'Demo Student', 'STUDENT', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

INSERT INTO student_profiles (user_id, school_name, education_level, graduation_year)
VALUES (1, 'Demo University', 'UNDERGRADUATE', 2026)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO riasec_questions (dimension, prompt, display_order, active)
VALUES
    ('R', 'I like to work with tools, machines, or equipment.', 1, true),
    ('I', 'I like to solve complex math or science problems.', 2, true),
    ('A', 'I like to express myself through art, music, or writing.', 3, true),
    ('S', 'I like to teach, train, or help others.', 4, true),
    ('E', 'I like to lead a team or manage projects.', 5, true),
    ('C', 'I like to organize files, data, or schedules.', 6, true),
    ('R', 'I enjoy building or fixing things with my hands.', 7, true),
    ('I', 'I enjoy analyzing data to find patterns.', 8, true),
    ('A', 'I enjoy designing user interfaces or graphics.', 9, true),
    ('S', 'I enjoy advising people on their career choices.', 10, true),
    ('E', 'I enjoy giving presentations to influence others.', 11, true),
    ('C', 'I enjoy keeping detailed and accurate records.', 12, true);
