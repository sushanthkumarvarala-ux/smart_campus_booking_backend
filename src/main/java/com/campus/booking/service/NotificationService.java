package com.campus.booking.service;

import com.campus.booking.dto.notification.NotificationResponse;
import com.campus.booking.entity.User;

import java.util.List;

public interface NotificationService {

    void sendNotification(User user, String message);

    List<NotificationResponse> getUserNotifications(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    long getUnreadCount(Long userId);
}
