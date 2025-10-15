package com.odc.notificationservice.util;

import com.odc.notificationservice.dto.response.GetNotificationResponse;
import com.odc.notificationservice.entity.NotificationRecipient;

public class NotificationServiceUtil {
    public static GetNotificationResponse toGetNotificationResponse(NotificationRecipient notificationRecipient) {
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
