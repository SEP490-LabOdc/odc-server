package com.odc.notificationservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.entity.NotificationRecipient;
import com.odc.notificationservice.repository.NotificationRecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRecipientRepository notificationRecipientRepository;

    @Override
    public ApiResponse<List<GetNotificationResponse>> getNotifications(UUID userId, Boolean readStatus) {
        List<GetNotificationResponse> data = notificationRecipientRepository
                .findAllNotificationsByUserIdAndReadStatus(userId, readStatus)
                .stream()
                .map(this::toGetNotificationResponse)
                .toList();

        return ApiResponse.success(data);
    }

    @Override
    public ApiResponse<GetNotificationResponse> markAsRead(UUID notificationRecipientId) {
        NotificationRecipient recipient = notificationRecipientRepository
                .findByIdAndReadStatus(notificationRecipientId, false)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo."));

        recipient.setReadStatus(true);
        recipient.setReadAt(Instant.now());
        notificationRecipientRepository.save(recipient);

        return ApiResponse.success("Thao tác thành công.", toGetNotificationResponse(recipient));
    }

    @Override
    public ApiResponse<List<GetNotificationResponse>> markAllAsRead(UUID userId) {
        return ApiResponse.success("Thao tác thành công.", notificationRecipientRepository
                .findAllByUserIdAndReadStatus(userId, false)
                .stream().map(notificationRecipient -> {
                    notificationRecipient.setReadStatus(true);
                    notificationRecipient.setReadAt(Instant.now());
                    notificationRecipientRepository.save(notificationRecipient);
                    return toGetNotificationResponse(notificationRecipient);
                })
                .toList());
    }

    private GetNotificationResponse toGetNotificationResponse(NotificationRecipient notificationRecipient) {
        return GetNotificationResponse
                .builder()
                .notificationRecipientId(notificationRecipient.getId())
                .type(notificationRecipient.getNotification().getType())
                .title(notificationRecipient.getNotification().getTitle())
                .content(notificationRecipient.getNotification().getContent())
                .data(notificationRecipient.getNotification().getData())
                .category(notificationRecipient.getNotification().getCategory())
                .priority(notificationRecipient.getNotification().getPriority())
                .deepLink(notificationRecipient.getNotification().getDeepLink())
                .sentAt(notificationRecipient.getNotification().getSentAt())
                .readStatus(notificationRecipient.getReadStatus())
                .build();
    }
}
