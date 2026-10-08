package za.ac.mycput.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import za.ac.mycput.dto.CommonDtos.NotificationsResponse;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.INotificationService;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "In-app notifications for the logged-in user")
public class NotificationController {

    private final INotificationService notificationService;

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Your 20 most recent notifications and unread count")
    public NotificationsResponse list(@AuthenticationPrincipal AuthUser me) {
        return notificationService.list(me.userId());
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark one notification as read")
    public void markRead(@AuthenticationPrincipal AuthUser me, @PathVariable Integer notificationId) {
        notificationService.markRead(me.userId(), notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all your notifications as read")
    public void markAllRead(@AuthenticationPrincipal AuthUser me) {
        notificationService.markAllRead(me.userId());
    }
}
