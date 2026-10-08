package za.ac.mycput.domain.enums;

/** What triggered an in-app notification. Stored by name (VARCHAR). */
public enum NotificationType {
    APPLICATION_RECEIVED,
    APPLICATION_STATUS_CHANGED,
    APPLICATION_WITHDRAWN,
    COMPANY_VERIFIED,
    COMPANY_VERIFICATION_REVOKED,
    ACCOUNT_ENABLED,
    INTERVIEW_PROPOSED,
    INTERVIEW_CONFIRMED,
    INTERVIEW_CANCELLED,
    JOB_MATCH
}
