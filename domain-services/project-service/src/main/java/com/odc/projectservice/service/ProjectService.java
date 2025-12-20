package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.*;
import com.odc.projectservice.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    ApiResponse<ProjectResponse> createProject(UUID userId, CreateProjectRequest request);

    ApiResponse<ProjectResponse> updateProject(UUID projectId, UpdateProjectRequest request);

    ApiResponse<Void> deleteProject(UUID projectId);

    ApiResponse<ProjectResponse> getProjectById(UUID projectId);

    ApiResponse<List<ProjectResponse>> getAllProjects();

    ApiResponse<List<ProjectResponse>> searchProjects(SearchRequest request);

    ApiResponse<PaginatedResult<ProjectResponse>> searchProjectsWithPagination(SearchRequest request);

    ApiResponse<List<UserParticipantResponse>> getProjectParticipants(UUID projectId);

    ApiResponse<PaginatedResult<GetHiringProjectDetailResponse>> getHiringProjects(Integer page, Integer pageSize);

    ApiResponse<List<GetProjectApplicationResponse>> getProjectApplications(UUID projectId);

    ApiResponse<GetCompanyProjectResponse> getProjectsByUserId(UUID userId);

    ApiResponse<Void> updateIsOpenForApplications(UUID projectId, UpdateProjectOpenStatusRequest request);

    ApiResponse<Void> updateProjectStatus(UUID projectId, UpdateProjectStatusRequest request);

    ApiResponse<List<ProjectResponse>> getMyProjects(UUID userId, String status);

    ApiResponse<PaginatedResult<GetTalentApplicationResponse>> getTalentApplications(UUID userId, String search, int page, int size);

    ApiResponse<PaginatedResult<ProjectResponse>> getRelatedProjects(UUID projectId, int page, int size);

    ApiResponse<Void> completeProject(UUID userId, UUID projectId);

    ApiResponse<Void> closeProject(UUID userId, UUID projectId, CloseProjectRequest request);

    ApiResponse<GetCompanyProjectResponse> getProjectsByCompanyId(UUID companyId);
}
