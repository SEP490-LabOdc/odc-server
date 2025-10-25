package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProjectResponse {
    private UUID id;
    private UUID companyId;
    private UUID mentorId;
    private String title;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Set<SkillResponse> skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
