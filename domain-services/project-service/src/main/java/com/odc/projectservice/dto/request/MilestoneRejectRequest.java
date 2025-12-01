package com.odc.projectservice.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class MilestoneRejectRequest {
    private String feedbackContent;
    private List<String> attachmentUrls;
}
