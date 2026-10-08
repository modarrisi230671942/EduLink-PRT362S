package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.mycput.dto.AuthDtos.*;
import za.ac.mycput.dto.CommonDtos.MessageResponse;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IAuthService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, registration and account security")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT",
               description = "Locks the email for 15 minutes after 5 failed attempts.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register/student")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a student account (logs the student in)")
    public AuthResponse registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return authService.registerStudent(request);
    }

    @PostMapping("/register/company")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a company account (starts unverified)")
    public AuthResponse registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return authService.registerCompany(request);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "The currently logged-in user")
    public UserSummary me(@AuthenticationPrincipal AuthUser me) {
        return authService.currentUser(me.userId());
    }

    @PutMapping("/password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change your password")
    public MessageResponse changePassword(@AuthenticationPrincipal AuthUser me,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(me.userId(), request);
        return new MessageResponse("Password changed successfully.");
    }
}
