package za.ac.mycput.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import za.ac.mycput.domain.enums.ApplicationStatus;
import za.ac.mycput.domain.enums.JobType;

import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for /api/applications. */
public final class ApplicationDtos {

    private ApplicationDtos() {}

    public record ApplyRequest(
            @NotNull(message = "jobId is required") Integer jobId,
            @NotBlank(message = "A cover letter is required")
            @Size(min = 30, max = 3000, message = "Cover letter must be between 30 and 3000 characters") String coverLetter) {}

    public record ApplicationStatusRequest(@NotNull(message = "status is required") ApplicationStatus status) {}

    public record ApplicationResponse(
            Integer applicationId,
            ApplicationStatus status,
            String coverLetter,
            LocalDateTime appliedDate,
            LocalDateTime statusUpdatedAt,
            // Job
            Integer jobId,
            String jobTitle,
            JobType jobType,
            String jobLocation,
            Integer companyId,
            String companyName,
            // Applicant
            Integer studentId,
            String studentName,
            String studentEmail,
            String studentNumber,
            String course,
            String institution,
            Integer graduationYear,
            List<String> skills,
            boolean hasCv,
            // How well the applicant matches the job's requirements
            JobDtos.MatchResult match,
            // Scheduled interview, if any
            InterviewDtos.InterviewResponse interview) {}
}
