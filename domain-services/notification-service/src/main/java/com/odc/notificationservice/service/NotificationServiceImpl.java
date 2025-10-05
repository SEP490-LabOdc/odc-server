package com.odc.notificationservice.service;

import com.odc.common.exception.ResourceNotFoundException;
import com.odc.notificationservice.entity.NotificationRecipient;
import com.odc.notificationservice.repository.NotificationRecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRecipientRepository notificationRecipientRepository;

    @Override
    public void markAsRead(UUID userId, UUID notificationRecipientId) {
        NotificationRecipient notificationRecipient = notificationRecipientRepository
                .findByIdAndUserIdAndReadStatus(notificationRecipientId, userId, false).map(
                noti -> {
                    noti.setReadStatus(true);
                    noti.setReadAt(Instant.now());
                    return noti;
                }
        )
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification."));

        notificationRecipientRepository.save(notificationRecipient);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        List<NotificationRecipient> notificationRecipients = notificationRecipientRepository
                .findAllByUserIdAndReadStatus(userId, false).stream()
                .peek(notificationRecipient -> {
                    notificationRecipient.setReadStatus(true);
                    notificationRecipient.setReadAt(Instant.now());
                }).collect(Collectors.toList());

        notificationRecipientRepository.saveAll(notificationRecipients);
    }
}
