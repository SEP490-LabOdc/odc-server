package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class TalentMentorInfoResponse {
    private UUID userId;
    private String name;
    private String avatar;
    private String email;
    private String phone;
    private Boolean isLeader;
}
