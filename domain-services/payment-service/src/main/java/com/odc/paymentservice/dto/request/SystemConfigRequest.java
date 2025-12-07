package com.odc.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.Map;

@Getter
public class SystemConfigRequest {
    @NotBlank(message = "Tên config không được để trống")
    @Pattern(regexp = "^[a-z]+(-[a-z]+)+-\\d+$", message = "Tên phải dạng abc-xyz-123")
    private String name;

    private String description;

    private Map<String, Object> properties;
}
