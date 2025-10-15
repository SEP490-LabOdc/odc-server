package com.odc.notificationservice.service;

import com.odc.notificationservice.entity.NotificationRecipient;

import java.util.Set;
import java.util.UUID;

public interface WebSocketPublisher {
    void publish(Set<UUID> userIds, NotificationRecipient notification);
    void broadcast(NotificationRecipient recipient);
}
