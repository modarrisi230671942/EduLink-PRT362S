package za.ac.mycput.dto;

import jakarta.validation.constraints.*;
import za.ac.mycput.domain.enums.UserType;

import java.time.Instant;

/** Request/response bodies for /api/auth. */
public final class AuthDtos {

    private AuthDtos() {}

    /** Shared password policy: at least 8 characters with a letter and a number. */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$";
    public static final String PASSWORD_MESSAGE = "Password must be 8-100 characters and contain at least one letter and one number";
    public static final String URL_REGEX = "^$|^https?://[\\w.-]+(\\.[\\w.-]+)+[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*$";

    public record LoginRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record StudentRegisterRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 100) String email,
            @NotBlank(message = "Password is required") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
            @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
            @NotBlank(message = "Student number is required") @Size(max = 20) String studentNumber,
            @NotBlank(message = "Course is required") @Size(max = 100) String course,
            @NotBlank(message = "Institution is required") @Size(max = 100) String institution,
            @NotNull(message = "Graduation year is required") @Min(value = 2000, message = "Graduation year must be 2000 or later")
            @Max(value = 2100, message = "Graduation year looks invalid") Integer graduationYear,
            @Size(max = 1000, message = "Skills must be at most 1000 characters") String skills) {}

    public record CompanyRegisterRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 100) String email,
            @NotBlank(message = "Password is required") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
            @NotBlank(message = "Company name is required") @Size(max = 100) String companyName,
            @Size(max = 50) String industry,
            @Size(max = 100) String location,
            @Size(max = 255) @Pattern(regexp = URL_REGEX, message = "Website must start with http:// or https://") String website) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String newPassword) {}

    /** Who is logged in. {@code profileId} is the studentId or companyId (null for admins). */
    public record UserSummary(
            Integer userId,
            String email,
            UserType role,
            Integer profileId,
            String displayName,
            Boolean verified) {}

    public record AuthResponse(String token, Instant expiresAt, UserSummary user) {}
}
