package com.odc.projectservice.dto.response;

import com.odc.common.constant.ProjectClosureStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CompanyClosureView {
    private UUID id;
    private UUID projectId;
    private String summary;
    private ProjectClosureStatus status;
    private String labAdminComment;
    private LocalDateTime labAdminReviewedAt;
    private LocalDateTime createdAt;
}

