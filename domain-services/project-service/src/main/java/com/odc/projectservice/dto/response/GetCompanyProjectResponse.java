package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
public class GetCompanyProjectResponse {
    private UUID companyId;
    private String companyName;
    private List<GetProjectResponse> projectResponses;
}
