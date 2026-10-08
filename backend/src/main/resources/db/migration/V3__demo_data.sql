-- V3: Demo data for development and the project presentation.
-- Rows are matched by natural keys (email, student number, job title), so this is safe to run
-- on a database that already contains the original sample data: existing rows are skipped.
-- Passwords are inserted as plaintext here and immediately hashed with BCrypt by V4.

-- ---------- Users ----------
INSERT IGNORE INTO users (email, password_hash, user_type, created_at) VALUES
('alice.student@test.com',  'password123', 'student', '2026-03-02 09:00:00'),
('bob.student@test.com',    'password123', 'student', '2026-03-05 10:30:00'),
('thandi.student@test.com', 'password123', 'student', '2026-04-11 14:15:00'),
('sipho.student@test.com',  'password123', 'student', '2026-05-20 08:45:00'),
('lerato.student@test.com', 'password123', 'student', '2026-06-08 16:20:00'),
('techcompany@test.com',    'password123', 'company', '2026-02-15 11:00:00'),
('datawave@test.com',       'password123', 'company', '2026-03-18 13:00:00'),
('greenbuild@test.com',     'password123', 'company', '2026-09-01 09:30:00'),
('admin@edulink.com',       'admin123',    'admin',   '2026-01-10 08:00:00');

-- ---------- Students ----------
INSERT IGNORE INTO students (user_id, full_name, student_number, course, institution, graduation_year, skills)
SELECT user_id, 'Alice Mbatha', 'STU001', 'Computer Science', 'CPUT', 2026, 'Java, SQL, Python'
FROM users WHERE email = 'alice.student@test.com';
INSERT IGNORE INTO students (user_id, full_name, student_number, course, institution, graduation_year, skills)
SELECT user_id, 'Bob Ndlovu', 'STU002', 'Information Technology', 'UCT', 2025, 'JavaScript, SQL, React'
FROM users WHERE email = 'bob.student@test.com';
INSERT IGNORE INTO students (user_id, full_name, student_number, course, institution, graduation_year, skills)
SELECT user_id, 'Thandi Zulu', 'STU003', 'Data Science', 'Stellenbosch University', 2026, 'Python, SQL, Machine Learning, Statistics, Excel'
FROM users WHERE email = 'thandi.student@test.com';
INSERT IGNORE INTO students (user_id, full_name, student_number, course, institution, graduation_year, skills)
SELECT user_id, 'Sipho Dlamini', 'STU004', 'Software Development', 'CPUT', 2026, 'Java, Spring Boot, SQL, Git, HTML/CSS'
FROM users WHERE email = 'sipho.student@test.com';
INSERT IGNORE INTO students (user_id, full_name, student_number, course, institution, graduation_year, skills)
SELECT user_id, 'Lerato Mokoena', 'STU005', 'Network Engineering', 'Wits University', 2027, 'Linux, Networking, AWS, Bash'
FROM users WHERE email = 'lerato.student@test.com';

-- ---------- Companies ----------
INSERT IGNORE INTO companies (user_id, company_name, industry, location, website, is_verified)
SELECT user_id, 'TechCorp', 'Software Development', 'Cape Town', 'https://techcorp.example.com', TRUE
FROM users WHERE email = 'techcompany@test.com';
INSERT IGNORE INTO companies (user_id, company_name, industry, location, website, is_verified)
SELECT user_id, 'DataWave Analytics', 'Data & AI', 'Johannesburg', 'https://datawave.example.com', TRUE
FROM users WHERE email = 'datawave@test.com';
-- Deliberately unverified: demonstrates that the admin must verify a company before it can post jobs
INSERT IGNORE INTO companies (user_id, company_name, industry, location, website, is_verified)
SELECT user_id, 'GreenBuild Engineering', 'Civil Engineering', 'Durban', 'https://greenbuild.example.com', FALSE
FROM users WHERE email = 'greenbuild@test.com';

-- ---------- Job postings ----------
-- Helper pattern: company looked up by its user's email; skipped if the same title already exists for that company
INSERT INTO job_postings (company_id, title, description, requirements, location, job_type, application_deadline, is_active, posted_date)
SELECT c.company_id, v.title, v.description, v.requirements, v.location, v.job_type, v.deadline, TRUE, v.posted
FROM (
    SELECT 'techcompany@test.com' AS email, 'Junior Developer' AS title,
           'Join our engineering team to build cloud-based web applications with Java, Spring Boot and modern frontends.' AS description,
           'Java, Spring Boot, SQL, HTML/CSS' AS requirements, 'Cape Town' AS location, 'graduate' AS job_type,
           DATE '2026-12-30' AS deadline, TIMESTAMP '2026-03-10 09:00:00' AS posted
    UNION ALL SELECT 'techcompany@test.com', 'Database Intern',
           'Help optimise database schemas, write stored procedures, and assist with data migrations.',
           'SQL, Database Design, Attention to detail', 'Remote', 'internship', DATE '2026-11-15', TIMESTAMP '2026-03-12 09:00:00'
    UNION ALL SELECT 'techcompany@test.com', 'Frontend Developer (React)',
           'Build accessible, responsive user interfaces for our SaaS products using React and TypeScript.',
           'React, JavaScript, HTML/CSS, Git', 'Cape Town', 'graduate', DATE '2027-01-31', TIMESTAMP '2026-05-02 10:00:00'
    UNION ALL SELECT 'techcompany@test.com', 'QA Engineer',
           'Design and automate test suites for our web platform. Work closely with developers in an agile team.',
           'Testing, Java, Selenium, SQL', 'Cape Town', 'full-time', DATE '2026-12-20', TIMESTAMP '2026-06-15 10:00:00'
    UNION ALL SELECT 'techcompany@test.com', 'Winter Vacation Work',
           'Three-week winter vacation programme for second-year students. (Closed — deadline has passed.)',
           'Java, Teamwork', 'Cape Town', 'internship', DATE '2026-06-30', TIMESTAMP '2026-04-01 08:00:00'
    UNION ALL SELECT 'datawave@test.com', 'Data Analyst Intern',
           'Turn raw business data into dashboards and insights for our retail clients.',
           'Python, SQL, Excel, Power BI', 'Johannesburg', 'internship', DATE '2026-12-15', TIMESTAMP '2026-04-20 09:00:00'
    UNION ALL SELECT 'datawave@test.com', 'Machine Learning Graduate',
           'Join our AI team to build and deploy predictive models. Mentorship programme included.',
           'Python, Machine Learning, Statistics, SQL', 'Remote', 'graduate', DATE '2027-02-28', TIMESTAMP '2026-07-01 09:00:00'
    UNION ALL SELECT 'datawave@test.com', 'Cloud Support Engineer',
           'Support and automate our AWS cloud infrastructure. On-call rotation after onboarding.',
           'Linux, AWS, Networking, Bash', 'Cape Town', 'full-time', DATE '2026-11-30', TIMESTAMP '2026-08-05 09:00:00'
) v
JOIN users u ON u.email = v.email
JOIN companies c ON c.user_id = u.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM job_postings j WHERE j.company_id = c.company_id AND j.title = v.title
);

-- ---------- Applications ----------
-- Spread over several months so the admin analytics charts have a trend to show
INSERT IGNORE INTO applications (job_id, student_id, cover_letter, status, applied_date, status_updated_at)
SELECT j.job_id, s.student_id, v.cover_letter, v.status, v.applied, v.updated
FROM (
    SELECT 'Junior Developer' AS title, 'STU001' AS student_number,
           'I am Alice. I know Java and SQL and would love to join TechCorp!' AS cover_letter,
           'pending' AS status, TIMESTAMP '2026-04-02 10:00:00' AS applied, NULL AS updated
    UNION ALL SELECT 'Database Intern', 'STU002', 'I am Bob, studying at UCT. I want to improve my database skills.',
           'reviewed', TIMESTAMP '2026-04-18 11:00:00', TIMESTAMP '2026-04-25 09:00:00'
    UNION ALL SELECT 'Junior Developer', 'STU004', 'Spring Boot is my favourite framework and I built my final-year project with it.',
           'accepted', TIMESTAMP '2026-05-06 09:30:00', TIMESTAMP '2026-05-20 14:00:00'
    UNION ALL SELECT 'Data Analyst Intern', 'STU003', 'I have built several Power BI dashboards during my Data Science degree.',
           'accepted', TIMESTAMP '2026-05-22 13:00:00', TIMESTAMP '2026-06-03 10:00:00'
    UNION ALL SELECT 'Frontend Developer (React)', 'STU002', 'React is my strongest skill and I would love to grow it at TechCorp.',
           'reviewed', TIMESTAMP '2026-06-10 15:45:00', TIMESTAMP '2026-06-14 09:00:00'
    UNION ALL SELECT 'Database Intern', 'STU001', 'SQL is one of my core skills and I enjoy optimising queries.',
           'rejected', TIMESTAMP '2026-06-21 08:15:00', TIMESTAMP '2026-07-01 12:00:00'
    UNION ALL SELECT 'Machine Learning Graduate', 'STU003', 'My honours project was a machine learning model for crop yield prediction.',
           'pending', TIMESTAMP '2026-07-09 10:20:00', NULL
    UNION ALL SELECT 'QA Engineer', 'STU004', 'I wrote JUnit and Selenium tests for our capstone project.',
           'reviewed', TIMESTAMP '2026-07-28 16:00:00', TIMESTAMP '2026-08-02 09:00:00'
    UNION ALL SELECT 'Cloud Support Engineer', 'STU005', 'I hold an AWS Cloud Practitioner certificate and run my own Linux home lab.',
           'accepted', TIMESTAMP '2026-08-12 09:00:00', TIMESTAMP '2026-08-26 11:00:00'
    UNION ALL SELECT 'Data Analyst Intern', 'STU001', 'Python and SQL are my strengths and I am keen to learn Power BI.',
           'pending', TIMESTAMP '2026-08-30 14:30:00', NULL
    UNION ALL SELECT 'Machine Learning Graduate', 'STU001', 'I completed a machine learning short course and want to specialise in AI.',
           'pending', TIMESTAMP '2026-09-14 10:00:00', NULL
    UNION ALL SELECT 'Frontend Developer (React)', 'STU004', 'I have built responsive UIs with HTML/CSS and am learning React.',
           'pending', TIMESTAMP '2026-09-21 11:10:00', NULL
) v
JOIN job_postings j ON j.title = v.title
JOIN students s ON s.student_number = v.student_number;
