package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.projectservice.dto.request.*;
import com.odc.projectservice.dto.response.FeedbackResponse;
import com.odc.projectservice.dto.response.GetMilestoneExtensionRequestResponse;
import com.odc.projectservice.dto.response.MilestoneDocumentResponse;
import com.odc.projectservice.dto.response.ProjectMilestoneResponse;
import com.odc.projectservice.service.ProjectMilestoneService;
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

    //    @PreAuthorize("hasAuthority('MENTOR')")
    @PatchMapping("/{milestoneId}/start")
    public ResponseEntity<ApiResponse<ProjectMilestoneResponse>> updateMilestoneStatusToOngoing(@PathVariable UUID milestoneId) {
        return ResponseEntity.ok(projectMilestoneService.updateMilestoneStatusToOngoing(milestoneId));
    }

    @DeleteMapping("/{milestoneId}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteMilestoneAttachment(
            @PathVariable UUID milestoneId,
            @PathVariable UUID attachmentId) {

        ApiResponse<Void> response = projectMilestoneService.deleteMilestoneAttachment(milestoneId, attachmentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{milestoneId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveProjectMilestone(
            @PathVariable UUID milestoneId) {

        ApiResponse<Void> response = projectMilestoneService.approveProjectPlan(milestoneId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{milestoneId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectProjectMilestone(
            @PathVariable UUID milestoneId,
            @RequestBody MilestoneRejectRequest request
    ) {
        ApiResponse<Void> response = projectMilestoneService.rejectProjectMilestone(milestoneId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{milestoneId}/feedbacks")
    public ResponseEntity<ApiResponse<PaginatedResult<FeedbackResponse>>> getMilestoneFeedbacks(
            @PathVariable UUID milestoneId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        ApiResponse<PaginatedResult<FeedbackResponse>> response =
                projectMilestoneService.getMilestoneFeedbacks(milestoneId, page, size);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('MENTOR')")
    @PostMapping("/{milestoneId}/milestone-attachments")
    public ResponseEntity<ApiResponse<ProjectMilestoneResponse>> addMilestoneAttachments(
            @PathVariable UUID milestoneId,
            @Valid @RequestBody AddMilestoneAttachmentsRequest request) {

        ApiResponse<ProjectMilestoneResponse> response =
                projectMilestoneService.addMilestoneAttachments(milestoneId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{milestoneId}/documents")
    public ResponseEntity<ApiResponse<List<MilestoneDocumentResponse>>> getMilestoneDocuments(
            @PathVariable UUID milestoneId) {
        ApiResponse<List<MilestoneDocumentResponse>> response =
                projectMilestoneService.getMilestoneDocuments(milestoneId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{milestoneId}/extension-requests")
    public ResponseEntity<ApiResponse<Void>> createMilestoneExtensionRequest(
            @PathVariable UUID milestoneId,
            @Valid @RequestBody CreateExtensionRequest request
    ) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectMilestoneService.createExtensionRequest(userId, milestoneId, request));
    }

    @PatchMapping("/{milestoneId}/extension-requests/{id}")
    public ResponseEntity<ApiResponse<Void>> updateMilestoneExtensionRequest(
            @PathVariable UUID milestoneId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMilestoneExtensionStatusRequest request
    ) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(projectMilestoneService.updateStatusExtensionRequest(userId, id, milestoneId, request));
    }

    @GetMapping("/{milestoneId}/extension-requests/my")
    public ApiResponse<PaginatedResult<GetMilestoneExtensionRequestResponse>> getMyRequests(
            @PathVariable UUID milestoneId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return projectMilestoneService.getMyRequestsByMilestone(
                milestoneId,
                userId,
                page,
                size,
                sortDir
        );
    }

    // Company
    @GetMapping("/{milestoneId}/extension-requests")
    public ApiResponse<PaginatedResult<GetMilestoneExtensionRequestResponse>> getRequestsForCompany(
            @PathVariable UUID milestoneId,
            @RequestParam UUID projectId,
            @RequestParam UUID companyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return projectMilestoneService.getRequestsByMilestoneForCompany(
                projectId,
                milestoneId,
                page,
                size,
                sortDir,
                companyId
        );
    }
}
