package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.CreateProjectRequest;
import com.odc.projectservice.dto.request.UpdateProjectRequest;
import com.odc.projectservice.dto.response.GetProjectApplicationResponse;
import com.odc.projectservice.dto.response.GetStudentProjectResponse;
import com.odc.projectservice.dto.response.ProjectResponse;
import com.odc.projectservice.dto.response.UserParticipantResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    ApiResponse<ProjectResponse> createProject(CreateProjectRequest request);

    ApiResponse<ProjectResponse> updateProject(UUID projectId, UpdateProjectRequest request);

    ApiResponse<Void> deleteProject(UUID projectId);

    ApiResponse<ProjectResponse> getProjectById(UUID projectId);

    ApiResponse<List<ProjectResponse>> getAllProjects();

    ApiResponse<List<ProjectResponse>> searchProjects(SearchRequest request);

    ApiResponse<PaginatedResult<ProjectResponse>> searchProjectsWithPagination(SearchRequest request);

    ApiResponse<List<UserParticipantResponse>> getProjectParticipants(UUID projectId);

    ApiResponse<PaginatedResult<GetStudentProjectResponse>> getHiringProjects(Integer page, Integer pageSize);

    ApiResponse<List<GetProjectApplicationResponse>> getProjectApplications(UUID projectId);
}
