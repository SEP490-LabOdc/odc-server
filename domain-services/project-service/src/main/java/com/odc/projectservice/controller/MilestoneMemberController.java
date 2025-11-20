package com.odc.projectservice.controller;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;
import com.odc.projectservice.dto.request.RemoveMilestoneMembersRequest;
import com.odc.projectservice.dto.response.GetMilestoneMember;
import com.odc.projectservice.dto.response.GetMilestoneMemberResponse;
import com.odc.projectservice.service.MilestoneMemberService;
import jakarta.ws.rs.QueryParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @PostMapping("/milestone-members/remove")
    public ResponseEntity<ApiResponse<Void>> removeMembersFromMilestone(
            @RequestBody RemoveMilestoneMembersRequest request
    ) {
        return ResponseEntity.ok(
                milestoneMemberService.removeProjectMembersFromMilestone(request)
        );
    }

    @GetMapping("/project-milestones/{milestoneId}/milestone-members")
    public ResponseEntity<ApiResponse<GetMilestoneMemberResponse>> getMilestoneMembers(
            @PathVariable UUID milestoneId,
            @QueryParam("isActive") Boolean isActive
    ) {
        return ResponseEntity.ok(
                milestoneMemberService.getMilestoneMembers(milestoneId, isActive)
        );
    }
}
