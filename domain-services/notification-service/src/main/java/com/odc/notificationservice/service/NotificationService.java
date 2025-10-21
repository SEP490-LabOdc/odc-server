package com.odc.notificationservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.notificationservice.dto.response.GetNotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    ApiResponse<List<GetNotificationResponse>> getNotifications(UUID userId, Boolean readStatus);

    /**
     * Marks a notification as read, ensuring that the user has permission to do so.
     *
     * @param userId                  The ID of the user requesting the action.
     * @param notificationRecipientId The ID of the notification to mark as read.
     * @return An ApiResponse containing the updated notification response.
     */
    ApiResponse<GetNotificationResponse> markAsRead(UUID userId, UUID notificationRecipientId);

    ApiResponse<List<GetNotificationResponse>> markAllAsRead(UUID userId);
}
