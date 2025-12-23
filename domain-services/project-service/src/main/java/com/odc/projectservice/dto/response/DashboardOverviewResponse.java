package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardOverviewResponse {
    private Long pendingProjects;
    private Long activeProjects;
    private Long recruitingProjects;

    private Long joinedStudents;
    private Long availableMentors;
}
