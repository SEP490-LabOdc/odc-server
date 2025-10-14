package com.odc.checklistservice.controller;

import com.odc.checklistservice.dto.request.CreateChecklistTemplateRequest;
import com.odc.checklistservice.dto.request.UpdateChecklistTemplateRequest;
import com.odc.checklistservice.dto.response.GetChecklistTemplateResponse;
import com.odc.checklistservice.service.ChecklistTemplateService;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.SearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checklist-templates")
@RequiredArgsConstructor
public class ChecklistTemplateController {

    private final ChecklistTemplateService checklistTemplateService;

    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createChecklistTemplate(
            @Valid @RequestBody CreateChecklistTemplateRequest request) {
        ApiResponse<UUID> apiResponse = checklistTemplateService.createChecklistTemplate(request);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<?>> universalSearchChecklistTemplate(@RequestBody SearchRequest request) {
        boolean isPaginated = request.getPage() != null && request.getSize() != null;
        if (isPaginated) {
            return ResponseEntity.ok(checklistTemplateService.searchChecklistTemplatesWithPagination(request));
        }

        return ResponseEntity.ok(checklistTemplateService.searchAllChecklistTemplates(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetChecklistTemplateResponse>> getChecklistTemplateById(
            @PathVariable UUID id) {
        ApiResponse<GetChecklistTemplateResponse> apiResponse = checklistTemplateService.getChecklistTemplateById(id);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UUID>> updateChecklistTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateChecklistTemplateRequest request) {
        ApiResponse<UUID> apiResponse = checklistTemplateService.updateChecklistTemplate(id, request);
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UUID>> deleteChecklistTemplate(@PathVariable UUID id) {
        ApiResponse<UUID> apiResponse = checklistTemplateService.deleteChecklistTemplate(id);
        return ResponseEntity.ok(apiResponse);
    }
}
