package za.ac.mycput.service.impl;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import za.ac.mycput.service.support.NotificationPushListener;
import org.springframework.transaction.annotation.Transactional;
import za.ac.mycput.domain.Notification;
import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.NotificationType;
import za.ac.mycput.dto.CommonDtos.NotificationsResponse;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.NotificationRepository;
import za.ac.mycput.service.INotificationService;
import za.ac.mycput.service.support.DtoMapper;

@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher events;

    public NotificationServiceImpl(NotificationRepository notificationRepository, ApplicationEventPublisher events) {
        this.notificationRepository = notificationRepository;
        this.events = events;
    }

    @Override
    @Transactional
    public void notify(User recipient, NotificationType type, String message, String link) {
        String safeMessage = message.length() > 500 ? message.substring(0, 497) + "..." : message;
        Notification saved = notificationRepository.save(new Notification.Builder()
                .setUser(recipient)
                .setType(type)
                .setMessage(safeMessage)
                .setLink(link)
                .build());
        // Delivered live over WebSocket once the transaction commits (see NotificationPushListener)
        events.publishEvent(new NotificationPushListener.NotificationCreated(
                recipient.getUserId(), DtoMapper.toNotificationResponse(saved)));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationsResponse list(Integer userId) {
        var items = notificationRepository.findTop20ByUser_UserIdOrderByCreatedAtDescNotificationIdDesc(userId).stream()
                .map(DtoMapper::toNotificationResponse)
                .toList();
        return new NotificationsResponse(notificationRepository.countByUser_UserIdAndIsReadFalse(userId), items);
    }

    @Override
    @Transactional
    public void markRead(Integer userId, Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getUserId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Notification"));
        notification.markRead();
    }

    @Override
    @Transactional
    public void markAllRead(Integer userId) {
        notificationRepository.markAllRead(userId);
    }
}
