package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.projectservice.dto.request.CreateReportLabAdminRequest;
import com.odc.projectservice.dto.request.CreateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportRequest;
import com.odc.projectservice.dto.request.UpdateReportStatusRequest;
import com.odc.projectservice.dto.response.GetReportToLabAdminResponse;
import com.odc.projectservice.dto.response.ReportResponse;
import com.odc.projectservice.dto.response.UserParticipantResponse;

import java.util.List;
import java.util.UUID;

public interface ReportService {
    ApiResponse<ReportResponse> createReport(UUID userId, CreateReportRequest request);

    ApiResponse<ReportResponse> updateReport(UUID userId, UUID reportId, UpdateReportRequest request);

    ApiResponse<Void> reviewReport(UUID userId, UUID reportId, UpdateReportStatusRequest request);

    ApiResponse<PaginatedResult<ReportResponse>> getReceivedReports(UUID userId, int page, int size);

    ApiResponse<PaginatedResult<ReportResponse>> getSentReports(UUID userId, int page, int size);

    ApiResponse<ReportResponse> getReportDetail(UUID reportId);

    ApiResponse<PaginatedResult<ReportResponse>> getProjectReports(UUID projectId, int page, int size);

    ApiResponse<PaginatedResult<ReportResponse>> getReportsByMilestoneId(UUID milestoneId, int page, int size);

    ApiResponse<List<UserParticipantResponse>> getReportRecipients(UUID projectId, UUID milestoneId);

    ApiResponse<Void> createReportToLabAdmin(UUID userId, CreateReportLabAdminRequest request);

    ApiResponse<Void> reviewReportByLabAdmin(UUID userId, UUID reportId, UpdateReportStatusRequest request);

    ApiResponse<PaginatedResult<GetReportToLabAdminResponse>> getReportToLabAdmin(Integer page, Integer pageSize);
}