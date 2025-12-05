package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SystemTemplateResponse {
    private UUID id;
    private String name;
    private String type;
    private String category;
    private String fileUrl;
    private String fileName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}