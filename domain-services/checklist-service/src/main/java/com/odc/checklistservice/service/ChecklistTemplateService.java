package com.odc.checklistservice.service;

import com.odc.checklistservice.dto.request.CreateChecklistTemplateRequest;
import com.odc.checklistservice.dto.request.UpdateChecklistTemplateRequest;
import com.odc.checklistservice.dto.response.GetChecklistTemplateResponse;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;

import java.util.List;
import java.util.UUID;

public interface ChecklistTemplateService {
    ApiResponse<UUID> createChecklistTemplate(CreateChecklistTemplateRequest request);

    ApiResponse<UUID> updateChecklistTemplate(UUID id, UpdateChecklistTemplateRequest request);

    ApiResponse<UUID> deleteChecklistTemplate(UUID id);

    ApiResponse<GetChecklistTemplateResponse> getChecklistTemplateById(UUID id);

    ApiResponse<List<GetChecklistTemplateResponse>> searchAllChecklistTemplates(SearchRequest request);

    ApiResponse<PaginatedResult<GetChecklistTemplateResponse>> searchChecklistTemplatesWithPagination(SearchRequest request);
}
