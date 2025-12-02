package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReportResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;

    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;
    private String reporterAvatar;

    private UUID recipientId;

    private String reportType;
    private String status;

    private String content;
    private List<String> attachmentsUrl;

    private LocalDate reportingDate;
    private LocalDateTime createdAt;
    private String feedback;

    private UUID milestoneId;
    private String milestoneTitle;
}