package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class ProjectMemberInfoResponse {
    private UUID id;
    private String fullName;
    private String roleInProject;
    private boolean isLeader;
}
