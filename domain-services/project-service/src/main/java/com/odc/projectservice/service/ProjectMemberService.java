package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.response.GetProjectMemberByProjectIdResponse;
import com.odc.projectservice.dto.response.MentorResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberService {
    ApiResponse<Void> addBatchProjectMembers(AddBatchProjectMembersRequest request);

    ApiResponse<List<MentorResponse>> getAvailableMentors(UUID projectId);

    ApiResponse<List<GetProjectMemberByProjectIdResponse>> getProjectMembersByProjectId(UUID projectId, UUID milestoneId);

    ApiResponse<Void> removeMemberFromProject(UUID projectId, UUID projectMemberId);
}
