package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.mycput.domain.enums.ApplicationStatus;
import za.ac.mycput.dto.ApplicationDtos.ApplicationResponse;
import za.ac.mycput.dto.InterviewDtos.InterviewResponse;
import za.ac.mycput.service.IInterviewService;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.dto.ProfileDtos.CompanyProfileRequest;
import za.ac.mycput.dto.ProfileDtos.CompanyResponse;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IApplicationService;
import za.ac.mycput.service.ICompanyService;
import za.ac.mycput.service.IJobPostingService;

import java.util.List;

/** Everything a logged-in company can do with its own data. Restricted to ROLE_COMPANY in SecurityConfig. */
@RestController
@RequestMapping("/api/companies/me")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Company", description = "The logged-in company's profile, jobs and applicants")
public class CompanyController {

    private final ICompanyService companyService;
    private final IJobPostingService jobService;
    private final IApplicationService applicationService;
    private final IInterviewService interviewService;

    public CompanyController(ICompanyService companyService,
                             IJobPostingService jobService,
                             IApplicationService applicationService,
                             IInterviewService interviewService) {
        this.companyService = companyService;
        this.jobService = jobService;
        this.applicationService = applicationService;
        this.interviewService = interviewService;
    }

    @GetMapping
    @Operation(summary = "Your company profile (includes verification status)")
    public CompanyResponse profile(@AuthenticationPrincipal AuthUser me) {
        return companyService.getProfile(me.userId());
    }

    @PutMapping
    @Operation(summary = "Update your company profile")
    public CompanyResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CompanyProfileRequest request) {
        return companyService.updateProfile(me.userId(), request);
    }

    @GetMapping("/jobs")
    @Operation(summary = "All your job postings with application counts")
    public List<JobResponse> jobs(@AuthenticationPrincipal AuthUser me) {
        return jobService.companyJobs(me.userId());
    }

    @GetMapping("/applications")
    @Operation(summary = "Applications to your jobs, optionally filtered by status and/or job")
    public List<ApplicationResponse> applications(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(required = false) ApplicationStatus status,
                                                  @RequestParam(required = false) Integer jobId) {
        return applicationService.companyApplications(me.userId(), status, jobId);
    }

    @GetMapping("/interviews")
    @Operation(summary = "Upcoming interviews and pending invitations for your vacancies")
    public List<InterviewResponse> interviews(@AuthenticationPrincipal AuthUser me) {
        return interviewService.upcoming(me);
    }
}
