package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class UserParticipantResponse {
    private UUID id;
    private String name;
    private String roleName;
    private boolean isLeader;
}
