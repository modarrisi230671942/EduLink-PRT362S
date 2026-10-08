package za.ac.mycput.domain;

import jakarta.persistence.*;
import za.ac.mycput.domain.enums.JobType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** A vacancy posted by a company. */
@Entity
@Table(name = "job_postings")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Integer jobId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    /** Comma-separated list, e.g. "Java, Spring Boot, SQL". Used for skill matching. */
    @Column(name = "requirements", length = 1000)
    private String requirements;

    @Column(name = "location", length = 100)
    private String location;

    @Convert(converter = JobType.DbConverter.class)
    @Column(name = "job_type", length = 20)
    private JobType jobType = JobType.GRADUATE;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "posted_date", updatable = false)
    private LocalDateTime postedDate;

    protected JobPosting() {}

    private JobPosting(Builder builder) {
        this.jobId = builder.jobId;
        this.company = builder.company;
        this.title = builder.title;
        this.description = builder.description;
        this.requirements = builder.requirements;
        this.location = builder.location;
        this.jobType = builder.jobType;
        this.applicationDeadline = builder.applicationDeadline;
        this.isActive = builder.isActive;
    }

    @PrePersist
    void onCreate() {
        if (postedDate == null) {
            postedDate = LocalDateTime.now();
        }
    }

    // ── Domain behaviour ─────────────────────────────────────────────────────

    public void update(String title, String description, String requirements, String location,
                       JobType jobType, LocalDate applicationDeadline) {
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.location = location;
        this.jobType = jobType;
        this.applicationDeadline = applicationDeadline;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public boolean isDeadlinePassed(LocalDate today) {
        return applicationDeadline != null && applicationDeadline.isBefore(today);
    }

    /** A job accepts applications only while it is active, before its deadline, and from a verified company. */
    public boolean isOpenForApplications(LocalDate today) {
        return isActive() && !isDeadlinePassed(today) && company.isVerified();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Integer getJobId() {
        return jobId;
    }

    public Company getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRequirements() {
        return requirements;
    }

    public String getLocation() {
        return location;
    }

    public JobType getJobType() {
        return jobType;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public LocalDateTime getPostedDate() {
        return postedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobPosting that)) return false;
        return jobId != null && Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }

    @Override
    public String toString() {
        return "JobPosting{jobId=" + jobId + ", title='" + title + "', jobType=" + jobType + ", isActive=" + isActive + '}';
    }

    public static class Builder {
        private Integer jobId;
        private Company company;
        private String title;
        private String description;
        private String requirements;
        private String location;
        private JobType jobType = JobType.GRADUATE;
        private LocalDate applicationDeadline;
        private Boolean isActive = true;

        public Builder setJobId(Integer jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder setCompany(Company company) {
            this.company = company;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setRequirements(String requirements) {
            this.requirements = requirements;
            return this;
        }

        public Builder setLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder setJobType(JobType jobType) {
            this.jobType = jobType;
            return this;
        }

        public Builder setApplicationDeadline(LocalDate applicationDeadline) {
            this.applicationDeadline = applicationDeadline;
            return this;
        }

        public Builder setIsActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public JobPosting build() {
            return new JobPosting(this);
        }
    }
}
