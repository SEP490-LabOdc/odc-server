package com.odc.projectservice.controller;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;
import com.odc.projectservice.service.MilestoneMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MilestoneMemberController {
    private final MilestoneMemberService milestoneMemberService;

    @PostMapping("/milestone-members/talent")
    public ResponseEntity<ApiResponse<Void>> addProjectMembersToMilestone(
            @RequestBody AddProjectMemberRequest request
    ) {
        return ResponseEntity.ok(
                milestoneMemberService.addProjectMembers(request, Role.TALENT)
        );
    }
}
