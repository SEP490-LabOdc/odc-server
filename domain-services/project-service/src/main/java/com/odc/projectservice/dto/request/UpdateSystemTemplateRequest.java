package com.odc.projectservice.dto.request;

import lombok.Data;

@Data
public class UpdateSystemTemplateRequest {
    private String name;
    private String type;
    private String category;
    private String fileUrl;
    private String fileName;
    private String description;
}