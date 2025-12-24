package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateExtensionRequest {
    @NotNull(message = "Không được để trống ngày gia hạn kết thúc milestone")
    private LocalDate requestedEndDate;

    @NotNull(message = "Không được để trống ngày kết thúc hiện tại")
    private LocalDate currentEndDate;

    @NotBlank(message = "Không được để trống lý do")
    private String requestReason;
}
