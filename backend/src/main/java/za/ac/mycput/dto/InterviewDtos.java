package za.ac.mycput.dto;

import jakarta.validation.constraints.*;
import za.ac.mycput.domain.enums.InterviewMode;
import za.ac.mycput.domain.enums.InterviewStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for interview scheduling. */
public final class InterviewDtos {

    private InterviewDtos() {}

    public record ProposeInterviewRequest(
            @NotNull(message = "Interview type is required") InterviewMode mode,
            @Size(max = 255, message = "Location/link must be at most 255 characters") String location,
            @Size(max = 1000, message = "Notes must be at most 1000 characters") String notes,
            @NotNull(message = "Duration is required") @Min(value = 15, message = "Duration must be at least 15 minutes")
            @Max(value = 240, message = "Duration must be at most 4 hours") Integer durationMinutes,
            @NotEmpty(message = "Propose at least one time slot")
            @Size(max = 3, message = "Propose at most three time slots")
            List<@NotNull @Future(message = "Time slots must be in the future") LocalDateTime> slots) {}

    public record ConfirmInterviewRequest(@NotNull(message = "slotId is required") Integer slotId) {}

    public record SlotResponse(Integer slotId, LocalDateTime startsAt) {}

    public record InterviewResponse(
            Integer interviewId,
            Integer applicationId,
            InterviewStatus status,
            InterviewMode mode,
            String location,
            String notes,
            Integer durationMinutes,
            List<SlotResponse> slots,
            LocalDateTime confirmedStart,
            LocalDateTime confirmedEnd,
            LocalDateTime updatedAt,
            // Context, so interview lists can be shown without loading the application
            String jobTitle,
            String companyName,
            String studentName) {}
}
