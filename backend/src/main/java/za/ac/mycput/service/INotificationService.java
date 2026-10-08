package za.ac.mycput.service;

import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.NotificationType;
import za.ac.mycput.dto.CommonDtos.NotificationsResponse;

public interface INotificationService {

    void notify(User recipient, NotificationType type, String message, String link);

    NotificationsResponse list(Integer userId);

    void markRead(Integer userId, Integer notificationId);

    void markAllRead(Integer userId);
}
