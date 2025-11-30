package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    private LocalDate startDate;

    private LocalDate endDate;

    private List<String> attachmentUrls;
}
