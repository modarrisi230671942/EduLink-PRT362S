package za.ac.mycput.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1lZHVsaW5rLXVuaXQtdGVzdHMtb25seS0yMDI2";

    private final JwtService jwt = new JwtService(SECRET, 60);
    private final UserRepository users = mock(UserRepository.class);
    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(jwt, users);

    private final User alice = new User.Builder().setUserId(7).setEmail("alice@test.com")
            .setUserType(UserType.STUDENT).setIsActive(true).build();

    private Message<byte[]> frame(StompCommand command, String token, String destination, java.security.Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (token != null) accessor.addNativeHeader("Authorization", "Bearer " + token);
        if (destination != null) accessor.setDestination(destination);
        if (user != null) accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connectWithValidTokenIdentifiesTheUser() {
        when(users.findById(7)).thenReturn(Optional.of(alice));
        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, jwt.issue(alice).token(), null, null), null);
        assertThat(StompHeaderAccessor.wrap(result).getUser().getName()).isEqualTo("7");
    }

    @Test
    void connectWithoutOrWithForgedTokenIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "forged.token", null, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connectForDisabledAccountIsRejected() {
        User disabled = new User.Builder().setUserId(7).setEmail("alice@test.com")
                .setUserType(UserType.STUDENT).setIsActive(false).build();
        when(users.findById(7)).thenReturn(Optional.of(disabled));
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, jwt.issue(alice).token(), null, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void canOnlySubscribeToOwnUserQueues() {
        var user = new WebSocketAuthInterceptor.StompUser("7");
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/user/queue/notifications", user), null);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/queue/notifications", user), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/user/queue/notifications", null), null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
