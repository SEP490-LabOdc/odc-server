package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSystemTemplateRequest {
    @NotBlank(message = "Tên template không được để trống")
    private String name;

    @NotBlank(message = "Loại template không được để trống (VD: REPORT_WEEKLY, TASK_MANAGEMENT)")
    private String type;

    @NotBlank(message = "File URL không được để trống")
    private String fileUrl;

    @NotBlank(message = "Tên file gốc không được để trống")
    private String fileName;

    @NotBlank(message = "Tên nhóm template không được để trống (VD: PROJECT, REPORT)")
    private String category;

    private String description;
}