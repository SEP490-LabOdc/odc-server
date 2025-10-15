package com.odc.companyservice.dto.request;

import lombok.Getter;

@Getter
public class CreateChecklistItemRequest {
    private String templateItemId, status, notes, completedById;
}
