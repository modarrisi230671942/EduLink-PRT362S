# Changelog

All notable changes to EduLink are recorded here, newest first.
For the reasoning behind each change, see [`SYSTEM_AUDIT.md`](SYSTEM_AUDIT.md). The IDs (C1, S3, Q2…) refer to findings in that report.

---

## [2.2.2] — 2026-09-28 — Logo, README, database scripts

### Added
- **EduLink logo files** in `docs/assets/`: the full logo (light and dark, SVG and PNG) and the app icon.
  - They are rebuilt from the original brand: the indigo `#4f46e5` "E" icon from the first desktop version, and the indigo/sky-blue "EduLink" wordmark from the first web version.
  - The letters are converted to shapes, so the logo looks the same without the Outfit font installed.
  - The browser-tab icon now uses the same Outfit "E" instead of Arial.
- **`database/demo_data.sql`.** Full demo data: 12 accounts, 10 vacancies, 15 applications, 3 interviews, saved jobs and notifications.
  - It uses BCrypt password hashes, so every account can log in.
  - It is safe to run repeatedly, and works in phpMyAdmin, Workbench and the command line.
- **`database/queries.sql`.** Keeps the team's original report queries, and adds admin tasks (verify a company, deactivate a user, create an admin, reset a password, read the activity log) and maintenance queries.
- **README screenshots** (`docs/screenshots/`), taken from the running system.

### Changed
- **README rewritten.** It covers:
  - what EduLink is, the problem it solves and who it is for
  - step-by-step setup with IntelliJ and VS Code, using XAMPP *or* MySQL Workbench
  - all demo accounts, the SQL scripts and the admin portal
  - a troubleshooting table
- **`database/schema.sql`** now covers every table (V1–V6). It creates a separate `edulink_schema_reference` database, so running it can never affect `edulink_db`.

### Fixed
- **Admin analytics said "75% acceptance rate" above "3 of 15 applications accepted".** The rate is accepted ÷ *decided* (accepted + rejected) applications, so the caption was wrong.
  - The API now also returns `decidedApplications`, and the caption reads "3 accepted of 4 decided".
  - New test in `FeatureIntegrationTest`: **48 backend tests**.

---

## [2.2.1] — 2026-09-28 — Fix: registration and password change hung

### Fixed
- **Registering a student or company, and changing a password, hung for 50 seconds and then failed on MySQL.** The browser showed "Cannot reach the EduLink server".
  - **Cause:** the activity-log entry is written in its own transaction. `audit_log.actor_user_id` had a foreign key to `users`, and the user row was not committed yet (new) or was locked (password change). So the log write waited for the action, while the action waited for the log write.
  - A second bug turned this into a failure. The log write is meant to be best-effort, but its failed transaction still aborted the user's action.
  - **Fixes:**
    - Migration **V6** removes that foreign key. An activity log is a history record and should not depend on live rows.
    - `AuditServiceImpl` now wraps the whole log transaction, including its commit, so a logging problem can never fail the user's action.
- A request that times out now says "The server took too long to respond" instead of claiming the server is offline.

### Tests
- The integration tests use a schema generated without that foreign key, so they could not see the bug, and no test covered a *successful* registration.
- New `AccountIntegrationTest` (student and company registration, password change) and `AuditServiceImplTest` (logging failures are swallowed): **47 backend tests**.
- Also verified by hand against MariaDB 10.4.

---

## [2.2.0] — 2026-09-28 — Java 25 LTS

### Changed
- **Java 17 → Java 25**, the current long-term-support (LTS) release. The Docker images and GitHub Actions CI use Temurin 25.
- **Spring Boot 3.2.5 → 3.5.16.** Spring Boot 3.2 cannot read Java 25 class files, and 3.5 supports Java 25 with no code changes. This also brings Hibernate 6.6 and Flyway 11. Flyway 11 accepted the existing database as-is ("schema up to date", no new migrations).
- Library updates: springdoc-openapi 2.5.0 → 2.8.17, jjwt 0.12.5 → 0.12.7, PDFBox 3.0.3 → 3.0.8.
- Mockito is now loaded as a Java agent during tests. Newer JDKs warn about, and will later block, agents that attach themselves at runtime.

### Verified
- All 41 backend tests pass on Java 25.
- Started against the real MySQL/MariaDB database:
  - all nine demo accounts log in
  - protected endpoints still return 401 without a token
  - Swagger UI loads
  - the WebSocket accepts a valid JWT and rejects a connection without one

---

## [2.1.0 / Phase 3] — 2026-09-28 — Real-time, interviews, CV intelligence, DevOps

### Added
- **Real-time notifications (WebSocket / STOMP).**
  - Notifications are pushed to the browser the moment they happen: they appear in the bell and as toasts.
  - Open pages refresh themselves. A student's application list updates as soon as the company acts, and the company's applicant list updates when someone applies.
  - Messages are sent only *after* the database transaction commits.
  - Connections must present a valid JWT, and each user can subscribe only to their own queue (`WebSocketAuthInterceptor`).
  - A 60-second fallback poll covers networks that block WebSockets.
- **Interview scheduling.**
  - Companies propose 1-3 time slots (online, in person or phone) with a meeting link or address and notes.
  - The student confirms one with a click.
  - Both sides can cancel, and both can download a calendar invite (`.ics`) with a 30-minute reminder for Google Calendar, Outlook and Apple Calendar.
  - Scheduling moves the application to "Under review". Declining a candidate cancels any planned interview.
  - New *Interviews* page for companies and an *Upcoming interviews* panel for students.
- **Skills from your CV.** EduLink reads the uploaded PDF with Apache PDFBox and matches it against a dictionary of about 170 skills with aliases (e.g. "JS" → JavaScript, "k8s" → Kubernetes). Suggestions appear right after upload and can be added with one click. It runs fully offline, with no external AI service. The dictionary is a plain text file the team can extend.
- **Job alerts.** When a vacancy is posted, every opted-in student whose skills match at least 60% is notified. There is a switch on the profile page.
- **Saved jobs.** Students can bookmark jobs from any card or job view. There is a new *Saved* page.
- **Admin activity log (audit trail).** Records:
  - successful and failed logins, account lockouts, registrations and password changes
  - company verification and revocation, account enable/disable, and jobs removed by an admin

  Each entry has time and IP address. The log is written in its own transaction, so failed actions are still recorded. There is a filterable *Activity log* page.
- **Dark mode.** Uses Bootstrap 5.3 colour modes plus a full set of EduLink colour variables. It follows the OS setting until toggled, with no white flash on load. Charts use dark-surface colours.
- **Docker.**
  - `docker compose up --build` starts MySQL, the API and the frontend (nginx) on any machine.
  - Multi-stage images, and the API runs as a non-root user.
  - Health-checked database, persistent volumes for data and CVs, and `.env.example` for secrets.
- **GitHub Actions CI.** Every push builds and tests the backend on Java 25, tests and builds the frontend, and builds the Docker images. There is a status badge in the README.
- Flyway migration **V5** (interviews, interview slots, saved jobs, audit log, job-alert preference).

### Changed
- Deleting a job also removes its interviews and bookmarks explicitly, so it no longer depends only on database cascades.
- Monotone line smoothing on the analytics chart, so the curve never suggests values that aren't in the data.

### Tests
- The backend grew from 26 to **41 tests**:
  - `FeatureIntegrationTest`: interview lifecycle and access control, saved jobs, job alerts, activity log and lockout
  - `WebSocketAuthInterceptorTest`
  - `SkillExtractorTest`, which generates a real PDF
  - `IcsCalendarTest`
  - shared `AbstractIntegrationTest` set-up
- The frontend has **8 tests**.

---

## [2.0.0 / Phase 2] — 2026-09-28 — Security, React frontend, new features

### Security (fixes S1–S8)
- **Spring Security + JWT authentication** (`security/`). Every protected endpoint requires a signed token. Tokens expire after 8 hours. The user is re-loaded from the database on every request, so a disabled account loses access immediately. *(S1)*
- **Role-based access control.** URL rules per role (`/api/admin/**`, `/api/students/**`, `/api/companies/**`, and every job and application write) plus `@PreAuthorize` on mutating endpoints. Role checks run *before* request validation, so a user without the role cannot probe the API's input rules; this was found by the integration tests. *(S2)*
- **BCrypt password hashing.** Migration `V4__Hash_plaintext_passwords` hashes every existing plaintext password in place, so users keep their passwords. *(S3)*
- **Ownership checks.** The acting student or company is always resolved from the token (`ProfileLookup`), never from request bodies. Jobs, applications, CVs and notifications check that they belong to the caller. *(S4)*
- **XSS eliminated** by moving to React, which escapes all output. User-supplied URLs are restricted to `http(s)` (`safeUrl`). *(S5)*
- **Bean Validation** on every request DTO. Password policy: 8+ characters with a letter and a number, enforced on both server and client. *(S6)*
- **Business rules enforced on the server:**
  - unverified companies cannot post
  - no applications after the deadline or to closed jobs
  - no duplicate applications
  - only pending applications can be withdrawn *(S7)*
- **Login brute-force protection:** 5 failed attempts lock the email for 15 minutes. Constant-time responses prevent email enumeration.
- **CORS** restricted to the frontend origin. Stack traces and exception messages are never sent to clients. *(S8)*
- **Safe file uploads:** PDF signature check, 5 MB limit, random UUID file names, path-traversal protection.

### Backend architecture (fixes Q3–Q8)
- **Request/response DTOs** (`dto/`) replace `Map<String, Object>` bodies and raw entities. *(Q3, Q4)*
- **Global exception handler** with one JSON error shape, including field-level validation errors. *(Q5)*
- **RESTful routes**, e.g. `PUT /api/jobs/{id}`, `PATCH /api/applications/{id}/status`, `DELETE /api/jobs/{id}`, `/api/students/me`, and `/api/admin/**` for admin functions. *(Q6)*
- **Java enums** for `UserType`, `JobType`, `ApplicationStatus` and `NotificationType`, with JPA converters that keep the existing MySQL ENUM values. *(Q7)*
- **Swagger UI / OpenAPI** at `/swagger-ui.html`, with JWT "Authorize" support. *(Q8)*
- **Flyway database migrations** (`db/migration`) replace `schema.sql`/`data.sql`. Existing databases are baselined and upgraded in place.
- **Performance:** lazy loading with fetch joins and batch lookups (no N+1 queries), server-side pagination and search, and new indexes.
- Entities keep the team's **Builder pattern** and gain domain methods (`changeStatus`, `attachCv`, `isOpenForApplications`, …) in place of copy-and-overwrite updates.

### New features
- **Skill-match scoring** (`SkillMatcher`): % match between a student's skills and each job's requirements, with matched and missing lists. It is shown to students on every job and to companies on every applicant.
- **Personalised job recommendations** for students.
- **CV upload** (PDF). Students upload, view and remove it. Companies view the CV of their applicants.
- **In-app notifications** (bell icon) for:
  - new applications
  - withdrawals
  - status changes
  - company verification and revocation
  - account re-activation
- **Admin analytics dashboard** with charts: applications per month, by status, jobs by type, top employers, acceptance rate. Each chart has a table view.
- **Server-side job search** by keyword, type and sort order, with pagination. Expired jobs and jobs from unverified companies are hidden automatically.
- Students can **withdraw** pending applications. Companies can **edit** vacancies. Admins can **search and filter users**.
- **Change password** for all users.
- Public **landing page** with live platform statistics.
- Richer **demo data**: 5 students, 3 companies (1 unverified), 8 vacancies (1 expired), 12 applications over 6 months.

### Frontend (fixes Q2)
- Rebuilt as a **React 18 single-page application** with Vite, React Router, React-Bootstrap and Chart.js, replacing four standalone HTML pages with inline scripts.
- **Structure:**
  - reusable components
  - a central API client with automatic token handling and session-expiry logout
  - route guards per role
  - toast messages and styled confirmation dialogs
- Kept the original EduLink brand (indigo/sky palette, Outfit font).
- **Code splitting:** each page loads on demand, and the chart library only downloads for admins. The main bundle went from 548 KB to 338 KB.
- Responsive layout, keyboard-accessible controls, ARIA labels, and reduced-motion support.

### Testing (fixes Q1)
- **Backend:**
  - `SkillMatcherTest`
  - `JwtServiceTest`
  - `LoginAttemptServiceTest`
  - `ApplicationServiceImplTest` (Mockito)
  - `SecurityIntegrationTest` (MockMvc + in-memory H2), which replays every attack from the audit
- **Frontend:** Vitest unit tests for the formatting and security helpers.

### Repository hygiene (fixes Q9)
- Added `.gitignore` covering build output, `node_modules`, IDE settings, uploaded CVs and the bundled Maven distribution.

### Removed
- **Java Swing desktop GUI** (`gui/` package). It was not in the agreed tech stack and bypassed the API security. It remains available in Git history.
- Old HTML/JS frontend pages (`admin.html`, `company.html`, `student.html`, `css/`, `js/`), replaced by the React app.
- `schema.sql` / `data.sql` start-up scripts, replaced by Flyway migrations. `database/sample_data.sql` removed (seed data now lives in migration V3). `database/schema.sql` is kept as a reference copy of the full v2 schema.
- Committed Maven distribution (`backend/maven/`) and personal IntelliJ settings (`backend/.idea/`).
- Unused generic `IService` / `IUserService` interfaces and the permissive `WebConfig` CORS rule.

---

## [Phase 1] — 2026-09-28 — Make it run & fix critical defects

### Fixed
- **C1** Backend now starts an embedded web server on port 8080. Previously `spring.main.web-application-type=none` meant the REST API never started, so the frontend could not load any data.
- **C4** Passwords are no longer included in API responses. `User.passwordHash` was nested in every job, student, company and application response.
- **C5** Seed data can be re-run safely (`INSERT IGNORE` with fixed IDs), and all documented test accounts can log in.
- **B1** Frontend `apiCall()` no longer crashes on empty responses (e.g. 404), and shows a clear message when the backend is offline.
- **B2 / B3 / B4** Apply, Toggle Active and View Letter buttons no longer break on text containing an apostrophe.
- **B5** Student job list no longer crashes on jobs without requirements.

### Changed
- **C2** Database switched from H2 (in-memory) back to **MySQL**, as agreed in the original tech stack. The `edulink_db` database is created automatically on first run.
- **C2** The Java Swing desktop GUI is now **optional**: it opens only with `--spring.profiles.active=desktop`. The web frontend is the primary UI again.
- **C3** Minimum Java version lowered from 21 to **17** (still runs on 21).
- `database/sample_data.sql` aligned with `backend/src/main/resources/data.sql`.
- `README.md` rewritten with the correct tech stack and step-by-step run instructions (IntelliJ + VS Code Live Server).

### Removed
- H2 database dependency.
