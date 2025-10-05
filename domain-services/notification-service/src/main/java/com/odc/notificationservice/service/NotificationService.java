package com.odc.notificationservice.service;

import java.util.UUID;

public interface NotificationService {
    void markAsRead(UUID userId, UUID notificationRecipientId);

    void markAllAsRead(UUID userId);
}
