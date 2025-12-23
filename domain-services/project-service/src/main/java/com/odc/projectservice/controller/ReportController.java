package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.projectservice.dto.request.CreateReportLabAdminRequest;
import com.odc.projectservice.dto.request.CreateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportStatusRequest;
import com.odc.projectservice.dto.response.GetReportToLabAdminResponse;
import com.odc.projectservice.dto.response.ReportResponse;
import com.odc.projectservice.dto.response.UserParticipantResponse;
import com.odc.projectservice.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/for-lab-admin")
    public ResponseEntity<ApiResponse<PaginatedResult<GetReportToLabAdminResponse>>>
    getReportsToLabAdmin(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        ApiResponse<PaginatedResult<GetReportToLabAdminResponse>> response =
                reportService.getReportToLabAdmin(page, pageSize);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(@Valid @RequestBody CreateReportRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(reportService.createReport(userId, request));
    }

    @PostMapping("/for-lab-admin")
    public ResponseEntity<ApiResponse<Void>> createReportToLabAdmin(@Valid @RequestBody CreateReportLabAdminRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(reportService.createReportToLabAdmin(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReportRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(reportService.updateReport(userId, id, request));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<ApiResponse<Void>> reviewReport(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(reportService.reviewReport(userId, id, request));
    }

    @PutMapping("/{id}/lab-admin-review")
    public ResponseEntity<ApiResponse<Void>> reviewReportByLabAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(reportService.reviewReportByLabAdmin(userId, id, request));
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<PaginatedResult<ReportResponse>>> getReceived(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(reportService.getReceivedReports(userId, page, size));
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<PaginatedResult<ReportResponse>>> getSent(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(reportService.getSentReports(userId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getReportDetail(id));
    }

    @GetMapping("/project/{projectId}")
//    @PreAuthorize("hasAuthority('LAB_ADMIN') or hasAuthority('MENTOR')")
    public ResponseEntity<ApiResponse<PaginatedResult<ReportResponse>>> getProjectReports(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reportService.getProjectReports(projectId, page, size));
    }

    @GetMapping("/milestone/{milestoneId}")
    public ResponseEntity<ApiResponse<PaginatedResult<ReportResponse>>> getReportsByMilestoneId(
            @PathVariable UUID milestoneId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reportService.getReportsByMilestoneId(milestoneId, page, size));
    }

    @GetMapping("/recipients")
    public ResponseEntity<ApiResponse<List<UserParticipantResponse>>> getRecipients(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID milestoneId) {

        return ResponseEntity.ok(reportService.getReportRecipients(projectId, milestoneId));
    }

    @PutMapping("/{reportId}/publish-to-company/{userCompanyId}")
    public ResponseEntity<ApiResponse<Void>> publishReportToCompany(
            @PathVariable UUID reportId,
            @PathVariable UUID userCompanyId) {
        return ResponseEntity.ok(reportService.publishToCompany(reportId, userCompanyId));
    }
}