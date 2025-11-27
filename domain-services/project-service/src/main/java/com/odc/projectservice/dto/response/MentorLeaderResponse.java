package com.odc.projectservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorLeaderResponse {
    private UUID projectMemberId;
    private UUID userId;
    private UUID projectId;
    private String fullName;
    private String email;
    private Boolean isLeader;
}