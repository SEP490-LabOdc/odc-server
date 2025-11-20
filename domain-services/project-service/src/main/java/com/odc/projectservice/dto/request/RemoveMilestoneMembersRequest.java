package com.odc.projectservice.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RemoveMilestoneMembersRequest {
    private UUID milestoneId;
    private List<UUID> projectMemberIds;
}

