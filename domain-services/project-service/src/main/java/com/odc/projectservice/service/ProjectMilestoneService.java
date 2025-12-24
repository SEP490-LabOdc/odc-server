package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.projectservice.dto.request.*;
import com.odc.projectservice.dto.response.FeedbackResponse;
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

    ApiResponse<ProjectMilestoneResponse> updateMilestoneStatusToOngoing(UUID milestoneId);

    ApiResponse<Void> deleteMilestoneAttachment(UUID milestoneId, UUID attachmentId);

    ApiResponse<Void> approveProjectPlan(UUID milestoneId);

    ApiResponse<Void> rejectProjectMilestone(UUID milestoneId, MilestoneRejectRequest request);

    ApiResponse<PaginatedResult<FeedbackResponse>> getMilestoneFeedbacks(
            UUID milestoneId,
            Integer page,
            Integer size
    );

    ApiResponse<ProjectMilestoneResponse> addMilestoneAttachments(
            UUID milestoneId,
            AddMilestoneAttachmentsRequest request
    );

    ApiResponse<List<MilestoneDocumentResponse>> getMilestoneDocuments(UUID milestoneId);

    ApiResponse<Void> createExtensionRequest(UUID userMentorId, UUID milestoneId, CreateExtensionRequest request);
}
