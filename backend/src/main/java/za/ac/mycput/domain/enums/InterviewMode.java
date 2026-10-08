package za.ac.mycput.domain.enums;

/** How an interview takes place. Stored by name. */
public enum InterviewMode {
    ONLINE, IN_PERSON, PHONE;

    public String label() {
        return switch (this) {
            case ONLINE -> "Online";
            case IN_PERSON -> "In person";
            case PHONE -> "Phone";
        };
    }
}
