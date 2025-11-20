package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;
import com.odc.projectservice.dto.request.RemoveMilestoneMembersRequest;
import com.odc.projectservice.dto.response.GetMilestoneMemberResponse;

import java.util.UUID;

public interface MilestoneMemberService {
    ApiResponse<Void> addProjectMembers(AddProjectMemberRequest request, Role allowedRole);

    ApiResponse<Void> removeProjectMembersFromMilestone(RemoveMilestoneMembersRequest request);

    ApiResponse<GetMilestoneMemberResponse> getMilestoneMembers(UUID milestoneId, Boolean isActive);
}
