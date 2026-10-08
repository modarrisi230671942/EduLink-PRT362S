package za.ac.mycput.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** A job a student has bookmarked. Composite key: (student, job). */
@Entity
@Table(name = "saved_jobs")
public class SavedJob {

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "student_id")
        private Integer studentId;

        @Column(name = "job_id")
        private Integer jobId;

        protected Key() {}

        public Key(Integer studentId, Integer jobId) {
            this.studentId = studentId;
            this.jobId = jobId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(studentId, key.studentId) && Objects.equals(jobId, key.jobId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentId, jobId);
        }
    }

    @EmbeddedId
    private Key id;

    @MapsId("studentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @MapsId("jobId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private JobPosting job;

    @Column(name = "saved_at", updatable = false)
    private LocalDateTime savedAt;

    protected SavedJob() {}

    public SavedJob(Student student, JobPosting job) {
        this.id = new Key(student.getStudentId(), job.getJobId());
        this.student = student;
        this.job = job;
    }

    @PrePersist
    void onCreate() {
        if (savedAt == null) {
            savedAt = LocalDateTime.now();
        }
    }

    public Key getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public JobPosting getJob() {
        return job;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
