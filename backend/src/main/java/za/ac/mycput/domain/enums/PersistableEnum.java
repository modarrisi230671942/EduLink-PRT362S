package za.ac.mycput.domain.enums;

/**
 * An enum whose database value differs from its Java name,
 * e.g. JobType.FULL_TIME is stored as 'full-time' to match the MySQL ENUM column.
 */
public interface PersistableEnum {
    String dbValue();
}
