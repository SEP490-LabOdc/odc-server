package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Builder
public class GetProjectResponse {
    private UUID id;
    private String title;
    private String description;
    private String status;
    private String startDate;
    private String endDate;
    private String budget;
    private Set<SkillResponse> skills;
}
