package com.odc.notificationservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.notificationservice.dto.response.GetNotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    ApiResponse<List<GetNotificationResponse>> getNotifications(UUID userId, Boolean readStatus);
    ApiResponse<GetNotificationResponse> markAsRead(UUID notificationRecipientId);
    ApiResponse<List<GetNotificationResponse>> markAllAsRead(UUID userId);
}
