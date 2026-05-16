DROP DATABASE IF EXISTS edulink_db;
CREATE DATABASE edulink_db;
USE edulink_db;

-- Users table (base)
CREATE TABLE users (
                       user_id INT PRIMARY KEY AUTO_INCREMENT,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       user_type ENUM('student','company','admin') NOT NULL,
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students
CREATE TABLE students (
                          student_id INT PRIMARY KEY AUTO_INCREMENT,
                          user_id INT UNIQUE NOT NULL,
                          full_name VARCHAR(100) NOT NULL,
                          student_number VARCHAR(20) UNIQUE NOT NULL,
                          course VARCHAR(100) NOT NULL,
                          institution VARCHAR(100) NOT NULL,
                          graduation_year INT,
                          skills TEXT,
                          FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Companies
CREATE TABLE companies (
                           company_id INT PRIMARY KEY AUTO_INCREMENT,
                           user_id INT UNIQUE NOT NULL,
                           company_name VARCHAR(100) NOT NULL,
                           industry VARCHAR(50),
                           location VARCHAR(100),
                           website VARCHAR(255),
                           is_verified BOOLEAN DEFAULT FALSE,
                           FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Job postings
CREATE TABLE job_postings (
                              job_id INT PRIMARY KEY AUTO_INCREMENT,
                              company_id INT NOT NULL,
                              title VARCHAR(100) NOT NULL,
                              description TEXT,
                              requirements TEXT,
                              location VARCHAR(100),
                              job_type ENUM('full-time','internship','graduate') DEFAULT 'graduate',
                              application_deadline DATE,
                              is_active BOOLEAN DEFAULT TRUE,
                              posted_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE
);

-- Applications
CREATE TABLE applications (
                              application_id INT PRIMARY KEY AUTO_INCREMENT,
                              job_id INT NOT NULL,
                              student_id INT NOT NULL,
                              cover_letter TEXT,
                              status ENUM('pending','reviewed','accepted','rejected') DEFAULT 'pending',
                              applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (job_id) REFERENCES job_postings(job_id) ON DELETE CASCADE,
                              FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
                              UNIQUE KEY unique_app (job_id, student_id)
);

SHOW TABLES;