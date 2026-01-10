package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSkillRequest {
    @NotBlank(message = "Tên kỹ năng không được để trống")
    private String name;
    private String description;
    private Boolean isDeleted;
}
