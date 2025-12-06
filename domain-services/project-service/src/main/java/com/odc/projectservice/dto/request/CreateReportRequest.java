package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateReportRequest {
    private UUID projectId;

    private UUID milestoneId;

    @NotNull(message = "Loại báo cáo không được để trống")
    private String reportType; // DAILY_REPORT, WEEKLY_REPORT...

    private String content; // Nội dung text (cho Daily)

    private List<String> attachmentsUrl; // Link file đính kèm

    private List<UUID> recipientIds;
}