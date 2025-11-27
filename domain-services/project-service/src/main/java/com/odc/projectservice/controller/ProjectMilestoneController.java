package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.CreateProjectMilestoneRequest;
import com.odc.projectservice.dto.request.UpdateProjectMilestoneRequest;
import com.odc.projectservice.dto.response.MilestoneDocumentResponse;
import com.odc.projectservice.dto.response.ProjectMilestoneResponse;
import com.odc.projectservice.service.ProjectMilestoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-milestones")
@RequiredArgsConstructor
public class ProjectMilestoneController {
    private final ProjectMilestoneService projectMilestoneService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectMilestoneResponse>> createProjectMilestone(
            @Valid @RequestBody CreateProjectMilestoneRequest request) {
        ApiResponse<ProjectMilestoneResponse> response = projectMilestoneService.createProjectMilestone(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{milestoneId}")
    public ResponseEntity<ApiResponse<ProjectMilestoneResponse>> updateProjectMilestone(
            @PathVariable UUID milestoneId,
            @Valid @RequestBody UpdateProjectMilestoneRequest request) {
        ApiResponse<ProjectMilestoneResponse> response = projectMilestoneService.updateProjectMilestone(milestoneId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectMilestoneResponse>>> getAllProjectMilestones() {
        ApiResponse<List<ProjectMilestoneResponse>> response = projectMilestoneService.getAllProjectMilestones();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{milestoneId}")
    public ResponseEntity<ApiResponse<ProjectMilestoneResponse>> getProjectMilestoneById(
            @PathVariable UUID milestoneId) {
        ApiResponse<ProjectMilestoneResponse> response = projectMilestoneService.getProjectMilestoneById(milestoneId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{milestoneId}")
    public ResponseEntity<ApiResponse<Void>> deleteProjectMilestone(@PathVariable UUID milestoneId) {
        ApiResponse<Void> response = projectMilestoneService.deleteProjectMilestone(milestoneId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{milestoneId}/documents")
    public ResponseEntity<ApiResponse<List<MilestoneDocumentResponse>>> getMilestoneDocuments(
            @PathVariable UUID milestoneId) {
        ApiResponse<List<MilestoneDocumentResponse>> response =
                projectMilestoneService.getDocumentsByMilestoneId(milestoneId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
