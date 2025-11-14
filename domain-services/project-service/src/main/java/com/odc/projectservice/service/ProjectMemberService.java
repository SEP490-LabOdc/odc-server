package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.response.AddBatchProjectMembersResponse;
import com.odc.userservice.dto.response.MentorResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberService {
    ApiResponse<AddBatchProjectMembersResponse> addBatchProjectMembers(AddBatchProjectMembersRequest request);


    ApiResponse<List<MentorResponse>> getAvailableMentors(UUID projectId);
}
