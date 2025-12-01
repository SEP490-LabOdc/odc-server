package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class FeedbackResponse {
    private UUID id;
    private UUID userId;
    private String content;
    private List<String> attachmentUrls;
    private LocalDateTime createdAt;
}

