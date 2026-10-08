package za.ac.mycput.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    /** A clock the test can move forward. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-28T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void locksAfterFiveFailuresAndUnlocksAfterFifteenMinutes() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(clock);

        for (int i = 0; i < 4; i++) {
            service.recordFailure("Alice@Test.com");
        }
        assertThat(service.minutesLocked("alice@test.com")).isZero();

        service.recordFailure("alice@test.com");
        assertThat(service.minutesLocked("alice@test.com")).isEqualTo(15);

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(service.minutesLocked("alice@test.com")).isZero();
    }

    @Test
    void successfulLoginResetsTheCounter() {
        LoginAttemptService service = new LoginAttemptService(new MutableClock());
        for (int i = 0; i < 4; i++) {
            service.recordFailure("bob@test.com");
        }
        service.recordSuccess("bob@test.com");
        service.recordFailure("bob@test.com");
        assertThat(service.minutesLocked("bob@test.com")).isZero();
    }
}
