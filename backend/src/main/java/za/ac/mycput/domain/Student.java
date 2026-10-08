package za.ac.mycput.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/** A student's profile, including the metadata of their uploaded CV. */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Integer studentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "student_number", unique = true, nullable = false, length = 20)
    private String studentNumber;

    @Column(name = "course", nullable = false, length = 100)
    private String course;

    @Column(name = "institution", nullable = false, length = 100)
    private String institution;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    /** Comma-separated list, e.g. "Java, SQL, Python". Used for skill matching. */
    @Column(name = "skills", length = 1000)
    private String skills;

    /** Name of the stored file on disk (a random UUID, never user-controlled). */
    @Column(name = "cv_file_name")
    private String cvFileName;

    /** The file name the student uploaded, shown in the UI and used for downloads. */
    @Column(name = "cv_original_name")
    private String cvOriginalName;

    @Column(name = "cv_uploaded_at")
    private LocalDateTime cvUploadedAt;

    /** Whether to notify the student when a newly posted job matches their skills. */
    @Column(name = "job_alerts", nullable = false)
    private Boolean jobAlerts = true;

    protected Student() {}

    private Student(Builder builder) {
        this.studentId = builder.studentId;
        this.user = builder.user;
        this.fullName = builder.fullName;
        this.studentNumber = builder.studentNumber;
        this.course = builder.course;
        this.institution = builder.institution;
        this.graduationYear = builder.graduationYear;
        this.skills = builder.skills;
    }

    // ── Domain behaviour ─────────────────────────────────────────────────────

    public void updateProfile(String fullName, String course, String institution, Integer graduationYear, String skills) {
        this.fullName = fullName;
        this.course = course;
        this.institution = institution;
        this.graduationYear = graduationYear;
        this.skills = skills;
    }

    public void attachCv(String storedFileName, String originalName) {
        this.cvFileName = storedFileName;
        this.cvOriginalName = originalName;
        this.cvUploadedAt = LocalDateTime.now();
    }

    public void removeCv() {
        this.cvFileName = null;
        this.cvOriginalName = null;
        this.cvUploadedAt = null;
    }

    public boolean hasCv() {
        return cvFileName != null;
    }

    public void setJobAlerts(boolean enabled) {
        this.jobAlerts = enabled;
    }

    public boolean wantsJobAlerts() {
        return Boolean.TRUE.equals(jobAlerts);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Integer getStudentId() {
        return studentId;
    }

    public User getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getCourse() {
        return course;
    }

    public String getInstitution() {
        return institution;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public String getSkills() {
        return skills;
    }

    public String getCvFileName() {
        return cvFileName;
    }

    public String getCvOriginalName() {
        return cvOriginalName;
    }

    public LocalDateTime getCvUploadedAt() {
        return cvUploadedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student student)) return false;
        return studentId != null && Objects.equals(studentId, student.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId);
    }

    @Override
    public String toString() {
        return "Student{studentId=" + studentId + ", fullName='" + fullName + "', studentNumber='" + studentNumber + "'}";
    }

    public static class Builder {
        private Integer studentId;
        private User user;
        private String fullName;
        private String studentNumber;
        private String course;
        private String institution;
        private Integer graduationYear;
        private String skills;

        public Builder setStudentId(Integer studentId) {
            this.studentId = studentId;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder setStudentNumber(String studentNumber) {
            this.studentNumber = studentNumber;
            return this;
        }

        public Builder setCourse(String course) {
            this.course = course;
            return this;
        }

        public Builder setInstitution(String institution) {
            this.institution = institution;
            return this;
        }

        public Builder setGraduationYear(Integer graduationYear) {
            this.graduationYear = graduationYear;
            return this;
        }

        public Builder setSkills(String skills) {
            this.skills = skills;
            return this;
        }

        public Student build() {
            return new Student(this);
        }
    }
}
