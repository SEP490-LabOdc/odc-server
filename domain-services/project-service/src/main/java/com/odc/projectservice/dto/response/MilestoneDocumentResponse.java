package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class MilestoneDocumentResponse {
    private UUID id;
    private String fileName;
    private String fileUrl;
    private String s3Key;
    private LocalDateTime uploadedAt;
    private String entityId;
}