package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProjectMilestoneResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private List<TalentMentorInfoResponse> talents;
    private List<TalentMentorInfoResponse> mentors;
}
