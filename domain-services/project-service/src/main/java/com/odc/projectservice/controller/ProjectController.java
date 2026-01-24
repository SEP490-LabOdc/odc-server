package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.*;
import com.odc.projectservice.dto.response.*;
import com.odc.projectservice.service.ProjectApplicationService;
import com.odc.projectservice.service.ProjectDocumentService;
import com.odc.projectservice.service.ProjectMilestoneService;
import com.odc.projectservice.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectMilestoneService projectMilestoneService;
    private final ProjectApplicationService projectApplicationService;
    private final ProjectDocumentService projectDocumentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody CreateProjectRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        ApiResponse<ProjectResponse> response = projectService.createProject(userId, request);
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

    @PreAuthorize("hasAuthority('MENTOR')")
    @GetMapping("/{projectId}/applicants")
    public ResponseEntity<ApiResponse<List<GetProjectApplicationResponse>>> getProjectApplications(
            @PathVariable UUID projectId
    ) {
        return ResponseEntity.ok(projectService.getProjectApplications(projectId));
    }

    @GetMapping("/my-company-projects")
    public ResponseEntity<ApiResponse<GetCompanyProjectResponse>> getCompanyProjects() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectService.getProjectsByUserId(userId));
    }

    @GetMapping("/companies/{companyId}")
    public ResponseEntity<ApiResponse<GetCompanyProjectResponse>> getCompanyProjects(@PathVariable UUID companyId) {
        return ResponseEntity.ok((projectService.getProjectsByCompanyId(companyId)));
    }

    @GetMapping("/my-projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects(
            @RequestParam(required = false) String status) {  // Thêm query parameter
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectService.getMyProjects(userId, status));
    }

    @GetMapping("/hiring")
    public ResponseEntity<ApiResponse<?>> getHiringProjects(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ResponseEntity.ok(projectService.getHiringProjects(page, pageSize));
    }

    @GetMapping("/{projectId}/milestones")
    public ResponseEntity<ApiResponse<List<ProjectMilestoneResponse>>> getProjectMilestones(
            @PathVariable UUID projectId) {
        ApiResponse<List<ProjectMilestoneResponse>> response =
                projectMilestoneService.getAllProjectMilestonesByProjectId(projectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{projectId}/open-for-applications")
    public ResponseEntity<ApiResponse<Void>> openForApplications(
            @PathVariable UUID projectId,
            @RequestBody UpdateProjectOpenStatusRequest request
    ) {
        return ResponseEntity.ok(projectService.updateIsOpenForApplications(projectId, request));
    }

    @PatchMapping("/{projectId}/status")
    public ResponseEntity<ApiResponse<Void>> updateProjectStatus(
            @PathVariable UUID projectId,
            @RequestBody UpdateProjectStatusRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProjectStatus(projectId, request));
    }

    @GetMapping("/my-applications")
    public ResponseEntity<ApiResponse<PaginatedResult<GetTalentApplicationResponse>>> getMyApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectService.getTalentApplications(userId, search, page, size));
    }

    @GetMapping("{projectId}/application-status")
    public ResponseEntity<ApiResponse<ProjectApplicationStatusResponse>> getProjectApplicationStatus(
            @PathVariable UUID projectId
    ) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectApplicationService.getProjectApplicationStatus(projectId, userId));
    }

    @GetMapping("/{projectId}/documents")
    public ResponseEntity<ApiResponse<List<ProjectDocumentResponse>>> getProjectDocuments(
            @PathVariable UUID projectId) {
        ApiResponse<List<ProjectDocumentResponse>> response =
                projectDocumentService.getProjectDocumentsByProjectId(projectId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{projectId}/related")
    public ResponseEntity<ApiResponse<PaginatedResult<ProjectResponse>>> getRelatedProjects(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ApiResponse<PaginatedResult<ProjectResponse>> response = projectService.getRelatedProjects(projectId, page, size);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('COMPANY')")
    @PatchMapping("/{projectId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeProject(
            @PathVariable UUID projectId
    ) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectService.completeProject(userId, projectId));
    }

    @PreAuthorize("hasAuthority('LAB_ADMIN')")
    @PatchMapping("/{projectId}/close")
    public ResponseEntity<ApiResponse<Void>> closeProject(
            @PathVariable UUID projectId,
            @RequestBody(required = false) CloseProjectRequest request
    ) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectService.closeProject(userId, projectId, request));
    }

    @GetMapping("/statistics/new-last-6-months")
    // @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'LAB_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProjectMonthlyStatisticResponse>>> getNewProjectsLast6Months() {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getNewProjectsLast6Months())
        );
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview() {
        return ResponseEntity.ok(
                projectService.getOverview()
        );
    }

    @PostMapping("/{projectId}/closure-requests")
    public ResponseEntity<ApiResponse<Void>> sendClosureRequest(@PathVariable UUID projectId,
                                                                @RequestBody CreateClosureRequest request) {
        return ResponseEntity.ok(projectService.sendClosureRequest(projectId, request));
    }
}
