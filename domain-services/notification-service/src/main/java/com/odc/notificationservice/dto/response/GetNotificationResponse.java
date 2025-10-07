package com.odc.notificationservice.dto.response;

import lombok.Builder;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
@Setter
public class GetNotificationResponse {
    private UUID notificationRecipientId;
    private String type;
    private String title;
    private String content;
    private Map<String, Object> data;
    private String category;
    private String priority;
    private String deepLink;
    private Instant sentAt;
    private Boolean readStatus;
}
