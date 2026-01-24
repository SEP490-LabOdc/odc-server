package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateClosureRequest {
    @NotBlank(message = "Không được để trống lý do")
    private String reason;

    @NotBlank(message = "Không được để trống phần mô tả ngắn gọn")
    private String summary;
}
