package com.odc.projectservice.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateReportRequest {
    private String content;
    private List<String> attachmentsUrl;
}