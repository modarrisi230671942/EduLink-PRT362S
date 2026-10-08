package za.ac.mycput.service.support;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Builds a minimal iCalendar (RFC 5545) file with one event, which Google Calendar, Outlook and
 * Apple Calendar can all import. Times are converted from the server's time zone to UTC.
 */
public final class IcsCalendar {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private IcsCalendar() {}

    public static String event(String uid, String summary, String description, String location,
                               LocalDateTime start, LocalDateTime end) {
        return event(uid, summary, description, location, start, end, ZoneId.systemDefault(), Instant.now());
    }

    static String event(String uid, String summary, String description, String location,
                        LocalDateTime start, LocalDateTime end, ZoneId zone, Instant now) {
        StringBuilder ics = new StringBuilder()
                .append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//EduLink//Interview Scheduler//EN\r\n")
                .append("CALSCALE:GREGORIAN\r\n")
                .append("METHOD:PUBLISH\r\n")
                .append("BEGIN:VEVENT\r\n")
                .append("UID:").append(escape(uid)).append("\r\n")
                .append("DTSTAMP:").append(UTC.format(now)).append("\r\n")
                .append("DTSTART:").append(UTC.format(start.atZone(zone))).append("\r\n")
                .append("DTEND:").append(UTC.format(end.atZone(zone))).append("\r\n")
                .append("SUMMARY:").append(escape(summary)).append("\r\n")
                .append("DESCRIPTION:").append(escape(description)).append("\r\n");
        if (location != null && !location.isBlank()) {
            ics.append("LOCATION:").append(escape(location)).append("\r\n");
        }
        return ics.append("BEGIN:VALARM\r\n")
                .append("TRIGGER:-PT30M\r\n")
                .append("ACTION:DISPLAY\r\n")
                .append("DESCRIPTION:Interview in 30 minutes\r\n")
                .append("END:VALARM\r\n")
                .append("END:VEVENT\r\n")
                .append("END:VCALENDAR\r\n")
                .toString();
    }

    /** Escapes text per RFC 5545 (backslash, semicolon, comma, newline). */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }
}
