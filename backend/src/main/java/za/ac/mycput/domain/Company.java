package za.ac.mycput.domain;

import jakarta.persistence.*;

import java.util.Objects;

/** An employer's profile. Must be verified by an admin before it can post jobs. */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer companyId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "industry", length = 50)
    private String industry;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "website")
    private String website;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    protected Company() {}

    private Company(Builder builder) {
        this.companyId = builder.companyId;
        this.user = builder.user;
        this.companyName = builder.companyName;
        this.industry = builder.industry;
        this.location = builder.location;
        this.website = builder.website;
        this.isVerified = builder.isVerified;
    }

    // ── Domain behaviour ─────────────────────────────────────────────────────

    public void updateProfile(String companyName, String industry, String location, String website) {
        this.companyName = companyName;
        this.industry = industry;
        this.location = location;
        this.website = website;
    }

    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(isVerified);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Integer getCompanyId() {
        return companyId;
    }

    public User getUser() {
        return user;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getIndustry() {
        return industry;
    }

    public String getLocation() {
        return location;
    }

    public String getWebsite() {
        return website;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company company)) return false;
        return companyId != null && Objects.equals(companyId, company.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId);
    }

    @Override
    public String toString() {
        return "Company{companyId=" + companyId + ", companyName='" + companyName + "', isVerified=" + isVerified + '}';
    }

    public static class Builder {
        private Integer companyId;
        private User user;
        private String companyName;
        private String industry;
        private String location;
        private String website;
        private Boolean isVerified = false;

        public Builder setCompanyId(Integer companyId) {
            this.companyId = companyId;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setCompanyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder setIndustry(String industry) {
            this.industry = industry;
            return this;
        }

        public Builder setLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder setWebsite(String website) {
            this.website = website;
            return this;
        }

        public Builder setIsVerified(Boolean isVerified) {
            this.isVerified = isVerified;
            return this;
        }

        public Company build() {
            return new Company(this);
        }
    }
}
