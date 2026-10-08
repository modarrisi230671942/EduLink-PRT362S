package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.mycput.dto.InterviewDtos.ConfirmInterviewRequest;
import za.ac.mycput.dto.InterviewDtos.InterviewResponse;
import za.ac.mycput.dto.InterviewDtos.ProposeInterviewRequest;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IInterviewService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Interviews", description = "Propose, confirm and cancel interviews; download calendar invites")
public class InterviewController {

    private final IInterviewService interviewService;

    public InterviewController(IInterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/applications/{applicationId}/interview")
    @PreAuthorize("hasRole('COMPANY')")
    @Operation(summary = "Propose (or reschedule) an interview with 1-3 time slots",
               description = "The applicant is notified and asked to choose a slot.")
    public InterviewResponse propose(@AuthenticationPrincipal AuthUser me, @PathVariable Integer applicationId,
                                     @Valid @RequestBody ProposeInterviewRequest request) {
        return interviewService.propose(me.userId(), applicationId, request);
    }

    @PostMapping("/interviews/{interviewId}/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Accept one of the proposed time slots")
    public InterviewResponse confirm(@AuthenticationPrincipal AuthUser me, @PathVariable Integer interviewId,
                                     @Valid @RequestBody ConfirmInterviewRequest request) {
        return interviewService.confirm(me.userId(), interviewId, request.slotId());
    }

    @PostMapping("/interviews/{interviewId}/cancel")
    @PreAuthorize("hasAnyRole('COMPANY', 'STUDENT')")
    @Operation(summary = "Cancel an interview (hiring company or applicant)")
    public InterviewResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Integer interviewId) {
        return interviewService.cancel(me, interviewId);
    }

    @GetMapping("/interviews/{interviewId}/calendar")
    @PreAuthorize("hasAnyRole('COMPANY', 'STUDENT')")
    @Operation(summary = "Download a confirmed interview as an .ics calendar file")
    public ResponseEntity<byte[]> calendar(@AuthenticationPrincipal AuthUser me, @PathVariable Integer interviewId) {
        byte[] ics = interviewService.calendarFile(me, interviewId).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "calendar", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("edulink-interview-" + interviewId + ".ics").build().toString())
                .body(ics);
    }
}
