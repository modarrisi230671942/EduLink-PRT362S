package za.ac.mycput.dto;

import jakarta.validation.constraints.*;
import za.ac.mycput.domain.enums.JobType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for /api/jobs. */
public final class JobDtos {

    private JobDtos() {}

    public record JobRequest(
            @NotBlank(message = "Title is required") @Size(max = 100) String title,
            @NotBlank(message = "Description is required") @Size(max = 5000) String description,
            @NotBlank(message = "Requirements are required") @Size(max = 1000) String requirements,
            @NotBlank(message = "Location is required") @Size(max = 100) String location,
            @NotNull(message = "Job type is required") JobType jobType,
            @NotNull(message = "Application deadline is required")
            @FutureOrPresent(message = "Application deadline cannot be in the past") LocalDate applicationDeadline) {}

    public record JobStatusRequest(@NotNull(message = "active is required") Boolean active) {}

    /**
     * How well a student's skills cover a job's requirements.
     * {@code score} is 0-100; {@code matched}/{@code missing} list the job's requirements.
     */
    public record MatchResult(int score, List<String> matched, List<String> missing) {}

    /**
     * A job as shown in the UI. Fields that depend on who is asking are null when they don't apply:
     * {@code match}/{@code applied}/{@code saved} for students, {@code applicationCount} for the owning company.
     */
    public record JobResponse(
            Integer jobId,
            String title,
            String description,
            String requirements,
            List<String> requirementList,
            String location,
            JobType jobType,
            LocalDate applicationDeadline,
            LocalDateTime postedDate,
            boolean active,
            boolean open,
            Integer companyId,
            String companyName,
            String companyIndustry,
            String companyWebsite,
            Long applicationCount,
            MatchResult match,
            Boolean applied,
            Boolean saved) {}
}
