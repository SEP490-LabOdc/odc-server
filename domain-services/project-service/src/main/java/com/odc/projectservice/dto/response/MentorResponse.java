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
public class MentorResponse {
    private UUID id;
    private String name;
    private String email;
    private String avatarUrl;
    private Integer projectCount;
}