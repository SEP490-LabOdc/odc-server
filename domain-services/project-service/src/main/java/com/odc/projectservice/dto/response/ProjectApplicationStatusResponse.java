package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProjectApplicationStatusResponse {
    private UUID projectApplicationId;
    private boolean canApply;
    private String fileLink, fileName, status;
    private LocalDateTime submittedAt;
}
