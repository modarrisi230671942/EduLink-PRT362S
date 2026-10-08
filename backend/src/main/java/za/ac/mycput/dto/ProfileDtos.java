package za.ac.mycput.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for student and company profiles. */
public final class ProfileDtos {

    private ProfileDtos() {}

    public record StudentProfileRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
            @NotBlank(message = "Course is required") @Size(max = 100) String course,
            @NotBlank(message = "Institution is required") @Size(max = 100) String institution,
            @NotNull(message = "Graduation year is required") @Min(value = 2000, message = "Graduation year must be 2000 or later")
            @Max(value = 2100, message = "Graduation year looks invalid") Integer graduationYear,
            @Size(max = 1000, message = "Skills must be at most 1000 characters") String skills,
            /** Optional: null leaves the current setting unchanged. */
            Boolean jobAlerts) {}

    /** Skills found in the student's CV. {@code newSkills} are the ones not yet on their profile. */
    public record CvSkillsResponse(List<String> found, List<String> newSkills) {}

    public record StudentResponse(
            Integer studentId,
            String email,
            String fullName,
            String studentNumber,
            String course,
            String institution,
            Integer graduationYear,
            String skills,
            List<String> skillList,
            boolean hasCv,
            String cvFileName,
            LocalDateTime cvUploadedAt,
            boolean jobAlerts) {}

    public record CompanyProfileRequest(
            @NotBlank(message = "Company name is required") @Size(max = 100) String companyName,
            @Size(max = 50) String industry,
            @Size(max = 100) String location,
            @Size(max = 255) @Pattern(regexp = AuthDtos.URL_REGEX, message = "Website must start with http:// or https://") String website) {}

    public record CompanyResponse(
            Integer companyId,
            Integer userId,
            String email,
            String companyName,
            String industry,
            String location,
            String website,
            boolean verified,
            boolean active,
            LocalDateTime createdAt) {}
}
