package com.odc.companyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CompanyMonthlyStatisticResponse {
    private String month; // YYYY-MM
    private Long total;
}
