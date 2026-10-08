package za.ac.mycput.service.support;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class IcsCalendarTest {

    @Test
    void buildsAValidEventInUtc() {
        String ics = IcsCalendar.event("interview-7@edulink", "Interview: Junior Developer", "Bring your CV",
                "https://meet.example.com/x", LocalDateTime.of(2026, 10, 5, 10, 0), LocalDateTime.of(2026, 10, 5, 10, 45),
                ZoneId.of("Africa/Johannesburg"), Instant.parse("2026-09-28T12:00:00Z"));

        assertThat(ics)
                .startsWith("BEGIN:VCALENDAR\r\n")
                .contains("UID:interview-7@edulink\r\n")
                // 10:00 in Johannesburg (UTC+2) is 08:00 UTC
                .contains("DTSTART:20261005T080000Z\r\n")
                .contains("DTEND:20261005T084500Z\r\n")
                .contains("LOCATION:https://meet.example.com/x\r\n")
                .endsWith("END:VCALENDAR\r\n");
    }

    @Test
    void escapesSpecialCharacters() {
        assertThat(IcsCalendar.escape("Room 4; Block B, CPUT\nBring ID"))
                .isEqualTo("Room 4\\; Block B\\, CPUT\\nBring ID");
    }
}
