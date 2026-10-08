package za.ac.mycput.domain.enums;

import jakarta.persistence.AttributeConverter;

/**
 * Maps a {@link PersistableEnum} to and from its database value.
 * Subclasses only need to pass their enum class to the constructor.
 */
public abstract class PersistableEnumConverter<E extends Enum<E> & PersistableEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected PersistableEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.dbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.dbValue().equalsIgnoreCase(dbValue)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + dbValue);
    }
}
