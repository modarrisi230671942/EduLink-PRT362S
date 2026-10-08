-- V2: Phase 2 features — CV upload, in-app notifications, application status tracking,
-- and indexes for the new server-side search / filtering.

-- CV upload: file is stored on disk (edulink.upload-dir); the database keeps its metadata
ALTER TABLE students
    ADD COLUMN cv_file_name VARCHAR(255) NULL,
    ADD COLUMN cv_original_name VARCHAR(255) NULL,
    ADD COLUMN cv_uploaded_at TIMESTAMP NULL;

-- When a company last changed an application's status (shown to students)
ALTER TABLE applications
    ADD COLUMN status_updated_at TIMESTAMP NULL;

-- In-app notifications (bell icon in the navbar)
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    type VARCHAR(40) NOT NULL,
    message VARCHAR(500) NOT NULL,
    link VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_jobs_active_deadline ON job_postings (is_active, application_deadline);
CREATE INDEX idx_applications_status ON applications (status);
