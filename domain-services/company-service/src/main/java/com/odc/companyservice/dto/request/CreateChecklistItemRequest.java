package com.odc.companyservice.dto.request;

import lombok.Getter;

@Getter
public class CreateChecklistItemRequest {
    private String templateItemId, completedById;
    private Boolean isChecked;
}
