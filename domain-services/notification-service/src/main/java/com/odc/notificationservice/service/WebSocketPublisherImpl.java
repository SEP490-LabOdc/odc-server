package com.odc.notificationservice.service;

import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.entity.NotificationRecipient;
import com.odc.notificationservice.util.NotificationServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketPublisherImpl implements WebSocketPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(Set<UUID> userIds, NotificationRecipient notification) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("No user IDs provided for WebSocket notification of type {}", notification.getNotification().getType());
            return;
        }

        userIds.forEach(userId -> {
            try {
                String destination = String.format("/topic/notifications/%s", userId);
                messagingTemplate.convertAndSend(destination, NotificationServiceUtil.toGetNotificationResponse(notification));

                log.debug("sent WebSocket notification '{}' to user {}", notification.getNotification().getType(), userId);
            } catch (Exception e) {
                log.error("error sending WebSocket notification to user {}: {}", userId, e.getMessage(), e);
            }
        });
    }

    @Override
    public void broadcast(NotificationRecipient recipient) {
        try {
            GetNotificationResponse response = NotificationServiceUtil.toGetNotificationResponse(recipient);
            messagingTemplate.convertAndSend("/topic/notifications/all", response);
            log.info("broadcast notification [{}] to all users", response.getType());
        } catch (Exception ex) {
            log.error("failed to send WebSocket notification to all users");
        }
    }
}
