package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.CreateProjectMilestoneRequest;
import com.odc.projectservice.dto.request.UpdateProjectMilestoneRequest;
import com.odc.projectservice.dto.response.MilestoneDocumentResponse;
import com.odc.projectservice.dto.response.ProjectMilestoneResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectMilestoneService {
    ApiResponse<ProjectMilestoneResponse> createProjectMilestone(CreateProjectMilestoneRequest request);

    ApiResponse<ProjectMilestoneResponse> updateProjectMilestone(UUID milestoneId, UpdateProjectMilestoneRequest request);

    ApiResponse<List<ProjectMilestoneResponse>> getAllProjectMilestones();

    ApiResponse<List<ProjectMilestoneResponse>> getAllProjectMilestonesByProjectId(UUID projectId);

    ApiResponse<ProjectMilestoneResponse> getProjectMilestoneById(UUID milestoneId);

    ApiResponse<Void> deleteProjectMilestone(UUID milestoneId);

    ApiResponse<List<MilestoneDocumentResponse>> getDocumentsByMilestoneId(UUID milestoneId);

}
