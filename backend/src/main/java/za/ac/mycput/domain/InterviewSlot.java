package za.ac.mycput.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/** One proposed start time for an interview. */
@Entity
@Table(name = "interview_slots")
public class InterviewSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Integer slotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    protected InterviewSlot() {}

    InterviewSlot(Interview interview, LocalDateTime startsAt) {
        this.interview = interview;
        this.startsAt = startsAt;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public Interview getInterview() {
        return interview;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterviewSlot that)) return false;
        return slotId != null && Objects.equals(slotId, that.slotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId);
    }
}
