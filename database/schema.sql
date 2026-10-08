-- =====================================================================================
--  EduLink — complete database schema (REFERENCE COPY)
-- =====================================================================================
--  You do NOT need to run this to use EduLink. The backend creates and upgrades the real
--  database (edulink_db) automatically with Flyway migrations:
--      backend/src/main/resources/db/migration   (V1 – V6, plus V4 in backend/src/main/java/db/migration)
--
--  This file is the combined result of all those migrations, in one readable place. Use it for:
--    • documentation and reports
--    • an ER diagram in MySQL Workbench: run this script, then Database → Reverse Engineer
--      → choose "edulink_schema_reference"
--
--  It deliberately creates a SEPARATE database (edulink_schema_reference), so running it can
--  never interfere with the application's own edulink_db.
-- =====================================================================================

CREATE DATABASE IF NOT EXISTS edulink_schema_reference;
USE edulink_schema_reference;

-- Login accounts for every role
CREATE TABLE users (
    user_id       INT PRIMARY KEY AUTO_INCREMENT,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,                 -- BCrypt hash, never the raw password
    user_type     ENUM('student','company','admin') NOT NULL,
    is_active     BOOLEAN DEFAULT TRUE,                  -- FALSE = locked out by an admin
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Student profile (one per student user)
CREATE TABLE students (
    student_id       INT PRIMARY KEY AUTO_INCREMENT,
    user_id          INT UNIQUE NOT NULL,
    full_name        VARCHAR(100) NOT NULL,
    student_number   VARCHAR(20) UNIQUE NOT NULL,
    course           VARCHAR(100) NOT NULL,
    institution      VARCHAR(100) NOT NULL,
    graduation_year  INT,
    skills           TEXT,                               -- comma-separated, used for skill matching
    cv_file_name     VARCHAR(255) NULL,                  -- random UUID file name on disk
    cv_original_name VARCHAR(255) NULL,
    cv_uploaded_at   TIMESTAMP NULL,
    job_alerts       BOOLEAN NOT NULL DEFAULT TRUE,      -- notify about new jobs matching ≥ 60% of skills
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Employer profile (one per company user)
CREATE TABLE companies (
    company_id   INT PRIMARY KEY AUTO_INCREMENT,
    user_id      INT UNIQUE NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    industry     VARCHAR(50),
    location     VARCHAR(100),
    website      VARCHAR(255),
    is_verified  BOOLEAN DEFAULT FALSE,                  -- must be TRUE (set by an admin) before posting jobs
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Vacancies
CREATE TABLE job_postings (
    job_id               INT PRIMARY KEY AUTO_INCREMENT,
    company_id           INT NOT NULL,
    title                VARCHAR(100) NOT NULL,
    description          TEXT,
    requirements         TEXT,                           -- comma-separated, used for skill matching
    location             VARCHAR(100),
    job_type             ENUM('full-time','internship','graduate') DEFAULT 'graduate',
    application_deadline DATE,
    is_active            BOOLEAN DEFAULT TRUE,
    posted_date          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE
);

-- A student's application for a vacancy
CREATE TABLE applications (
    application_id    INT PRIMARY KEY AUTO_INCREMENT,
    job_id            INT NOT NULL,
    student_id        INT NOT NULL,
    cover_letter      TEXT,
    status            ENUM('pending','reviewed','accepted','rejected') DEFAULT 'pending',
    applied_date      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status_updated_at TIMESTAMP NULL,
    FOREIGN KEY (job_id) REFERENCES job_postings(job_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    UNIQUE KEY unique_app (job_id, student_id)           -- one application per student per job
);

-- In-app notifications (bell icon, pushed live over WebSocket)
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id         INT NOT NULL,
    type            VARCHAR(40) NOT NULL,                -- e.g. APPLICATION_RECEIVED, INTERVIEW_PROPOSED, JOB_MATCH
    message         VARCHAR(500) NOT NULL,
    link            VARCHAR(255),                        -- page to open when clicked
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Interview for an application: the company proposes up to 3 slots, the student confirms one
CREATE TABLE interviews (
    interview_id     INT PRIMARY KEY AUTO_INCREMENT,
    application_id   INT NOT NULL UNIQUE,
    mode             VARCHAR(20) NOT NULL,               -- ONLINE, IN_PERSON, PHONE
    location         VARCHAR(255),                       -- meeting link, address or phone number
    notes            VARCHAR(1000),
    duration_minutes INT NOT NULL DEFAULT 45,
    status           VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',   -- PROPOSED, CONFIRMED, CANCELLED
    confirmed_start  DATETIME NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NULL,
    FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE
);

CREATE TABLE interview_slots (
    slot_id      INT PRIMARY KEY AUTO_INCREMENT,
    interview_id INT NOT NULL,
    starts_at    DATETIME NOT NULL,
    FOREIGN KEY (interview_id) REFERENCES interviews(interview_id) ON DELETE CASCADE
);

-- Jobs a student has bookmarked
CREATE TABLE saved_jobs (
    student_id INT NOT NULL,
    job_id     INT NOT NULL,
    saved_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, job_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES job_postings(job_id) ON DELETE CASCADE
);

-- Security & administration activity log. Deliberately has NO foreign keys: it is a history
-- record that must survive user deletion and never block the action being logged (see V6).
CREATE TABLE audit_log (
    log_id        INT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id INT NULL,                              -- who did it (NULL for unknown emails)
    actor_email   VARCHAR(100),
    action        VARCHAR(40) NOT NULL,                  -- LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, COMPANY_VERIFIED...
    target_type   VARCHAR(30),
    target_id     INT NULL,
    details       VARCHAR(500),
    ip_address    VARCHAR(45),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_jobs_active_deadline    ON job_postings (is_active, application_deadline);
CREATE INDEX idx_applications_status     ON applications (status);
CREATE INDEX idx_audit_created           ON audit_log (created_at);
CREATE INDEX idx_audit_action            ON audit_log (action);
CREATE INDEX idx_audit_actor             ON audit_log (actor_user_id);
