package com.odc.projectservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMilestoneAttachmentRequest {
    private String name;        // cập nhật tên hiển thị
    private String fileName;    // cập nhật tên file thực tế
    private String url;         // cập nhật link
}

