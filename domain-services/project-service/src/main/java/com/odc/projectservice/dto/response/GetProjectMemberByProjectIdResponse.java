package com.odc.projectservice.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GetProjectMemberByProjectIdResponse {
    private UUID projectMemberId;
    private UUID userId;
    private String fullName, email, phone, avatarUrl, roleName;
    private Boolean isLeader, isActive;
    private LocalDateTime joinedAt, leftAt;
}
