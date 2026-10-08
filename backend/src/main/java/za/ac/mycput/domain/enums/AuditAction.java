package za.ac.mycput.domain.enums;

/** Events recorded in the admin activity log. Stored by name. */
public enum AuditAction {
    // Security events
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    PASSWORD_CHANGED,
    USER_REGISTERED,
    // Administrator actions
    COMPANY_VERIFIED,
    COMPANY_VERIFICATION_REVOKED,
    USER_ENABLED,
    USER_DISABLED,
    JOB_DELETED_BY_ADMIN
}
