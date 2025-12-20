package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateReportStatusRequest {

    @NotNull(message = "MilestoneId không được để trống")
    private UUID milestoneId;
    @NotBlank(message = "Status không được để trống")
    private String status; // APPROVED, REJECTED...
    private String feedback; // Ghi chú phản hồi (lý do từ chối...)
}