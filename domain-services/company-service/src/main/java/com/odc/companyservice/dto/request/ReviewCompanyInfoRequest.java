package com.odc.companyservice.dto.request;

import com.odc.company.v1.CreateChecklistRequest;
import lombok.Getter;

@Getter
public class ReviewCompanyInfoRequest {
    private String status;
    private CreateChecklistRequest createChecklistRequest;
}
