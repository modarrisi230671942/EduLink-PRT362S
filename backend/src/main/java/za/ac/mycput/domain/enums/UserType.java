package za.ac.mycput.domain.enums;

import jakarta.persistence.Converter;

/** The three account roles. Stored in lowercase ('student', 'company', 'admin'). */
public enum UserType implements PersistableEnum {
    STUDENT, COMPANY, ADMIN;

    @Override
    public String dbValue() {
        return name().toLowerCase();
    }

    /** Spring Security authority name, e.g. ROLE_STUDENT. */
    public String authority() {
        return "ROLE_" + name();
    }

    @Converter(autoApply = true)
    public static class DbConverter extends PersistableEnumConverter<UserType> {
        public DbConverter() {
            super(UserType.class);
        }
    }
}
