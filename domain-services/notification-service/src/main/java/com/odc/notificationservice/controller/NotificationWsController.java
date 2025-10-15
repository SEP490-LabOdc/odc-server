package com.odc.notificationservice.controller;

import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class NotificationWsController {
    private final NotificationService notificationService;

    @MessageMapping("/users/{userId}/notifications/{notificationRecipientId}/read")
    @SendToUser("/queue/notifications")
    public GetNotificationResponse markNotificationRecipientAsRead(UUID userId, UUID notificationRecipientId) {
        return notificationService.markAsRead(notificationRecipientId).getData();
    }


}
