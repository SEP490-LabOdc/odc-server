package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.CreateSystemTemplateRequest;
import com.odc.projectservice.dto.request.UpdateSystemTemplateRequest;
import com.odc.projectservice.dto.response.SystemTemplateResponse;

import java.util.UUID;

public interface SystemTemplateService {
    ApiResponse<SystemTemplateResponse> createTemplate(CreateSystemTemplateRequest request);

    ApiResponse<SystemTemplateResponse> updateTemplate(UUID id, UpdateSystemTemplateRequest request);

    ApiResponse<Void> deleteTemplate(UUID id);

    ApiResponse<PaginatedResult<SystemTemplateResponse>> searchTemplates(SearchRequest request);

    ApiResponse<SystemTemplateResponse> getLatestTemplateByType(String type);
}