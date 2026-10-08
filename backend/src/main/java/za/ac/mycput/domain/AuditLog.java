package za.ac.mycput.domain;

import jakarta.persistence.*;
import za.ac.mycput.domain.enums.AuditAction;

import java.time.LocalDateTime;

/** One entry in the admin activity log. Entries are never edited or deleted by the application. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    /** Null for anonymous events (e.g. a failed login for an unknown email). */
    @Column(name = "actor_user_id")
    private Integer actorUserId;

    @Column(name = "actor_email", length = 100)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {}

    public AuditLog(Integer actorUserId, String actorEmail, AuditAction action, String targetType,
                    Integer targetId, String details, String ipAddress) {
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getLogId() {
        return logId;
    }

    public Integer getActorUserId() {
        return actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
