package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.AddBatchProjectMembersRequest;
import com.odc.projectservice.dto.response.AddBatchProjectMembersResponse;
import com.odc.projectservice.service.ProjectMemberService;
import com.odc.userservice.dto.response.MentorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @PostMapping
    public ResponseEntity<ApiResponse<AddBatchProjectMembersResponse>> addBatchProjectMembers(
            @Valid @RequestBody AddBatchProjectMembersRequest request) {
        ApiResponse<AddBatchProjectMembersResponse> response = projectMemberService.addBatchProjectMembers(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/available-mentors/{projectId}")
    public ResponseEntity<ApiResponse<List<MentorResponse>>> getAvailableMentors(
            @PathVariable UUID projectId) {
        ApiResponse<List<MentorResponse>> response = projectMemberService.getAvailableMentors(projectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}