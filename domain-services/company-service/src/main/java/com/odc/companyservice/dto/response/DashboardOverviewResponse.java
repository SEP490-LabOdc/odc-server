package com.odc.companyservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardOverviewResponse {
    private Long pendingCompanies;
    private Long activeCompanies;
}
