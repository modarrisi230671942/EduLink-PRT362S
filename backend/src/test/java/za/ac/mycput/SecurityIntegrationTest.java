package za.ac.mycput;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the security rules through the real HTTP layer (MockMvc + in-memory H2).
 * Each test corresponds to a finding in docs/SYSTEM_AUDIT.md.
 */
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("S1: protected endpoints require a valid login token")
    void anonymousAndForgedTokensAreRejected() throws Exception {
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/stats").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized());
        // Public job browsing still works without logging in
        mvc.perform(get("/api/jobs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Junior Developer"));
    }

    @Test
    @DisplayName("S2: roles are enforced on the server, before input is even validated")
    void studentsCannotUseAdminOrCompanyEndpoints() throws Exception {
        String student = login("alice@test.com");
        mvc.perform(get("/api/admin/stats").header("Authorization", student)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", student)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/activity").header("Authorization", student)).andExpect(status().isForbidden());
        postJson("/api/jobs", student, Map.of("title", "x")).andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/stats").header("Authorization", login("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.students").value(2));
    }

    @Test
    @DisplayName("S3: passwords are stored as BCrypt hashes and wrong passwords are refused")
    void passwordsAreHashed() throws Exception {
        String stored = users.findByEmailIgnoreCase("alice@test.com").orElseThrow().getPasswordHash();
        assertThat(stored).startsWith("$2").isNotEqualTo(PASSWORD);

        postJson("/api/auth/login", null, Map.of("email", "alice@test.com", "password", "wrong-password1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("S4: the student is taken from the token, and companies can only review their own applicants")
    void ownershipIsEnforced() throws Exception {
        String alice = login("alice@test.com");
        String body = postJson("/api/applications", alice, Map.of("jobId", openJob.getJobId(), "coverLetter", COVER_LETTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentName").value("Alice"))
                .andExpect(jsonPath("$.match.score").value(50))
                .andReturn().getResponse().getContentAsString();
        int applicationId = json.readTree(body).get("applicationId").asInt();

        // An unrelated company cannot change the status
        mvc.perform(patch("/api/applications/" + applicationId + "/status")
                        .header("Authorization", login("hr@newco.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isForbidden());

        // Bob cannot withdraw Alice's application
        mvc.perform(delete("/api/applications/" + applicationId).header("Authorization", login("bob@test.com")))
                .andExpect(status().isForbidden());

        // The owning company can, and Alice is notified
        mvc.perform(patch("/api/applications/" + applicationId + "/status")
                        .header("Authorization", login("hr@techcorp.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(get("/api/notifications").header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("S6: invalid input is rejected with field-level messages")
    void validationErrorsAreReported() throws Exception {
        String response = postJson("/api/auth/register/student", null, Map.of(
                        "email", "not-an-email", "password", "short", "fullName", "",
                        "studentNumber", "STU9", "course", "CS", "institution", "CPUT", "graduationYear", 2026))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        JsonNode fieldErrors = json.readTree(response).get("fieldErrors");
        assertThat(fieldErrors.has("email")).isTrue();
        assertThat(fieldErrors.has("password")).isTrue();
        assertThat(fieldErrors.has("fullName")).isTrue();
    }

    @Test
    @DisplayName("S7: unverified companies cannot post jobs, and duplicate applications are refused")
    void businessRulesAreEnforcedOnTheServer() throws Exception {
        Map<String, Object> job = Map.of("title", "Tester", "description", "Test things", "requirements", "Testing",
                "location", "Durban", "jobType", "INTERNSHIP", "applicationDeadline", LocalDate.now().plusDays(5).toString());
        postJson("/api/jobs", login("hr@newco.com"), job)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("verified")));

        String alice = login("alice@test.com");
        Map<String, Object> apply = Map.of("jobId", openJob.getJobId(), "coverLetter", COVER_LETTER);
        postJson("/api/applications", alice, apply).andExpect(status().isCreated());
        postJson("/api/applications", alice, apply).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Disabled accounts are locked out immediately, even with a previously valid token")
    void disabledAccountLosesAccess() throws Exception {
        String bob = login("bob@test.com");
        Integer bobId = users.findByEmailIgnoreCase("bob@test.com").orElseThrow().getUserId();

        mvc.perform(patch("/api/admin/users/" + bobId + "/status")
                        .header("Authorization", login("admin@test.com"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/students/me").header("Authorization", bob)).andExpect(status().isUnauthorized());
        postJson("/api/auth/login", null, Map.of("email", "bob@test.com", "password", PASSWORD))
                .andExpect(status().isForbidden());
    }
}
