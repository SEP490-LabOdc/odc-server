package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;

public interface MilestoneMemberService {
    ApiResponse<Void> addProjectMembers(AddProjectMemberRequest request, Role allowedRole);
}
