package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProjectParticipantsResponse {
    private MentorInfoResponse mentor;
    private List<ProjectMemberInfoResponse> talents;
}
