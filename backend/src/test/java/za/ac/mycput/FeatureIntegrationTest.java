package za.ac.mycput;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import za.ac.mycput.domain.enums.AuditAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end tests of the v2.1 features: interviews, saved jobs, job alerts and the activity log. */
class FeatureIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> interviewProposal(LocalDateTime... slots) {
        return Map.of("mode", "ONLINE", "location", "https://meet.example.com/abc", "durationMinutes", 45,
                "notes", "Bring your portfolio", "slots", List.of(slots).stream().map(LocalDateTime::toString).toList());
    }

    @Test
    @DisplayName("Interview: company proposes, student confirms, both can download a calendar invite")
    void interviewLifecycle() throws Exception {
        String alice = login("alice@test.com");
        int applicationId = aliceApplies(alice);
        String company = login("hr@techcorp.com");

        LocalDateTime first = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.HOURS).withHour(10);
        LocalDateTime second = first.plusDays(1);

        // Another company cannot schedule for this applicant
        postJson("/api/applications/" + applicationId + "/interview", login("hr@newco.com"), interviewProposal(first))
                .andExpect(status().isForbidden());

        String proposed = postJson("/api/applications/" + applicationId + "/interview", company, interviewProposal(second, first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSED"))
                .andExpect(jsonPath("$.slots.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        JsonNode interview = json.readTree(proposed);
        int interviewId = interview.get("interviewId").asInt();
        int earliestSlotId = interview.get("slots").get(0).get("slotId").asInt(); // slots are sorted by time

        // Scheduling moves the application to "reviewed", and Alice sees the invitation
        mvc.perform(get("/api/students/me/applications").header("Authorization", alice))
                .andExpect(jsonPath("$[0].status").value("REVIEWED"))
                .andExpect(jsonPath("$[0].interview.status").value("PROPOSED"));

        // Bob cannot confirm Alice's interview; Alice can
        postJson("/api/interviews/" + interviewId + "/confirm", login("bob@test.com"), Map.of("slotId", earliestSlotId))
                .andExpect(status().isForbidden());
        postJson("/api/interviews/" + interviewId + "/confirm", alice, Map.of("slotId", earliestSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedStart").value(org.hamcrest.Matchers.startsWith(first.toLocalDate().toString())));

        mvc.perform(get("/api/interviews/" + interviewId + "/calendar").header("Authorization", company))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string(containsString("BEGIN:VEVENT")))
                .andExpect(content().string(containsString("SUMMARY:Interview: Junior Developer")));
        mvc.perform(get("/api/interviews/" + interviewId + "/calendar").header("Authorization", login("bob@test.com")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/companies/me/interviews").header("Authorization", company))
                .andExpect(jsonPath("$[0].studentName").value("Alice"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Interview time slots must be in the future")
    void pastSlotsAreRejected() throws Exception {
        int applicationId = aliceApplies(login("alice@test.com"));
        postJson("/api/applications/" + applicationId + "/interview", login("hr@techcorp.com"),
                interviewProposal(LocalDateTime.now().minusDays(1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Saved jobs: bookmark, list, and the flag appears in search results")
    void savedJobs() throws Exception {
        String alice = login("alice@test.com");
        mvc.perform(put("/api/students/me/saved-jobs/" + openJob.getJobId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/students/me/saved-jobs").header("Authorization", alice))
                .andExpect(jsonPath("$[0].title").value("Junior Developer"))
                .andExpect(jsonPath("$[0].saved").value(true));
        mvc.perform(get("/api/jobs").header("Authorization", alice))
                .andExpect(jsonPath("$.content[0].saved").value(true));

        mvc.perform(delete("/api/students/me/saved-jobs/" + openJob.getJobId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/students/me/saved-jobs").header("Authorization", alice))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Job alerts: a new job notifies students whose skills match it well")
    void jobAlerts() throws Exception {
        Map<String, Object> job = Map.of("title", "Frontend Developer", "description", "Build UIs",
                "requirements", "React", "location", "Remote", "jobType", "GRADUATE",
                "applicationDeadline", LocalDate.now().plusDays(20).toString());
        postJson("/api/jobs", login("hr@techcorp.com"), job).andExpect(status().isCreated());

        // Bob (React) matches 100%, Alice (Java, SQL) matches 0%
        mvc.perform(get("/api/notifications").header("Authorization", login("bob@test.com")))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].type").value("JOB_MATCH"));
        mvc.perform(get("/api/notifications").header("Authorization", login("alice@test.com")))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("Activity log: failed logins, lockouts and admin actions are recorded")
    void activityLog() throws Exception {
        for (int i = 0; i < 5; i++) {
            postJson("/api/auth/login", null, Map.of("email", "bob@test.com", "password", "wrong-password" + i))
                    .andExpect(status().isUnauthorized());
        }
        // Now locked, even with the right password
        postJson("/api/auth/login", null, Map.of("email", "bob@test.com", "password", PASSWORD))
                .andExpect(status().isTooManyRequests());

        String admin = login("admin@test.com");
        Integer newCoId = companies.findAll().stream()
                .filter(c -> c.getCompanyName().equals("NewCo")).findFirst().orElseThrow().getCompanyId();
        mvc.perform(patch("/api/admin/companies/" + newCoId + "/verification").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verified\":true}"))
                .andExpect(status().isOk());

        assertThat(auditLog.findAll()).extracting(l -> l.getAction())
                .contains(AuditAction.LOGIN_FAILED, AuditAction.ACCOUNT_LOCKED, AuditAction.LOGIN_SUCCESS,
                        AuditAction.COMPANY_VERIFIED);

        mvc.perform(get("/api/admin/activity").param("action", "ACCOUNT_LOCKED").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actorEmail").value("bob@test.com"));
    }

    @Test
    @DisplayName("Analytics: the acceptance rate is accepted ÷ decided applications, and both counts are reported")
    void acceptanceRateCountsOnlyDecidedApplications() throws Exception {
        String company = login("hr@techcorp.com");
        int aliceApplication = aliceApplies(login("alice@test.com"));
        String bobBody = postJson("/api/applications", login("bob@test.com"), Map.of("jobId", openJob.getJobId(), "coverLetter", COVER_LETTER))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int bobApplication = json.readTree(bobBody).get("applicationId").asInt();

        for (var decision : Map.of(aliceApplication, "ACCEPTED", bobApplication, "REJECTED").entrySet()) {
            mvc.perform(patch("/api/applications/" + decision.getKey() + "/status").header("Authorization", company)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + decision.getValue() + "\"}"))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/api/admin/stats").header("Authorization", login("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.applications").value(2))
                .andExpect(jsonPath("$.totals.acceptedApplications").value(1))
                .andExpect(jsonPath("$.totals.decidedApplications").value(2))
                .andExpect(jsonPath("$.totals.acceptanceRate").value(50));
    }
}
