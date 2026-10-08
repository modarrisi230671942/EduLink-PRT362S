package za.ac.mycput.security;

import org.junit.jupiter.api.Test;
import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.UserType;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1lZHVsaW5rLXVuaXQtdGVzdHMtb25seS0yMDI2";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWZvci1lZHVsaW5rLXVuaXQtdGVzdHMtMjAyNg==";

    private final User user = new User.Builder()
            .setUserId(42)
            .setEmail("alice@test.com")
            .setPasswordHash("x")
            .setUserType(UserType.STUDENT)
            .build();

    @Test
    void issuedTokenVerifiesToTheUserId() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.issue(user).token();
        assertThat(jwt.verify(token)).contains(42);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.issue(user).token();
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        assertThat(jwt.verify(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreignToken = new JwtService(OTHER_SECRET, 60).issue(user).token();
        assertThat(new JwtService(SECRET, 60).verify(foreignToken)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiresImmediately = new JwtService(SECRET, -1);
        String token = expiresImmediately.issue(user).token();
        assertThat(expiresImmediately.verify(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(new JwtService(SECRET, 60).verify("not-a-jwt")).isEmpty();
    }
}
