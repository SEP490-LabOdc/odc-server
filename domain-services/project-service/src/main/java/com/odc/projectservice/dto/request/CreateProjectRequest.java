package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
public class CreateProjectRequest {
    @NotNull(message = "Id công ty không được để trống")
    private UUID companyId;
    private UUID mentorId;

    @NotBlank(message = "Tiêu đề dự án không được để trống")
    @Size(max = 255, message = "Tiêu đề dự án không được vượt quá 255 ký tự")
    private String title;

    private String description;

    @NotBlank(message = "Trạng thái dự án không được để trống")
    private String status;

    private LocalDate startDate;
    private LocalDate endDate;

    @Positive(message = "Ngân sách dự án phải là số dương")
    private BigDecimal budget;

    private Set<UUID> skillIds;
}
