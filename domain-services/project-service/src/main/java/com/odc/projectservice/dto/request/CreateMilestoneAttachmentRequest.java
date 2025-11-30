package com.odc.projectservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateMilestoneAttachmentRequest {
    private String name;
    private String fileName;
    private String url;
}

