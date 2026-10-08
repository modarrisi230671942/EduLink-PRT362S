package za.ac.mycput.service.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import za.ac.mycput.dto.CommonDtos.NotificationResponse;

/**
 * Pushes each new notification to its recipient's open browser tabs over WebSocket.
 * Runs only AFTER the database transaction commits, so the browser never receives a notification
 * for a change that was rolled back, and any data it reloads is already saved.
 */
@Component
public class NotificationPushListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationPushListener.class);

    /** Published by NotificationServiceImpl when a notification is saved. */
    public record NotificationCreated(Integer recipientUserId, NotificationResponse notification) {}

    private final SimpMessagingTemplate messaging;

    public NotificationPushListener(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onNotificationCreated(NotificationCreated event) {
        try {
            messaging.convertAndSendToUser(String.valueOf(event.recipientUserId()), "/queue/notifications",
                    event.notification());
        } catch (RuntimeException e) {
            // The notification is still saved; the browser will pick it up on its next refresh
            log.warn("Could not push notification to user {}", event.recipientUserId(), e);
        }
    }
}
