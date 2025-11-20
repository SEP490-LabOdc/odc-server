package com.odc.projectservice.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class GetMilestoneMemberResponse {
    private List<GetMilestoneMember> mentors, talents;
}
