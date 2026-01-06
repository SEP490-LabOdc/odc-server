package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PaidProjectMilestoneResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private String title;
    private BigDecimal budget;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}