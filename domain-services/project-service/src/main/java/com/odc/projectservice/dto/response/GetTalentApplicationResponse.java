package com.odc.projectservice.dto.response;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class GetTalentApplicationResponse {
    private UUID id;
    private UUID userId;
    private UUID projectId;
    private String projectName;
    private String cvUrl;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
