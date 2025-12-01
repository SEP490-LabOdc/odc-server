package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.request.ToggleMentorLeaderRequest;
import com.odc.projectservice.dto.response.GetProjectMemberByProjectIdResponse;
import com.odc.projectservice.dto.response.MentorResponse;
import com.odc.projectservice.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping("/project-members")
    public ResponseEntity<ApiResponse<Void>> addBatchProjectMembers(
            @Valid @RequestBody AddBatchProjectMembersRequest request) {
        ApiResponse<Void> response = projectMemberService.addBatchProjectMembers(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/project-members/available-mentors/{projectId}")
    public ResponseEntity<ApiResponse<List<MentorResponse>>> getAvailableMentors(
            @PathVariable UUID projectId) {
        ApiResponse<List<MentorResponse>> response = projectMemberService.getAvailableMentors(projectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/projects/{projectId}/project-members")
    public ResponseEntity<ApiResponse<List<GetProjectMemberByProjectIdResponse>>> getProjectMembers(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID milestoneId
    ) {
        return ResponseEntity.ok(projectMemberService.getProjectMembersByProjectId(projectId, milestoneId));
    }

    @PatchMapping("/projects/{projectId}/mentors/{mentorId}/leader")
    public ResponseEntity<ApiResponse<UUID>> toggleMentorLeader(
            @PathVariable UUID projectId,
            @PathVariable UUID mentorId,
            @Valid @RequestBody ToggleMentorLeaderRequest request) {
        ApiResponse<UUID> response = projectMemberService.toggleMentorLeader(projectId, mentorId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/projects/{projectId}/talents/{talentId}/leader")
    public ResponseEntity<ApiResponse<UUID>> toggleTalentLeader(
            @PathVariable UUID projectId,
            @PathVariable UUID talentId,
            @Valid @RequestBody ToggleMentorLeaderRequest request) {
        ApiResponse<UUID> response = projectMemberService.toggleTalentLeader(projectId, talentId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}