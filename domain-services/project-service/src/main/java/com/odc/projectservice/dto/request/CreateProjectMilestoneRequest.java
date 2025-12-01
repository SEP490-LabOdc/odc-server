package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateProjectMilestoneRequest {
    @NotNull(message = "Id dự án không được để trống")
    private UUID projectId;

    @NotBlank(message = "Tiêu đề milestone không được để trống")
    @Size(max = 255, message = "Tiêu đề milestone không được vượt quá 255 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Percentage cannot be null")
    @DecimalMin(value = "0.0001", inclusive = false, message = "Phần trăm phải lớn hơn 0%")
    @DecimalMax(value = "100", inclusive = true, message = "Phần trăm không được vượt quá 100%")
    private Float percentage;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<CreateMilestoneAttachmentRequest> attachmentUrls;
}
