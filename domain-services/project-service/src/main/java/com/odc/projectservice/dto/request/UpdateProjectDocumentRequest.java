package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProjectDocumentRequest {

    @NotBlank(message = "Tên tài liệu không được để trống")
    @Size(max = 255, message = "Tên tài liệu không được vượt quá 255 ký tự")
    private String documentName;

    @NotBlank(message = "URL tài liệu không được để trống")
    private String documentUrl;

    @Size(max = 50, message = "Loại tài liệu không được vượt quá 50 ký tự")
    private String documentType;
}
