package com.odc.companyservice.dto.request;

import lombok.Getter;

@Getter
public class ReviewCompanyInfoRequest {
    private String status;
    private CreateChecklistRequest createChecklistRequest;
}
