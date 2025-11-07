package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProjectDocumentResponse {
    private UUID id;
    private UUID projectId;
    private String documentName;
    private String documentUrl;
    private String documentType;
    private LocalDateTime uploadedAt;
}
