package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.CreateProjectRequest;
import com.odc.projectservice.dto.request.UpdateProjectRequest;
import com.odc.projectservice.dto.response.ProjectResponse;
import com.odc.projectservice.dto.response.UserParticipantResponse;
import com.odc.projectservice.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ApiResponse<ProjectResponse> response = projectService.createProject(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        ApiResponse<ProjectResponse> response = projectService.updateProject(projectId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable UUID projectId) {
        ApiResponse<Void> response = projectService.deleteProject(projectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable UUID projectId) {
        ApiResponse<ProjectResponse> response = projectService.getProjectById(projectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects() {
        ApiResponse<List<ProjectResponse>> response = projectService.getAllProjects();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchProjects(@RequestBody SearchRequest request) {
        boolean isPaginated = request.getPage() != null && request.getSize() != null;

        if (isPaginated) {
            return ResponseEntity.ok(projectService.searchProjectsWithPagination(request));
        }

        return ResponseEntity.ok(projectService.searchProjects(request));
    }

    @GetMapping("/{projectId}/participants")
    public ResponseEntity<ApiResponse<List<UserParticipantResponse>>> getProjectParticipants(
            @PathVariable UUID projectId) {
        ApiResponse<List<UserParticipantResponse>> response = projectService.getProjectParticipants(projectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
