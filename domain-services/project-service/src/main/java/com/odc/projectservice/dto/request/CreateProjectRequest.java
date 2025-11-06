package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateProjectRequest {
    @NotNull(message = "Id công ty không được để trống")
    private UUID companyId;

    @NotBlank(message = "Tiêu đề dự án không được để trống")
    @Size(max = 255, message = "Tiêu đề dự án không được vượt quá 255 ký tự")
    private String title;

    private String description;

    private Set<UUID> skillIds;
}
