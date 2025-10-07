package com.odc.notificationservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationRestController {
    private final NotificationService notificationService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<GetNotificationResponse>>> getNotifications(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId, null));
    }

    @GetMapping("/users/{userId}/unread")
    public ResponseEntity<ApiResponse<List<GetNotificationResponse>>> getUnreadNotifications(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId, false));
    }
}
