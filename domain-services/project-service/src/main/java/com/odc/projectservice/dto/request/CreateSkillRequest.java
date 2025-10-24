package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSkillRequest {
    @NotBlank(message = "Tên kỹ năng không được để trống")
    private String name;
    private String description;
}
