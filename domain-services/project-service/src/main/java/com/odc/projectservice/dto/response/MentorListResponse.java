package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class MentorListResponse {
    private UUID id;
    private String name;
    private Long projectCount;

}
