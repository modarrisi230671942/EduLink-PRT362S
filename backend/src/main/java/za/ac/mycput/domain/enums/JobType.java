package za.ac.mycput.domain.enums;

import jakarta.persistence.Converter;

/** Kind of opportunity. Stored as 'full-time', 'internship' or 'graduate'. */
public enum JobType implements PersistableEnum {
    FULL_TIME("full-time"),
    INTERNSHIP("internship"),
    GRADUATE("graduate");

    private final String dbValue;

    JobType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    @Converter(autoApply = true)
    public static class DbConverter extends PersistableEnumConverter<JobType> {
        public DbConverter() {
            super(JobType.class);
        }
    }
}
