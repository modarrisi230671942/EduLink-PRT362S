package za.ac.mycput.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import za.ac.mycput.repository.UserRepository;

import java.security.Principal;

/**
 * Secures the WebSocket channel:
 * <ul>
 *   <li>CONNECT must carry a valid JWT ("Authorization: Bearer ...") for an active account.</li>
 *   <li>SUBSCRIBE is only allowed to the caller's own queues ({@code /user/queue/...}).</li>
 * </ul>
 * Any other attempt is rejected, which closes the connection with a STOMP ERROR frame.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";

    /** The STOMP session's user. Its name is the user ID, which is how messages are routed to it. */
    public record StompUser(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> {
                String destination = accessor.getDestination();
                if (accessor.getUser() == null || destination == null || !destination.startsWith("/user/queue/")) {
                    throw new AccessDeniedException("Subscription not allowed");
                }
            }
            case SEND -> throw new AccessDeniedException("Clients cannot send messages");
            default -> { }
        }
        return message;
    }

    private StompUser authenticate(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            throw new AccessDeniedException("Missing token");
        }
        return jwtService.verify(header.substring(BEARER.length()))
                .flatMap(userRepository::findById)
                .filter(user -> user.isActive())
                .map(user -> new StompUser(String.valueOf(user.getUserId())))
                .orElseThrow(() -> new AccessDeniedException("Invalid token"));
    }
}
