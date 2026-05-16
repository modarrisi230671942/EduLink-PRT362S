USE edulink_db;

-- Insert users
INSERT INTO users (email, password_hash, user_type) VALUES
                                                        ('alice.student@test.com', 'hash1', 'student'),
                                                        ('bob.student@test.com', 'hash2', 'student'),
                                                        ('techcompany@test.com', 'hash3', 'company');

-- Insert students
INSERT INTO students (user_id, full_name, student_number, course, institution, graduation_year, skills) VALUES
                                                                                                            (1, 'Alice Mbatha', 'STU001', 'Computer Science', 'CPUT', 2026, 'Java, SQL, Python'),
                                                                                                            (2, 'Bob Ndlovu', 'STU002', 'Information Technology', 'UCT', 2025, 'JavaScript, SQL, React');

-- Insert company
INSERT INTO companies (user_id, company_name, industry, location, is_verified) VALUES
    (3, 'TechCorp', 'Software', 'Cape Town', TRUE);

-- Insert job postings
INSERT INTO job_postings (company_id, title, description, requirements, location, job_type, application_deadline) VALUES
                                                                                                                      (1, 'Junior Developer', 'Build web apps', 'Java or Python, SQL', 'Cape Town', 'graduate', '2026-08-30'),
                                                                                                                      (1, 'Database Intern', 'Help with SQL', 'Basic SQL, attention to detail', 'Cape Town', 'internship', '2026-09-15');

-- Insert applications
INSERT INTO applications (job_id, student_id, cover_letter, status) VALUES
                                                                        (1, 1, 'I know Java and SQL', 'pending'),
                                                                        (2, 2, 'I want to learn databases', 'reviewed');

SELECT 'Data inserted successfully' AS Status;