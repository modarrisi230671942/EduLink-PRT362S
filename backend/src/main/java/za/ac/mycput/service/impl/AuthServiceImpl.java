package za.ac.mycput.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.mycput.domain.Company;
import za.ac.mycput.domain.Student;
import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.service.IAuditService;
import za.ac.mycput.dto.AuthDtos.*;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.CompanyRepository;
import za.ac.mycput.repository.StudentRepository;
import za.ac.mycput.repository.UserRepository;
import za.ac.mycput.security.JwtService;
import za.ac.mycput.security.LoginAttemptService;
import za.ac.mycput.service.IAuthService;

@Service
public class AuthServiceImpl implements IAuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final IAuditService audit;

    /** Compared against when the email does not exist, so both cases take the same time (no user enumeration). */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository,
                           StudentRepository studentRepository,
                           CompanyRepository companyRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           LoginAttemptService loginAttempts,
                           IAuditService audit) {
        this.audit = audit;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode("not-a-real-password-0");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim();

        long minutesLocked = loginAttempts.minutesLocked(email);
        if (minutesLocked > 0) {
            throw ApiException.tooManyRequests(
                    "Too many failed login attempts. Try again in " + minutesLocked + " minute(s).");
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean passwordOk = passwordEncoder.matches(request.password(),
                user != null ? user.getPasswordHash() : dummyHash);
        Integer userId = user == null ? null : user.getUserId();

        if (user == null || !passwordOk) {
            boolean nowLocked = loginAttempts.recordFailure(email);
            audit.record(userId, email, AuditAction.LOGIN_FAILED, "USER", userId,
                    user == null ? "Unknown email" : "Wrong password");
            if (nowLocked) {
                audit.record(userId, email, AuditAction.ACCOUNT_LOCKED, "USER", userId,
                        "Locked for 15 minutes after repeated failed logins");
            }
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            audit.record(userId, email, AuditAction.LOGIN_FAILED, "USER", userId, "Account is disabled");
            throw ApiException.forbidden("Your account has been deactivated. Please contact the administrator.");
        }

        loginAttempts.recordSuccess(email);
        audit.record(userId, user.getEmail(), AuditAction.LOGIN_SUCCESS, "USER", userId, null);
        return authResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerStudent(StudentRegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        ensureEmailAvailable(email);
        if (studentRepository.existsByStudentNumberIgnoreCase(request.studentNumber().trim())) {
            throw ApiException.conflict("This student number is already registered.");
        }

        User user = userRepository.save(new User.Builder()
                .setEmail(email)
                .setPasswordHash(passwordEncoder.encode(request.password()))
                .setUserType(UserType.STUDENT)
                .setIsActive(true)
                .build());

        studentRepository.save(new Student.Builder()
                .setUser(user)
                .setFullName(request.fullName().trim())
                .setStudentNumber(request.studentNumber().trim().toUpperCase())
                .setCourse(request.course().trim())
                .setInstitution(request.institution().trim())
                .setGraduationYear(request.graduationYear())
                .setSkills(trimToNull(request.skills()))
                .build());

        audit.record(user.getUserId(), email, AuditAction.USER_REGISTERED, "STUDENT", user.getUserId(), null);
        return authResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerCompany(CompanyRegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        ensureEmailAvailable(email);

        User user = userRepository.save(new User.Builder()
                .setEmail(email)
                .setPasswordHash(passwordEncoder.encode(request.password()))
                .setUserType(UserType.COMPANY)
                .setIsActive(true)
                .build());

        // New companies start unverified: an admin must approve them before they can post jobs
        companyRepository.save(new Company.Builder()
                .setUser(user)
                .setCompanyName(request.companyName().trim())
                .setIndustry(trimToNull(request.industry()))
                .setLocation(trimToNull(request.location()))
                .setWebsite(trimToNull(request.website()))
                .setIsVerified(false)
                .build());

        audit.record(user.getUserId(), email, AuditAction.USER_REGISTERED, "COMPANY", user.getUserId(),
                "Awaiting verification: " + request.companyName().trim());
        return authResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary currentUser(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        return summary(user);
    }

    @Override
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is incorrect.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("The new password must be different from the current one.");
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        audit.record(userId, user.getEmail(), AuditAction.PASSWORD_CHANGED, "USER", userId, null);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AuthResponse authResponse(User user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.token(), token.expiresAt(), summary(user));
    }

    private UserSummary summary(User user) {
        return switch (user.getUserType()) {
            case STUDENT -> studentRepository.findByUser_UserId(user.getUserId())
                    .map(s -> new UserSummary(user.getUserId(), user.getEmail(), user.getUserType(),
                            s.getStudentId(), s.getFullName(), null))
                    .orElseGet(() -> basicSummary(user));
            case COMPANY -> companyRepository.findByUser_UserId(user.getUserId())
                    .map(c -> new UserSummary(user.getUserId(), user.getEmail(), user.getUserType(),
                            c.getCompanyId(), c.getCompanyName(), c.isVerified()))
                    .orElseGet(() -> basicSummary(user));
            case ADMIN -> new UserSummary(user.getUserId(), user.getEmail(), user.getUserType(),
                    null, "System Administrator", null);
        };
    }

    private static UserSummary basicSummary(User user) {
        return new UserSummary(user.getUserId(), user.getEmail(), user.getUserType(), null, user.getEmail(), null);
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("This email address is already registered.");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
