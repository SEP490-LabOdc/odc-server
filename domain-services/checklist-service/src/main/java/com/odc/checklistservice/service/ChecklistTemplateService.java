package com.odc.checklistservice.service;

import com.odc.checklistservice.dto.request.CreateChecklistTemplateRequest;
import com.odc.checklistservice.dto.request.UpdateChecklistTemplateRequest;
import com.odc.checklistservice.dto.response.GetChecklistTemplateResponse;
import com.odc.common.dto.ApiResponse;

import java.util.UUID;

public interface ChecklistTemplateService {
    ApiResponse<UUID> createChecklistTemplate(CreateChecklistTemplateRequest request);

    ApiResponse<UUID> updateChecklistTemplate(UUID id, UpdateChecklistTemplateRequest request);

    ApiResponse<UUID> deleteChecklistTemplate(UUID id);

    ApiResponse<GetChecklistTemplateResponse> getChecklistTemplateById(UUID id);
}
