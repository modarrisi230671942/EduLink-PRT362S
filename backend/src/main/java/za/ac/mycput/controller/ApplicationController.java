package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.mycput.dto.ApplicationDtos.ApplicationResponse;
import za.ac.mycput.dto.ApplicationDtos.ApplicationStatusRequest;
import za.ac.mycput.dto.ApplicationDtos.ApplyRequest;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IApplicationService;

@RestController
@RequestMapping("/api/applications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Applications", description = "Apply, withdraw and review job applications")
public class ApplicationController {

    private final IApplicationService applicationService;

    public ApplicationController(IApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Apply for a job",
               description = "Rejected if the job is closed, past its deadline, or already applied for.")
    public ApplicationResponse apply(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ApplyRequest request) {
        return applicationService.apply(me.userId(), request);
    }

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Withdraw one of your pending applications")
    public void withdraw(@AuthenticationPrincipal AuthUser me, @PathVariable Integer applicationId) {
        applicationService.withdraw(me.userId(), applicationId);
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Change an applicant's status (company that owns the job only)",
               description = "The student receives an in-app notification.")
    public ApplicationResponse updateStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Integer applicationId,
                                            @Valid @RequestBody ApplicationStatusRequest request) {
        return applicationService.updateStatus(me.userId(), applicationId, request.status());
    }

    @GetMapping("/{applicationId}/cv")
    @PreAuthorize("hasAnyRole('COMPANY', 'STUDENT')")
    @Operation(summary = "Download the applicant's CV (hiring company or the applicant only)")
    public ResponseEntity<Resource> applicantCv(@AuthenticationPrincipal AuthUser me, @PathVariable Integer applicationId) {
        return FileResponses.pdf(applicationService.applicantCv(me, applicationId));
    }
}
