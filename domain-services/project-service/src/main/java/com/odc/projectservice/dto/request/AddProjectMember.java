package com.odc.projectservice.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class AddProjectMember {
    private UUID projectMemberId;
}
