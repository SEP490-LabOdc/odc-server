package com.odc.notificationservice.service;

import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.entity.NotificationRecipient;
import com.odc.notificationservice.util.NotificationServiceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketPublisherImpl implements WebSocketPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(List<NotificationRecipient> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            log.warn("No recipients provided for WebSocket notification.");
            return;
        }

        recipients.forEach(recipient -> {
            try {
                // Each user gets a message with their own specific notificationRecipientId
                GetNotificationResponse response = NotificationServiceUtil.toGetNotificationResponse(recipient);
                String destination = "/queue/notifications";

                messagingTemplate.convertAndSendToUser(recipient.getUserId().toString(), destination, response);

                log.debug("Sent WebSocket notification '{}' to user {}", recipient.getNotification().getType(), recipient.getUserId());
            } catch (Exception e) {
                log.error("Error sending WebSocket notification to user {}: {}", recipient.getUserId(), e.getMessage(), e);
            }
        });
    }

    @Override
    public void broadcast(NotificationRecipient recipient) {
        try {
            GetNotificationResponse response = NotificationServiceUtil.toGetNotificationResponse(recipient);
            messagingTemplate.convertAndSend("/topic/notifications/all", response);
            log.info("Broadcast notification [{}] to all users", response.getType());
        } catch (Exception ex) {
            log.error("Failed to send WebSocket notification to all users: {}", ex.getMessage(), ex);
        }
    }
}
