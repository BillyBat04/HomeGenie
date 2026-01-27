package com.homegenie.notificationservice.controller;

import com.homegenie.notificationservice.model.Notification;
import com.homegenie.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification", description = "Notification management operations")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

        @Operation(summary = "Get user notifications", description = "Retrieve all notifications for specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Notification.class))))
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        log.info("📬 Getting notifications for user: {}", userId);
        List<Notification> notifications = notificationService.getNotificationsByUser(userId);
        return ResponseEntity.ok(notifications);
    }

        @Operation(summary = "Get unread notifications", description = "Retrieve only unread notifications for user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unread notifications retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Notification.class))))
    })
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        log.info("📬 Getting unread notifications for user: {}", userId);
        List<Notification> notifications = notificationService.getUnreadNotificationsByUser(userId);
        return ResponseEntity.ok(notifications);
    }

        @Operation(summary = "Mark as read", description = "Update notification status to read")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification marked as read")
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        log.info("👁️ Marking notification as read: {}", notificationId);
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

        @Operation(summary = "Health check", description = "Check if Notification Service is running")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
}
