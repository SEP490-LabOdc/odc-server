package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.CreateSystemTemplateRequest;
import com.odc.projectservice.dto.request.UpdateSystemTemplateRequest;
import com.odc.projectservice.dto.response.SystemTemplateResponse;
import com.odc.projectservice.service.SystemTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/system-templates")
@RequiredArgsConstructor
public class SystemTemplateController {

    private final SystemTemplateService systemTemplateService;

    // --- ADMIN APIs ---

    @PostMapping
//    @PreAuthorize("hasAuthority('LAB_ADMIN') or hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SystemTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateSystemTemplateRequest request) {
        return ResponseEntity.ok(systemTemplateService.createTemplate(request));
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasAuthority('LAB_ADMIN') or hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<SystemTemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @RequestBody UpdateSystemTemplateRequest request) {
        return ResponseEntity.ok(systemTemplateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('LAB_ADMIN') or hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(systemTemplateService.deleteTemplate(id));
    }

    @PostMapping("/search")
//    @PreAuthorize("hasAuthority('LAB_ADMIN') or hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedResult<SystemTemplateResponse>>> searchTemplates(
            @RequestBody SearchRequest request) {
        return ResponseEntity.ok(systemTemplateService.searchTemplates(request));
    }

    // --- USER APIs (Talent, Leader, Mentor, Company) ---

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<SystemTemplateResponse>> getLatestTemplateByType(
            @RequestParam String type) {
        return ResponseEntity.ok(systemTemplateService.getLatestTemplateByType(type));
    }
}