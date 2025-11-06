package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.ApplyProjectRequest;
import com.odc.projectservice.dto.response.ApplyProjectResponse;

public interface ProjectApplicationService {
    ApiResponse<ApplyProjectResponse> applyProject(ApplyProjectRequest request);
}
