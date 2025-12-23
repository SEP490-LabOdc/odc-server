package com.odc.projectservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectMonthlyStatisticResponse {
    private String month; // YYYY-MM
    private Long total;
}
