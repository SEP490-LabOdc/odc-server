package com.odc.notificationservice.event.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.odc.commonlib.event.EventHandler;
import com.odc.company.v1.RecipientResolverServiceGrpc;
import com.odc.company.v1.ResolveRecipientsRequest;
import com.odc.notification.v1.NotificationEvent;
import com.odc.notificationservice.entity.Notification;
import com.odc.notificationservice.entity.NotificationRecipient;
import com.odc.notificationservice.repository.NotificationRecipientRepository;
import com.odc.notificationservice.repository.NotificationRepository;
import com.odc.notificationservice.service.FcmService;
import com.odc.notificationservice.service.WebSocketPublisher;
import com.odc.notificationservice.util.NotificationServiceUtil;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationHandler implements EventHandler {
    private final FcmService fcmService;
    private final WebSocketPublisher webSocketPublisher;
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final ManagedChannel userServiceChannel;

    @Override
    public String getTopic() {
        return "notifications";
    }

    @Override
    public void handle(byte[] eventPayload) {
        try {
            NotificationEvent notificationEvent = NotificationEvent.parseFrom(eventPayload);
            log.info("received notification event: {}", notificationEvent);

            Notification notification = Notification
                    .builder()
                    .type(notificationEvent.getType())
                    .title(notificationEvent.getTitle())
                    .content(notificationEvent.getContent())
                    .data(new HashMap<>(notificationEvent.getDataMap()))
                    .category(notificationEvent.getCategory())
                    .priority(notificationEvent.getPriority())
                    .deepLink(notificationEvent.getDeepLink())
                    .sentAt(Instant.ofEpochMilli(notificationEvent.getCreatedAt()))
                    .build();

            notificationRepository.save(notification);

            RecipientResolverServiceGrpc.RecipientResolverServiceBlockingStub blockingStub = RecipientResolverServiceGrpc.newBlockingStub(userServiceChannel);

            List<String> userIds = blockingStub.resolveRecipients(
                    ResolveRecipientsRequest.newBuilder()
                            .setTarget(notificationEvent.getTarget())
                            .build()
            ).getUserIdsList();

            log.info("user id list: {}", userIds);

            if (userIds.isEmpty()) {
                log.warn("No recipients found for notification event");
                return;
            }

            // 1. Prepare all recipients and save them in a single batch operation
            List<NotificationRecipient> recipients = userIds.stream()
                    .map(userId -> NotificationRecipient
                            .builder()
                            .notification(notification)
                            .userId(UUID.fromString(userId))
                            .deliveredVia(notificationEvent.getChannelsList().toString())
                            .readStatus(false)
                            .build())
                    .collect(Collectors.toList());

            notificationRecipientRepository.saveAll(recipients);

            // 2. Process channels after collecting all recipients
            notificationEvent.getChannelsList().forEach(channel -> {
                switch (channel) {
                    case CHANNEL_UNSPECIFIED:
                        // TODO
                        break;

                    case MOBILE:
                        // The first recipient is used to get generic notification details.
                        // The service will then fetch tokens for all userIds.
                        fcmService.sendToDevices(
                                NotificationServiceUtil.toGetNotificationResponse(recipients.get(0)),
                                userIds
                        );
                        break; // Fixed missing break

                    case WEB:
                        // The service will handle sending a personalized message to each recipient.
                        webSocketPublisher.publish(recipients);
                        break;
                }
            });

        } catch (InvalidProtocolBufferException ex) {
            log.error("failed to parse notification event payload: {}", ex.getMessage());
        }
    }
}
