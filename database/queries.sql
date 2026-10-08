-- =====================================================================================
--  EduLink — useful SQL queries
-- =====================================================================================
--  Run these in phpMyAdmin (XAMPP → SQL tab) or MySQL Workbench, one query at a time
--  (Workbench: put the cursor in a query and press Ctrl+Enter).
--
--  Everything an administrator does here can also be done in the Admin portal of the web app
--  (log in as admin@edulink.com / admin123). The web app is preferred because it also sends
--  notifications and writes the activity log; use SQL for reports, fixes and demos.
-- =====================================================================================

USE edulink_db;
SET NAMES utf8mb4;   -- keeps characters such as "—" intact in every SQL client


-- =====================================================================================
--  PART A — Reports (the team's original queries, updated)
-- =====================================================================================

-- A1. All applications with student names, job titles and companies
SELECT a.application_id, s.full_name, j.title, c.company_name, a.status, a.applied_date
FROM applications a
JOIN students s     ON a.student_id = s.student_id
JOIN job_postings j ON a.job_id = j.job_id
JOIN companies c    ON j.company_id = c.company_id
ORDER BY a.applied_date DESC;

-- A2. Job postings with company name and number of applications
SELECT j.job_id, j.title, c.company_name, j.job_type, j.application_deadline,
       COUNT(a.application_id) AS applications
FROM job_postings j
JOIN companies c         ON j.company_id = c.company_id
LEFT JOIN applications a ON j.job_id = a.job_id
GROUP BY j.job_id, j.title, c.company_name, j.job_type, j.application_deadline
ORDER BY applications DESC;

-- A3. Students whose skills mention SQL, next to job 1's requirements
--     (the web app does proper skill matching; see "Match %" on every job)
SELECT s.full_name, s.skills, j.title, j.requirements
FROM students s
CROSS JOIN job_postings j
WHERE j.job_id = 1 AND s.skills LIKE '%SQL%';

-- A4. Admin view: all users with their role and display name
SELECT u.user_id, u.email, u.user_type, u.is_active, u.created_at,
       COALESCE(s.full_name, c.company_name, 'Administrator') AS display_name
FROM users u
LEFT JOIN students s  ON u.user_id = s.user_id
LEFT JOIN companies c ON u.user_id = c.user_id
ORDER BY u.user_type, u.user_id;

-- A5. Application status summary per company and job
SELECT c.company_name, j.title, a.status, COUNT(*) AS total
FROM applications a
JOIN job_postings j ON a.job_id = j.job_id
JOIN companies c    ON j.company_id = c.company_id
GROUP BY c.company_name, j.title, a.status
ORDER BY c.company_name, j.title;

-- A6. Open vacancies right now (what students see on "Find jobs")
SELECT j.title, c.company_name, j.location, j.job_type, j.application_deadline
FROM job_postings j
JOIN companies c ON j.company_id = c.company_id
WHERE j.is_active = TRUE
  AND c.is_verified = TRUE
  AND (j.application_deadline IS NULL OR j.application_deadline >= CURDATE())
ORDER BY j.application_deadline;

-- A7. Applications per month (the line chart on the admin dashboard)
SELECT DATE_FORMAT(applied_date, '%Y-%m') AS month, COUNT(*) AS applications
FROM applications
GROUP BY month
ORDER BY month;

-- A8. Acceptance rate per company
SELECT c.company_name,
       COUNT(*) AS applications,
       SUM(a.status = 'accepted') AS accepted,
       ROUND(100 * SUM(a.status = 'accepted') / COUNT(*), 1) AS acceptance_rate_percent
FROM applications a
JOIN job_postings j ON a.job_id = j.job_id
JOIN companies c    ON j.company_id = c.company_id
GROUP BY c.company_name
ORDER BY acceptance_rate_percent DESC;

-- A9. Upcoming interviews
SELECT s.full_name, j.title, c.company_name, i.mode, i.status, i.confirmed_start, i.location
FROM interviews i
JOIN applications a ON i.application_id = a.application_id
JOIN students s     ON a.student_id = s.student_id
JOIN job_postings j ON a.job_id = j.job_id
JOIN companies c    ON j.company_id = c.company_id
WHERE i.status <> 'CANCELLED'
ORDER BY COALESCE(i.confirmed_start, i.created_at);

-- A10. Most bookmarked (saved) jobs
SELECT j.title, c.company_name, COUNT(*) AS times_saved
FROM saved_jobs sj
JOIN job_postings j ON sj.job_id = j.job_id
JOIN companies c    ON j.company_id = c.company_id
GROUP BY j.job_id, j.title, c.company_name
ORDER BY times_saved DESC;


-- =====================================================================================
--  PART B — Admin tasks
-- =====================================================================================

-- B1. Companies waiting for verification (they cannot post jobs yet)
SELECT c.company_id, c.company_name, u.email, u.created_at
FROM companies c
JOIN users u ON c.user_id = u.user_id
WHERE c.is_verified = FALSE;

-- B2. Verify a company (change the email to the company you want to approve)
UPDATE companies
SET is_verified = TRUE
WHERE user_id = (SELECT user_id FROM users WHERE email = 'greenbuild@test.com');

-- B3. Deactivate an account (the user is locked out immediately) ...
UPDATE users SET is_active = FALSE WHERE email = 'bob.student@test.com';
-- ... and reactivate it
UPDATE users SET is_active = TRUE  WHERE email = 'bob.student@test.com';

-- B4. Create an extra administrator (password: admin123)
INSERT IGNORE INTO users (email, password_hash, user_type, is_active)
VALUES ('careers.office@edulink.com', '$2a$10$0B.lxOqgb9Bk3k/8gihczOD0Mo9oQ7qCoyTRvtWM9GTgtUUDmB6he', 'admin', TRUE);

-- B5. Reset a forgotten password to "password123"
--     (passwords are BCrypt hashes; a plain-text password in this column will NOT work)
UPDATE users
SET password_hash = '$2a$10$VA0ZeO9cIdc6ucZwrtLTcultUHc6BfZhvkYsubv87L4en47itXD0K'
WHERE email = 'alice.student@test.com';
--     Note: after 5 wrong passwords an email is locked for 15 minutes. That lock is kept in the
--     backend's memory, so restarting the backend also clears it.

-- B6. Activity log: the latest 50 security events (logins, failed logins, lockouts, admin actions)
SELECT created_at, action, actor_email, target_type, target_id, details, ip_address
FROM audit_log
ORDER BY created_at DESC
LIMIT 50;

-- B7. Failed logins per email (spot brute-force attempts)
SELECT actor_email, COUNT(*) AS failed_logins, MAX(created_at) AS last_attempt
FROM audit_log
WHERE action = 'LOGIN_FAILED'
GROUP BY actor_email
ORDER BY failed_logins DESC;

-- B8. Security check: every password is a BCrypt hash (starts with $2a$ or $2b$). Should return 0 rows.
SELECT email FROM users WHERE password_hash NOT LIKE '$2a$%' AND password_hash NOT LIKE '$2b$%';


-- =====================================================================================
--  PART C — Database maintenance
-- =====================================================================================

-- C1. Which database migrations have been applied (Flyway keeps this table up to date)
SELECT installed_rank, version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- C2. Start completely fresh (deletes ALL data!):
--     1. stop the backend  2. run the line below  3. start the backend again.
--     Flyway recreates every table and the core demo data; then run database/demo_data.sql for the rest.
-- DROP DATABASE edulink_db;
