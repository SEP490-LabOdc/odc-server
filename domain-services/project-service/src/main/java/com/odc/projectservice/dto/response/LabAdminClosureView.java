package com.odc.projectservice.dto.response;

import com.odc.common.constant.ProjectClosureStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class LabAdminClosureView {
    private UUID id;
    private UUID projectId;
    private String projectTitle;
    private UUID createdBy;
    private String createdByName, createdByAvatar;
    private String reason;
    private String summary;
    private ProjectClosureStatus status;
    private LocalDateTime createdAt;
}

