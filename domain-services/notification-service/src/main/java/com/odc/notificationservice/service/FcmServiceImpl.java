package com.odc.notificationservice.service;

import com.google.firebase.messaging.*;
import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {
    private static final int MAX_BATCH_SIZE = 500;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public void sendToDevices(GetNotificationResponse notification, List<String> deviceTokens) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn("No device tokens provided. Skip sending FCM notification.");
            return;
        }

        log.info("Sending FCM notification '{}' to {} devices", notification.getType(), deviceTokens.size());

        // Split tokens into smaller batches (max 500 per FCM batch)
        List<List<String>> batches = splitIntoBatches(deviceTokens);

        for (List<String> batch : batches) {
            MulticastMessage message = buildMulticastMessage(notification, batch);

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                log.info("FCM batch sent: success={}, failure={}",
                        response.getSuccessCount(), response.getFailureCount());

                // Handle invalid tokens (optional)
                handleFailedTokens(batch, response);

            } catch (FirebaseMessagingException e) {
                log.error("Error sending FCM message batch: {}", e.getMessage(), e);
            }
        }
    }

    private MulticastMessage buildMulticastMessage(GetNotificationResponse notification, List<String> tokens) {
        return MulticastMessage.builder()
                .putAllData(convertNotificationToMap(notification))
                .addAllTokens(tokens)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getContent())
                        .build())
                .build();
    }

    private Map<String, String> convertNotificationToMap(GetNotificationResponse n) {
        Map<String, String> map = new HashMap<>();
        map.put("notificationRecipientId", String.valueOf(n.getNotificationRecipientId()));
        map.put("type", n.getType());
        map.put("category", n.getCategory());
        map.put("priority", n.getPriority());
        map.put("deepLink", n.getDeepLink());
        map.put("sentAt", n.getSentAt() != null ? n.getSentAt().toString() : "");
        map.put("readStatus", String.valueOf(n.getReadStatus()));

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
                String errorCode = sendResponses.get(i).getException().getErrorCode().toString();

                if ("registration-token-not-registered".equals(errorCode)) {
                    invalidTokens.add(token);
                }
            }
        }

        if (!invalidTokens.isEmpty()) {
            log.warn("Removing {} invalid FCM tokens", invalidTokens.size());
            deviceTokenRepository.deleteAllByTokenIn(invalidTokens);
        }
    }

    private List<List<String>> splitIntoBatches(List<String> tokens) {
        return new ArrayList<>(
                tokens.stream()
                        .collect(Collectors.groupingBy(token -> tokens.indexOf(token) / MAX_BATCH_SIZE))
                        .values()
        );
    }
}
