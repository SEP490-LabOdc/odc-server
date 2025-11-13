package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProjectStatusRequest {

    @NotBlank(message = "Trạng thái dự án không được để trống.")
    private String status;
}
