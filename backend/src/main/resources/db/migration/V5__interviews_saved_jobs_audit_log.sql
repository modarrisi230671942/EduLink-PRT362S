-- V5: Interview scheduling, saved jobs, job-alert preference and the admin activity (audit) log.

-- A company proposes up to three time slots; the student confirms one
CREATE TABLE interviews (
    interview_id INT PRIMARY KEY AUTO_INCREMENT,
    application_id INT NOT NULL UNIQUE,
    mode VARCHAR(20) NOT NULL,                 -- ONLINE, IN_PERSON, PHONE
    location VARCHAR(255),                     -- meeting link, address or phone number
    notes VARCHAR(1000),
    duration_minutes INT NOT NULL DEFAULT 45,
    status VARCHAR(20) NOT NULL DEFAULT 'PROPOSED',   -- PROPOSED, CONFIRMED, CANCELLED
    confirmed_start DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    FOREIGN KEY (application_id) REFERENCES applications(application_id) ON DELETE CASCADE
);

CREATE TABLE interview_slots (
    slot_id INT PRIMARY KEY AUTO_INCREMENT,
    interview_id INT NOT NULL,
    starts_at DATETIME NOT NULL,
    FOREIGN KEY (interview_id) REFERENCES interviews(interview_id) ON DELETE CASCADE
);

-- Jobs a student has bookmarked
CREATE TABLE saved_jobs (
    student_id INT NOT NULL,
    job_id INT NOT NULL,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, job_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES job_postings(job_id) ON DELETE CASCADE
);

-- Students choose whether to be notified about newly posted jobs that match their skills
ALTER TABLE students ADD COLUMN job_alerts BOOLEAN NOT NULL DEFAULT TRUE;

-- Security & administration audit trail (who did what, when, from where)
CREATE TABLE audit_log (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id INT NULL,
    actor_email VARCHAR(100),
    action VARCHAR(40) NOT NULL,
    target_type VARCHAR(30),
    target_id INT NULL,
    details VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_created ON audit_log (created_at);
CREATE INDEX idx_audit_action ON audit_log (action);
