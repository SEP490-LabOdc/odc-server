package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class AddBatchProjectMembersResponse {
    private List<UUID> addedUserIds;
    private List<UUID> skippedUserIds;
    private int totalAdded;
    private int totalSkipped;
}
