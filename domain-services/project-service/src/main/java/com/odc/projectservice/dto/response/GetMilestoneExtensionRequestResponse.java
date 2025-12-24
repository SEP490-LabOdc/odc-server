package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class GetMilestoneExtensionRequestResponse {

    private UUID id;

    private UUID milestoneId;
    private String milestoneTitle;

    private UUID projectId;
    private String projectTitle;

    private LocalDate currentEndDate;
    private LocalDate requestedEndDate;

    private String requestReason;
    private String status;

    private LocalDateTime createdAt;   // ngày mentor gửi request
    private UUID requestedBy;

    private LocalDateTime reviewedAt;
    private UUID reviewedBy;
    private String reviewReason;
}

