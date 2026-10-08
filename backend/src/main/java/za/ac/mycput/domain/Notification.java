package za.ac.mycput.domain;

import jakarta.persistence.*;
import za.ac.mycput.domain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.Objects;

/** An in-app message shown under the navbar bell icon. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** Frontend route to open when the notification is clicked, e.g. "/student/applications". */
    @Column(name = "link")
    private String link;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected Notification() {}

    private Notification(Builder builder) {
        this.user = builder.user;
        this.type = builder.type;
        this.message = builder.message;
        this.link = builder.link;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markRead() {
        this.isRead = true;
    }

    public Integer getNotificationId() {
        return notificationId;
    }

    public User getUser() {
        return user;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public boolean isRead() {
        return Boolean.TRUE.equals(isRead);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return notificationId != null && Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }

    public static class Builder {
        private User user;
        private NotificationType type;
        private String message;
        private String link;

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setType(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setLink(String link) {
            this.link = link;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }
}
