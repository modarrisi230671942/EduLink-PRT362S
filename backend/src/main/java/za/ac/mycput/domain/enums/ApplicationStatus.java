package za.ac.mycput.domain.enums;

import jakarta.persistence.Converter;

/** Where an application is in the hiring process. Stored in lowercase. */
public enum ApplicationStatus implements PersistableEnum {
    PENDING, REVIEWED, ACCEPTED, REJECTED;

    @Override
    public String dbValue() {
        return name().toLowerCase();
    }

    /** Human-readable label used in notification messages. */
    public String label() {
        return switch (this) {
            case PENDING -> "pending";
            case REVIEWED -> "under review";
            case ACCEPTED -> "accepted";
            case REJECTED -> "unsuccessful";
        };
    }

    @Converter(autoApply = true)
    public static class DbConverter extends PersistableEnumConverter<ApplicationStatus> {
        public DbConverter() {
            super(ApplicationStatus.class);
        }
    }
}
