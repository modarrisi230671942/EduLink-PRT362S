package za.ac.mycput.domain;

import jakarta.persistence.*;
import za.ac.mycput.domain.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** A student's application to a job posting. A student can apply to each job only once. */
@Entity
@Table(
    name = "applications",
    uniqueConstraints = {@UniqueConstraint(name = "unique_app", columnNames = {"job_id", "student_id"})}
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Integer applicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "cover_letter", length = 5000)
    private String coverLetter;

    @Convert(converter = ApplicationStatus.DbConverter.class)
    @Column(name = "status", length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "applied_date", updatable = false)
    private LocalDateTime appliedDate;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    protected Application() {}

    private Application(Builder builder) {
        this.applicationId = builder.applicationId;
        this.job = builder.job;
        this.student = builder.student;
        this.coverLetter = builder.coverLetter;
        this.status = builder.status;
    }

    @PrePersist
    void onCreate() {
        if (appliedDate == null) {
            appliedDate = LocalDateTime.now();
        }
    }

    // ── Domain behaviour ─────────────────────────────────────────────────────

    public void changeStatus(ApplicationStatus newStatus) {
        this.status = newStatus;
        this.statusUpdatedAt = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Integer getApplicationId() {
        return applicationId;
    }

    public JobPosting getJob() {
        return job;
    }

    public Student getStudent() {
        return student;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Application that)) return false;
        return applicationId != null && Objects.equals(applicationId, that.applicationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId);
    }

    @Override
    public String toString() {
        return "Application{applicationId=" + applicationId + ", status=" + status + ", appliedDate=" + appliedDate + '}';
    }

    public static class Builder {
        private Integer applicationId;
        private JobPosting job;
        private Student student;
        private String coverLetter;
        private ApplicationStatus status = ApplicationStatus.PENDING;

        public Builder setApplicationId(Integer applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder setJob(JobPosting job) {
            this.job = job;
            return this;
        }

        public Builder setStudent(Student student) {
            this.student = student;
            return this;
        }

        public Builder setCoverLetter(String coverLetter) {
            this.coverLetter = coverLetter;
            return this;
        }

        public Builder setStatus(ApplicationStatus status) {
            this.status = status;
            return this;
        }

        public Application build() {
            return new Application(this);
        }
    }
}
