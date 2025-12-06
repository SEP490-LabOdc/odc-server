package com.odc.projectservice.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GetMilestoneMember {
    private UUID milestoneMemberId;
    private UUID projectMemberId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private boolean isLeader;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}

