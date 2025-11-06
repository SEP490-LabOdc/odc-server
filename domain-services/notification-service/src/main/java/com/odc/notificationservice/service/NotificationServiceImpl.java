package com.odc.notificationservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.entity.NotificationRecipient;
import com.odc.notificationservice.repository.NotificationRecipientRepository;
import com.odc.notificationservice.util.NotificationServiceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final WebSocketPublisher webSocketPublisher; // Injected for real-time updates

    @Override
    public ApiResponse<List<GetNotificationResponse>> getNotifications(UUID userId, Boolean readStatus) {
        List<GetNotificationResponse> data = notificationRecipientRepository
                .findAllNotificationsByUserIdAndReadStatus(userId, readStatus)
                .stream()
                .map(NotificationServiceUtil::toGetNotificationResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(data);
    }

    @Override
    @Transactional
    public ApiResponse<GetNotificationResponse> markAsRead(UUID userId, UUID notificationRecipientId) {
        NotificationRecipient recipient = notificationRecipientRepository
                .findById(notificationRecipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo."));

        // Security Check: Ensure the user owns this notification
        if (!recipient.getUserId().equals(userId)) {
            throw new SecurityException("User does not have permission to mark this notification as read.");
        }

        if (recipient.getReadStatus()) {
            return ApiResponse.success("Thông báo đã được đọc trước đó.", NotificationServiceUtil.toGetNotificationResponse(recipient));
        }

        recipient.setReadStatus(true);
        recipient.setReadAt(Instant.now());
        notificationRecipientRepository.save(recipient);

        // Real-time update: Broadcast the change to all clients of this user
        webSocketPublisher.publish(List.of(recipient));

        return ApiResponse.success("Thao tác thành công.", NotificationServiceUtil.toGetNotificationResponse(recipient));
    }

    @Override
    @Transactional
    public ApiResponse<List<GetNotificationResponse>> markAllAsRead(UUID userId) {
        List<NotificationRecipient> recipientsToUpdate = notificationRecipientRepository
                .findAllByUserIdAndReadStatus(userId, false);

        if (recipientsToUpdate.isEmpty()) {
            return ApiResponse.success("Không có thông báo chưa đọc nào.", List.of());
        }

        Instant now = Instant.now();
        recipientsToUpdate.forEach(recipient -> {
            recipient.setReadStatus(true);
            recipient.setReadAt(now);
        });

        notificationRecipientRepository.saveAll(recipientsToUpdate);

        // Real-time update: Broadcast the changes to all clients of this user
        webSocketPublisher.publish(recipientsToUpdate);

        List<GetNotificationResponse> responseData = recipientsToUpdate.stream()
                .map(NotificationServiceUtil::toGetNotificationResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Tất cả thông báo đã được đánh dấu là đã đọc.", responseData);
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteNotificationRecipient(UUID notificationRecipientId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new SecurityException("Người dùng chưa được xác thực.");
        }

        UUID currentUserId;
        try {
            currentUserId = (UUID) authentication.getPrincipal();
        } catch (ClassCastException e) {
            throw new SecurityException("Lỗi xác thực người dùng.");
        }

        NotificationRecipient recipient = notificationRecipientRepository
                .findById(notificationRecipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo."));

        if (!recipient.getUserId().equals(currentUserId)) {
            throw new SecurityException("Người dùng không có quyền xóa người nhận thông báo này.");
        }
        notificationRecipientRepository.delete(recipient);

        return ApiResponse.success("Xóa thông báo thành công.", null);
    }
}
