package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;
import com.odc.projectservice.dto.request.RemoveMilestoneMembersRequest;
import com.odc.projectservice.dto.response.TalentInMilestoneResponse;

import java.util.List;
import java.util.UUID;

public interface MilestoneMemberService {
    ApiResponse<Void> addProjectMembers(AddProjectMemberRequest request, Role allowedRole);

    ApiResponse<Void> removeProjectMembersFromMilestone(RemoveMilestoneMembersRequest request);

    ApiResponse<List<TalentInMilestoneResponse>> getActiveTalentsInMilestone(UUID milestoneId);
}
