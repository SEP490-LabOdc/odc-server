package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Tiêu đề dự án không được để trống")
    @Size(max = 255, message = "Tiêu đề dự án không được vượt quá 255 ký tự")
    private String title;

    private String description;

    @Positive(message = "Ngân sách phải là một số dương")
    private BigDecimal budget;

    private LocalDate startDate;

    private LocalDate endDate;

    private Set<UUID> skillIds;
}
