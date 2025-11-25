package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.CreateProjectDocumentRequest;
import com.odc.projectservice.dto.request.UpdateProjectDocumentRequest;
import com.odc.projectservice.dto.response.ProjectDocumentResponse;

import java.util.List;
import java.util.UUID;

public interface ProjectDocumentService {
    ApiResponse<ProjectDocumentResponse> createProjectDocument(CreateProjectDocumentRequest request);

    ApiResponse<ProjectDocumentResponse> updateProjectDocument(UUID documentId, UpdateProjectDocumentRequest request);

    ApiResponse<Void> deleteProjectDocument(UUID documentId);

    ApiResponse<ProjectDocumentResponse> getProjectDocumentById(UUID documentId);

    ApiResponse<List<ProjectDocumentResponse>> getAllProjectDocuments();

    ApiResponse<List<ProjectDocumentResponse>> searchProjectDocuments(SearchRequest request);

    ApiResponse<PaginatedResult<ProjectDocumentResponse>> searchProjectDocumentsWithPagination(SearchRequest request);

    ApiResponse<List<ProjectDocumentResponse>> getProjectDocumentsByProjectId(UUID projectId);
}
