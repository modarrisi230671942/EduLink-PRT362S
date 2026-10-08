package za.ac.mycput.domain;

import jakarta.persistence.*;
import za.ac.mycput.domain.enums.UserType;

import java.time.LocalDateTime;
import java.util.Objects;

/** A login account. Every Student and Company has exactly one User; admins have no profile. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    /** BCrypt hash — never the raw password. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Convert(converter = UserType.DbConverter.class)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // JPA requires a default constructor
    protected User() {}

    private User(Builder builder) {
        this.userId = builder.userId;
        this.email = builder.email;
        this.passwordHash = builder.passwordHash;
        this.userType = builder.userType;
        this.isActive = builder.isActive;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ── Domain behaviour ─────────────────────────────────────────────────────

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Integer getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserType getUserType() {
        return userType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return userId != null && Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        // Deliberately excludes passwordHash
        return "User{userId=" + userId + ", email='" + email + "', userType=" + userType + ", isActive=" + isActive + '}';
    }

    public static class Builder {
        private Integer userId;
        private String email;
        private String passwordHash;
        private UserType userType;
        private Boolean isActive = true;

        public Builder setUserId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder setUserType(UserType userType) {
            this.userType = userType;
            return this;
        }

        public Builder setIsActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
