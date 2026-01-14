package com.odc.operationservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.operationservice.dto.request.CreateUpdateRequestRequest;
import com.odc.operationservice.dto.response.GetUpdateRequestResponse;
import com.odc.operationservice.service.UpdateRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/update-requests")
@RequiredArgsConstructor
public class UpdateRequestController {

    private final UpdateRequestService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUpdateRequest(
            @Valid @RequestBody CreateUpdateRequestRequest request
    ) {
        return ResponseEntity.ok(service.sendUpdateRequest(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetUpdateRequestResponse>> getDetail(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(service.getDetail(id));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PaginatedResult<GetUpdateRequestResponse>>> search(
            @Valid @RequestBody SearchRequest request
    ) {
        return ResponseEntity.ok(service.searchUpdateRequests(request));
    }
}
