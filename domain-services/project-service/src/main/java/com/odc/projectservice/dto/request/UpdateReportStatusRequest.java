package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateReportStatusRequest {
    @NotBlank(message = "Status không được để trống")
    private String status; // APPROVED, REJECTED...

    private String feedback; // Ghi chú phản hồi (lý do từ chối...)
}