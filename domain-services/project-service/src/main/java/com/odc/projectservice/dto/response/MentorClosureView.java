package com.odc.projectservice.dto.response;

import com.odc.common.constant.ProjectClosureStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MentorClosureView {
    private UUID id;
    private ProjectClosureStatus status;
    private String reason;
    private String summary;
    private String labAdminComment;
    private String companyComment;
    private LocalDateTime createdAt;
}

