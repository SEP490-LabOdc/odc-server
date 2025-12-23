package com.odc.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDashboardStatisticResponse {
    private Long totalMentors;
    private Long totalStudents;
}
