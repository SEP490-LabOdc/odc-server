package com.odc.projectservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class UpdateMilestoneMemberRoleRequest {
    @JsonProperty("isLeader")
    private boolean isLeader;
}
