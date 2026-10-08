package za.ac.mycput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import za.ac.mycput.domain.*;
import za.ac.mycput.domain.enums.JobType;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.repository.*;
import za.ac.mycput.security.LoginAttemptService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared set-up for integration tests: the real application with MockMvc and an in-memory H2 database.
 * Every test starts from the same small data set:
 * students alice & bob, verified company TechCorp (with one open job), unverified company NewCo, and an admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

    static final String PASSWORD = "password123";
    static final String COVER_LETTER = "I would love to join your team and I have the skills you need.";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository users;
    @Autowired StudentRepository students;
    @Autowired CompanyRepository companies;
    @Autowired JobPostingRepository jobs;
    @Autowired ApplicationRepository applications;
    @Autowired NotificationRepository notifications;
    @Autowired InterviewRepository interviews;
    @Autowired SavedJobRepository savedJobs;
    @Autowired AuditLogRepository auditLog;
    @Autowired LoginAttemptService loginAttempts;

    JobPosting openJob;

    @BeforeEach
    void seed() {
        // Login lockouts are kept in memory; clear them so one test's failed logins can't affect another
        List.of("alice@test.com", "bob@test.com", "admin@test.com", "hr@techcorp.com", "hr@newco.com")
                .forEach(loginAttempts::recordSuccess);

        auditLog.deleteAll();
        notifications.deleteAll();
        interviews.deleteAll();
        savedJobs.deleteAll();
        applications.deleteAll();
        jobs.deleteAll();
        students.deleteAll();
        companies.deleteAll();
        users.deleteAll();

        User alice = user("alice@test.com", UserType.STUDENT);
        User bob = user("bob@test.com", UserType.STUDENT);
        user("admin@test.com", UserType.ADMIN);
        User techUser = user("hr@techcorp.com", UserType.COMPANY);
        User newCoUser = user("hr@newco.com", UserType.COMPANY);

        students.save(new Student.Builder().setUser(alice).setFullName("Alice").setStudentNumber("STU001")
                .setCourse("CS").setInstitution("CPUT").setGraduationYear(2026).setSkills("Java, SQL").build());
        students.save(new Student.Builder().setUser(bob).setFullName("Bob").setStudentNumber("STU002")
                .setCourse("IT").setInstitution("UCT").setGraduationYear(2026).setSkills("React").build());
        Company techCorp = companies.save(new Company.Builder().setUser(techUser).setCompanyName("TechCorp")
                .setIsVerified(true).build());
        companies.save(new Company.Builder().setUser(newCoUser).setCompanyName("NewCo").setIsVerified(false).build());

        openJob = jobs.save(new JobPosting.Builder().setCompany(techCorp).setTitle("Junior Developer")
                .setDescription("Build things").setRequirements("Java, Spring Boot").setLocation("Cape Town")
                .setJobType(JobType.GRADUATE).setApplicationDeadline(LocalDate.now().plusDays(30)).setIsActive(true)
                .build());
    }

    User user(String email, UserType type) {
        return users.save(new User.Builder().setEmail(email).setPasswordHash(passwordEncoder.encode(PASSWORD))
                .setUserType(type).setIsActive(true).build());
    }

    /** Logs in through the real endpoint and returns an "Authorization" header value. */
    String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    ResultActions postJson(String url, String token, Object body) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (token != null) {
            request.header("Authorization", token);
        }
        return mvc.perform(request);
    }

    /** Alice applies for the open job and the new application's ID is returned. */
    int aliceApplies(String aliceToken) throws Exception {
        String body = postJson("/api/applications", aliceToken, Map.of("jobId", openJob.getJobId(), "coverLetter", COVER_LETTER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("applicationId").asInt();
    }
}
