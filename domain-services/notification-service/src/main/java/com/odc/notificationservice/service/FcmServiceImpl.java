package com.odc.notificationservice.service;

import com.google.firebase.messaging.*;
import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {
    private static final int MAX_BATCH_SIZE = 500;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public void sendToDevices(GetNotificationResponse notification, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("No user IDs provided. Skip sending FCM notification.");
            return;
        }

        // 1. Convert userIds from List<String> to Set<UUID>
        Set<UUID> userUUIDs = userIds.stream().map(UUID::fromString).collect(Collectors.toSet());

        // 2. Fetch actual device tokens from the database
        List<String> actualDeviceTokens = deviceTokenRepository.findDeviceTokensByUserIds(userUUIDs);

        if (actualDeviceTokens.isEmpty()) {
            log.warn("No device tokens found for the given users. Skip sending FCM notification.");
            return;
        }

        log.info("Sending FCM notification '{}' to {} devices for {} users", notification.getType(), actualDeviceTokens.size(), userIds.size());

        // 3. Split tokens into smaller batches (more efficient implementation)
        List<List<String>> batches = splitIntoBatches(actualDeviceTokens);

        for (List<String> batch : batches) {
            // Note: The notification payload is generic for all users in the batch.
            // Recipient-specific data (like notificationRecipientId) cannot be included here.
            MulticastMessage message = buildMulticastMessage(notification, batch);

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info("FCM batch sent: success={}, failure={}",
                        response.getSuccessCount(), response.getFailureCount());

                // 4. Handle invalid tokens
                handleFailedTokens(batch, response);

            } catch (FirebaseMessagingException e) {
                log.error("Error sending FCM message batch: {}", e.getMessage(), e);
            }
        }
    }

    private MulticastMessage buildMulticastMessage(GetNotificationResponse notification, List<String> tokens) {
        return MulticastMessage.builder()
                .putAllData(convertNotificationToGenericMap(notification))
                .addAllTokens(tokens)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getContent())
                        .build())
                .build();
    }

    private Map<String, String> convertNotificationToGenericMap(GetNotificationResponse n) {
        Map<String, String> map = new HashMap<>();
        // Payload should be generic for a multicast message.
        // Do not include recipient-specific data like 'notificationRecipientId' or 'readStatus'.
        map.put("type", n.getType());
        map.put("category", n.getCategory());
        map.put("priority", n.getPriority());
        map.put("deepLink", n.getDeepLink());
        map.put("sentAt", n.getSentAt() != null ? n.getSentAt().toString() : "");

        if (n.getData() != null) {
            n.getData().forEach((k, v) -> map.put(k, String.valueOf(v)));
        }

        return map;
    }

    private void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> sendResponses = response.getResponses();

        for (int i = 0; i < sendResponses.size(); i++) {
            if (!sendResponses.get(i).isSuccessful()) {
                String token = tokens.get(i);
                FirebaseMessagingException exception = sendResponses.get(i).getException();

                // Add a null check for safety and use the modern way to get the error code
                if (exception != null && exception.getMessagingErrorCode() != null) {
                    if (MessagingErrorCode.UNREGISTERED.equals(exception.getMessagingErrorCode())) {
                        invalidTokens.add(token);
                    }
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            log.warn("Removing {} invalid FCM tokens", invalidTokens.size());
            // The repository method should be transactional
            deviceTokenRepository.deleteAllByTokenIn(invalidTokens);
        }
    }

    /**
     * More efficient batch splitting with O(n) complexity.
     */
    private List<List<String>> splitIntoBatches(List<String> tokens) {
        int totalSize = tokens.size();
        if (totalSize == 0) {
            return new ArrayList<>();
        }

        int numBatches = (int) Math.ceil((double) totalSize / MAX_BATCH_SIZE);

        return IntStream.range(0, numBatches)
                .mapToObj(i -> tokens.subList(i * MAX_BATCH_SIZE, Math.min((i + 1) * MAX_BATCH_SIZE, totalSize)))
                .collect(Collectors.toList());
    }
}
