# EduLink — System Audit & Remediation Report

| | |
|---|---|
| **Project** | EduLink (PRT362S / PRT372S Final Year Project) |
| **Date** | 28 September 2026 |
| **Scope** | Full codebase as received: `backend/`, `frontend/`, `database/` |
| **Status** | Phase 1 (make it run, fix critical bugs) **complete** · Phase 2 (security, React frontend, features, tests) **complete**. See section 9. |

---

## 1. Summary

The codebase compiled and ran on one team member's machine, but it **did not run for anyone else**. It also **did not match the tech stack the team agreed on** in the original README (Spring Boot REST API + MySQL + HTML/CSS/JS + Bootstrap 5).

During the audit we found:

- **5 critical defects** that stopped the system from working or leaked data. All 5 are **fixed** in Phase 1.
- **8 security gaps.** At the moment anyone who opens the browser developer tools can become an administrator.
- **9 code-quality and architecture gaps** that a final-year project will be marked down for: no tests, no DTOs, no validation, no API documentation and no frontend framework.
- **Several missing features** that users of a job portal would expect.

This document records what we found, what has been changed so far, and what we propose to do next. Every individual change is also logged in [`CHANGELOG.md`](CHANGELOG.md).

> This is a review of the **code**, not of any person. The aim is to agree as a team on what "done" looks like before the final presentation.

---

## 2. How the audit was done

1. We read every source file in the backend (controllers, services, repositories, entities, GUI) and the frontend (all four pages plus `app.js`).
2. We compared the running system against the tech stack in the original README.
3. We ran the backend on a second machine against a real MySQL server. Then we called every REST endpoint the frontend uses: login, register, browse jobs, apply, update status, post job, verify company and toggle user.
4. We inspected the JSON responses and the database contents directly.

---

## 3. Critical defects (Phase 1: all fixed)

| # | Defect | Impact | Fix |
|---|---|---|---|
| C1 | `spring.main.web-application-type=none` in `application.properties` | **No web server started.** The HTML frontend had nothing to call, so every page showed "Failed to load". | Removed the setting. The REST API now serves on `http://localhost:8080/api`. |
| C2 | The project was switched to an **H2 in-memory database** and a **Java Swing desktop GUI** | Deviated from the agreed stack (MySQL + web frontend). All data was lost every time the app stopped. | Switched back to **MySQL** and removed the H2 dependency. The Swing GUI is kept but disabled by default (`desktop` profile). |
| C3 | Build required **Java 21** | Would not compile on JDK 17, which is the minimum for Spring Boot 3 and common on lab machines. | Set `java.version` to 17. The project still builds and runs on JDK 21. *Later raised to Java 25 LTS in version 2.2.0; see CHANGELOG.* |
| C4 | **Password leak.** `User.passwordHash` was serialised into every JSON response. | Because `User` is nested inside `Company` and `Student`, **every job listing sent the company's password to every student's browser.** | Added `@JsonIgnore` to the field. Verified that no response contains `passwordHash`. |
| C5 | Seed data was broken | `data.sql` crashed the second startup on MySQL (duplicate emails) and would have duplicated job postings on every restart. `database/sample_data.sql` stored `hash1`/`hash2`/`hash3` as passwords, so **none of the documented test accounts could log in**, and it had no admin account. | Seed rows now use fixed IDs with `INSERT IGNORE`, so re-running is harmless. Both seed scripts now match, with working passwords and an admin account. |

### Smaller bugs fixed in Phase 1

| # | Bug | Fix |
|---|---|---|
| B1 | `apiCall()` in `app.js` crashed with a JSON parse error on any empty response, such as a 404. It showed a meaningless error when the backend was down. | Parses JSON only when there is a body. Shows "Cannot reach the EduLink backend…" when the server is offline. |
| B2 | **Apply** button (student page) broke for any job title containing an apostrophe, e.g. *"Developer's Assistant"*. | The title is looked up by job ID instead of being embedded in the `onclick` attribute. |
| B3 | **Toggle Active** button (company page) broke for any job whose text contained an apostrophe. The whole job was embedded as JSON inside an HTML attribute. | The job is looked up by ID from the loaded list. |
| B4 | **View Letter** broke on cover letters containing `'`, and showed the text "null" when there was no letter. | Apostrophes are escaped, and there is a fallback message. |
| B5 | Student job list crashed when a job had no requirements (`null.toLowerCase()`). | Null-safe. |
| B6 | The README described a desktop app, which contradicted the agreed stack. | README rewritten with correct stack and run instructions. |

---

## 4. Security gaps (all fixed in Phase 2; see section 9)

These are the issues a lecturer or external examiner is most likely to test live during the presentation.

| # | Gap | How to demonstrate it today | Severity |
|---|---|---|---|
| S1 | **No authentication on the API.** Every endpoint is public. The "session" is just a JSON object in `localStorage`. | In the browser console run `localStorage.setItem('edulink_session', JSON.stringify({userType:'admin'}))`, then open `admin.html`. You are now the admin. | Critical |
| S2 | **No authorisation / role checks on the server.** Roles are only checked in page JavaScript. | Any visitor can call `POST /api/companies/{id}/verify` to verify their own company, or `POST /api/auth/users/4/toggle` to lock out the admin. | Critical |
| S3 | **Passwords stored in plaintext.** The column is named `password_hash` but contains the raw password. | `SELECT password_hash FROM users;` | Critical |
| S4 | **Insecure direct object references.** The frontend sends `studentId` and `companyId` in request bodies, and the server trusts them. | Apply to a job *as another student*, or edit another company's profile, by changing one number. | High |
| S5 | **Stored cross-site scripting (XSS).** 25 places build HTML with `innerHTML` from user-entered text (names, job titles, cover letters, websites). | Register a company named `<img src=x onerror=alert(1)>`. The script runs in the admin's browser. | High |
| S6 | **No input validation.** Blank names, invalid emails, 1-character passwords and negative graduation years are all accepted. | Register with password `a`. | Medium |
| S7 | **Business rules only enforced in the browser.** Unverified companies can post jobs by calling the API directly. Students can apply after the deadline because the server never checks it. | `POST /api/jobs/create` with an unverified company ID. | Medium |
| S8 | **CORS allows every origin** (`*`), and internal exception messages are returned to the client. | — | Low |

---

## 5. Code quality & architecture gaps (all fixed in Phase 2; see section 9)

| # | Gap | Why it matters for marks |
|---|---|---|
| Q1 | **No automated tests.** `backend/src/test` does not exist. | Testing is usually a marked deliverable, and QA is a named team role. |
| Q2 | **No frontend framework.** There are four standalone HTML pages with ~900 lines of inline JavaScript, much of it duplicated. | Hard to maintain. It does not demonstrate final-year-level engineering. |
| Q3 | **Controllers accept `Map<String, Object>`** and parse fields by hand. | No type safety, a crash on a missing field, and no automatic validation. The standard approach is request/response **DTOs**. |
| Q4 | **JPA entities returned directly** from the API. | Leaks internal fields (C4 was caused by this) and returns deeply nested objects. |
| Q5 | **No global error handling.** Every controller has its own `try/catch` with different error formats. | Inconsistent API. Use `@RestControllerAdvice` instead. |
| Q6 | **Non-RESTful endpoints**, e.g. `POST /jobs/update`, `POST /applications/update-status`, `DELETE /jobs/delete/{id}`, and admin user management under `/auth`. | Examiners check for REST conventions (`PUT /jobs/{id}`, `PATCH /applications/{id}/status`, `DELETE /jobs/{id}`). |
| Q7 | Status and job type stored as **free-text strings** in Java. | Typos compile fine. These should be Java `enum`s. |
| Q8 | **No API documentation.** | Swagger / OpenAPI is expected for a REST API. |
| Q9 | **Repository hygiene.** A full Maven distribution (`backend/maven/`) and personal IntelliJ settings (`backend/.idea/`) are committed. There is an incomplete Maven wrapper and no `.gitignore`. | Bloats the repo and causes "works on my machine" problems. |

---

## 6. Missing features

Features users of a student job portal would expect but that the system does not have:

- **CV / document upload.** Companies can't see a student's CV.
- **Server-side search, filtering and pagination.** All jobs are downloaded and filtered in the browser.
- **Notifications.** Students are not told when an application status changes.
- **Job detail view.** Students only see a truncated description card.
- **Application withdrawal** for students; **account / job deletion** for admins.
- **Deadline handling.** Expired jobs still appear and still accept applications.
- **Skill matching.** Student skills and job requirements are both stored but never compared.
- **Analytics.** The admin dashboard shows four counters only.

---

## 7. Files changed in Phase 1

| File | Change |
|---|---|
| `backend/pom.xml` | Removed H2. Java 17. Updated description. |
| `backend/src/main/resources/application.properties` | Rewritten for MySQL and web server. Database auto-created. |
| `backend/src/main/resources/data.sql` | Idempotent seed data (`INSERT IGNORE`, fixed IDs). |
| `backend/src/main/java/za/ac/mycput/EduLinkApplication.java` | Starts the REST API. Opens the Swing GUI only with the `desktop` profile. |
| `backend/src/main/java/za/ac/mycput/gui/*.java` (5 files) | Added `@Profile("desktop")`. |
| `backend/src/main/java/za/ac/mycput/domain/User.java` | `@JsonIgnore` on `passwordHash`. |
| `frontend/js/app.js` | Robust `apiCall()` with offline message. |
| `frontend/student.html` | Fixes B2, B4, B5. |
| `frontend/company.html` | Fix B3. |
| `database/sample_data.sql` | Working passwords, admin account, matches `data.sql`. |
| `README.md` | Correct stack and run instructions. |

**Verification:** backend compiled with Maven and started against MySQL. Tomcat started on port 8080. All four test accounts log in. Browse, apply, status update, post job, register and CORS preflight were tested via the REST API. No response contains `passwordHash`.

---

## 8. Phase 2 plan (as agreed)

Decisions made for Phase 2: **React + Vite** for the frontend; all four proposed features (skill matching, CV upload, notifications, admin analytics); **remove** the Swing GUI.

1. **Security.** Spring Security with JWT authentication, BCrypt password hashing, role-based access control on every endpoint, ownership checks, input validation, restricted CORS, and XSS-safe rendering. (Fixes S1–S8.)
2. **Backend architecture.** DTOs, enums, global exception handler, RESTful routes, Swagger/OpenAPI docs, server-side business rules. (Fixes Q3–Q8.)
3. **Frontend framework.** Rebuild the frontend as a single-page application with a modern framework, reusable components and client-side routing. (Fixes Q2.)
4. **Testing.** Unit tests (JUnit 5 + Mockito) for services and integration tests for controllers and security. (Fixes Q1.)
5. **Features.** Selected from section 6.
6. **Repository hygiene.** `.gitignore`, remove committed Maven/IDE files, proper Maven wrapper. (Fixes Q9.)

---

## 9. Phase 2 results: how each finding was resolved

Full details are in [`CHANGELOG.md`](CHANGELOG.md#200--phase-2--2026-09-28--security-react-frontend-new-features).

| ID | Finding | Resolution | Where |
|---|---|---|---|
| S1 | No authentication | JWT authentication. The token is required on all non-public endpoints, and the user is re-checked on every request. | `security/JwtService`, `JwtAuthenticationFilter`, `SecurityConfig` |
| S2 | No server-side roles | Role rules per URL area, plus `@PreAuthorize` | `SecurityConfig`, controllers |
| S3 | Plaintext passwords | BCrypt. Existing passwords were hashed by migration V4. | `V4__Hash_plaintext_passwords` |
| S4 | IDOR (IDs trusted from client) | The caller's profile is resolved from the token, and ownership is checked on every write and download | `ProfileLookup`, service implementations |
| S5 | Stored XSS | React escapes output. Only `http(s)` links are allowed. | `frontend/`, `utils/format.js#safeUrl` |
| S6 | No validation | Bean Validation on all DTOs. Matching client-side checks. | `dto/`, `GlobalExceptionHandler` |
| S7 | Rules only in browser | Verification, deadline, active, duplicate and withdraw rules enforced in services | `JobPostingServiceImpl`, `ApplicationServiceImpl` |
| S8 | CORS `*`, leaked errors | CORS restricted to the frontend origin. Generic error messages. | `SecurityConfig`, `GlobalExceptionHandler` |
| Q1 | No tests | JUnit/Mockito unit tests, a MockMvc security integration suite, Vitest | `backend/src/test`, `frontend/src/utils/format.test.js` |
| Q2 | No frontend framework | React 18 single-page app with routing, contexts and reusable components | `frontend/src` |
| Q3/Q4 | Maps in, entities out | Request/response DTO records | `dto/`, `DtoMapper` |
| Q5 | Inconsistent errors | One `ApiError` JSON shape | `GlobalExceptionHandler` |
| Q6 | Non-RESTful routes | Resource-oriented routes with proper verbs and status codes | `controller/` |
| Q7 | Free-text statuses | Java enums with DB converters | `domain/enums/` |
| Q8 | No API docs | Swagger UI | `/swagger-ui.html` |
| Q9 | Repository hygiene | `.gitignore`. Bundled Maven and IDE files excluded. | `.gitignore` |

**Added beyond the audit:**
- Flyway migrations
- login lockout
- skill matching and recommendations
- CV upload
- notifications
- admin analytics
- server-side search and pagination
- change password
- application withdrawal

**Phase 3 (v2.1):**
- real-time WebSocket notifications
- interview scheduling with calendar invites
- skill extraction from CVs
- job alerts
- saved jobs
- an admin activity (audit) log
- dark mode
- Docker Compose
- GitHub Actions CI

Test coverage grew to 41 backend tests and 8 frontend tests. See `CHANGELOG.md`.
