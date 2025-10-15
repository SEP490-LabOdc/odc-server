package com.odc.companyservice.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateChecklistRequest {
    private String templateId, companyId, assigneeId, status;
    private List<CreateChecklistItemRequest> items;
}
