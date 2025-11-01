package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
public class UpdateProjectRequest {
    private UUID mentorId;

    @NotBlank(message = "Tiêu đề dự án không được để trống")
    private String title;
    private String description;
    @NotBlank(message = "Trạng thái dự án không được để trống")
    private String status;

    private LocalDate startDate;
    private LocalDate endDate;

    @Positive(message = "Ngân sách phải là một số dương")
    private BigDecimal budget;

    private Set<UUID> skillIds;
}
