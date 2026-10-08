package za.ac.mycput.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection: after {@value #MAX_FAILURES} wrong passwords for the same email,
 * that email is locked for {@link #LOCK_DURATION}. Kept in memory, so it resets on restart.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant lockedUntil) {}

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    /** Minutes left on the lock, or 0 if the email may try to log in. */
    public long minutesLocked(String email) {
        Attempts current = attempts.get(key(email));
        if (current == null || current.lockedUntil() == null) {
            return 0;
        }
        Duration left = Duration.between(clock.instant(), current.lockedUntil());
        if (left.isNegative() || left.isZero()) {
            attempts.remove(key(email));
            return 0;
        }
        return Math.max(1, (left.toSeconds() + 59) / 60);
    }

    /** Records a wrong password. Returns true if this failure has just locked the email. */
    public boolean recordFailure(String email) {
        Attempts updated = attempts.compute(key(email), (k, current) -> {
            int failures = (current == null ? 0 : current.failures()) + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? clock.instant().plus(LOCK_DURATION) : null;
            return new Attempts(failures, lockedUntil);
        });
        return updated.failures() == MAX_FAILURES;
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
