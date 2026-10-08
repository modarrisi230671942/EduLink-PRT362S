package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.mycput.domain.enums.JobType;
import za.ac.mycput.dto.CommonDtos.PageResponse;
import za.ac.mycput.dto.JobDtos.JobRequest;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.dto.JobDtos.JobStatusRequest;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IJobPostingService;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Browse, search and manage job postings")
public class JobPostingController {

    private final IJobPostingService jobService;

    public JobPostingController(IJobPostingService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    @Operation(summary = "Search open jobs (public)",
               description = "Only active jobs from verified companies whose deadline has not passed. "
                       + "When called by a logged-in student, each job includes a skill-match score.")
    public PageResponse<JobResponse> search(
            @AuthenticationPrincipal AuthUser viewer,
            @Parameter(description = "Matches title, requirements, company or location") @RequestParam(required = false) String q,
            @RequestParam(required = false) JobType type,
            @Parameter(description = "'newest' (default) or 'deadline'") @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        return jobService.search(viewer, q, type, sort, page, size);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Job details (public)")
    public JobResponse get(@AuthenticationPrincipal AuthUser viewer, @PathVariable Integer jobId) {
        return jobService.getJob(viewer, jobId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPANY')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Post a new job (verified companies only)")
    public JobResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody JobRequest request) {
        return jobService.create(me.userId(), request);
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("hasRole('COMPANY')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edit one of your jobs")
    public JobResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable Integer jobId,
                              @Valid @RequestBody JobRequest request) {
        return jobService.update(me.userId(), jobId, request);
    }

    @PatchMapping("/{jobId}/status")
    @PreAuthorize("hasRole('COMPANY')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Open or close one of your jobs")
    public JobResponse setStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Integer jobId,
                                 @Valid @RequestBody JobStatusRequest request) {
        return jobService.setActive(me.userId(), jobId, request.active());
    }

    @DeleteMapping("/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a job and its applications (owner or admin)")
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Integer jobId) {
        jobService.delete(me, jobId);
    }
}
