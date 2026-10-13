package com.campus.booking.controller;

import com.campus.booking.dto.common.ApiResponse;
import com.campus.booking.dto.notification.NotificationResponse;
import com.campus.booking.security.UserPrincipal;
import com.campus.booking.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Notifications", description = "Endpoints for user alerts, approval notifications, and read statuses")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get current user notifications ordered newest first")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<NotificationResponse> notifications = notificationService.getUserNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a specific notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.message("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read for current user")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.message("All notifications marked as read"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count for badge rendering")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(count);
    }
}
