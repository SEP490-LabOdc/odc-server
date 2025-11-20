package com.odc.projectservice.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AddProjectMemberRequest {
    private UUID milestoneId;
    private List<UUID> projectMemberIds;
}
