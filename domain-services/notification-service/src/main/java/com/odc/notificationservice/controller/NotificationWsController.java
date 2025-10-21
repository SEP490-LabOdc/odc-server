package com.odc.notificationservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class NotificationWsController {
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles a WebSocket message to mark a specific notification as read for a user.
     * It includes an authorization check to ensure a user can only mark their own notifications as read.
     *
     * @param userId                  The ID of the user making the request.
     * @param notificationRecipientId The ID of the notification recipient to be marked as read.
     * @return The updated notification details, sent back to the user's private queue.
     */
    @MessageMapping("/users/{userId}/notifications/{notificationRecipientId}/read")
    @SendToUser("/queue/notifications")
    public ApiResponse<GetNotificationResponse> markNotificationRecipientAsRead(
            @DestinationVariable("userId") UUID userId,
            @DestinationVariable("notificationRecipientId") UUID notificationRecipientId) {
        // Pass both userId and notificationRecipientId to the service for authorization
        return notificationService.markAsRead(userId, notificationRecipientId);
    }

    @MessageMapping("/notify/{userId}")
    public void notify(@Payload Map<String, Object> payload, @DestinationVariable("userId") String userId) {
        String msg = payload.get("content").toString();
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", "Server response: " + msg);
    }
}
