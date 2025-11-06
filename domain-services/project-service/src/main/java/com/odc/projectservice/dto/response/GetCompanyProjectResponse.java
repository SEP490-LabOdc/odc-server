package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Setter
@Builder
public class GetCompanyProjectResponse {
    private UUID companyId;
    private String companyName;
    private List<GetProjectResponse> projectResponses;

    @Setter
    @Builder
    public static class GetProjectResponse {
        private UUID id;
        private String title;
        private String description;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal budget;
        private Set<SkillResponse> skills;
    }
}
