-- Remove previous mock questions
DELETE FROM riasec_questions;

-- Insert 42 standard RIASEC short-form questions
-- Realistic (R)
INSERT INTO riasec_questions (dimension, prompt, display_order, active) VALUES
('R', 'I like to work on cars or machinery.', 1, true),
('R', 'I enjoy building things with my hands.', 2, true),
('R', 'I like to assemble or repair equipment.', 3, true),
('R', 'I prefer outdoor physical activities over sitting at a desk.', 4, true),
('R', 'I like to operate heavy machinery or technical equipment.', 5, true),
('R', 'I enjoy working with electronic devices and hardware.', 6, true),
('R', 'I like to fix things around the house.', 7, true);

-- Investigative (I)
INSERT INTO riasec_questions (dimension, prompt, display_order, active) VALUES
('I', 'I enjoy solving complex math or science problems.', 8, true),
('I', 'I like to do research and discover new facts.', 9, true),
('I', 'I enjoy analyzing data to find patterns or trends.', 10, true),
('I', 'I like to read scientific articles or journals.', 11, true),
('I', 'I enjoy figuring out how complex systems work.', 12, true),
('I', 'I like conducting experiments to test ideas.', 13, true),
('I', 'I prefer intellectual challenges and theoretical problems.', 14, true);

-- Artistic (A)
INSERT INTO riasec_questions (dimension, prompt, display_order, active) VALUES
('A', 'I like to express myself through art, music, or writing.', 15, true),
('A', 'I enjoy designing new and creative solutions.', 16, true),
('A', 'I like attending concerts, theaters, or art exhibits.', 17, true),
('A', 'I prefer work that allows for imagination and originality.', 18, true),
('A', 'I enjoy creating visual designs or graphics.', 19, true),
('A', 'I like to play a musical instrument or sing.', 20, true),
('A', 'I enjoy writing stories, poems, or scripts.', 21, true);

-- Social (S)
INSERT INTO riasec_questions (dimension, prompt, display_order, active) VALUES
('S', 'I like to teach or train others.', 22, true),
('S', 'I enjoy helping people solve their personal problems.', 23, true),
('S', 'I like working in groups and collaborating on tasks.', 24, true),
('S', 'I prefer to mediate disputes and help people compromise.', 25, true),
('S', 'I enjoy caring for people who are sick or in need.', 26, true),
('S', 'I like volunteering for community service or charities.', 27, true),
('S', 'I enjoy giving advice and mentoring others.', 28, true);

-- Enterprising (E)
INSERT INTO riasec_questions (dimension, prompt, display_order, active) VALUES
('E', 'I like to lead teams and manage projects.', 29, true),
('E', 'I enjoy convincing people to buy a product or idea.', 30, true),
('E', 'I prefer taking risks to start a new business or venture.', 31, true),
('E', 'I like giving speeches or presentations to influence others.', 32, true),
('E', 'I enjoy directing the work of others and making decisions.', 33, true),
('E', 'I like negotiating deals or contracts.', 34, true),
('E', 'I enjoy striving for leadership roles in organizations.', 35, true);

-- Conventional (C)
INSERT INTO riasec_questions (dimension, prompt, display_order, active) VALUES
('C', 'I like to keep things highly organized and structured.', 36, true),
('C', 'I enjoy following clear rules and procedures.', 37, true),
('C', 'I prefer working with numbers, records, or precise data.', 38, true),
('C', 'I like to create schedules and plan details meticulously.', 39, true),
('C', 'I enjoy doing office work and managing files.', 40, true),
('C', 'I prefer tasks that require careful attention to detail.', 41, true),
('C', 'I like working with spreadsheets and financial records.', 42, true);
