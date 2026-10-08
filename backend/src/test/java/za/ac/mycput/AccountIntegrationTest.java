package za.ac.mycput;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.ac.mycput.domain.AuditLog;
import za.ac.mycput.domain.enums.AuditAction;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Registration and password change: the actions that write an activity-log entry about a user
 * whose row is created or changed in the same, not yet committed, transaction.
 */
class AccountIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("A new student can register, is logged in straight away, and the registration is audited")
    void studentRegistration() throws Exception {
        postJson("/api/auth/register/student", null, Map.of(
                        "email", "new.student@test.com", "password", "password123", "fullName", "New Student",
                        "studentNumber", "STU900", "course", "Computer Science", "institution", "CPUT",
                        "graduationYear", 2027, "skills", "Java, SQL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("STUDENT"));

        assertThat(users.findByEmailIgnoreCase("new.student@test.com")).isPresent();
        assertThat(auditLog.findAll()).extracting(AuditLog::getAction, AuditLog::getActorEmail)
                .contains(Tuple.tuple(AuditAction.USER_REGISTERED, "new.student@test.com"));
        login("new.student@test.com");
    }

    @Test
    @DisplayName("A new company can register and starts unverified")
    void companyRegistration() throws Exception {
        postJson("/api/auth/register/company", null, Map.of(
                        "email", "hr@startup.com", "password", "password123", "companyName", "Startup",
                        "industry", "Software", "location", "Cape Town"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("COMPANY"))
                .andExpect(jsonPath("$.user.verified").value(false));

        assertThat(auditLog.findAll()).extracting(AuditLog::getAction).contains(AuditAction.USER_REGISTERED);
    }

    @Test
    @DisplayName("Changing the password works, is audited, and the old password stops working")
    void passwordChange() throws Exception {
        String token = login("alice@test.com");
        mvc.perform(put("/api/auth/password").header("Authorization", token)
                        .contentType("application/json")
                        .content(json.writeValueAsString(Map.of("currentPassword", PASSWORD, "newPassword", "newPassw0rd"))))
                .andExpect(status().isOk());

        assertThat(auditLog.findAll()).extracting(AuditLog::getAction).contains(AuditAction.PASSWORD_CHANGED);
        postJson("/api/auth/login", null, Map.of("email", "alice@test.com", "password", PASSWORD))
                .andExpect(status().isUnauthorized());
        postJson("/api/auth/login", null, Map.of("email", "alice@test.com", "password", "newPassw0rd"))
                .andExpect(status().isOk());
    }
}
