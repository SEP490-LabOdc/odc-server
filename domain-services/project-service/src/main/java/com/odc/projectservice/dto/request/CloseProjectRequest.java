package com.odc.projectservice.dto.request;

import lombok.Data;

@Data
public class CloseProjectRequest {
    private String reason;
    private String content;
}