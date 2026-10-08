package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.ac.mycput.dto.ApplicationDtos.ApplicationResponse;
import za.ac.mycput.dto.JobDtos.JobResponse;
import za.ac.mycput.dto.InterviewDtos.InterviewResponse;
import za.ac.mycput.dto.ProfileDtos.CvSkillsResponse;
import za.ac.mycput.dto.ProfileDtos.StudentProfileRequest;
import za.ac.mycput.dto.ProfileDtos.StudentResponse;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IApplicationService;
import za.ac.mycput.service.IInterviewService;
import za.ac.mycput.service.IStudentService;

import java.util.List;

/** Everything a logged-in student can do with their own data. Restricted to ROLE_STUDENT in SecurityConfig. */
@RestController
@RequestMapping("/api/students/me")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Student", description = "The logged-in student's profile, CV, applications and recommendations")
public class StudentController {

    private final IStudentService studentService;
    private final IApplicationService applicationService;
    private final IInterviewService interviewService;

    public StudentController(IStudentService studentService, IApplicationService applicationService,
                             IInterviewService interviewService) {
        this.studentService = studentService;
        this.applicationService = applicationService;
        this.interviewService = interviewService;
    }

    @GetMapping
    @Operation(summary = "Your profile")
    public StudentResponse profile(@AuthenticationPrincipal AuthUser me) {
        return studentService.getProfile(me.userId());
    }

    @PutMapping
    @Operation(summary = "Update your profile")
    public StudentResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody StudentProfileRequest request) {
        return studentService.updateProfile(me.userId(), request);
    }

    @GetMapping("/applications")
    @Operation(summary = "Your applications, newest first")
    public List<ApplicationResponse> applications(@AuthenticationPrincipal AuthUser me) {
        return applicationService.studentApplications(me.userId());
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Open jobs ranked by how well they match your skills")
    public List<JobResponse> recommendations(@AuthenticationPrincipal AuthUser me,
                                             @RequestParam(defaultValue = "6") int limit) {
        return studentService.recommendations(me.userId(), Math.min(Math.max(limit, 1), 20));
    }

    @PostMapping(value = "/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace your CV (PDF, max 5 MB)")
    public StudentResponse uploadCv(@AuthenticationPrincipal AuthUser me, @RequestPart("file") MultipartFile file) {
        return studentService.uploadCv(me.userId(), file);
    }

    @GetMapping("/cv")
    @Operation(summary = "Download your CV")
    public ResponseEntity<Resource> downloadCv(@AuthenticationPrincipal AuthUser me) {
        return FileResponses.pdf(studentService.downloadCv(me.userId()));
    }

    @DeleteMapping("/cv")
    @Operation(summary = "Remove your CV")
    public StudentResponse deleteCv(@AuthenticationPrincipal AuthUser me) {
        return studentService.deleteCv(me.userId());
    }

    @GetMapping("/cv/skills")
    @Operation(summary = "Skills found in your CV",
               description = "Reads the PDF and returns recognised skills, plus those not yet on your profile.")
    public CvSkillsResponse cvSkills(@AuthenticationPrincipal AuthUser me) {
        return studentService.cvSkills(me.userId());
    }

    @GetMapping("/saved-jobs")
    @Operation(summary = "Your bookmarked jobs")
    public List<JobResponse> savedJobs(@AuthenticationPrincipal AuthUser me) {
        return studentService.savedJobs(me.userId());
    }

    @PutMapping("/saved-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bookmark a job")
    public void saveJob(@AuthenticationPrincipal AuthUser me, @PathVariable Integer jobId) {
        studentService.saveJob(me.userId(), jobId);
    }

    @DeleteMapping("/saved-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a bookmark")
    public void unsaveJob(@AuthenticationPrincipal AuthUser me, @PathVariable Integer jobId) {
        studentService.unsaveJob(me.userId(), jobId);
    }

    @GetMapping("/interviews")
    @Operation(summary = "Your upcoming interviews and invitations")
    public List<InterviewResponse> interviews(@AuthenticationPrincipal AuthUser me) {
        return interviewService.upcoming(me);
    }
}
