package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ApplyProjectRequest {
    @NotNull(message = "userId không được bỏ trống")
    private UUID userId;

    @NotNull(message = "projectId không được bỏ trống")
    private UUID projectId;

    @NotBlank(message = "cvUrl không được bỏ trống")
    private String cvUrl;
}
