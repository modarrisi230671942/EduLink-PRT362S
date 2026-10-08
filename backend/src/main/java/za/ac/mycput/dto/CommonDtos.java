package za.ac.mycput.dto;

import org.springframework.data.domain.Page;
import za.ac.mycput.domain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Shared response shapes: pagination, notifications, errors and simple messages. */
public final class CommonDtos {

    private CommonDtos() {}

    /** A stable JSON shape for paginated results (Spring's Page is not meant to be serialised directly). */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    public record NotificationResponse(
            Integer notificationId,
            NotificationType type,
            String message,
            String link,
            boolean read,
            LocalDateTime createdAt) {}

    public record NotificationsResponse(long unreadCount, List<NotificationResponse> items) {}

    public record MessageResponse(String message) {}

    /** Every error returned by the API has this shape. {@code fieldErrors} is only present for validation errors. */
    public record ApiError(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors) {}
}
