package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class GetHiringProjectDetailResponse {
    private UUID projectId;
    private String projectName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int currentApplicants;
    private List<UserParticipantResponse> mentors;
    private List<SkillResponse> skills;
}
