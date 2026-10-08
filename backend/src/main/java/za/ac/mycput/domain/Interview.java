package za.ac.mycput.domain;

import jakarta.persistence.*;
import za.ac.mycput.domain.enums.InterviewMode;
import za.ac.mycput.domain.enums.InterviewStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An interview for one application. The company proposes 1-3 time slots and the student confirms one.
 * Proposing again (rescheduling) replaces the slots and resets the status to PROPOSED.
 */
@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_id")
    private Integer interviewId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private InterviewMode mode;

    @Column(name = "location")
    private String location;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 45;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.PROPOSED;

    @Column(name = "confirmed_start")
    private LocalDateTime confirmedStart;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startsAt ASC")
    private final List<InterviewSlot> slots = new ArrayList<>();

    protected Interview() {}

    public Interview(Application application) {
        this.application = application;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ── Domain behaviour ─────────────────────────────────────────────────────

    /** Sets (or replaces) the proposal. Any previous confirmation is cleared. */
    public void propose(InterviewMode mode, String location, String notes, int durationMinutes, List<LocalDateTime> startTimes) {
        this.mode = mode;
        this.location = location;
        this.notes = notes;
        this.durationMinutes = durationMinutes;
        this.status = InterviewStatus.PROPOSED;
        this.confirmedStart = null;
        this.updatedAt = LocalDateTime.now();
        this.slots.clear();
        startTimes.stream().sorted().forEach(start -> this.slots.add(new InterviewSlot(this, start)));
    }

    /** The student picks one of the proposed slots. */
    public void confirm(InterviewSlot slot) {
        this.confirmedStart = slot.getStartsAt();
        this.status = InterviewStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = InterviewStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime confirmedEnd() {
        return confirmedStart == null ? null : confirmedStart.plusMinutes(durationMinutes);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Integer getInterviewId() {
        return interviewId;
    }

    public Application getApplication() {
        return application;
    }

    public InterviewMode getMode() {
        return mode;
    }

    public String getLocation() {
        return location;
    }

    public String getNotes() {
        return notes;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public LocalDateTime getConfirmedStart() {
        return confirmedStart;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<InterviewSlot> getSlots() {
        return slots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Interview that)) return false;
        return interviewId != null && Objects.equals(interviewId, that.interviewId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interviewId);
    }
}
