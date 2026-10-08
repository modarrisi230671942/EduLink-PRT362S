package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.service.IAuditService;
import za.ac.mycput.dto.AdminDtos.*;
import za.ac.mycput.dto.CommonDtos.PageResponse;
import za.ac.mycput.dto.ProfileDtos.CompanyResponse;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IAdminService;

import java.util.List;

/** Administrator console. Restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Analytics, company verification and user management")
public class AdminController {

    private final IAdminService adminService;
    private final IAuditService auditService;

    public AdminController(IAdminService adminService, IAuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @GetMapping("/activity")
    @Operation(summary = "Activity log: logins, failed logins, lockouts and admin actions, newest first")
    public PageResponse<AuditLogResponse> activity(@RequestParam(required = false) AuditAction action,
                                                   @RequestParam(required = false) String actor,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return auditService.search(action, actor, page, size);
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard totals and chart data")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    @Operation(summary = "Search users by role and/or email")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) UserType role,
                                                 @RequestParam(required = false) String search,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return adminService.users(role, search, page, size);
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Enable or disable a user account")
    public AdminUserResponse setUserStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Integer userId,
                                           @Valid @RequestBody UserStatusRequest request) {
        return adminService.setUserActive(me, userId, request.active());
    }

    @GetMapping("/companies")
    @Operation(summary = "All companies, unverified first; optional filter by verification")
    public List<CompanyResponse> companies(@RequestParam(required = false) Boolean verified) {
        return adminService.companies(verified);
    }

    @PatchMapping("/companies/{companyId}/verification")
    @Operation(summary = "Verify or revoke a company (the company is notified)")
    public CompanyResponse setVerification(@AuthenticationPrincipal AuthUser me, @PathVariable Integer companyId,
                                           @Valid @RequestBody VerificationRequest request) {
        return adminService.setCompanyVerified(me, companyId, request.verified());
    }
}
