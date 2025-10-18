package com.odc.notificationservice.service;

import com.odc.notificationservice.entity.NotificationRecipient;

import java.util.List;

public interface WebSocketPublisher {
    /**
     * Publishes a personalized notification to each recipient in the list.
     * @param recipients The list of notification recipients.
     */
    void publish(List<NotificationRecipient> recipients);

    /**
     * Broadcasts a notification to all connected users.
     * @param recipient The notification to broadcast.
     */
    void broadcast(NotificationRecipient recipient);
}
