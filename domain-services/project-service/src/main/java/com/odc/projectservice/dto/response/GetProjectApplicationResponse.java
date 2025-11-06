package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Builder
public class GetProjectApplicationResponse {
    private UUID id;
    private UUID userId;
    private String name, cvUrl, status;
    private LocalDateTime appliedAt;
}
