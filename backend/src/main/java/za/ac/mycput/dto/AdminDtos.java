package za.ac.mycput.dto;

import jakarta.validation.constraints.NotNull;
import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.domain.enums.UserType;

import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for /api/admin and /api/public. */
public final class AdminDtos {

    private AdminDtos() {}

    public record UserStatusRequest(@NotNull(message = "active is required") Boolean active) {}

    public record VerificationRequest(@NotNull(message = "verified is required") Boolean verified) {}

    public record AdminUserResponse(
            Integer userId,
            String email,
            UserType role,
            String displayName,
            boolean active,
            LocalDateTime createdAt) {}

    public record AuditLogResponse(
            Integer logId,
            AuditAction action,
            String actorEmail,
            String targetType,
            Integer targetId,
            String details,
            String ipAddress,
            LocalDateTime createdAt) {}

    /** One bar/slice in a chart. */
    public record LabelCount(String label, long count) {}

    public record Totals(
            long students,
            long companies,
            long verifiedCompanies,
            long pendingCompanies,
            long jobs,
            long openJobs,
            long applications,
            long acceptedApplications,
            /** Applications with a final decision (accepted + rejected). */
            long decidedApplications,
            /** Accepted as a percentage of decided applications. */
            int acceptanceRate) {}

    public record AdminStatsResponse(
            Totals totals,
            List<LabelCount> applicationsPerMonth,
            List<LabelCount> applicationsByStatus,
            List<LabelCount> jobsByType,
            List<LabelCount> topCompanies) {}

    /** Headline numbers for the public landing page. */
    public record PublicStatsResponse(long openJobs, long verifiedCompanies, long students) {}
}
