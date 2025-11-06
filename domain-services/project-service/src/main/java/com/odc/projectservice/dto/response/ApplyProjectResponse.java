package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Builder
public class ApplyProjectResponse {
    private UUID id;
    private String cvUrl;
    private String status;
    private LocalDateTime appliedAt;
}
