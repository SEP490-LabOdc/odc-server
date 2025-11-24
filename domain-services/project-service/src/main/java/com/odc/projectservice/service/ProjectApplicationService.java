package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.ApplyProjectRequest;
import com.odc.projectservice.dto.response.ApplyProjectResponse;
import com.odc.projectservice.dto.response.ProjectApplicationStatusResponse;
import com.odc.projectservice.dto.response.UserSubmittedCvResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectApplicationService {
    ApiResponse<ApplyProjectResponse> applyProject(ApplyProjectRequest request);

    ApiResponse<List<UserSubmittedCvResponse>> getUserSubmittedCvs(UUID userId);

    ApiResponse<ProjectApplicationStatusResponse> getProjectApplicationStatus(UUID projectId, UUID userId);
}
