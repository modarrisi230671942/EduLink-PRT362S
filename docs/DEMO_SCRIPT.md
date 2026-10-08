# EduLink — Final Presentation Demo Script (≈12 minutes)

A suggested running order for the live demo. Each step names **who drives**, so the whole team is seen contributing, and **what to say**, so every feature is tied to the problem it solves.

## Before you start (10 minutes before)

- [ ] Start the system, either:
  - `docker compose up --build`, then open <http://localhost:3000>, **or**
  - XAMPP MySQL → IntelliJ `EduLinkApplication` → VS Code `npm run dev`, then open <http://localhost:5173>
- [ ] Open **two browser windows side by side**, one normal and one private/incognito (they need separate logins):
  - **Left:** log in as **Student** (Alice)
  - **Right:** log in as **Company** (TechCorp)
- [ ] Have a PDF CV ready on the desktop. Any CV listing a few skills works, e.g. "Python, Docker, Power BI, Teamwork".
- [ ] Open a third tab on <http://localhost:8080/swagger-ui.html>
- [ ] Zoom both windows to about 110% so the room can read them.

---

## 1. The problem (1 min) — *System Analyst*

Open the landing page. Explain the four problems at the top of the README:
- students don't know which jobs fit them
- applications vanish without a response
- fake adverts target graduates
- employers are flooded with unsuitable applicants and waste time arranging interviews

Point out that the live numbers come from the database, not hard-coded text.

## 2. Student: matching and CV intelligence (2.5 min) — *Frontend Developer* (left window)

1. **Dashboard**: *Recommended for you*, ranked by skill match, plus the profile-strength checklist.
2. Open **QA Engineer**. Show the **match ring**, *Matched* and *Skills to develop*.
   > "Alice isn't guessing any more. She matches 50% and knows exactly what to learn."
3. **Profile & CV** → upload the PDF. Within seconds: *"We found N skills in your CV that aren't on your profile"*. Click **Add all** → **Save**.
   > "EduLink read the CV itself: no AI subscription, fully offline, using a skill dictionary with aliases, so 'JS' in a CV becomes JavaScript."
4. Go back to *Find jobs*: the match percentages have changed. **Bookmark** a job, then open **Saved**.
5. Leave Alice on **Applications**.

## 3. The real-time moment (2 min) — *Backend Developer* (right window, then left)

This is the highlight. Keep both windows visible.

1. **Right (TechCorp)** → *Applicants* → sorted by **Best match first** → open **Alice Mbatha / Junior Developer**. Show the requirement match, cover letter and **View CV**.
2. Click **Invite to interview**. Choose Online, paste a meeting link, keep the three suggested times, and click **Send invitation**.
3. **Look at the left window.** Without anyone touching it, a toast pops up, the bell shows **1**, and the invitation with three time options appears on Alice's page.
   > "No refresh. This is a WebSocket connection, authenticated with the same JWT as the API."
4. **Left:** Alice clicks **Accept this time**. **Right:** open *Interviews*. It's confirmed, and TechCorp got a live notification.
5. Click **Add to calendar** and open the `.ics` file. It opens in Outlook or Calendar with a 30-minute reminder.

## 4. Admin: trust and oversight (2 min) — *Chairperson*

1. Log in as **Admin**. **Analytics**: applications per month, by status, by type, top employers. Click **Table** on a chart: every chart has an accessible table view.
2. Toggle **dark mode** (moon icon). Everything, including the charts, switches.
3. The yellow banner shows a company waiting. Go to **Companies** → **Verify** GreenBuild.
   > "Only verified employers can post. That's how we stop fake adverts. GreenBuild was notified instantly."

## 5. Security — "try to break it" (2.5 min) — *QA Engineer*

This is what separates this project from a typical one. Do it live.

1. **Brute force.** Log out, then try `bob.student@test.com` with a wrong password **5 times**. The 6th attempt, even with the right password, says *"Too many failed login attempts"*.
2. Log in as Admin → **Activity log** → filter **Account locked**. The lockout, the failed attempts and their IP address are all there.
   > "Every login, failure, lockout and admin action is audited."
3. **Old system vs new.** In the original version, one line in the browser console made anyone an admin. Now open `http://localhost:8080/api/admin/stats` in a new tab: **401 "Please log in to continue."**
4. **Database.** `SELECT email, password_hash FROM users;` shows BCrypt hashes only.
5. **Tests.** Show the green ✔ on GitHub Actions, or run all tests in IntelliJ (right-click `src/test/java` → Run): 48 tests replay every attack and feature, all green.

## 6. Engineering wrap-up (1 min) — *Technical Lead*

Mention in one breath:
- layered Spring Boot API with DTOs, validation and Swagger
- Flyway migrations V1–V6, which upgraded the old database in place
- React single-page app with route guards, code splitting and dark mode
- WebSockets for real-time updates
- Docker Compose for one-command setup
- CI on every push
- the audit report in `docs/`, which shows the team found and fixed its own weaknesses

---

## Likely examiner questions

| Question | Short answer |
|---|---|
| Why JWT and not sessions? | Stateless: it suits a separate SPA frontend and scales horizontally. We still re-check the user in the database on every request and WebSocket connection, so disabling an account takes effect immediately. |
| How is the WebSocket secured? | The browser sends its JWT in the STOMP CONNECT frame. `WebSocketAuthInterceptor` rejects missing, forged or disabled accounts, only allows subscriptions to `/user/queue/…`, and blocks client-sent messages. Spring routes each message only to its owner. |
| Why send notifications "after commit"? | If the transaction rolled back, the user would be told about something that never happened. `@TransactionalEventListener` guarantees the data is saved before the browser hears about it. |
| How does CV skill extraction work? | PDFBox extracts the text (first 10 pages). Text and dictionary phrases are normalised (lowercase, punctuation → spaces) and matched as whole words, so "Java" never matches inside "JavaScript". Common English words like "go" and "spring" are deliberately excluded. See `SkillExtractor` and `SkillExtractorTest`. |
| How does skill matching work? | A requirement matches if it and a skill are the same phrase, or one contains the other as whole words. Score = matched ÷ total requirements. See `SkillMatcher`. |
| Where is the token stored, and isn't that an XSS risk? | `localStorage`. The risk is mitigated because React escapes all output and user URLs are restricted to http(s). httpOnly cookies are the next hardening step. |
| What happens to existing data when the schema changes? | Flyway applies only new numbered migrations. The original database was baselined and upgraded in place, including hashing its plaintext passwords (V4). |
| Why Docker? | "It works on my machine" was literally how this project started. Now any machine with Docker runs the identical stack, and CI proves every push builds. |
| What would you do next? | Email/SMS notifications, password reset by email, httpOnly-cookie tokens, HTTPS deployment to the cloud, and an optional AI cover-letter coach. |
