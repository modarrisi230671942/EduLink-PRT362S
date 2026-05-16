USE edulink_db;

-- Query 1: All applications with student names and job titles
SELECT a.application_id, s.full_name, j.title, a.status, a.applied_date
FROM applications a
         JOIN students s ON a.student_id = s.student_id
         JOIN job_postings j ON a.job_id = j.job_id;

-- Query 2: Job postings with company name and application count
SELECT j.title, c.company_name, COUNT(a.application_id) AS app_count
FROM job_postings j
         JOIN companies c ON j.company_id = c.company_id
         LEFT JOIN applications a ON j.job_id = a.job_id
GROUP BY j.job_id, j.title, c.company_name;

-- Query 3: Students matching a specific job's skill requirement
SELECT s.full_name, s.skills, j.title
FROM students s
         CROSS JOIN job_postings j
WHERE j.job_id = 1 AND s.skills LIKE '%SQL%';

-- Query 4: Admin view - all users with their type
SELECT u.user_id, u.email, u.user_type,
       COALESCE(s.full_name, c.company_name, 'Admin') AS display_name
FROM users u
         LEFT JOIN students s ON u.user_id = s.user_id
         LEFT JOIN companies c ON u.user_id = c.user_id;

-- Query 5: Application status summary per company
SELECT c.company_name, j.title, a.status, COUNT(*) AS total
FROM applications a
         JOIN job_postings j ON a.job_id = j.job_id
         JOIN companies c ON j.company_id = c.company_id
GROUP BY c.company_name, j.title, a.status;